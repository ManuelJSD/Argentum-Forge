package org.argentumforge.engine.gui.forms;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.*;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.utils.editor.*;
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
    private FTriggerEditor triggerEditor;
    private FTransferEditor transferEditor;
    private FParticleEditor particleEditor;
    private float[] ambientColorArr;
    private org.argentumforge.engine.renderer.Texture toolbarIcons;

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

        // Cargar iconos de la barra de herramientas
        toolbarIcons = new org.argentumforge.engine.renderer.Texture();
        toolbarIcons.loadTexture(toolbarIcons, "gui", "toolbar_icons.png", true);
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

        drawMenuBar();
        ImGui.end(); // End DockSpace window

        // drawTabs(); // Tabs are redundant with docking potentially, but let's keep
        // them logic-wise if needed or refactor later.
        // For now, let's look if we can integrate them or just keep them floating for a
        // moment.
        // Actually, map contexts should probably be separate windows if we want full
        // docking power.
        // For this step, we keep the old tabs but might need to adjust Z-order.

        drawTabs();
        this.drawStatusBar();
        this.drawButtons();
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
                            if (org.argentumforge.engine.utils.MapManager.checkUnsavedChanges()) {
                                contextToClose = context;
                            } else {
                                open.set(true); // Cancelar cierre
                            }
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

    // Barra de estado inferior
    private void drawStatusBar() {
        // Altura de la barra
        float statusHeight = STATUS_BAR_HEIGHT;
        ImGuiViewport viewport = ImGui.getMainViewport();
        // Posición en la parte inferior de la ventana
        ImGui.setNextWindowPos(0, viewport.getSizeY() - statusHeight);
        // Ancho completo
        ImGui.setNextWindowSize(viewport.getSizeX(), statusHeight);

        // Estilo: Fondo oscuro, sin bordes ni decoraciones
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.1f, 0.1f, 0.1f, 0.9f);

        if (ImGui.begin("StatusBar",
                ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoInputs
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings
                        | ImGuiWindowFlags.NoBringToFrontOnFocus | ImGuiWindowFlags.NoFocusOnAppearing)) {

            // Datos a mostrar
            String fpsText = FPS + " FPS";
            String zoomText = "Zoom: " + (int) (org.argentumforge.engine.scenes.Camera.getZoomScale() * 100) + "%";

            // Coordenadas del Usuario
            int userX = org.argentumforge.engine.game.User.INSTANCE.getUserPos().getX();
            int userY = org.argentumforge.engine.game.User.INSTANCE.getUserPos().getY();
            String userPosText = "User: " + userX + ", " + userY;

            // Coordenadas del Mouse
            int mx = (int) MouseListener.getX() - Camera.POS_SCREEN_X;
            int my = (int) MouseListener.getY() - Camera.POS_SCREEN_Y;
            int tileMouseX = EditorInputManager.getTileMouseX(mx);
            int tileMouseY = EditorInputManager.getTileMouseY(my);
            String mousePosText = "Mouse: " + tileMouseX + ", " + tileMouseY;

            // Renderizado con espaciado
            ImGui.text(fpsText);
            ImGui.sameLine(0, 20); // Spacing
            ImGui.text("|");
            ImGui.sameLine(0, 20);
            ImGui.text(zoomText);
            ImGui.sameLine(0, 20);
            ImGui.text("|");

            // Highlight User Pos
            ImGui.sameLine(0, 20);
            ImGui.textColored(0.2f, 0.8f, 1.0f, 1.0f, userPosText);

            // Highlight Mouse Pos if valid
            ImGui.sameLine(0, 20);
            ImGui.text("|");
            ImGui.sameLine(0, 20);

            boolean validDetails = tileMouseX >= Camera.XMinMapSize && tileMouseX <= Camera.XMaxMapSize &&
                    tileMouseY >= Camera.YMinMapSize && tileMouseY <= Camera.YMaxMapSize;

            if (validDetails) {
                ImGui.textColored(1.0f, 0.8f, 0.2f, 1.0f, mousePosText);
            } else {
                ImGui.textDisabled(mousePosText);
            }

        }
        ImGui.end();
        ImGui.popStyleColor();
    }

    // Botones principales
    private void drawButtons() {
        ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(0, 49);
        ImGui.setNextWindowSize(viewport.getSizeX(), 60); // Ajustado al ancho del viewport
        if (ImGui.begin("ToolBar", ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoBackground
                | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            drawEditorButtons();
        }
        ImGui.end();
    }

    /**
     * Dibuja los botones para mostrar/ocultar los editores de superficies y
     * bloqueos.
     */
    /**
     * Dibuja la barra de herramientas compacta.
     */
    private void drawEditorButtons() {
        ImGui.setCursorPos(10, 5); // Alineado arriba dentro de la mini-ventana

        // Configuración de estilo par botones
        float btnSize = 48;
        float uvStep = 1.0f / 3.0f; // 3x3 grid
        float zoom = 0.03f; // Ajuste fino (3%) para eliminar márgenes transparentes sin recortar el icono

        ImGui.pushStyleVar(ImGuiStyleVar.FrameBorderSize, 0); // Eliminar borde de ImGui

        // --- GRUPO 1: EDICIÓN (Visual) ---

        // Superficies (Fila 0, Col 0)
        drawIconButton("Su", I18n.INSTANCE.get("toolbar.surface"), "FSurfaceEditor", surfaceEditor,
                btnSize, 0 * uvStep + zoom, 0 * uvStep + zoom, 1 * uvStep - zoom, 1 * uvStep - zoom);

        ImGui.sameLine();

        // Objetos (Fila 1, Col 1)
        drawIconButton("Ob", I18n.INSTANCE.get("menu.view.objects"), "FObjEditor", objEditor,
                btnSize, 1 * uvStep + zoom, 1 * uvStep + zoom, 2 * uvStep - zoom, 2 * uvStep - zoom);

        ImGui.sameLine();

        // NPCs (Fila 1, Col 0)
        drawIconButton("NP", I18n.INSTANCE.get("menu.view.npcs"), "FNpcEditor", npcEditor,
                btnSize, 0 * uvStep + zoom, 1 * uvStep + zoom, 1 * uvStep - zoom, 2 * uvStep - zoom);

        ImGui.sameLine();

        // Partículas (Fila 2, Col 0)
        drawIconButton("Pa", I18n.INSTANCE.get("menu.view.particles"), "FParticleEditor", particleEditor,
                btnSize, 0 * uvStep + zoom, 2 * uvStep + zoom, 1 * uvStep - zoom, 3 * uvStep - zoom);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 2: LÓGICA ---

        // Bloqueos (Fila 0, Col 1)
        drawIconButton("Bl", I18n.INSTANCE.get("menu.view.blocks"), "FBlockEditor", blockEditor,
                btnSize, 1 * uvStep + zoom, 0 * uvStep + zoom, 2 * uvStep - zoom, 1 * uvStep - zoom);

        ImGui.sameLine();

        // Triggers (Fila 0, Col 2)
        drawIconButton("Tg", I18n.INSTANCE.get("menu.view.triggers"), "FTriggerEditor", triggerEditor,
                btnSize, 2 * uvStep + zoom, 0 * uvStep + zoom, 3 * uvStep - zoom, 1 * uvStep - zoom);

        ImGui.sameLine();

        // Traslados (Fila 1, Col 2)
        drawIconButton("Tl", I18n.INSTANCE.get("menu.view.transfers"), "FTransferEditor", transferEditor,
                btnSize, 2 * uvStep + zoom, 1 * uvStep + zoom, 3 * uvStep - zoom, 2 * uvStep - zoom);

        // --- SEPARADOR ---
        drawToolbarSeparator();

        // --- GRUPO 3: UTILIDAD ---

        // Minimapa (Fila 2, Col 1)
        drawIconButton("MM", I18n.INSTANCE.get("menu.view.minimap"), "FMinimap", minimap,
                btnSize, 1 * uvStep + zoom, 2 * uvStep + zoom, 2 * uvStep - zoom, 3 * uvStep - zoom);

        ImGui.sameLine();

        // Botón Selección (Fila 2, Col 2)
        boolean selectionActive = Selection.getInstance().isActive();
        if (selectionActive) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
        }

        ImGui.pushID("btnSelect");
        if (toolbarIcons.getId() > 0) {
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 0);
            if (ImGui.imageButton(toolbarIcons.getId(), btnSize, btnSize, 2 * uvStep + zoom, 2 * uvStep + zoom,
                    3 * uvStep - zoom,
                    3 * uvStep - zoom)) {
                toggleSelection();
            }
            ImGui.popStyleVar();
        } else {
            if (ImGui.button("Sel", btnSize, btnSize)) {
                toggleSelection();
            }
        }
        ImGui.popID();

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.INSTANCE.get("common.selection"));
        }
        if (selectionActive) {
            ImGui.popStyleColor();
        }
        ImGui.popStyleVar(); // Pop BorderSize
    }

    private void drawToolbarSeparator() {
        ImGui.sameLine(0, 10);

        float x = ImGui.getCursorScreenPosX();
        float y = ImGui.getCursorScreenPosY();
        float h = 48; // Button height

        // Draw vertical line centered
        ImGui.getWindowDrawList().addLine(
                x, y + 8,
                x, y + h - 8,
                ImGui.getColorU32(ImGuiCol.Separator));

        ImGui.dummy(1, h); // Advance cursor
        ImGui.sameLine(0, 10);
    }

    private void toggleSelection() {
        Selection sel = Selection.getInstance();
        sel.setActive(!sel.isActive());

        if (sel.isActive()) {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOn"), REGULAR,
                    new RGBColor(0f, 1f, 0f));
            // Desactivar otros modos
            Surface.getInstance().setMode(0);
            org.argentumforge.engine.utils.editor.Npc.getInstance().setMode(0);
            org.argentumforge.engine.utils.editor.Obj.getInstance().setMode(0);
            org.argentumforge.engine.utils.editor.Block.getInstance().setMode(0);
            org.argentumforge.engine.utils.editor.Trigger.getInstance().setMode(0);
            org.argentumforge.engine.utils.editor.Transfer.getInstance().setMode(0);
        } else {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOff"), REGULAR,
                    new RGBColor(1f, 1f, 0f));
        }
    }

    private void drawIconButton(String fallbackLabel, String tooltip, String formName, Form formInstance,
            float size, float u0, float v0, float u1, float v1) {
        ImGui.pushID(formName);
        // Resaltar si está activo (visible)
        boolean isVisible = ImGUISystem.INSTANCE.isFormVisible(formName);
        if (isVisible) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_PRIMARY);
        }

        // Si la textura cargó correctamente, usar ImageButton, sino fallback a Texto
        if (toolbarIcons.getId() > 0) {
            // Eliminar padding para que el icono ocupe todo el botón
            ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0, 0);
            if (ImGui.imageButton(toolbarIcons.getId(), size, size, u0, v0, u1, v1)) {
                toggleForm(isVisible, formInstance);
            }
            ImGui.popStyleVar();
        } else {
            if (ImGui.button(fallbackLabel, size, size)) {
                toggleForm(isVisible, formInstance);
            }
        }

        if (isVisible) {
            ImGui.popStyleColor();
        }

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(tooltip);
        }
        ImGui.popID();
    }

    private void toggleForm(boolean isVisible, Form formInstance) {
        if (isVisible) {
            ImGUISystem.INSTANCE.deleteFrmArray(formInstance);
        } else {
            ImGUISystem.INSTANCE.show(formInstance);
        }
    }

    /**
     * Dibuja los botones para mostrar/ocultar los editores de superficies y
     * bloqueos. (Legacy, kept just in case but overridden by drawEditorButtons)
     */
    private void drawCompactButton(String label, String tooltip, String formName, Form formInstance, float w, float h) {
        // ... Unused now
    }

    private void drawMenuBar() {

        if (ImGui.beginMenuBar()) {

            RenderSettings renderSettings = options.getRenderSettings();

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file"))) {

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.new"))) {
                    this.newMap();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.open"))) {
                    this.loadMapAction();
                }

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file.recent"))) {
                    java.util.List<String> recentMaps = options.getRecentMaps();
                    if (recentMaps.isEmpty()) {
                        ImGui.textDisabled(I18n.INSTANCE.get("menu.file.recent.none"));
                    } else {
                        // Create a copy to avoid ConcurrentModificationException when loading a map
                        // modifies the list
                        for (String mapPath : new java.util.ArrayList<>(recentMaps)) {
                            if (ImGui.menuItem(mapPath)) {
                                org.argentumforge.engine.utils.MapManager.loadMap(mapPath);
                            }
                        }
                    }
                    ImGui.endMenu();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.save"), "Ctrl+S")) {
                    org.argentumforge.engine.utils.MapFileUtils.quickSaveMap();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.saveAs"), "Ctrl+Shift+S")) {
                    org.argentumforge.engine.utils.MapFileUtils.saveMapAs();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("options.title"))) {
                    ImGUISystem.INSTANCE.show(new FOptions());
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.enableDocking"), "Restart Required",
                        options.isDockingEnabled())) {
                    options.setDockingEnabled(!options.isDockingEnabled());
                    options.save();
                    javax.swing.JOptionPane.showMessageDialog(null,
                            "Debe reiniciar la aplicación para aplicar los cambios de Docking.");
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.exit"))) {
                    org.argentumforge.engine.Engine.closeClient();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.edit"))) {
                CommandManager manager = CommandManager.getInstance();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.undo"), "Ctrl+Z", false, manager.canUndo())) {
                    manager.undo();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.redo"), "Ctrl+Y", false, manager.canRedo())) {
                    manager.redo();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.cut"), "Ctrl+X", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    cutSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.copy"), "Ctrl+C", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    copySelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.paste"), "Ctrl+V", false,
                        !Clipboard.getInstance().isEmpty())) {
                    pasteSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.delete"), "Supr", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    deleteSelection();
                }

                ImGui.endMenu();
            }

            /*
             * if (ImGui.beginMenu("Editores")) {
             * if (ImGui.menuItem("Superficies", "",
             * ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor"))) {
             * if (ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")) {
             * ImGUISystem.INSTANCE.deleteFrmArray(surfaceEditor);
             * } else {
             * ImGUISystem.INSTANCE.show(surfaceEditor);
             * }
             * }
             * if (ImGui.menuItem("Bloqueos", "",
             * ImGUISystem.INSTANCE.isFormVisible("FBlockEditor"))) {
             * if (ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")) {
             * ImGUISystem.INSTANCE.deleteFrmArray(blockEditor);
             * } else {
             * ImGUISystem.INSTANCE.show(blockEditor);
             * }
             * }
             * ImGui.endMenu();
             * }
             */

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.map"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.properties"), "F6")) {
                    ImGUISystem.INSTANCE.show(new FInfoMap());
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.validate"))) {
                    if (org.argentumforge.engine.utils.GameData.getActiveContext() != null) {
                        ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FMapValidator());
                    } else {
                        javax.swing.JOptionPane.showMessageDialog(null, I18n.INSTANCE.get("msg.noActiveMap"));
                    }
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.resetZoom"), "Ctrl+0")) {
                    Camera.setTileSize(32);
                }

                ImGui.separator();

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.layers"))) {
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer1"), "", renderSettings.getShowLayer()[0])) {
                        renderSettings.getShowLayer()[0] = !renderSettings.getShowLayer()[0];
                        options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer2"), "", renderSettings.getShowLayer()[1])) {
                        renderSettings.getShowLayer()[1] = !renderSettings.getShowLayer()[1];
                        options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer3"), "", renderSettings.getShowLayer()[2])) {
                        renderSettings.getShowLayer()[2] = !renderSettings.getShowLayer()[2];
                        options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer4"), "", renderSettings.getShowLayer()[3])) {
                        renderSettings.getShowLayer()[3] = !renderSettings.getShowLayer()[3];
                        options.save();
                    }
                    ImGui.endMenu();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.blocks"), "", renderSettings.getShowBlock())) {
                    renderSettings.setShowBlock(!renderSettings.getShowBlock());
                    options.save();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.objects"), "", renderSettings.getShowOJBs())) {
                    renderSettings.setShowOJBs(!renderSettings.getShowOJBs());
                    options.save();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.npcs"), "", renderSettings.getShowNPCs())) {
                    renderSettings.setShowNPCs(!renderSettings.getShowNPCs());
                    options.save();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.transfers"), "", renderSettings.getShowMapTransfer())) {
                    renderSettings.setShowMapTransfer(!renderSettings.getShowMapTransfer());
                    options.save();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.triggers"), "", renderSettings.getShowTriggers())) {
                    renderSettings.setShowTriggers(!renderSettings.getShowTriggers());
                    options.save();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.particles"), "", renderSettings.getShowParticles())) {
                    renderSettings.setShowParticles(!renderSettings.getShowParticles());
                    options.save();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.grid"), "G", renderSettings.isShowGrid())) {
                    renderSettings.setShowGrid(!renderSettings.isShowGrid());
                    options.save();
                }

                ImGui.separator();

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.minimap"))) {
                    if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.layers"))) {
                        for (int i = 0; i < 4; i++) {
                            if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer") + " " + (i + 1), "",
                                    renderSettings.getMinimapLayers()[i])) {
                                renderSettings.getMinimapLayers()[i] = !renderSettings.getMinimapLayers()[i];
                                options.save();
                            }
                        }
                        ImGui.endMenu();
                    }

                    ImGui.separator();

                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.npcs"), "",
                            renderSettings.isShowMinimapNPCs())) {
                        renderSettings.setShowMinimapNPCs(!renderSettings.isShowMinimapNPCs());
                        options.save();
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.exits"), "",
                            renderSettings.isShowMinimapExits())) {
                        renderSettings.setShowMinimapExits(!renderSettings.isShowMinimapExits());
                        options.save();
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.triggers"), "",
                            renderSettings.isShowMinimapTriggers())) {
                        renderSettings.setShowMinimapTriggers(!renderSettings.isShowMinimapTriggers());
                        options.save();
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.blocks"), "",
                            renderSettings.isShowMinimapBlocks())) {
                        renderSettings.setShowMinimapBlocks(!renderSettings.isShowMinimapBlocks());
                        options.save();
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.map"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.goto"), "F4")) {
                    ImGUISystem.INSTANCE.show(new FGoTo());
                }

                ImGui.separator();

                // Otras opciones de mapa futuras (Validar, Propiedades)
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.validate"))) {
                    // Lógica de validación TODO
                    // org.argentumforge.engine.utils.MapValidator.validate();
                    javax.swing.JOptionPane.showMessageDialog(null, "Próximamente...");
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.tools"))) {
                /*
                 * if (ImGui.menuItem(I18n.INSTANCE.get("menu.tools.setupWizard"))) {
                 * FSetupWizard wizard = new FSetupWizard(() -> {
                 * // Al finalizar, recargar recursos si cambiaron las rutas
                 * org.argentumforge.engine.utils.GameData.init();
                 * }, false);
                 * ImGUISystem.INSTANCE.show(wizard);
                 * }
                 */

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.export"))) {
                    javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
                    fileChooser.setDialogTitle("Exportar Mapa como Imagen");
                    fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Imagen PNG", "png"));
                    if (fileChooser.showSaveDialog(null) == javax.swing.JFileChooser.APPROVE_OPTION) {
                        String path = fileChooser.getSelectedFile().getAbsolutePath();
                        if (!path.toLowerCase().endsWith(".png")) {
                            path += ".png";
                        }
                        org.argentumforge.engine.utils.MapExporter.exportMap(path);
                        javax.swing.JOptionPane.showMessageDialog(null, "Mapa exportado correctamente a:\n" + path);
                    }
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.tools.generateColors"))) {
                    int response = javax.swing.JOptionPane.showConfirmDialog(null,
                            I18n.INSTANCE.get("msg.generateColorsConfirm"),
                            I18n.INSTANCE.get("menu.tools.generateColors"), javax.swing.JOptionPane.YES_NO_OPTION);

                    if (response == javax.swing.JOptionPane.YES_OPTION) {
                        MinimapColorGenerator.generateBinary();
                    }
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.misc"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.walkMode"), "",
                        org.argentumforge.engine.game.User.INSTANCE.isWalkingmode())) {
                    org.argentumforge.engine.game.User.INSTANCE
                            .setWalkingmode(!org.argentumforge.engine.game.User.INSTANCE.isWalkingmode());
                }

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.misc.ambient"))) {
                    if (ImGui.colorEdit3(I18n.INSTANCE.get("menu.misc.ambient.color"), ambientColorArr)) {
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(ambientColorArr[0],
                                ambientColorArr[1],
                                ambientColorArr[2]);
                    }

                    ImGui.separator();

                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.day"))) {
                        ambientColorArr[0] = 1.0f;
                        ambientColorArr[1] = 1.0f;
                        ambientColorArr[2] = 1.0f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(1.0f, 1.0f, 1.0f);
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.afternoon"))) {
                        ambientColorArr[0] = 0.8f;
                        ambientColorArr[1] = 0.5f;
                        ambientColorArr[2] = 0.3f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(0.8f, 0.5f, 0.3f);
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.night"))) {
                        ambientColorArr[0] = 0.2f;
                        ambientColorArr[1] = 0.2f;
                        ambientColorArr[2] = 0.4f;
                        org.argentumforge.engine.game.Weather.INSTANCE.setAmbientColor(0.2f, 0.2f, 0.4f);
                    }

                    ImGui.endMenu();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("grhlib.title"), "")) {
                    if (ImGUISystem.INSTANCE.isFormVisible("FGrhLibrary")) {
                        ImGUISystem.INSTANCE.deleteFrmArray(grhLibrary);
                    } else {
                        grhLibrary = new FGrhLibrary();
                        ImGUISystem.INSTANCE.show(grhLibrary);
                    }
                }

                ImGui.endMenu();
            }

            ImGui.endMenuBar();
        }

    }

    private void loadMapAction() {
        org.argentumforge.engine.utils.MapFileUtils.openAndLoadMap();
    }

    private void newMap() {
        org.argentumforge.engine.utils.MapManager.createEmptyMap(100, 100);
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

    private void copySelection() {
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

    private void cutSelection() {
        copySelection();
        deleteSelection();
    }

    private void pasteSelection() {
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

    private void deleteSelection() {
        Selection sel = Selection.getInstance();
        if (sel.getSelectedEntities().isEmpty())
            return;

        CommandManager.getInstance().executeCommand(new DeleteEntitiesCommand(sel.getSelectedEntities()));
        sel.getSelectedEntities().clear();
    }
}
