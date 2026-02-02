package org.argentumforge.engine.gui.forms;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.flag.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.utils.editor.*;
import org.argentumforge.engine.gui.forms.main.MainMenuBar;
import org.argentumforge.engine.gui.forms.main.MainStatusBar;
import org.argentumforge.engine.gui.forms.main.MainToolbar;
import org.argentumforge.engine.i18n.I18n;

import static org.argentumforge.engine.utils.GameData.options;
import org.argentumforge.engine.utils.GithubReleaseChecker;
// Removed java.awt.Desktop and java.net.URI
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.scenes.GameScene;
import org.argentumforge.engine.gui.components.ContextMenu;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Interfaz principal del editor.
 * Gestiona el menú superior, la barra de herramientas y los sub-ventanas de
 * edición (Superficies, Bloqueos, etc.).
 */

public final class FMain extends Form {

    private static final int TRANSPARENT_COLOR = Theme.TRANSPARENT;

    private FSurfaceEditor surfaceEditor;
    private FBlockEditor blockEditor;
    private FNpcEditor npcEditor;
    private FObjEditor objEditor;
    private FMinimap minimap;
    private FGrhLibrary grhLibrary;
    private FPhotoMode photoModeUI = new FPhotoMode();
    private boolean updatePopupShown = false;
    private FTriggerEditor triggerEditor;
    private FTransferEditor transferEditor;
    private FParticleEditor particleEditor;
    private FSpeedControl speedControl;

    private float[] ambientColorArr;

    // Components
    private final MainMenuBar menuBar;
    private final MainToolbar toolbar;
    private final MainStatusBar statusBar;

    private final java.util.List<IMapEditor> mapEditors = new java.util.ArrayList<>();
    private org.argentumforge.engine.utils.MapContext lastContextSeen = null;
    private boolean syncRequested = false;

    public FMain() {
        ambientColorArr = new float[] {
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getRed(),
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getGreen(),
                org.argentumforge.engine.game.Weather.INSTANCE.getWeatherColor().getBlue()
        };
        surfaceEditor = new FSurfaceEditor();
        blockEditor = new FBlockEditor();
        npcEditor = new FNpcEditor();
        objEditor = new FObjEditor();
        minimap = new FMinimap();
        grhLibrary = new FGrhLibrary();
        triggerEditor = new FTriggerEditor();
        transferEditor = new FTransferEditor();
        particleEditor = new FParticleEditor();
        speedControl = new FSpeedControl();

        mapEditors.add(surfaceEditor);
        mapEditors.add(blockEditor);
        mapEditors.add(npcEditor);
        mapEditors.add(objEditor);
        mapEditors.add(triggerEditor);
        mapEditors.add(transferEditor);
        mapEditors.add(particleEditor);

        // Initialize reusable components
        this.menuBar = new MainMenuBar(this);
        this.toolbar = new MainToolbar(this);
        this.statusBar = new MainStatusBar();

        // Initialize Theme
        org.argentumforge.engine.gui.ThemeManager.getInstance().init();

    }

    private void updateEditorsContext(org.argentumforge.engine.utils.MapContext context) {
        for (IMapEditor editor : mapEditors) {
            editor.setContext(context);
        }
    }

    @Override
    public void close() {
        toolbar.cleanup();
        super.close();
    }

