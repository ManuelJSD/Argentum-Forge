package org.argentumforge.engine.gui.forms;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.utils.editor.*;
import org.argentumforge.engine.gui.forms.main.MainMenuBar;
import org.argentumforge.engine.gui.forms.main.MainStatusBar;
import org.argentumforge.engine.gui.forms.main.MainToolbar;
import org.argentumforge.engine.i18n.I18n;

import static org.argentumforge.engine.utils.GameData.options;
import static org.argentumforge.engine.utils.Time.FPS;
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
    private static final float STATUS_BAR_HEIGHT = 30.0f;

    private FSurfaceEditor surfaceEditor;
    private FBlockEditor blockEditor;
    private FNpcEditor npcEditor;
    private FObjEditor objEditor;
    private FMinimap minimap;
    private FGrhLibrary grhLibrary;
    private FPhotoMode photoModeUI = new FPhotoMode();
    private FTriggerEditor triggerEditor;
    private FTransferEditor transferEditor;
    private FParticleEditor particleEditor;
    private FSpeedControl speedControl;

    private float[] ambientColorArr;

    // Components
    private final MainMenuBar menuBar;
    private final MainToolbar toolbar;
    private final MainStatusBar statusBar;

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

        // Initialize reusable components
        this.menuBar = new MainMenuBar(this);
        this.toolbar = new MainToolbar(this);
        this.statusBar = new MainStatusBar();

    }

    @Override
    public void render() {
        // Setup DockSpace
        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(viewport.getSizeX(), viewport.getSizeY() - STATUS_BAR_HEIGHT);
        ImGui.setNextWindowViewport(viewport.getID());

        int windowFlags = ImGuiWindowFlags.MenuBar | ImGuiWindowFlags.NoDocking;
        windowFlags |= ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize
                | ImGuiWindowFlags.NoMove;
        windowFlags |= ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoNavFocus
                | ImGuiWindowFlags.NoBackground;

        ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0.0f);
        // ImGui.pushStyleVar(ImGuiStyleVar.WindowBorderSize, 0.0f); // User requested
        // border
        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 0.0f, 0.0f);

        ImGui.begin("DockSpace Demo", windowFlags);
        ImGui.popStyleVar(2);

        int dockspaceId = ImGui.getID("MyDockSpace");
        ImGui.dockSpace(dockspaceId, 0.0f, 0.0f, ImGuiDockNodeFlags.PassthruCentralNode);

        menuBar.render();

        ImGui.end(); // End DockSpace window

        // drawTabs(); // Tabs are redundant with docking potentially, but let's keep
        // them logic-wise if needed or refactor later.
        // For now, let's look if we can integrate them or just keep them floating for a
        // moment.
        // Actually, map contexts should probably be separate windows if we want full
        // docking power.
        // For this step, we keep the old tabs but might need to adjust Z-order.

        if (options.getRenderSettings().isPhotoModeActive()) {
            photoModeUI.render();
            return;
        }

        drawTabs();
        statusBar.render();
        toolbar.render();

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

    }

    private void drawTabs() {
        java.util.List<org.argentumforge.engine.utils.MapContext> openMaps = org.argentumforge.engine.utils.GameData
                .getOpenMaps();
        if (openMaps.isEmpty())
            return;

        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(0, 19); // Debajo de la barra de menú Principal
        ImGui.setNextWindowSize(viewport.getSizeX(), 30);
        if (ImGui.begin("WorkspaceTabsWindow", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoScrollbar | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
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
                            org.argentumforge.engine.utils.GameData.setActiveContext(context);
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

        CommandManager.getInstance().executeCommand(new PasteEntitiesCommand(clip.getItems(), tx, ty));
    }

    public void deleteSelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        CommandManager.getInstance().executeCommand(new DeleteEntitiesCommand(sel.getSelectedEntities()));
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
