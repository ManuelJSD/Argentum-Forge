package org.argentumforge.engine.gui.forms;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.flag.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.Theme;

import org.argentumforge.engine.gui.forms.main.MainMenuBar;
import org.argentumforge.engine.gui.forms.main.MainStatusBar;
import org.argentumforge.engine.gui.forms.main.MainToolbar;
import org.argentumforge.engine.gui.forms.main.FilmstripBar;
import org.argentumforge.engine.i18n.I18n;

import static org.argentumforge.engine.utils.GameData.options;
import org.argentumforge.engine.utils.GithubReleaseChecker;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.scenes.GameScene;
import org.argentumforge.engine.gui.components.ContextMenu;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.gui.ThemeManager;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.utils.editor.MinimapColorGenerator;
import org.argentumforge.engine.game.Options;
import imgui.ImGuiViewport;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.game.EditorController;
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

    // Componentes
    private final MainMenuBar menuBar;
    private final MainToolbar toolbar;
    private final MainStatusBar statusBar;
    private final FilmstripBar filmstripBar;

    private final java.util.List<IMapEditor> mapEditors = new java.util.ArrayList<>();
    private MapContext lastContextSeen = null;
    private boolean syncRequested = false;

    public FMain() {
        ambientColorArr = new float[] {
                Weather.INSTANCE.getWeatherColor().getRed(),
                Weather.INSTANCE.getWeatherColor().getGreen(),
                Weather.INSTANCE.getWeatherColor().getBlue()
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

        // Inicializar componentes reutilizables
        this.menuBar = new MainMenuBar(this);
        this.toolbar = new MainToolbar(this);
        this.statusBar = new MainStatusBar();
        this.filmstripBar = new FilmstripBar();

        // Inicializar Tema
        // Inicializar Tema
        ThemeManager.getInstance().init();

    }

    private void updateEditorsContext(MapContext context) {
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
        ImGuiViewport viewport = ImGui.getMainViewport();
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

        // Calcular margen superior para no tapar el DockSpace con la Toolbar y Tabs
        // Calcular margen superior para no tapar el DockSpace con la Toolbar y Tabs
        float topMargin = 52.0f; // Altura de MainToolbar
        if (!GameData.getOpenMaps().isEmpty()) {
            topMargin += 22.0f; // Altura de Tabs
        }
        ImGui.setCursorPosY(topMargin);

        // Crear el DockSpace real
        int dockspaceId = ImGui.getID("MainEditorDockSpace");
        ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.PassthruCentralNode);

        ImGui.end();

        // 3. RENDERIZADO DE COMPONENTES FIJOS (Se apoyan en el DockSpace o WorkArea)
        if (!isPhoto) {
            statusBar.render();
            toolbar.render();
            filmstripBar.render();
        }

        if (isPhoto) {
            photoModeUI.render();
            return;
        }

        // 3. RENDERIZADO DE TABS DE MAPAS
        drawTabs();

        Console.INSTANCE.drawConsole();
        handleShortcuts();

        if (Engine.getCurrentScene() instanceof GameScene) {
            ((GameScene) Engine.getCurrentScene()).renderImGuiOverlays();
        }

        // Modal de Progreso Global para Generación del Minimapa
        if (MinimapColorGenerator.generating) {
            ImGui.openPopup(I18n.INSTANCE.get("msg.processingColors"));
        }

        // Renderizar Menú Contextual (siempre disponible para aparecer)
        ContextMenu.render();

        if (MinimapColorGenerator.generating) {
            float centerX = viewport.getWorkPosX() + viewport.getWorkSizeX() * 0.5f;
            float centerY = viewport.getWorkPosY() + viewport.getWorkSizeY() * 0.5f;
            ImGui.setNextWindowPos(centerX, centerY, ImGuiCond.Always, 0.5f, 0.5f);
        }

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("msg.processingColors"),
                ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoTitleBar)) {
            if (!MinimapColorGenerator.generating) {
                ImGui.textColored(ImGui.getColorU32(0.0f, 1.0f, 0.0f, 1.0f), I18n.INSTANCE.get("msg.complete"));
                if (ImGui.button(I18n.INSTANCE.get("common.close"), 300, 0)) {
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.text(I18n.INSTANCE.get("msg.processingColors"));
                ImGui.separator();
                ImGui.progressBar(MinimapColorGenerator.progress / 100.0f, 300,
                        20,
                        String.format("%.0f%%", MinimapColorGenerator.progress));
            }
            ImGui.endPopup();
        }

        // Lógica de Notificación de Actualizaciones (Movido desde FLauncher)
        if (GithubReleaseChecker.isUpdateAvailable() && !updatePopupShown) {
            GithubReleaseChecker.ReleaseInfo r = GithubReleaseChecker.getLatestRelease();
            String ignored = Options.INSTANCE.getIgnoredUpdateTag();

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

            // Botón "No mostrar de nuevo"
            ImGui.spacing();
            if (ImGui.button(I18n.INSTANCE.get("update.skip"), 308, 20)) {
                if (release != null) {
                    Options.INSTANCE.setIgnoredUpdateTag(release.tagName);
                    Options.INSTANCE.save();
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
        java.util.List<MapContext> openMaps = GameData
                .getOpenMaps();
        if (openMaps.isEmpty())
            return;

        ImGuiViewport viewport = ImGui.getMainViewport();
        float toolbarHeight = 52.0f;
        float tabsHeight = 22.0f;

        // Anclar debajo de la toolbar
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY() + toolbarHeight);
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), tabsHeight);
        ImGui.setNextWindowViewport(viewport.getID());

        // Detectar si el contexto cambió fuera de este loop (ej: teletransporte)
        MapContext currentActive = GameData
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
                MapContext contextToClose = null;

                for (MapContext context : openMaps) {
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
                                        .setSavedUserX(User.INSTANCE.getUserPos().getX());
                                currentActive
                                        .setSavedUserY(User.INSTANCE.getUserPos().getY());
                            }

                            // Diferir cambio al final del frame para prevenir corrupción de ImGui
                            Engine.INSTANCE.runOnMainThread(() -> {
                                GameData.setActiveContext(context);
                                updateEditorsContext(context);

                                // Restaurar posición
                                User.INSTANCE.getUserPos().setX(context.getSavedUserX());
                                User.INSTANCE.getUserPos().setY(context.getSavedUserY());
                                User.INSTANCE.getAddToUserPos().setX(0);
                                User.INSTANCE.getAddToUserPos().setY(0);
                                User.INSTANCE.setUserMoving(false);
                            });
                        }
                        ImGui.endTabItem();
                    }

                    if (!open.get()) {
                        if (context.isModified()) {
                            // Cambiar temporalmente al contexto para guardar si es necesario
                            // Diferir cambio y chequeo modal
                            Engine.INSTANCE.runOnMainThread(() -> {
                                GameData.setActiveContext(context);
                                MapManager.checkUnsavedChangesAsync(
                                        () -> GameData.closeMap(context),
                                        null);
                            });
                            open.set(true); // Cancelar cierre inmediato mientras preguntamos
                        } else {
                            contextToClose = context;
                        }
                    }
                }

                if (contextToClose != null) {
                    final MapContext finalToClose = contextToClose;
                    Engine.INSTANCE.runOnMainThread(() -> {
                        GameData.closeMap(finalToClose);
                    });
                }

                ImGui.endTabBar();
            }
            ImGui.popStyleVar();
        }
        ImGui.end();

        // Actualizar último contexto visto para el siguiente frame
        lastContextSeen = GameData.getActiveContext();
    }

    // Getters para MainToolbar y MainMenuBar
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
            EditorController.INSTANCE.copySelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_X))
            EditorController.INSTANCE.cutSelection();
        if (modifierPressed && ImGui.isKeyPressed(GLFW_KEY_V))
            EditorController.INSTANCE.pasteSelection();
        if (ImGui.isKeyPressed(GLFW_KEY_DELETE))
            EditorController.INSTANCE.deleteSelection();
    }

}
