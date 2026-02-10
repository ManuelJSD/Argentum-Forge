package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;

/**
 * Formulario que muestra un mapa en miniatura (minimapa) del escenario actual.
 * 
 * Renderiza las capas de gráficos, bloqueos y la posición del usuario.
 * Permite la navegación rápida haciendo clic sobre el minimapa.
 */
public final class FMinimap extends Form {

    private static final int MINIMAP_SIZE = 200; // 2 píxeles por tile (100x100)
    private static final int TILE_SIZE = 2;

    public FMinimap() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(MINIMAP_SIZE + 20, MINIMAP_SIZE + 40, ImGuiCond.FirstUseEver);

        if (ImGui.begin(I18n.INSTANCE.get("minimap.title"), ImGuiWindowFlags.None)) {

            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float contentX = ImGui.getWindowPosX() + 10;
            float contentY = ImGui.getWindowPosY() + 30;

            ImDrawList drawList = ImGui.getWindowDrawList();

            // Fondo Negro base
            drawList.addRectFilled(contentX, contentY, contentX + MINIMAP_SIZE, contentY + MINIMAP_SIZE,
                    ImGui.getColorU32(0.0f, 0.0f, 0.0f, 1.0f));

            org.argentumforge.engine.utils.MapContext context = GameData.getActiveContext();
            if (context != null && context.getMapData() != null) {
                var mapData = context.getMapData();

                for (int y = 1; y <= 100; y++) {
                    for (int x = 1; x <= 100; x++) {
                        float tX = contentX + (x - 1) * TILE_SIZE;
                        float tY = contentY + (y - 1) * TILE_SIZE;

                        // Capa 1 (Suelo)
                        if (mapData[x][y].getLayer(1).getGrhIndex() > 0) {
                            int grhIndex = mapData[x][y].getLayer(1).getGrhIndex();
                            if (AssetRegistry.minimapColors.containsKey(grhIndex)) {
                                int color = AssetRegistry.minimapColors.get(grhIndex);
                                drawList.addRectFilled(tX, tY, tX + TILE_SIZE, tY + TILE_SIZE, color);
                            }
                        }

                        // Bloqueos
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapBlocks() && mapData[x][y].getBlocked()) {
                            drawList.addRectFilled(tX, tY, tX + TILE_SIZE, tY + TILE_SIZE,
                                    ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.3f));
                        }

                        // Exits
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapExits()
                                && mapData[x][y].getExitMap() > 0) {
                            drawList.addRectFilled(tX, tY, tX + TILE_SIZE, tY + TILE_SIZE,
                                    ImGui.getColorU32(0.0f, 0.0f, 1.0f, 0.8f));
                        }

                        // Triggers
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapTriggers()
                                && mapData[x][y].getTrigger() > 0) {
                            drawList.addRectFilled(tX, tY, tX + TILE_SIZE, tY + TILE_SIZE,
                                    ImGui.getColorU32(0.6f, 0.0f, 0.8f, 0.6f));
                        }

                        // NPCs
                        int charIndex = mapData[x][y].getCharIndex();
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapNPCs() && charIndex > 0) {
                            drawList.addCircleFilled(tX + TILE_SIZE / 2, tY + TILE_SIZE / 2, 1.5f,
                                    ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f));
                        }
                    }
                }
            }

            // User Pos
            int userX = User.INSTANCE.getUserPos().getX();
            int userY = User.INSTANCE.getUserPos().getY();
            // Draw a slightly larger rect/box for user
            drawList.addRect(contentX + (userX - 1) * TILE_SIZE - 1, contentY + (userY - 1) * TILE_SIZE - 1,
                    contentX + (userX - 1) * TILE_SIZE + TILE_SIZE + 1,
                    contentY + (userY - 1) * TILE_SIZE + TILE_SIZE + 1,
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f));

            // Teleport Click
            if (ImGui.isWindowHovered() && ImGui.isMouseClicked(0)) {
                float localX = mouseX - contentX;
                float localY = mouseY - contentY;
                if (localX >= 0 && localX < MINIMAP_SIZE && localY >= 0 && localY < MINIMAP_SIZE) {
                    int targetX = (int) (localX / TILE_SIZE) + 1;
                    int targetY = (int) (localY / TILE_SIZE) + 1;
                    // Validity check
                    if (targetX >= 1 && targetX <= 100 && targetY >= 1 && targetY <= 100) {
                        // Borders check
                        targetX = Math.max(org.argentumforge.engine.scenes.Camera.minXBorder,
                                Math.min(targetX, org.argentumforge.engine.scenes.Camera.maxXBorder));
                        targetY = Math.max(org.argentumforge.engine.scenes.Camera.minYBorder,
                                Math.min(targetY, org.argentumforge.engine.scenes.Camera.maxYBorder));

                        User.INSTANCE.getUserPos().setX(targetX);
                        User.INSTANCE.getUserPos().setY(targetY);
                        User.INSTANCE.getAddToUserPos().setX(0);
                        User.INSTANCE.getAddToUserPos().setY(0);
                        User.INSTANCE.setUserMoving(false);
                    }
                }
            }

            // Advertencia si no hay colores generados
            String fileName = org.argentumforge.engine.utils.ProfileManager.INSTANCE.getProfilesDir() + "/minimap.bin";
            if (org.argentumforge.engine.utils.ProfileManager.INSTANCE.getCurrentProfile() != null) {
                fileName = org.argentumforge.engine.utils.ProfileManager.INSTANCE.getProfilesDir() + "/minimap_"
                        + org.argentumforge.engine.utils.ProfileManager.INSTANCE.getCurrentProfile().getName() + ".bin";
            }
            boolean binExists = java.nio.file.Files.exists(java.nio.file.Path.of(fileName));

            if (!binExists || AssetRegistry.minimapColors.isEmpty()) {
                ImGui.setCursorPos(20, MINIMAP_SIZE + 10);
                if (ImGui.button(I18n.INSTANCE.get("minimap.generateNow"))) {
                    org.argentumforge.engine.utils.editor.MinimapColorGenerator.generateBinary();
                }
            }

            ImGui.end();
        }
    }
}
