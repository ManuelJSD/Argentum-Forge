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
// static import removed
import static org.argentumforge.engine.utils.AssetRegistry.grhData;
import static org.argentumforge.engine.utils.AssetRegistry.objs;
import org.argentumforge.engine.utils.inits.ObjData;
import org.argentumforge.engine.game.models.ObjectType;

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
    private PostProcessor postProcessor;

    public MapRenderer(Camera camera) {
        this.camera = camera;
    }

    public void render(int pixelOffsetX, int pixelOffsetY) {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;

        var mapData = context.getMapData();
        RenderSettings renderSettings = Options.INSTANCE.getRenderSettings();

        renderFirstLayer(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
        renderSecondLayer(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
        renderThirdLayer(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
        renderFourthLayer(mapData, renderSettings, pixelOffsetX, pixelOffsetY);

        // Ocultar overlays técnicos en modo foto
        if (!renderSettings.isPhotoModeActive()) {
            renderBlockOverlays(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
            renderTranslationOverlays(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
        }

        if (renderSettings.isPhotoModeActive()) {
            renderPhotoEffects(renderSettings);
        }
    }

    private void renderFirstLayer(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowLayer()[0]) {
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y].getLayer(1).getGrhIndex() != 0) {
                        int finalX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int finalY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        drawTexture(mapData[x][y].getLayer(1),
                                finalX, finalY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void renderSecondLayer(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
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
                                int drawX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                                int drawY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                                // Sombras proyectadas para objetos pequeños
                                if (renderSettings.isPhotoModeActive() && renderSettings.isPhotoShadows()) {
                                    ObjData objModel = objs.get(mapData[x][y].getObjIndex());
                                    boolean isDoor = objModel != null && objModel.getType() == ObjectType.DOOR.getId();

                                    if (!isDoor) {
                                        renderShadow(mapData[x][y].getObjGrh(), drawX + 2, drawY + 1,
                                                0.35f, 1.0f, 0.45f, 22.0f, renderSettings.isPhotoSoftShadows());
                                    }
                                }

                                drawTexture(mapData[x][y].getObjGrh(), drawX, drawY,
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

    private void renderShadow(org.argentumforge.engine.utils.inits.GrhInfo grh, int x, int y, float alpha, float scaleX,
            float scaleY, float skewX, boolean soft) {
        if (soft) {
            float softAlpha = alpha * 0.4f;
            drawTexture(grh, x - 1, y, true, true, false, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
            drawTexture(grh, x + 1, y, true, true, false, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
            drawTexture(grh, x, y - 1, true, true, false, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
        }
        drawTexture(grh, x, y, true, true, false, alpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
    }

    private void renderCharShadow(int charIndex, int x, int y, float alpha, float scaleX, float scaleY, float skewX,
            boolean soft) {
        if (soft) {
            float softAlpha = alpha * 0.4f;
            drawCharacter(charIndex, x - 1, y, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
            drawCharacter(charIndex, x + 1, y, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
            drawCharacter(charIndex, x, y - 1, softAlpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
        }
        drawCharacter(charIndex, x, y, alpha, new RGBColor(0, 0, 0), scaleX, scaleY, skewX);
    }

    private void renderThirdLayer(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
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
                                int drawX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                                int drawY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                                // Sombras proyectadas para objetos grandes
                                if (renderSettings.isPhotoModeActive() && renderSettings.isPhotoShadows()) {
                                    ObjData objModel = objs.get(mapData[x][y].getObjIndex());
                                    boolean isDoor = objModel != null && objModel.getType() == ObjectType.DOOR.getId();

                                    if (!isDoor) {
                                        renderShadow(mapData[x][y].getObjGrh(), drawX + 2, drawY + 1,
                                                0.35f, 1.0f, 0.45f, 25.0f, renderSettings.isPhotoSoftShadows());
                                    }
                                }

                                drawTexture(mapData[x][y].getObjGrh(), drawX, drawY,
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
                        int drawX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int drawY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        if (isUserChar) {
                            if (user.isWalkingmode()) {
                                // Sombra para el usuario (Proyectada y Suave)
                                if (renderSettings.isPhotoModeActive() && renderSettings.isPhotoShadows()) {
                                    renderCharShadow(charIndex, drawX + 1, drawY + 1, 0.45f,
                                            1.0f, 0.45f, 25.0f, renderSettings.isPhotoSoftShadows());
                                }
                                drawCharacter(charIndex, drawX, drawY, 1.0f, weather.getWeatherColor());
                            }
                        } else {
                            if (renderSettings.getShowNPCs()) {
                                // Sombra para NPCs (Proyectada y Suave)
                                if (renderSettings.isPhotoModeActive() && renderSettings.isPhotoShadows()) {
                                    renderCharShadow(charIndex, drawX + 1, drawY + 1, 0.45f,
                                            1.0f, 0.45f, 25.0f, renderSettings.isPhotoSoftShadows());
                                }
                                drawCharacter(charIndex, drawX, drawY, 1.0f, weather.getWeatherColor());
                            }
                        }
                    }
                }

                if (renderSettings.getShowLayer()[2]) {
                    if (mapData[x][y].getLayer(3).getGrhIndex() != 0) {
                        int drawX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int drawY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        // Sombras proyectadas para capas de Tiles (paredes, edificios)
                        if (renderSettings.isPhotoModeActive() && renderSettings.isPhotoShadows()) {
                            renderShadow(mapData[x][y].getLayer(3), drawX + 4, drawY + 4,
                                    0.4f, 1.0f, 0.5f, 30.0f, renderSettings.isPhotoSoftShadows());
                        }

                        drawTexture(mapData[x][y].getLayer(3), drawX, drawY,
                                true, true, false, 1.0f, weather.getWeatherColor());
                    }
                }
                camera.incrementScreenX();
            }
            camera.incrementScreenY();
        }
    }

    private void renderFourthLayer(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
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

    private void renderBlockOverlays(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
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

    private void renderTranslationOverlays(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX,
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
        if (renderSettings.isPhotoModeActive())
            return;

        if (org.argentumforge.engine.utils.editor.Trigger.getInstance().isActive()
                || renderSettings.getShowTriggers()
                || org.argentumforge.engine.utils.editor.Particle.getInstance().isActive()
                || renderSettings.getShowParticles()) {

            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            imgui.ImDrawList drawList = imgui.ImGui.getBackgroundDrawList();

            var context = org.argentumforge.engine.utils.GameData.getActiveContext();
            if (context == null || context.getMapData() == null)
                return;
            var mapData = context.getMapData();

            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {

                    if (mapData[x][y].getTrigger() > 0 && renderSettings.getShowTriggers()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = String.valueOf(mapData[x][y].getTrigger());
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = screenY + (TILE_PIXEL_SIZE - 28) / 2;

                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        drawList.addText(textX, textY, 0xFFFFFFFF, idText);
                    }

                    if (mapData[x][y].getParticleIndex() > 0 && renderSettings.getShowParticles()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = "P" + mapData[x][y].getParticleIndex();
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = screenY + (TILE_PIXEL_SIZE + 4) / 2;

                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        drawList.addText(textX, textY, 0xFFFFFF00, idText);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void renderPhotoEffects(RenderSettings renderSettings) {
        // Run Post-Processor for all "Ultra" effects (Filters, Bloom, DoF, Grain, Zoom,
        // Color Grading)
        // FLUSH BEFORE CAPTURE: glCopyTexImage2D needs pixels to be on the buffer
        // already
        org.argentumforge.engine.Engine.batch.end();

        int winWidth = org.argentumforge.engine.Window.INSTANCE.getWidth();
        int winHeight = org.argentumforge.engine.Window.INSTANCE.getHeight();

        if (postProcessor == null) {
            postProcessor = new PostProcessor(winWidth, winHeight);
        } else if (postProcessor.getWidth() != winWidth || postProcessor.getHeight() != winHeight) {
            postProcessor.resize(winWidth, winHeight);
        }
        postProcessor.apply(renderSettings, org.argentumforge.engine.utils.Time.getRunningTime());

        // RE-BEGIN: For subsequent UI or vignettes
        org.argentumforge.engine.Engine.batch.begin();

        // 2. Vignette (Drawn via Batch after Post-Process)
        if (renderSettings.isPhotoVignette()) {
            float intensity = renderSettings.getVignetteIntensity();
            if (user.isUnderCeiling())
                intensity = Math.min(0.95f, intensity + 0.15f);

            Drawn.drawVignette(org.argentumforge.engine.Window.INSTANCE.getWidth(),
                    org.argentumforge.engine.Window.INSTANCE.getHeight(),
                    intensity);
        }
    }
}
