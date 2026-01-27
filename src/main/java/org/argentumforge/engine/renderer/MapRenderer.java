package org.argentumforge.engine.renderer;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.scenes.Camera;

import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.editor.Selection.SelectedEntity;
import org.argentumforge.engine.game.User;

import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
import static org.argentumforge.engine.renderer.Drawn.drawGrhIndex;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.mapData;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;

/**
 * Encargado de renderizar las capas del mapa y los overlays técnicos.
 * Desacoplado de GameScene para mejorar la mantenibilidad.
 */
public class MapRenderer {

    private final Camera camera;
    private final Weather weather = Weather.INSTANCE;
    private final Selection selection = Selection.getInstance();
    private final User user = User.INSTANCE;

    private float alphaCeiling = 1.0f;

    public MapRenderer(Camera camera) {
        this.camera = camera;
    }

    public void render(int pixelOffsetX, int pixelOffsetY) {
        if (mapData == null)
            return;
        RenderSettings renderSettings = Options.INSTANCE.getRenderSettings();

        renderFirstLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderSecondLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderThirdLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderFourthLayer(renderSettings, pixelOffsetX, pixelOffsetY);
        renderBlockOverlays(renderSettings, pixelOffsetX, pixelOffsetY);
        renderTranslationOverlays(renderSettings, pixelOffsetX, pixelOffsetY);
    }

    private void renderFirstLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowLayer()[0]) {
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y].getLayer(1).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(1),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void renderSecondLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (!renderSettings.getShowLayer()[1] && !renderSettings.getShowOJBs())
            return;

        camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
        for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
            camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
            for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                if (renderSettings.getShowLayer()[1]) {
                    if (mapData[x][y].getLayer(2).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(2),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() == TILE_PIXEL_SIZE &&
                                grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() == TILE_PIXEL_SIZE) {

                            boolean isDragged = false;
                            if (selection.isDragging()) {
                                for (SelectedEntity se : selection.getSelectedEntities()) {
                                    if (se.x == x && se.y == y && se.type == Selection.EntityType.OBJECT) {
                                        isDragged = true;
                                        break;
                                    }
                                }
                            }

                            if (!isDragged) {
                                drawTexture(mapData[x][y].getObjGrh(),
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        true, true, false, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }
                camera.incrementScreenX();
            }
            camera.incrementScreenY();
        }
    }

    private void renderThirdLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
        for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
            camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
            for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() != TILE_PIXEL_SIZE &&
                                grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelHeight() != TILE_PIXEL_SIZE) {

                            boolean isDragged = false;
                            if (selection.isDragging()) {
                                for (SelectedEntity se : selection.getSelectedEntities()) {
                                    if (se.x == x && se.y == y && se.type == Selection.EntityType.OBJECT) {
                                        isDragged = true;
                                        break;
                                    }
                                }
                            }

                            if (!isDragged) {
                                drawTexture(mapData[x][y].getObjGrh(),
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        true, true, false, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }

                if (mapData[x][y].getCharIndex() != 0) {
                    final int charIndex = mapData[x][y].getCharIndex();
                    boolean isDragged = false;
                    if (selection.isDragging()) {
                        for (SelectedEntity se : selection.getSelectedEntities()) {
                            if (se.x == x && se.y == y && se.type == Selection.EntityType.NPC) {
                                isDragged = true;
                                break;
                            }
                        }
                    }

                    if (!isDragged) {
                        final boolean isUserChar = charIndex == user.getUserCharIndex();
                        if (isUserChar) {
                            if (user.isWalkingmode()) {
                                drawCharacter(charIndex,
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        1.0f,
                                        weather.getWeatherColor());
                            }
                        } else {
                            if (renderSettings.getShowNPCs()) {
                                drawCharacter(charIndex,
                                        POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                        POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                        1.0f,
                                        weather.getWeatherColor());
                            }
                        }
                    }
                }

                if (renderSettings.getShowLayer()[2]) {
                    if (mapData[x][y].getLayer(3).getGrhIndex() != 0) {
                        drawTexture(mapData[x][y].getLayer(3),
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }
                camera.incrementScreenX();
            }
            camera.incrementScreenY();
        }
    }

    private void renderFourthLayer(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowLayer()[3]) {
            checkEffectCeiling();
            if (alphaCeiling > 0.0f) {
                camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
                for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                    camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                    for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                        if (mapData[x][y].getLayer(4).getGrhIndex() > 0) {
                            drawTexture(mapData[x][y].getLayer(4),
                                    POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                    POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                    true, true, false, alphaCeiling, weather.getWeatherColor());
                        }
                        camera.incrementScreenX();
                    }
                    camera.incrementScreenY();
                }
            }
        }
    }

    private void renderBlockOverlays(RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowBlock()) {
            int grhBlock = 4;
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y].getBlocked()) {
                        drawGrhIndex(grhBlock,
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                renderSettings.getBlockOpacity(),
                                null);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void renderTranslationOverlays(RenderSettings renderSettings, final int pixelOffsetX,
            final int pixelOffsetY) {
        if (renderSettings.getShowMapTransfer()) {
            int grhTrans = 3;
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y].getExitMap() > 0) {
                        drawGrhIndex(grhTrans,
                                POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX,
                                POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY,
                                null);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void checkEffectCeiling() {
        if (user.isUnderCeiling()) {
            if (alphaCeiling > 0.0f)
                alphaCeiling -= 0.5f * org.argentumforge.engine.utils.Time.deltaTime;
        } else {
            if (alphaCeiling < 1.0f)
                alphaCeiling += 0.5f * org.argentumforge.engine.utils.Time.deltaTime;
        }
    }

    public float getAlphaCeiling() {
        return alphaCeiling;
    }

    public void setAlphaCeiling(float alphaCeiling) {
        this.alphaCeiling = alphaCeiling;
    }

    /**
     * Renderiza overlays amarillos sobre los tiles con triggers usando ImGui.
     */
    public void renderImGuiOverlays(int pixelOffsetX, int pixelOffsetY) {
        RenderSettings renderSettings = Options.INSTANCE.getRenderSettings();
        if (org.argentumforge.engine.utils.editor.Trigger.getInstance().isActive()
                || renderSettings.getShowTriggers()
                || org.argentumforge.engine.utils.editor.Particle.getInstance().isActive()
                || renderSettings.getShowParticles()) {

            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            imgui.ImDrawList drawList = imgui.ImGui.getBackgroundDrawList();

            if (mapData == null)
                return;

            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                    if (mapData[x][y].getTrigger() > 0 && renderSettings.getShowTriggers()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = String.valueOf(mapData[x][y].getTrigger());
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = screenY + (TILE_PIXEL_SIZE - 28) / 2; // Arriba si hay partículas? No, centrado.

                        // Sombra Negra (offset +1)
                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        // Texto Blanco (con opacidad completa)
                        drawList.addText(textX, textY, 0xFFFFFFFF, idText);
                    }

                    if (mapData[x][y].getParticleIndex() > 0 && renderSettings.getShowParticles()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = "P" + mapData[x][y].getParticleIndex();
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = screenY + (TILE_PIXEL_SIZE + 4) / 2; // Un poco más abajo que el centro

                        // Sombra Negra (offset +1)
                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        // Texto Cian para partículas (Visible)
                        drawList.addText(textX, textY, 0xFFFFFF00, idText);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }
}
