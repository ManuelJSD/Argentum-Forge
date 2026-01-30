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
import java.awt.Desktop;
import java.net.URI;
import org.argentumforge.engine.renderer.RGBColor;
import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import org.argentumforge.engine.utils.editor.commands.*;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.scenes.GameScene;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.scenes.Camera;
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

    }

    private void updateEditorsContext(org.argentumforge.engine.utils.MapContext context) {
        for (IMapEditor editor : mapEditors) {
            editor.setContext(context);
        }
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
                        Desktop.getDesktop().browse(new URI(release.htmlUrl));
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

        // Las pestañas ahora se dibujan de forma más limpia
        if (ImGui.begin("WorkspaceTabsWindow", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            // Reducir padding interno para las pestañas
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 10.0f, 2.0f);
            if (ImGui.beginTabBar("##WorkspaceTabs", ImGuiTabBarFlags.AutoSelectNewTabs)) {
                org.argentumforge.engine.utils.MapContext contextToClose = null;

                for (org.argentumforge.engine.utils.MapContext context : openMaps) {
                    ImBoolean open = new ImBoolean(true);
                    int flags = (context == org.argentumforge.engine.utils.GameData.getActiveContext())
                            ? ImGuiTabItemFlags.None
                            : ImGuiTabItemFlags.None;
                    // Cheat: ImGui maneja la selección automáticamente si el nombre es único.
                    // Usamos el hash para unicidad en el ID del tab.
                    if (ImGui.beginTabItem(context.getMapName() + "###Tab" + context.hashCode(), open, flags)) {
                        if (context != org.argentumforge.engine.utils.GameData.getActiveContext()) {
                            // Save current camera position to the PREVIOUS context
                            org.argentumforge.engine.utils.MapContext prevContext = org.argentumforge.engine.utils.GameData
                                    .getActiveContext();
                            if (prevContext != null) {
                                prevContext
                                        .setSavedUserX(org.argentumforge.engine.game.User.INSTANCE.getUserPos().getX());
                                prevContext
                                        .setSavedUserY(org.argentumforge.engine.game.User.INSTANCE.getUserPos().getY());
                            }

                            org.argentumforge.engine.utils.GameData.setActiveContext(context);
                            updateEditorsContext(context);

                            // Restore camera position from the NEW context
                            org.argentumforge.engine.game.User.INSTANCE.getUserPos().setX(context.getSavedUserX());
                            org.argentumforge.engine.game.User.INSTANCE.getUserPos().setY(context.getSavedUserY());
                            org.argentumforge.engine.game.User.INSTANCE.getAddToUserPos().setX(0); // Reset smooth
                                                                                                   // scroll offset
                            org.argentumforge.engine.game.User.INSTANCE.getAddToUserPos().setY(0);
                            org.argentumforge.engine.game.User.INSTANCE.setUserMoving(false);
                        }
                        ImGui.endTabItem();
                    }

                    if (!open.get()) {
                        if (context.isModified()) {
                            // Cambiar temporalmente al contexto para guardar si es necesario
                            org.argentumforge.engine.utils.GameData.setActiveContext(context);
                            open.set(true); // Cancelar cierre inmediato mientras preguntamos
                            org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(
                                    () -> org.argentumforge.engine.utils.GameData.closeMap(context),
                                    null);
                        } else {
                            contextToClose = context;
                        }
                    }
                }

                if (contextToClose != null) {
                    org.argentumforge.engine.utils.GameData.closeMap(contextToClose);
                }

                ImGui.endTabBar();
            }
            ImGui.popStyleVar();
        }
        ImGui.end();
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

    public void newMap() {
        org.argentumforge.engine.utils.MapManager.checkUnsavedChangesAsync(() -> {
            org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
            org.argentumforge.engine.utils.GameData.updateWindowTitle();
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.mapCreated"),
                    org.argentumforge.engine.game.console.FontStyle.BOLD, new RGBColor(0, 1, 0));
        }, null);
    }

    public void loadMapAction() {
        org.argentumforge.engine.utils.MapFileUtils.openAndLoadMap();
    }

    public void copySelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        // Usar la primera entidad como referencia para el offset
        int refX = sel.getSelectedEntities().get(0).x;
        int refY = sel.getSelectedEntities().get(0).y;

        Clipboard.getInstance().copy(sel.getSelectedEntities(), refX, refY);
        Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.clipboard.copied", sel.getSelectedEntities().size()),
                REGULAR,
                new RGBColor(0f, 1f, 1f));
    }

    public void cutSelection() {
        copySelection();
        deleteSelection();
    }

    public void pasteSelection() {
        Clipboard clip = Clipboard.getInstance();
        if (clip.isEmpty())
            return;

        if (!EditorInputManager.inGameArea())
            return;

        int mx = (int) MouseListener.getX() - Camera.POS_SCREEN_X;
        int my = (int) MouseListener.getY() - Camera.POS_SCREEN_Y;
        int tx = EditorInputManager.getTileMouseX(mx);
        int ty = EditorInputManager.getTileMouseY(my);

        CommandManager.getInstance().executeCommand(new PasteEntitiesCommand(
                org.argentumforge.engine.utils.GameData.getActiveContext(), clip.getItems(), tx, ty));
    }

    public void deleteSelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        CommandManager.getInstance().executeCommand(new DeleteEntitiesCommand(
                org.argentumforge.engine.utils.GameData.getActiveContext(), sel.getSelectedEntities()));
        sel.getSelectedEntities().clear();
    }

    private void handleShortcuts() {
        if (ImGui.getIO().getWantCaptureKeyboard())
            return;

        boolean modifierPressed = KeyHandler.isActionKeyPressed(Key.MULTI_SELECT);

        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_C))
            copySelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_X))
            cutSelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_V))
            pasteSelection();
        if (ImGui.isKeyPressed(GLFW_KEY_DELETE))
            deleteSelection();
    }

}
