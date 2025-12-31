package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.GameData;

public final class FMinimap extends Form {

    private static final int MINIMAP_SIZE = 200; // 2 pixels per tile (100x100)
    private static final int TILE_SIZE = 2;

    public FMinimap() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(MINIMAP_SIZE + 20, MINIMAP_SIZE + 40, ImGuiCond.FirstUseEver);
        if (ImGui.begin("Minimapa", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse)) {

            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float windowX = ImGui.getWindowPosX();
            float windowY = ImGui.getWindowPosY();
            float contentX = windowX + 10;
            float contentY = windowY + 30;

            ImDrawList drawList = ImGui.getWindowDrawList();

            // Dibujar fondo del mapa
            drawList.addRectFilled(contentX, contentY, contentX + MINIMAP_SIZE, contentY + MINIMAP_SIZE,
                    ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1.0f));

            if (GameData.mapData != null) {
                for (int y = 1; y <= 100; y++) {
                    for (int x = 1; x <= 100; x++) {
                        int grh = GameData.mapData[x][y].getLayer(1).getGrhIndex();
                        if (grh > 0) {
                            int color = getTileColor(grh);
                            drawList.addRectFilled(
                                    contentX + (x - 1) * TILE_SIZE,
                                    contentY + (y - 1) * TILE_SIZE,
                                    contentX + x * TILE_SIZE,
                                    contentY + y * TILE_SIZE,
                                    color);
                        }

                        // Bloqueos (opcional, en rojo tenue)
                        if (GameData.mapData[x][y].getBlocked()) {
                            drawList.addRectFilled(
                                    contentX + (x - 1) * TILE_SIZE,
                                    contentY + (y - 1) * TILE_SIZE,
                                    contentX + x * TILE_SIZE,
                                    contentY + y * TILE_SIZE,
                                    ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.3f));
                        }
                    }
                }
            }

            // Dibujar posicion del usuario
            int userX = User.INSTANCE.getUserPos().getX();
            int userY = User.INSTANCE.getUserPos().getY();
            drawList.addRect(
                    contentX + (userX - 1) * TILE_SIZE - 2,
                    contentY + (userY - 1) * TILE_SIZE - 2,
                    contentX + (userX - 1) * TILE_SIZE + 3,
                    contentY + (userY - 1) * TILE_SIZE + 3,
                    ImGui.getColorU32(1.0f, 1.0f, 1.0f, 1.0f));

            // Teletransporte al hacer click
            if (ImGui.isWindowHovered() && ImGui.isMouseDown(0)) {
                float localX = mouseX - contentX;
                float localY = mouseY - contentY;
                if (localX >= 0 && localX < MINIMAP_SIZE && localY >= 0 && localY < MINIMAP_SIZE) {
                    int targetX = (int) (localX / TILE_SIZE) + 1;
                    int targetY = (int) (localY / TILE_SIZE) + 1;

                    // Limitar a bordes legales para evitar que la cámara se salga
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

            ImGui.end();
        }
    }

    private int getTileColor(int grh) {
        // Heurística básica de colores para AO
        // Estos son ejemplos típicos, pueden variar según el servidor
        if (grh >= 1 && grh <= 100)
            return ImGui.getColorU32(0.1f, 0.6f, 0.1f, 1.0f); // Pasto
        if (grh >= 1500 && grh <= 1600)
            return ImGui.getColorU32(0.0f, 0.4f, 0.8f, 1.0f); // Agua
        if (grh >= 500 && grh <= 600)
            return ImGui.getColorU32(0.8f, 0.7f, 0.4f, 1.0f); // Arena

        // Color por defecto (verde suave)
        return ImGui.getColorU32(0.2f, 0.5f, 0.2f, 1.0f);
    }
}
