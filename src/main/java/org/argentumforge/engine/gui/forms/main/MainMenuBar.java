package org.argentumforge.engine.gui.forms.main;

import imgui.ImGui;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.gui.FileDialog;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.*;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapFileUtils;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.MinimapColorGenerator;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.MapExporter;

import java.io.File;

/**
 * Componente para renderizar la barra de menú superior.
 */
public class MainMenuBar {

    private final FMain parent;

    public MainMenuBar(FMain parent) {
        this.parent = parent;
    }

    private String getKeyName(int key) {
        int scancode = org.lwjgl.glfw.GLFW.glfwGetKeyScancode(key);
        String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(key, scancode);

        if (keyName == null) {
            return switch (key) {
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F1 -> "F1";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F2 -> "F2";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F3 -> "F3";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F4 -> "F4";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F5 -> "F5";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F6 -> "F6";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F7 -> "F7";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F8 -> "F8";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F9 -> "F9";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F10 -> "F10";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F11 -> "F11";
                case org.lwjgl.glfw.GLFW.GLFW_KEY_F12 -> "F12";
                default -> "KEY " + key;
            };
        }
        return keyName.toUpperCase();
    }

    public void render() {
        if (ImGui.beginMainMenuBar()) {

            RenderSettings renderSettings = GameData.options.getRenderSettings();

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file"))) {

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.new"))) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.newMap();
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.open"))) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.loadMapAction();
                }

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.file.recent"))) {
                    java.util.List<String> recentMaps = GameData.options.getRecentMaps();
                    if (recentMaps.isEmpty()) {
                        ImGui.textDisabled(I18n.INSTANCE.get("menu.file.recent.none"));
                    } else {
                        // Create a copy to avoid ConcurrentModificationException when loading a map
                        // uses this list
                        for (String mapPath : new java.util.ArrayList<>(recentMaps)) {
                            if (ImGui.menuItem(mapPath)) {
                                MapManager.loadMapAsync(mapPath, null);
                            }
                        }
                    }
                    ImGui.endMenu();
                }

                ImGui.separator();

                org.argentumforge.engine.utils.MapContext context = GameData.getActiveContext();
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

                if (ImGui.menuItem("Cambiar Perfil")) {
                    org.argentumforge.engine.Engine.INSTANCE.requestProfileChange();
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

                if (ImGui.menuItem(I18n.INSTANCE.get("history.title"))) {
                    ImGUISystem.INSTANCE.show(FHistory.getInstance());
                }

                ImGui.separator();

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.cut"), "Ctrl+X", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.cutSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.copy"), "Ctrl+C", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.copySelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.paste"), "Ctrl+V", false,
                        !Clipboard.getInstance().isEmpty())) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.pasteSelection();
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.delete"), "Supr", false,
                        !Selection.getInstance().getSelectedEntities().isEmpty())) {
                    org.argentumforge.engine.game.EditorController.INSTANCE.deleteSelection();
                }

                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.map"))) {
                String propsKey = getKeyName(org.argentumforge.engine.game.models.Key.MAP_PROPERTIES.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.properties"), propsKey)) {
                    ImGUISystem.INSTANCE.show(new FInfoMap());
                }

                if (ImGui.menuItem(I18n.INSTANCE.get("menu.map.validate"))) {
                    if (GameData.getActiveContext() != null) {
                        ImGUISystem.INSTANCE.show(new FMapValidator());
                    } else {
                        DialogManager.getInstance().showInfo("Mapa",
                                I18n.INSTANCE.get("msg.noActiveMap"));
                    }
                }

                ImGui.separator();

                String gotoKey = getKeyName(org.argentumforge.engine.game.models.Key.GOTO_POS.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.edit.goto"), gotoKey)) {
                    ImGUISystem.INSTANCE.show(new FGoTo());
                }
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.resetZoom"), "Ctrl+0")) {
                    Camera.setTileSize(32);
                }

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.layers"))) {
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

                String gridKey = getKeyName(org.argentumforge.engine.game.models.Key.TOGGLE_GRID.getKeyCode());
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.view.grid"), gridKey, renderSettings.isShowGrid())) {
                    renderSettings.setShowGrid(!renderSettings.isShowGrid());
                    GameData.options.save();
                }

                ImGui.separator();

                if (ImGui.beginMenu(I18n.INSTANCE.get("menu.view.minimap"))) {
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
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.file.export"))) {
                    String selectedFile = FileDialog.showSaveDialog(
                            "Exportar Mapa como Imagen",
                            new File(".").getAbsolutePath() + File.separator + "mapa.png",
                            "Imagen PNG",
                            "*.png");

                    if (selectedFile != null) {
                        String path = selectedFile;
                        if (!path.toLowerCase().endsWith(".png")) {
                            path += ".png";
                        }
                        MapExporter.exportMap(path);
                        DialogManager.getInstance().showInfo("Exportar Mapa",
                                "Mapa exportado correctamente a:\n" + path);
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
                ImGui.endMenu();
            }

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.misc"))) {

                String photoKey = getKeyName(org.argentumforge.engine.game.models.Key.TOGGLE_PHOTO_MODE.getKeyCode());
                if (ImGui.menuItem("Modo Foto", photoKey)) {
                    GameData.options.getRenderSettings().setPhotoModeActive(
                            !GameData.options.getRenderSettings().isPhotoModeActive());
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

            if (ImGui.beginMenu(I18n.INSTANCE.get("menu.help"))) {
                if (ImGui.menuItem(I18n.INSTANCE.get("menu.help.about"))) {
                    ImGUISystem.INSTANCE.show(new FAbout());
                }
                ImGui.endMenu();
            }
            ImGui.endMainMenuBar();
        }
    }
}
