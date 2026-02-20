package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.gui.FileDialog;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.*;
import org.argentumforge.engine.gui.forms.FPasteAdvanced;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.MapFileUtils;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.utils.editor.Block;
import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.MinimapColorGenerator;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.MapExporter;

import org.argentumforge.engine.Engine;
import org.argentumforge.engine.game.EditorController;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.utils.editor.Surface;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Componente para renderizar la barra de menú superior.
 */
public class MainMenuBar {

    private final FMain parent;

    public MainMenuBar(FMain parent) {
        this.parent = parent;
    }

    private String getKeyName(int key) {
        int scancode = GLFW.glfwGetKeyScancode(key);
        String keyName = GLFW.glfwGetKeyName(key, scancode);

        if (keyName == null) {
            return switch (key) {
                case GLFW.GLFW_KEY_F1 -> "F1";
                case GLFW.GLFW_KEY_F2 -> "F2";
                case GLFW.GLFW_KEY_F3 -> "F3";
                case GLFW.GLFW_KEY_F4 -> "F4";
                case GLFW.GLFW_KEY_F5 -> "F5";
                case GLFW.GLFW_KEY_F6 -> "F6";
                case GLFW.GLFW_KEY_F7 -> "F7";
                case GLFW.GLFW_KEY_F8 -> "F8";
                case GLFW.GLFW_KEY_F9 -> "F9";
                case GLFW.GLFW_KEY_F10 -> "F10";
                case GLFW.GLFW_KEY_F11 -> "F11";
                case GLFW.GLFW_KEY_F12 -> "F12";
                default -> "KEY " + key;
            };
        }
        return keyName.toUpperCase();
    }