    @Override
    public void render() {
        boolean isPhoto = options.getRenderSettings().isPhotoModeActive();

        // 1. BARRA DE MENÚ (Sistema principal)
        if (!isPhoto) {
            menuBar.render();
        }

        // 2. VENTANA RAÍZ PARA DOCKING
        imgui.ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY());
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY());
        ImGui.setNextWindowViewport(viewport.getID());

        // Estilo para que la ventana sea invisible y cubra el área de trabajo
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowRounding, 0.0f);
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowBorderSize, 0.0f);
        ImGui.pushStyleVar(imgui.flag.ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);

        int windowFlags = ImGuiWindowFlags.NoDocking;
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove;
        windowFlags |= ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoScrollWithMouse;

        ImGui.begin("MainDockSpaceHolder", windowFlags);
        ImGui.popStyleVar(3);

        // Crear el DockSpace real
        int dockspaceId = ImGui.getID("MainEditorDockSpace");
        ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.PassthruCentralNode);

        ImGui.end();

        // 3. RENDERIZADO DE COMPONENTES FIJOS (Se apoyan en el DockSpace o WorkArea)
        if (!isPhoto) {
            statusBar.render();
            toolbar.render();
        }

        if (isPhoto) {
            photoModeUI.render();
            return;
        }

        // 3. RENDERIZADO DE TABS DE MAPAS
        drawTabs();

        Console.INSTANCE.drawConsole();
        handleShortcuts();

        if (org.argentumforge.engine.Engine.getCurrentScene() instanceof GameScene) {
            ((GameScene) org.argentumforge.engine.Engine.getCurrentScene()).renderImGuiOverlays();
        }

        // Global Progress Modal for Minimap Generation
        if (org.argentumforge.engine.utils.editor.MinimapColorGenerator.generating) {
            ImGui.openPopup(I18n.INSTANCE.get("msg.processingColors"));
        }

        // Render Context Menu (always available to pop up)
        ContextMenu.render();

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("msg.processingColors"),
                ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar)) {
            if (!org.argentumforge.engine.utils.editor.MinimapColorGenerator.generating) {
                ImGui.textColored(ImGui.getColorU32(0.0f, 1.0f, 0.0f, 1.0f), I18n.INSTANCE.get("msg.complete"));
                if (ImGui.button(I18n.INSTANCE.get("common.close"), 300, 0)) {
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.text(I18n.INSTANCE.get("msg.processingColors"));
                ImGui.separator();
                ImGui.progressBar(org.argentumforge.engine.utils.editor.MinimapColorGenerator.progress / 100.0f, 300,
                        20,
                        String.format("%.0f%%", org.argentumforge.engine.utils.editor.MinimapColorGenerator.progress));
            }
            ImGui.endPopup();
        }

        // Update Notification Logic (Moved from FLauncher)
        if (GithubReleaseChecker.isUpdateAvailable() && !updatePopupShown) {
            GithubReleaseChecker.ReleaseInfo r = GithubReleaseChecker.getLatestRelease();
            String ignored = org.argentumforge.engine.game.Options.INSTANCE.getIgnoredUpdateTag();

            if (r != null && !r.tagName.equals(ignored)) {
                ImGui.openPopup(I18n.INSTANCE.get("update.available"));
                updatePopupShown = true;
            }
        }

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("update.available"), ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(I18n.INSTANCE.get("update.available"));
            ImGui.spacing();

            GithubReleaseChecker.ReleaseInfo release = GithubReleaseChecker.getLatestRelease();
            if (release != null) {
                ImGui.textColored(0.2f, 0.8f, 0.2f, 1.0f, release.tagName);
                if (release.isPrerelease) {
                    ImGui.sameLine();
                    ImGui.textColored(1.0f, 0.5f, 0.0f, 1.0f, "(Pre-release)");
                }
                ImGui.textWrapped(release.name);
            }

            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("update.download"), 200, 30)) {
                if (release != null) {
                    try {
                        Form.openURL(release.htmlUrl);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();

            if (ImGui.button(I18n.INSTANCE.get("update.close"), 100, 30)) {
                ImGui.closeCurrentPopup();
            }

            // "Don't show again" button
            ImGui.spacing();
            if (ImGui.button(I18n.INSTANCE.get("update.skip"), 308, 20)) {
                if (release != null) {
                    org.argentumforge.engine.game.Options.INSTANCE.setIgnoredUpdateTag(release.tagName);
                    org.argentumforge.engine.game.Options.INSTANCE.save();
                }
                ImGui.closeCurrentPopup();
            }
            if (ImGui.isItemHovered()) {
                ImGui.setTooltip(I18n.INSTANCE.get("update.skip_tooltip"));
            }

            ImGui.endPopup();
        }

    }

    private void drawTabs() {
        java.util.List<org.argentumforge.engine.utils.MapContext> openMaps = org.argentumforge.engine.utils.GameData
                .getOpenMaps();
        if (openMaps.isEmpty())
            return;

        imgui.ImGuiViewport viewport = ImGui.getMainViewport();
        float toolbarHeight = 52.0f;
        float tabsHeight = 22.0f;

        // Anclar debajo de la toolbar
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY() + toolbarHeight);
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), tabsHeight);
        ImGui.setNextWindowViewport(viewport.getID());

        // Detectar si el contexto cambió fuera de este loop (ej: teletransporte)
        org.argentumforge.engine.utils.MapContext currentActive = org.argentumforge.engine.utils.GameData
                .getActiveContext();

        // Si el contexto actual no es el último que vimos en el rendering, forzamos un
        // sync único
        if (currentActive != lastContextSeen) {
            syncRequested = true;
        }

        if (ImGui.begin("WorkspaceTabsWindow", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10.0f, 2.0f);
            if (ImGui.beginTabBar("##WorkspaceTabs", ImGuiTabBarFlags.None)) {
                org.argentumforge.engine.utils.MapContext contextToClose = null;

                for (org.argentumforge.engine.utils.MapContext context : openMaps) {
                    ImBoolean open = new ImBoolean(true);
                    int flags = ImGuiTabItemFlags.None;

                    // Sync único: una vez que ImGui procese este SetSelected, syncRequested bajará
                    if (syncRequested && context == currentActive) {
                        flags |= imgui.flag.ImGuiTabItemFlags.SetSelected;
                    }

                    if (ImGui.beginTabItem(context.getMapName() + "###Tab" + context.getFilePath(), open, flags)) {
                        // Si estamos en medio de un sync, esperamos a que ImGui active el tab
                        if (syncRequested && context == currentActive) {
                            syncRequested = false;
                        }

                        // Si ImGui dice que este tab está activo pero nuestro GameData tiene otro,
                        // el usuario hizo clic en el tab.
                        if (context != currentActive && !syncRequested) {
                            // Salvar posición anterior
                            if (currentActive != null) {
                                currentActive
                                        .setSavedUserX(org.argentumforge.engine.game.User.INSTANCE.getUserPos().getX());
                                currentActive
                                        .setSavedUserY(org.argentumforge.engine.game.User.INSTANCE.getUserPos().getY());
                            }

                            // Defer switch to end of frame to prevent ImGui corruption
                            org.argentumforge.engine.Engine.INSTANCE.runOnMainThread(() -> {
                                org.argentumforge.engine.utils.GameData.setActiveContext(context);
                                updateEditorsContext(context);

                                // Restaurar posición
                                org.argentumforge.engine.game.User.INSTANCE.getUserPos().setX(context.getSavedUserX());
                                org.argentumforge.engine.game.User.INSTANCE.getUserPos().setY(context.getSavedUserY());
                                org.argentumforge.engine.game.User.INSTANCE.getAddToUserPos().setX(0);
                                org.argentumforge.engine.game.User.INSTANCE.getAddToUserPos().setY(0);
                                org.argentumforge.engine.game.User.INSTANCE.setUserMoving(false);
                            });
                        }
                        ImGui.endTabItem();
                    }

                    if (!open.get()) {
                        if (context.isModified()) {
                            // Cambiar temporalmente al contexto para guardar si es necesario
                            // Defer switch and modal check
                            org.argentumforge.engine.Engine.INSTANCE.runOnMainThread(() -> {
                                org.argentumforge.engine.utils.GameData.setActiveContext(context);
                                org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(
                                        () -> org.argentumforge.engine.utils.GameData.closeMap(context),
                                        null);
                            });
                            open.set(true); // Cancelar cierre inmediato mientras preguntamos
                        } else {
                            contextToClose = context;
                        }
                    }
                }

                if (contextToClose != null) {
                    final org.argentumforge.engine.utils.MapContext finalToClose = contextToClose;
                    org.argentumforge.engine.Engine.INSTANCE.runOnMainThread(() -> {
                        org.argentumforge.engine.utils.GameData.closeMap(finalToClose);
                    });
                }

                ImGui.endTabBar();
            }
            ImGui.popStyleVar();
        }
        ImGui.end();

        // Actualizar último contexto visto para el siguiente frame
        lastContextSeen = org.argentumforge.engine.utils.GameData.getActiveContext();
    }

    // Getters for MainToolbar and MainMenuBar
    public FSurfaceEditor getSurfaceEditor() {
        return surfaceEditor;
    }

    public FObjEditor getObjEditor() {
        return objEditor;
    }

    public FNpcEditor getNpcEditor() {
        return npcEditor;
    }

    public FBlockEditor getBlockEditor() {
        return blockEditor;
    }

    public FTriggerEditor getTriggerEditor() {
        return triggerEditor;
    }

    public FTransferEditor getTransferEditor() {
        return transferEditor;
    }

    public FMinimap getMinimap() {
        return minimap;
    }

    public FParticleEditor getParticleEditor() {
        return particleEditor;
    }

    public FSpeedControl getSpeedControl() {
        return speedControl;
    }

    public float[] getAmbientColorArr() {
        return ambientColorArr;
    }

    private void handleShortcuts() {
        if (ImGui.getIO().getWantCaptureKeyboard())
            return;

        boolean modifierPressed = KeyHandler.isActionKeyPressed(Key.MULTI_SELECT);

        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_C))
            org.argentumforge.engine.game.EditorController.INSTANCE.copySelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_X))
            org.argentumforge.engine.game.EditorController.INSTANCE.cutSelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_V))
            org.argentumforge.engine.game.EditorController.INSTANCE.pasteSelection();
        if (ImGui.isKeyPressed(GLFW_KEY_DELETE))
            org.argentumforge.engine.game.EditorController.INSTANCE.deleteSelection();
    }

}
