package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.ImDrawList;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.game.Options;

/**
 * Formulario que muestra un mapa en miniatura (minimapa) del escenario actual.
 * 
 * Renderiza las capas de gráficos, bloqueos y la posición del usuario.
 * Permite la navegación rápida haciendo clic sobre el minimapa.
 */
public final class FMinimap extends Form {

    private static final int MINIMAP_SIZE = 200; // 2 pixels per tile (100x100)
    private static final int TILE_SIZE = 2;

    public FMinimap() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(MINIMAP_SIZE + 20, MINIMAP_SIZE + 40, ImGuiCond.Always);
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

            // Warning si no hay colores generados (o minimap.bin no existe en root)
            boolean binExists = java.nio.file.Files.exists(java.nio.file.Path.of("minimap.bin"));

            if (!binExists || AssetRegistry.minimapColors.isEmpty()) {
                ImGui.setCursorPos(20, 150);
                ImGui.textColored(ImGui.getColorU32(1.0f, 0.0f, 0.0f, 1.0f), "¡Colores no generados!");
                ImGui.setCursorPos(20, 170);
                if (ImGui.button("Generar ahora")) {
                    org.argentumforge.engine.utils.editor.MinimapColorGenerator.generateBinary();
                }
            }

            if (GameData.mapData != null) {
                for (int y = 1; y <= 100; y++) {
                    for (int x = 1; x <= 100; x++) {
                        // Dibujar capas seleccionadas
                        for (int layer = 1; layer <= 4; layer++) {
                            if (!Options.INSTANCE.getRenderSettings().getMinimapLayers()[layer - 1])
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
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapBlocks()
                                && GameData.mapData[x][y].getBlocked()) {
                            drawList.addRectFilled(
                                    contentX + (x - 1) * TILE_SIZE,
                                    contentY + (y - 1) * TILE_SIZE,
                                    contentX + x * TILE_SIZE,
                                    contentY + y * TILE_SIZE,
                                    ImGui.getColorU32(1.0f, 0.0f, 0.0f, 0.3f));
                        }

                        // Renderizar Traslados (Exits) - Azul
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapExits()
                                && GameData.mapData[x][y].getExitMap() > 0) {
                            drawList.addRectFilled(
                                    contentX + (x - 1) * TILE_SIZE,
                                    contentY + (y - 1) * TILE_SIZE,
                                    contentX + x * TILE_SIZE,
                                    contentY + y * TILE_SIZE,
                                    ImGui.getColorU32(0.0f, 0.0f, 1.0f, 0.8f));
                        }

                        // Renderizar Triggers - Violeta
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapTriggers()
                                && GameData.mapData[x][y].getTrigger() > 0) {
                            drawList.addRectFilled(
                                    contentX + (x - 1) * TILE_SIZE,
                                    contentY + (y - 1) * TILE_SIZE,
                                    contentX + x * TILE_SIZE,
                                    contentY + y * TILE_SIZE,
                                    ImGui.getColorU32(0.6f, 0.0f, 0.8f, 0.6f));
                        }

                        // Renderizar NPCs - Amarillo
                        // Usamos charIndex del mapa
                        int charIndex = GameData.mapData[x][y].getCharIndex();
                        if (Options.INSTANCE.getRenderSettings().isShowMinimapNPCs() && charIndex > 0) {
                            // Podríamos verificar si el char está activo o es usuario,
                            // pero charIndex en mapa suele estar sincronizado.
                            // El usuario se dibuja aparte, así que filtramos si es el propio usuario?
                            // No, mapData.getCharIndex() suele tener al user también.
                            // Dibujamos todos los chars como puntos amarillos pequeños.
                            drawList.addCircleFilled(
                                    contentX + (x - 1) * TILE_SIZE + 1,
                                    contentY + (y - 1) * TILE_SIZE + 1,
                                    1.5f,
                                    ImGui.getColorU32(1.0f, 1.0f, 0.0f, 1.0f));
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

            // Tooltip de coordenadas
            if (ImGui.isWindowHovered()) {
                float localX = mouseX - contentX;
                float localY = mouseY - contentY;
                if (localX >= 0 && localX < MINIMAP_SIZE && localY >= 0 && localY < MINIMAP_SIZE) {
                    int hoverX = (int) (localX / TILE_SIZE) + 1;
                    int hoverY = (int) (localY / TILE_SIZE) + 1;

                    if (hoverX >= 1 && hoverX <= 100 && hoverY >= 1 && hoverY <= 100) {
                        ImGui.setTooltip("X: " + hoverX + ", Y: " + hoverY);
                    }
                }
            }

            ImGui.end();
        }
    }

    private int getTileColor(int grh) {
        // Primero intentamos usar los colores definidos en MiniMap.dat
        if (AssetRegistry.minimapColors.containsKey(grh)) {
            return AssetRegistry.minimapColors.get(grh);
        }

        // Si es animado, probamos con el color del primer frame
        if (AssetRegistry.grhData[grh].getNumFrames() > 1) {
            int firstFrame = AssetRegistry.grhData[grh].getFrame(0);
            if (AssetRegistry.minimapColors.containsKey(firstFrame)) {
                return AssetRegistry.minimapColors.get(firstFrame);
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
