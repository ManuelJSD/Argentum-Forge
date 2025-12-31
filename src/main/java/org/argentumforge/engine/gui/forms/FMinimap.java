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
    private final boolean[] visibleLayers = { true, false, false, false };

    public FMinimap() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(MINIMAP_SIZE + 20, MINIMAP_SIZE + 80, ImGuiCond.Always);
        if (ImGui.begin("Minimapa", ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse)) {

            // Capas selector
            if (ImGui.beginMenuBar()) {
                if (ImGui.beginMenu("Capas")) {
                    for (int i = 0; i < 4; i++) {
                        if (ImGui.menuItem("Capa " + (i + 1), "", visibleLayers[i])) {
                            visibleLayers[i] = !visibleLayers[i];
                        }
                    }
                    ImGui.endMenu();
                }
                ImGui.endMenuBar();
            }

            // O simplemente botones para mayor comodidad si no queremos menu bar
            // (ImGuiWindowFlags.MenuBar)
            ImGui.text("Ver capas:");
            for (int i = 0; i < 4; i++) {
                if (i > 0)
                    ImGui.sameLine();
                boolean active = visibleLayers[i];
                if (active)
                    ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0xFF00FF00);
                if (ImGui.button(String.valueOf(i + 1), 40, 20)) {
                    visibleLayers[i] = !visibleLayers[i];
                }
                if (active)
                    ImGui.popStyleColor();
            }

            ImGui.separator();

            float mouseX = ImGui.getMousePosX();
            float mouseY = ImGui.getMousePosY();
            float windowX = ImGui.getWindowPosX();
            float windowY = ImGui.getWindowPosY();
            float contentX = windowX + 10;
            float contentY = windowY + 70; // Ajustado por el selector de capas

            ImDrawList drawList = ImGui.getWindowDrawList();

            // Dibujar fondo del mapa
            drawList.addRectFilled(contentX, contentY, contentX + MINIMAP_SIZE, contentY + MINIMAP_SIZE,
                    ImGui.getColorU32(0.1f, 0.1f, 0.1f, 1.0f));

            if (GameData.mapData != null) {
                for (int y = 1; y <= 100; y++) {
                    for (int x = 1; x <= 100; x++) {
                        // Dibujar capas seleccionadas
                        for (int layer = 1; layer <= 4; layer++) {
                            if (!visibleLayers[layer - 1])
                                continue;

                            int grh = GameData.mapData[x][y].getLayer(layer).getGrhIndex();
                            if (grh > 0) {
                                int color = getTileColor(grh);
                                drawList.addRectFilled(
                                        contentX + (x - 1) * TILE_SIZE,
                                        contentY + (y - 1) * TILE_SIZE,
                                        contentX + x * TILE_SIZE,
                                        contentY + y * TILE_SIZE,
                                        color);
                            }
                        }

                        // Bloqueos (siempre en la parte superior si está en capa 1?)
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
        // Primero intentamos usar MiniMap.dat
        if (GameData.minimapColors.containsKey(grh)) {
            return GameData.minimapColors.get(grh);
        }

        // Si es animado, probamos con el primer frame para MiniMap.dat también
        if (GameData.grhData[grh].getNumFrames() > 1) {
            int firstFrame = GameData.grhData[grh].getFrame(0);
            if (GameData.minimapColors.containsKey(firstFrame)) {
                return GameData.minimapColors.get(firstFrame);
            }
            grh = firstFrame;
        }

        // Heurística de colores típica de Argentum Online
        // Pasto / Llanura
        if (grh <= 600 || (grh >= 1000 && grh <= 1100))
            return ImGui.getColorU32(0.14f, 0.45f, 0.05f, 1.0f); // Verde oscuro
        if (grh >= 601 && grh <= 1000)
            return ImGui.getColorU32(0.20f, 0.55f, 0.10f, 1.0f); // Verde claro

        // Agua / Mar / Ríos
        if ((grh >= 1500 && grh <= 1650) || (grh >= 5665 && grh <= 5680) || (grh >= 13547 && grh <= 13562))
            return ImGui.getColorU32(0.05f, 0.15f, 0.60f, 1.0f); // Azul profundo

        // Arena / Desierto
        if (grh >= 3500 && grh <= 3800)
            return ImGui.getColorU32(0.85f, 0.75f, 0.45f, 1.0f); // Arena clara

        // Nieve / Hielo
        if (grh >= 4000 && grh <= 4300)
            return ImGui.getColorU32(0.90f, 0.95f, 1.0f, 1.0f); // Blanco/Cian mudo

        // Lava / Infierno
        if (grh >= 5800 && grh <= 5900)
            return ImGui.getColorU32(0.80f, 0.10f, 0.0f, 1.0f); // Rojo lava

        // Dungeon / Cueva / Piedra
        if (grh >= 5000 && grh <= 5500)
            return ImGui.getColorU32(0.30f, 0.30f, 0.35f, 1.0f); // Gris piedra

        // Bosque denso
        if (grh >= 10000 && grh <= 10500)
            return ImGui.getColorU32(0.05f, 0.30f, 0.05f, 1.0f); // Verde bosque

        // Color por defecto (verde intermedio)
        return ImGui.getColorU32(0.20f, 0.50f, 0.20f, 1.0f);
    }
}
