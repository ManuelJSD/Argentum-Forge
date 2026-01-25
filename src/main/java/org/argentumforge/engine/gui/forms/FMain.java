package org.argentumforge.engine.gui.forms;

import imgui.type.ImBoolean;
import imgui.ImGui;
import imgui.flag.*;
import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.widgets.ImGuiFilePicker;
import org.argentumforge.engine.gui.widgets.ImGuiFolderPicker;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.utils.MapManager;
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

import java.io.File;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Interfaz principal del editor.
 * Gestiona el menú superior, la barra de herramientas y los sub-ventanas de
 * edición (Superficies, Bloqueos, etc.).
 */

public final class FMain extends Form {
    private boolean showGenerateColorsPopup = false;
    private File pendingFile;

    private enum PendingAction {
        SAVE_MAP,
        EXPORT_MAP
    }

    private PendingAction pendingAction;
    private boolean showOverwritePopup = false;

    private ImGuiFilePicker openMapPicker;
    private ImGuiFolderPicker saveMapPicker;
    private ImGuiFolderPicker exportMapPicker;


    private static final int TRANSPARENT_COLOR = Theme.TRANSPARENT;

    private FSurfaceEditor surfaceEditor;
    private FBlockEditor blockEditor;
    private FNpcEditor npcEditor;
    private FObjEditor objEditor;
    private FMinimap minimap;
    private FGrhLibrary grhLibrary;
    private FTriggerEditor triggerEditor;
    private FTransferEditor transferEditor;
    private float[] ambientColorArr;

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
    }

    @Override
    public void render() {
        drawMenuBar();
        renderFilePickers();
        drawTabs();
        drawGenerateColorsPopup();
        MapManager.renderUnsavedChangesPopup();
        MapManager.drawUnsavedChangesPopup();
        this.renderFPS();
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

    private void renderFilePickers() {

        // OPEN MAP
        if (openMapPicker != null && openMapPicker.isOpen()) {
            openMapPicker.render();

            if (openMapPicker.hasResult()) {
                File f = openMapPicker.getSelectedFile();
                options.addRecentMap(f.getAbsolutePath());
                options.setLastMapPath(f.getParent());
                options.save();

                org.argentumforge.engine.utils.MapManager.loadMap(f.getAbsolutePath());
                openMapPicker = null;
            }
        }

        // SAVE MAP
        if (saveMapPicker != null && saveMapPicker.isOpen()) {
            saveMapPicker.render();

            File f = saveMapPicker.getResultFile();
            if (f != null) {
                pendingFile = f;
                pendingAction = PendingAction.SAVE_MAP;

                if (f.exists()) {
                    showOverwritePopup = true;
                } else {
                    org.argentumforge.engine.utils.GameData.saveMap(f.getAbsolutePath());
                    options.setLastMapPath(f.getParent());
                    options.save();
                }

                saveMapPicker = null;
            }
        }

        // EXPORT MAP
        if (exportMapPicker != null && exportMapPicker.isOpen()) {
            exportMapPicker.render();

            File f = exportMapPicker.getResultFile();
            if (f != null) {
                pendingFile = f;
                pendingAction = PendingAction.EXPORT_MAP;

                if (f.exists()) {
                    showOverwritePopup = true;
                } else {
                    org.argentumforge.engine.utils.MapExporter.exportMap(f.getAbsolutePath());
                }

                exportMapPicker = null;
            }
        }

        confirmOverwrite();
    }

    private void confirmOverwrite() {
        if (showOverwritePopup) {
            ImGui.openPopup("ConfirmOverwrite");
            showOverwritePopup = false;
        }

        if (ImGui.beginPopupModal("ConfirmOverwrite",
                ImGuiWindowFlags.AlwaysAutoResize | ImGuiWindowFlags.NoMove)) {

            ImGui.text("El archivo ya existe.");
            ImGui.text("¿Desea reemplazarlo?");
            ImGui.separator();

            if (ImGui.button("Reemplazar", 120, 0)) {

                if (pendingAction == PendingAction.SAVE_MAP) {
                    org.argentumforge.engine.utils.GameData.saveMap(pendingFile.getAbsolutePath());
                    options.setLastMapPath(pendingFile.getParent());
                    options.save();
                }

                if (pendingAction == PendingAction.EXPORT_MAP) {
                    org.argentumforge.engine.utils.MapExporter.exportMap(pendingFile.getAbsolutePath());
                }

                pendingFile = null;
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();

            if (ImGui.button("Cancelar", 120, 0)) {
                pendingFile = null;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    private File getLastMapDir() {
        String last = options.getLastMapPath();
        return (last != null && !last.isEmpty())
                ? new File(last)
                : new File(System.getProperty("user.home"));
    }

    private File ensureExtension(File f, String ext) {
        if (!f.getName().toLowerCase().endsWith("." + ext)) {
            return new File(f.getAbsolutePath() + "." + ext);
        }
        return f;
    }

    private void drawTabs() {
        var openMaps = org.argentumforge.engine.utils.GameData.getOpenMaps();
        if (openMaps.isEmpty())
            return;

        ImGui.setNextWindowPos(0, 19);
        ImGui.setNextWindowSize(Window.INSTANCE.getWidth(), 30);

        if (ImGui.begin("WorkspaceTabsWindow",
                ImGuiWindowFlags.NoDecoration
                        | ImGuiWindowFlags.NoBackground
                        | ImGuiWindowFlags.NoScrollbar
                        | ImGuiWindowFlags.NoMove
                        | ImGuiWindowFlags.NoSavedSettings)) {

            if (ImGui.beginTabBar("##WorkspaceTabs", ImGuiTabBarFlags.AutoSelectNewTabs)) {

                for (var context : openMaps) {
                    ImBoolean open = new ImBoolean(true);

                    if (ImGui.beginTabItem(
                            context.getMapName() + "###Tab" + context.hashCode(),
                            open)) {

                        if (context != org.argentumforge.engine.utils.GameData.getActiveContext()) {
                            org.argentumforge.engine.utils.GameData.setActiveContext(context);
                        }

                        ImGui.endTabItem();
                    }

                    // 👇 Pedido de cierre (NO cierre directo)
                    if (!open.get()) {
                        org.argentumforge.engine.utils.MapManager
                                .requestCloseContext(context);
                    }
                }

                ImGui.endTabBar();
            }
        }

        ImGui.end();
    }

    // FPS
    private void renderFPS() {
        final String txtStats = FPS + " FPS | Zoom: "
                + (int) (org.argentumforge.engine.scenes.Camera.getZoomScale() * 100) + "%";
        float widgetWidth = 165;

        ImGui.setNextWindowPos(Window.INSTANCE.getWidth() - widgetWidth - 10, 52);
        ImGui.setNextWindowSize(widgetWidth, 30);
        ImGui.pushStyleColor(ImGuiCol.WindowBg, 0.0f, 0.0f, 0.0f, 0.4f);
        if (ImGui.begin("Stats",
                ImGuiWindowFlags.NoDecoration | ImGuiWindowFlags.NoInputs
                        | ImGuiWindowFlags.NoMove | ImGuiWindowFlags.NoSavedSettings)) {
            ImGui.pushStyleVar(ImGuiStyleVar.SelectableTextAlign, 0.5f, 0.5f);
            ImGui.pushStyleColor(ImGuiCol.HeaderHovered, TRANSPARENT_COLOR);
            ImGui.pushStyleColor(ImGuiCol.HeaderActive, TRANSPARENT_COLOR);
            ImGui.selectable(txtStats, false, ImGuiSelectableFlags.None, widgetWidth - 10, 20);
            ImGui.popStyleColor(3);
            ImGui.popStyleVar();
        }
        ImGui.end();
    }

    // Botones principales
    private void drawButtons() {
        ImGui.setNextWindowPos(0, 49);
        ImGui.setNextWindowSize(1000, 40); // Aumentado para evitar que los botones se corten
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
    private void drawEditorButtons() {
        ImGui.setCursorPos(10, 5); // Alineado arriba dentro de la mini-ventana

        // Botón Superficies
        if (ImGui.button(I18n.INSTANCE.get("toolbar.surface"), 110, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FSurfaceEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(surfaceEditor);
            } else {
                ImGUISystem.INSTANCE.show(surfaceEditor);
            }
        }

        ImGui.sameLine();

        // Botón Bloqueos
        if (ImGui.button(I18n.INSTANCE.get("menu.view.blocks"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FBlockEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(blockEditor);
            } else {
                ImGUISystem.INSTANCE.show(blockEditor);
            }
        }

        ImGui.sameLine();

        // Botón Triggers
        if (ImGui.button(I18n.INSTANCE.get("menu.view.triggers"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FTriggerEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(triggerEditor);
            } else {
                ImGUISystem.INSTANCE.show(triggerEditor);
            }
        }

        ImGui.sameLine();

        // Botón NPCs
        if (ImGui.button(I18n.INSTANCE.get("menu.view.npcs"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FNpcEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(npcEditor);
            } else {
                ImGUISystem.INSTANCE.show(npcEditor);
            }
        }

        ImGui.sameLine();

        // Botón Objetos
        if (ImGui.button(I18n.INSTANCE.get("menu.view.objects"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FObjEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(objEditor);
            } else {
                ImGUISystem.INSTANCE.show(objEditor);
            }
        }

        ImGui.sameLine();

        // Botón Traslados
        if (ImGui.button(I18n.INSTANCE.get("menu.view.transfers"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FTransferEditor")) {
                ImGUISystem.INSTANCE.deleteFrmArray(transferEditor);
            } else {
                ImGUISystem.INSTANCE.show(transferEditor);
            }
        }

        ImGui.sameLine();

        // Botón Minimapa
        if (ImGui.button(I18n.INSTANCE.get("menu.view.minimap"), 100, 25)) {
            if (ImGUISystem.INSTANCE.isFormVisible("FMinimap")) {
                ImGUISystem.INSTANCE.deleteFrmArray(minimap);
            } else {
                ImGUISystem.INSTANCE.show(minimap);
            }
        }

        ImGui.sameLine();

        // Botón Selección
        boolean selectionActive = Selection.getInstance().isActive();
        if (selectionActive) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
        }
        if (ImGui.button(I18n.INSTANCE.get("common.selection"), 100, 25)) {
            Selection sel = Selection.getInstance();
            sel.setActive(!sel.isActive());

            if (sel.isActive()) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOn"), REGULAR,
                        new RGBColor(0f, 1f, 0f));
                // Desactivar otros modos
                Surface.getInstance().setMode(0);
                Npc.getInstance().setMode(0);
                Obj.getInstance().setMode(0);
                Block.getInstance().setMode(0);
                Trigger.getInstance().setMode(0);
                Transfer.getInstance().setMode(0);
            } else {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("msg.selectionModeOff"), REGULAR,
                        new RGBColor(1f, 1f, 0f));
            }
        }
        if (selectionActive) {
            ImGui.popStyleColor();
        }

    }

    private void drawMenuBar() {

        if (ImGui.beginMainMenuBar()) {

            RenderSettings renderSettings = options.getRenderSettings();

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file"))) {

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.new"))) {
                    this.newMap();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.open"))) {
                    File startDir = getLastMapDir();
                    openMapPicker = new ImGuiFilePicker(startDir, "map");
                    openMapPicker.open();
                }

//                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.open"))) {
//                    this.loadMapAction();
//                }

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

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.save"))) {
                    String defaultName = org.argentumforge.engine.utils.GameData
                            .getActiveContext()
                            .getMapName();

                    saveMapPicker = new ImGuiFolderPicker(
                            getLastMapDir(),
                            defaultName,
                            "map"
                    );
                    saveMapPicker.open();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.export"))) {
                    String defaultName = org.argentumforge.engine.utils.GameData
                            .getActiveContext()
                            .getMapName();

                    exportMapPicker = new ImGuiFolderPicker(
                            getLastMapDir(),
                            defaultName,
                            "png"
                    );
                    exportMapPicker.open();
                }


                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("options.title"))) {
                    ImGUISystem.INSTANCE.show(new FOptions());
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
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.properties"))) {
                    ImGUISystem.INSTANCE.show(new FInfoMap());
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.validate"))) {
                    if (org.argentumforge.engine.utils.GameData.getActiveContext() != null) {
                        ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FMapValidator());
                    } else {
                        ImGUISystem.INSTANCE.show(new FMessage(I18n.INSTANCE.get("msg.noActiveMap")));
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
                    ImGUISystem.INSTANCE.show(new FMessage("Próximamente..."));
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.tools"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.tools.setupWizard"))) {
                    FSetupWizard wizard = new FSetupWizard(() -> {
                        // Al finalizar, recargar recursos si cambiaron las rutas
                        org.argentumforge.engine.utils.GameData.init();
                    }, false);
                    ImGUISystem.INSTANCE.show(wizard);
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.tools.generateColors"))) {
                    showGenerateColorsPopup = true;
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

            ImGui.endMainMenuBar();
        }

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

    private void drawGenerateColorsPopup() {
        if (showGenerateColorsPopup) {
            ImGui.openPopup(I18n.INSTANCE.get("menu.tools.generateColors"));
            showGenerateColorsPopup = false;
        }

        if (ImGui.beginPopupModal(
                I18n.INSTANCE.get("menu.tools.generateColors"),
                ImGuiWindowFlags.AlwaysAutoResize)) {

            ImGui.textWrapped(I18n.INSTANCE.get("msg.generateColorsConfirm"));
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("common.yes"), 120, 0)) {
                MinimapColorGenerator.generateBinary();
                ImGui.closeCurrentPopup();
            }

            ImGui.sameLine();

            if (ImGui.button(I18n.INSTANCE.get("common.no"), 120, 0)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

}