    public void render() {
        boolean openBlockBorders = false;
        boolean openBlockAll = false;
        boolean openClearBorders = false;
        boolean openClearAll = false;
        boolean openFillLayer = false;
        boolean openClearLayer = false;
        boolean openFillBorders = false;
        boolean openClearBordersSurface = false;

        if (ImGui.beginMainMenuBar()) {

            RenderSettings renderSettings = GameData.options.getRenderSettings();

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file"))) {
                // ... (lines 68-132)
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.new"))) {
                    EditorController.INSTANCE.newMap();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.open"))) {
                    EditorController.INSTANCE.loadMapAction();
                }

                String reloadKey = getKeyName(Key.RELOAD_MAP.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.reload"), reloadKey, false,
                        GameData.getActiveContext() != null)) {
                    EditorController.INSTANCE.reloadMap();
                }

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file.recent"))) {
                    List<String> recentMaps = GameData.options.getRecentMaps();
                    if (recentMaps.isEmpty()) {
                        ImGui.textDisabled(I18n.INSTANCE.get("menu.file.recent.none"));
                    } else {
                        // Create a copy to avoid ConcurrentModificationException when loading a map
                        // uses this list
                        for (String mapPath : new ArrayList<>(recentMaps)) {
                            if (ImGui.menuItem(mapPath)) {
                                MapManager.loadMapAsync(mapPath, null);
                            }
                        }
                    }
                    ImGui.endMenu();
                }

                ImGui.separator();

                ImGui.separator();

                MapContext context = GameData.getActiveContext();
                boolean isModified = context != null && context.isModified();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.save"), "Ctrl+S", false, isModified)) {
                    MapFileUtils.quickSaveMap();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.saveAs"), "Ctrl+Shift+S", false, context != null)) {
                    MapFileUtils.saveMapAs();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("options.title"))) {
                    ImGUISystem.INSTANCE.show(new FOptions());
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.changeProfile"))) {
                    Engine.INSTANCE.requestProfileChange();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.exit"))) {
                    Engine.closeClient();
                }

                ImGui.endMenu();
            }

            // Edicion
            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.edit"))) {
                CommandManager manager = CommandManager.getInstance();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.undo"), "Ctrl+Z", false, manager.canUndo())) {
                    manager.undo();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.redo"), "Ctrl+Y", false, manager.canRedo())) {
                    manager.redo();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("history.title"))) {
                    ImGUISystem.INSTANCE.show(FHistory.getInstance());
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.cut"), "Ctrl+X", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    EditorController.INSTANCE.cutSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.copy"), "Ctrl+C", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    EditorController.INSTANCE.copySelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.paste"), "Ctrl+V", false,
                        !Clipboard.getInstance().isEmpty())) {
                    EditorController.INSTANCE.pasteSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.pasteAdvanced"), "Ctrl+Shift+V", false,
                        !Clipboard.getInstance().isEmpty())) {
                    ImGUISystem.INSTANCE.show(new FPasteAdvanced());
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.delete"), "Supr", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    EditorController.INSTANCE.deleteSelection();
                }

                ImGui.separator();

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.edit.insertDelete"))) {

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.block.blockBorders"))) {
                        openBlockBorders = true;
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.block.clearBorders"))) {
                        openClearBorders = true;
                    }

                    ImGui.separator();

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.block.blockAll"))) {
                        openBlockAll = true;
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.block.clearAll"))) {
                        openClearAll = true;
                    }

                    ImGui.separator();

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.surface.fillBorders"))) {
                        openFillBorders = true;
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.surface.clearBorders"))) {
                        openClearBordersSurface = true;
                    }

                    ImGui.separator();

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.surface.fillLayer"))) {
                        openFillLayer = true;
                    }

                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.surface.clearLayer"))) {
                        openClearLayer = true;
                    }

                    ImGui.endMenu();
                }

                ImGui.endMenu();
            }

            // ... (rest of menu)
            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.map"))) {
                // ...
                String propsKey = getKeyName(Key.MAP_PROPERTIES.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.properties"), propsKey)) {
                    ImGUISystem.INSTANCE.show(new FInfoMap());
                }
                // ...
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.validate"))) {
                    if (GameData.getActiveContext() != null) {
                        ImGUISystem.INSTANCE.show(new FMapValidator());
                    } else {
                        DialogManager.getInstance().showInfo(I18n.INSTANCE.get("menu.map"),
                                I18n.INSTANCE.get("msg.noActiveMap"));
                    }
                }

                ImGui.separator();

                String gotoKey = getKeyName(Key.GOTO_POS.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.goto"), gotoKey)) {
                    ImGUISystem.INSTANCE.show(new FGoTo());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view"))) {
                // ... (shortened for brevity, not changing)
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.resetZoom"), "Ctrl+0")) {
                    Camera.setTileSize(32);
                }
                // ...
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.guidesConfig"))) {
                    ImGUISystem.INSTANCE.show(new FOptions());
                }
                ImGui.separator();
                // ... (layers, blocks, etc)
                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.layers"))) {
                    // ...
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer1"), "", renderSettings.getShowLayer()[0])) {
                        renderSettings.getShowLayer()[0] = !renderSettings.getShowLayer()[0];
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer2"), "", renderSettings.getShowLayer()[1])) {
                        renderSettings.getShowLayer()[1] = !renderSettings.getShowLayer()[1];
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer3"), "", renderSettings.getShowLayer()[2])) {
                        renderSettings.getShowLayer()[2] = !renderSettings.getShowLayer()[2];
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer4"), "", renderSettings.getShowLayer()[3])) {
                        renderSettings.getShowLayer()[3] = !renderSettings.getShowLayer()[3];
                        GameData.options.save();
                    }
                    ImGui.endMenu();
                }
                // ...
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.blocks"), "", renderSettings.getShowBlock())) {
                    renderSettings.setShowBlock(!renderSettings.getShowBlock());
                    GameData.options.save();
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.objects"), "", renderSettings.getShowOJBs())) {
                    renderSettings.setShowOJBs(!renderSettings.getShowOJBs());
                    GameData.options.save();
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.npcs"), "", renderSettings.getShowNPCs())) {
                    renderSettings.setShowNPCs(!renderSettings.getShowNPCs());
                    GameData.options.save();
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.transfers"), "", renderSettings.getShowMapTransfer())) {
                    renderSettings.setShowMapTransfer(!renderSettings.getShowMapTransfer());
                    GameData.options.save();
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.triggers"), "", renderSettings.getShowTriggers())) {
                    renderSettings.setShowTriggers(!renderSettings.getShowTriggers());
                    GameData.options.save();
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.particles"), "", renderSettings.getShowParticles())) {
                    renderSettings.setShowParticles(!renderSettings.getShowParticles());
                    GameData.options.save();
                }
                ImGui.separator();
                // ...
                String gridKey = getKeyName(Key.TOGGLE_GRID.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.grid"), gridKey, renderSettings.isShowGrid())) {
                    renderSettings.setShowGrid(!renderSettings.isShowGrid());
                    GameData.options.save();
                }
                String viewportKey = getKeyName(Key.TOGGLE_VIEWPORT.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("options.viewport"), viewportKey,
                        renderSettings.isShowViewportOverlay())) {
                    renderSettings.setShowViewportOverlay(!renderSettings.isShowViewportOverlay());
                    GameData.options.save();
                }
                ImGui.separator();
                // ...
                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.minimap"))) {
                    // ...
                    if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.layers"))) {
                        for (int i = 0; i < 4; i++) {
                            if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.layer") + " " + (i + 1), "",
                                    renderSettings.getMinimapLayers()[i])) {
                                renderSettings.getMinimapLayers()[i] = !renderSettings.getMinimapLayers()[i];
                                GameData.options.save();
                            }
                        }
                        ImGui.endMenu();
                    }
                    ImGui.separator();
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.npcs"), "",
                            renderSettings.isShowMinimapNPCs())) {
                        renderSettings.setShowMinimapNPCs(!renderSettings.isShowMinimapNPCs());
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.exits"), "",
                            renderSettings.isShowMinimapExits())) {
                        renderSettings.setShowMinimapExits(!renderSettings.isShowMinimapExits());
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.triggers"), "",
                            renderSettings.isShowMinimapTriggers())) {
                        renderSettings.setShowMinimapTriggers(!renderSettings.isShowMinimapTriggers());
                        GameData.options.save();
                    }
                    if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.minimap.blocks"), "",
                            renderSettings.isShowMinimapBlocks())) {
                        renderSettings.setShowMinimapBlocks(!renderSettings.isShowMinimapBlocks());
                        GameData.options.save();
                    }
                    ImGui.endMenu();
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.tools"))) {
                // ...
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.export"))) {
                    String selectedFile = FileDialog.showSaveDialog(
                            I18n.INSTANCE.get("dialog.export.title"),
                            new File(".").getAbsolutePath() + File.separator + "mapa.png",
                            I18n.INSTANCE.get("dialog.export.png"),
                            "*.png");

                    if (selectedFile != null) {
                        String path = selectedFile;
                        if (!path.toLowerCase().endsWith(".png")) {
                            path += ".png";
                        }
                        MapExporter.exportMap(path);
                        DialogManager.getInstance().showInfo(I18n.INSTANCE.get("msg.export.title"),
                                I18n.INSTANCE.get("msg.export.success") + path);
                    }
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.tools.generateColors"))) {
                    DialogManager.getInstance().showConfirm(
                            I18n.INSTANCE.get("menu.tools.generateColors"),
                            I18n.INSTANCE.get("msg.generateColorsConfirm"),
                            MinimapColorGenerator::generateBinary,
                            null);
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("editor.area.title"))) {
                    ImGUISystem.INSTANCE.show(new FAreaOperations());
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("optimizer.title"))) {
                    ImGUISystem.INSTANCE.show(new FOptimizer());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.misc"))) {
                // ...
                String photoKey = getKeyName(Key.TOGGLE_PHOTO_MODE.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.photoMode"), photoKey)) {
                    GameData.options.getRenderSettings().setPhotoModeActive(
                            !GameData.options.getRenderSettings().isPhotoModeActive());
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.reloadGraphics"))) {
                    org.argentumforge.engine.renderer.Surface.INSTANCE.retryFailedTextures();
                    DialogManager.getInstance().showInfo(I18n.INSTANCE.get("msg.graphics.title"),
                            I18n.INSTANCE.get("msg.graphics.reloadRequested"));
                }
                ImGui.separator();
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.misc.walkMode"), "",
                        User.INSTANCE.isWalkingmode())) {
                    User.INSTANCE
                            .setWalkingmode(!User.INSTANCE.isWalkingmode());
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("options.moveSpeed"))) {
                    if (ImGUISystem.INSTANCE.isFormVisible("FSpeedControl")) {
                        ImGUISystem.INSTANCE.deleteFrmArray(parent.getSpeedControl());
                    } else {
                        ImGUISystem.INSTANCE.show(parent.getSpeedControl());
                    }
                }
                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.misc.ambient"))) {
                    float[] ambientColorArr = parent.getAmbientColorArr();
                    if (ImGui.colorEdit3(I18n.INSTANCE.get("menu.misc.ambient.color"), ambientColorArr)) {
                        Weather.INSTANCE.setAmbientColor(ambientColorArr[0],
                                ambientColorArr[1],
                                ambientColorArr[2]);
                    }
                    ImGui.endMenu();
                }
                ImGui.endMenu();
            }
            // ...
            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.help"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.help.commands"))) {
                    ImGUISystem.INSTANCE.show(new FHelpCommands());
                }
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.help.about"))) {
                    ImGUISystem.INSTANCE.show(new FAbout());
                }
                ImGui.endMenu();
            }

            ImGui.endMainMenuBar();
        }

        Block block = Block.getInstance();

        // --- Modificador de Estado para Popups ---
        if (openBlockBorders)
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.blockBorders.title"));
        if (openBlockAll)
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.blockAll.title"));
        if (openClearBorders)
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.clearBorders.title"));
        if (openClearAll)
            ImGui.openPopup(I18n.INSTANCE.get("editor.block.confirm.clearAll.title"));
        if (openFillLayer)
            ImGui.openPopup(I18n.INSTANCE.get("editor.surface.confirm.fillLayer.title"));
        if (openClearLayer)
            ImGui.openPopup(I18n.INSTANCE.get("editor.surface.confirm.clearLayer.title"));
        if (openFillBorders)
            ImGui.openPopup(I18n.INSTANCE.get("editor.surface.confirm.fillBorders.title"));
        if (openClearBordersSurface)
            ImGui.openPopup(I18n.INSTANCE.get("editor.surface.confirm.clearBorders.title"));

        // --- Modales de Confirmación ---
        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.blockBorders.title"),
                I18n.INSTANCE.get("editor.block.blockBorders"),
                I18n.INSTANCE.get("editor.block.confirm.blockBorders.msg"),
                () -> block.blockBorders(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.blockAll.title"),
                I18n.INSTANCE.get("editor.block.blockAll"),
                I18n.INSTANCE.get("editor.block.confirm.blockAll.msg"),
                () -> block.blockAll(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.clearBorders.title"),
                I18n.INSTANCE.get("editor.block.clearBorders"),
                I18n.INSTANCE.get("editor.block.confirm.clearBorders.msg"),
                () -> block.unblockBorders(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.block.confirm.clearAll.title"),
                I18n.INSTANCE.get("editor.block.clearAll"),
                I18n.INSTANCE.get("editor.block.confirm.clearAll.msg"),
                () -> block.unblockAll(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.surface.confirm.fillLayer.title"),
                I18n.INSTANCE.get("editor.surface.fillLayer"),
                I18n.INSTANCE.get("editor.surface.confirm.fillLayer.msg"),
                () -> Surface.getInstance()
                        .fillLayer(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.surface.confirm.clearLayer.title"),
                I18n.INSTANCE.get("editor.surface.clearLayer"),
                I18n.INSTANCE.get("editor.surface.confirm.clearLayer.msg"),
                () -> Surface.getInstance()
                        .clearLayer(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.surface.confirm.fillBorders.title"),
                I18n.INSTANCE.get("editor.surface.fillBorders"),
                I18n.INSTANCE.get("editor.surface.confirm.fillBorders.msg"),
                () -> Surface.getInstance()
                        .fillBorders(GameData.getActiveContext()));

        UIComponents.confirmDialog(
                I18n.INSTANCE.get("editor.surface.confirm.clearBorders.title"),
                I18n.INSTANCE.get("editor.surface.clearBorders"),
                I18n.INSTANCE.get("editor.surface.confirm.clearBorders.msg"),
                () -> Surface.getInstance()
                        .clearBorders(GameData.getActiveContext()));
    }
}
