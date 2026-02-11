package org.argentumforge.engine.renderer;

import org.argentumforge.engine.game.EditorController;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.Weather;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.listeners.MouseListener;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.editor.Selection.SelectedEntity;
import org.argentumforge.engine.game.User;
import static org.argentumforge.engine.game.models.Character.drawCharacter;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;
import static org.argentumforge.engine.renderer.Drawn.drawGrhIndex;
import static org.argentumforge.engine.scenes.Camera.*;
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

        if (!renderSettings.isPhotoModeActive()) {
            renderBlockOverlays(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
            renderTranslationOverlays(mapData, renderSettings, pixelOffsetX, pixelOffsetY);
            renderSelectionHighlight(pixelOffsetX, pixelOffsetY);
        }

        renderClipboardGhost(pixelOffsetX, pixelOffsetY);

    }

    private void renderViewportOverlay() {
        RenderSettings renderSettings = Options.INSTANCE.getRenderSettings();
        if (!renderSettings.isShowViewportOverlay())
            return;

        int vWidth = renderSettings.getViewportWidth();
        int vHeight = renderSettings.getViewportHeight();
        float[] vColor = renderSettings.getViewportColor();

        // Calcular dimensiones en píxeles basado en el zoom actual
        int widthPx = vWidth * TILE_PIXEL_SIZE;
        int heightPx = vHeight * TILE_PIXEL_SIZE;

        // Centro del viewport principal (Área de Trabajo)
        float centerX = imgui.ImGui.getMainViewport().getWorkPosX()
                + imgui.ImGui.getMainViewport().getWorkSizeX() / 2.0f;
        float centerY = imgui.ImGui.getMainViewport().getWorkPosY()
                + imgui.ImGui.getMainViewport().getWorkSizeY() / 2.0f;

        float x1 = centerX - (widthPx / 2.0f);
        float y1 = centerY - (heightPx / 2.0f);
        float x2 = x1 + widthPx;
        float y2 = y1 + heightPx;

        imgui.ImDrawList drawList = imgui.ImGui.getBackgroundDrawList();
        int color = imgui.ImGui.getColorU32(vColor[0], vColor[1], vColor[2], vColor[3]);

        // Dibujar borde del marco (3px grosor para visibilidad)
        drawList.addRect(x1, y1, x2, y2, color, 0, 0, 3.0f);

        // Opcional: Dibujar una ligera sombra dentro o fuera para resaltar?
        // Por ahora, solo el rectángulo está bien.
    }

    private void renderFirstLayer(org.argentumforge.engine.utils.inits.MapData[][] mapData,
            RenderSettings renderSettings, final int pixelOffsetX, final int pixelOffsetY) {
        if (renderSettings.getShowLayer()[0]) {
            camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y] != null && mapData[x][y].getLayer(1).getGrhIndex() != 0) {
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
                if (mapData[x][y] == null) {
                    camera.incrementScreenX();
                    continue;
                }
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
                if (mapData[x][y] == null) {
                    camera.incrementScreenX();
                    continue;
                }

                if (renderSettings.getShowOJBs()) {
                    if (mapData[x][y].getObjGrh().getGrhIndex() != 0) {
                        if (grhData[mapData[x][y].getObjGrh().getGrhIndex()].getPixelWidth() != TILE_PIXEL_SIZE ||
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
            // REMOVED: checkEffectCeiling() call - Automatic roof fading is now disabled
            if (alphaCeiling > 0.0f) {
                camera.setScreenY(camera.getMinYOffset() - TILE_BUFFER_SIZE);
                for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                    camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                    for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                        if (mapData[x][y] != null && mapData[x][y].getLayer(4).getGrhIndex() > 0) {
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
                    if (mapData[x][y] != null && mapData[x][y].getBlocked()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        float opacity = renderSettings.getBlockOpacity();
                        switch (renderSettings.getBlockIndicatorStyle()) {
                            case MODERN:
                                // Moderno: Recuadro rojo semitransparente con borde más sólido
                                Drawn.drawColoredRect(screenX + 2, screenY + 2,
                                        TILE_PIXEL_SIZE - 4, TILE_PIXEL_SIZE - 4,
                                        new RGBColor(1.0f, 0.2f, 0.2f), 0.4f * opacity);

                                // Pequeña "X"
                                Drawn.drawColoredRect(screenX + (int) (TILE_PIXEL_SIZE * 0.3f),
                                        screenY + (int) (TILE_PIXEL_SIZE * 0.3f),
                                        (int) (TILE_PIXEL_SIZE * 0.4f), (int) (TILE_PIXEL_SIZE * 0.4f),
                                        new RGBColor(1.0f, 0.6f, 0.6f), 0.6f * opacity);
                                break;

                            case MINIMAL:
                                // Minimalista: Pequeño cuadrado en el centro
                                int size = (int) (TILE_PIXEL_SIZE * 0.25f);
                                int offset = (TILE_PIXEL_SIZE - size) / 2;
                                Drawn.drawColoredRect(screenX + offset, screenY + offset,
                                        size, size,
                                        new RGBColor(1.0f, 0.2f, 0.2f), 0.7f * opacity);
                                break;

                            case SOLID:
                                // Sólido: Relleno completo del tile
                                Drawn.drawColoredRect(screenX, screenY,
                                        TILE_PIXEL_SIZE, TILE_PIXEL_SIZE,
                                        new RGBColor(1.0f, 0.0f, 0.0f), 0.6f * opacity);
                                break;

                            case MESH:
                                // Malla: X diagonal completa
                                // Línea /
                                Drawn.drawColoredRect(screenX, screenY,
                                        2, TILE_PIXEL_SIZE,
                                        new RGBColor(1.0f, 0.4f, 0.4f), 0.5f * opacity, (float) TILE_PIXEL_SIZE);
                                // Línea \
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - 2, screenY,
                                        2, TILE_PIXEL_SIZE,
                                        new RGBColor(1.0f, 0.4f, 0.4f), 0.5f * opacity, -(float) TILE_PIXEL_SIZE);
                                break;

                            case CORNERS:
                                // Esquinas: 4 L-shapes
                                int len = 8;
                                int th = 2;
                                RGBColor cornerColor = new RGBColor(1.0f, 0.3f, 0.3f);
                                // Top-Left
                                Drawn.drawColoredRect(screenX, screenY, len, th, cornerColor, 0.8f * opacity);
                                Drawn.drawColoredRect(screenX, screenY, th, len, cornerColor, 0.8f * opacity);
                                // Top-Right
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - len, screenY, len, th, cornerColor,
                                        0.8f * opacity);
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - th, screenY, th, len, cornerColor,
                                        0.8f * opacity);
                                // Bottom-Left
                                Drawn.drawColoredRect(screenX, screenY + TILE_PIXEL_SIZE - th, len, th, cornerColor,
                                        0.8f * opacity);
                                Drawn.drawColoredRect(screenX, screenY + TILE_PIXEL_SIZE - len, th, len, cornerColor,
                                        0.8f * opacity);
                                // Bottom-Right
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - len, screenY + TILE_PIXEL_SIZE - th,
                                        len, th, cornerColor, 0.8f * opacity);
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - th, screenY + TILE_PIXEL_SIZE - len,
                                        th, len, cornerColor, 0.8f * opacity);
                                break;

                            case CLASSIC:
                            default:
                                // Clásico: Gráfico indexado
                                drawGrhIndex(grhBlock,
                                        screenX,
                                        screenY,
                                        renderSettings.getBlockOpacity(),
                                        null);
                                break;
                        }
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
                    if (mapData[x][y] != null && mapData[x][y].getExitMap() > 0) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        float opacity = renderSettings.getTransferOpacity();
                        switch (renderSettings.getTransferIndicatorStyle()) {
                            case MODERN:
                                // Moderno: Recuadro azul cian semitransparente
                                Drawn.drawColoredRect(screenX + 2, screenY + 2,
                                        TILE_PIXEL_SIZE - 4, TILE_PIXEL_SIZE - 4,
                                        new RGBColor(0.2f, 0.8f, 1.0f), 0.4f * opacity);

                                // Borde interno o detalle
                                Drawn.drawColoredRect(screenX + 4, screenY + 4,
                                        TILE_PIXEL_SIZE - 8, TILE_PIXEL_SIZE - 8,
                                        new RGBColor(0.6f, 1.0f, 1.0f), 0.3f * opacity);
                                break;

                            case MINIMAL:
                                // Minimalista: Pequeño cuadrado en el centro
                                int size = (int) (TILE_PIXEL_SIZE * 0.25f);
                                int offset = (TILE_PIXEL_SIZE - size) / 2;
                                Drawn.drawColoredRect(screenX + offset, screenY + offset,
                                        size, size,
                                        new RGBColor(0.0f, 0.8f, 1.0f), 0.7f * opacity);
                                break;

                            case SOLID:
                                // Sólido: Relleno completo
                                Drawn.drawColoredRect(screenX, screenY,
                                        TILE_PIXEL_SIZE, TILE_PIXEL_SIZE,
                                        new RGBColor(0.0f, 1.0f, 1.0f), 0.6f * opacity);
                                break;

                            case MESH:
                                // Malla: X diagonal completa azul
                                Drawn.drawColoredRect(screenX, screenY,
                                        2, TILE_PIXEL_SIZE,
                                        new RGBColor(0.2f, 0.8f, 1.0f), 0.5f * opacity, (float) TILE_PIXEL_SIZE);
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - 2, screenY,
                                        2, TILE_PIXEL_SIZE,
                                        new RGBColor(0.2f, 0.8f, 1.0f), 0.5f * opacity, -(float) TILE_PIXEL_SIZE);
                                break;

                            case CORNERS:
                                // Esquinas: 4 L-shapes cyan
                                int len = 8;
                                int th = 2;
                                RGBColor cornerColor = new RGBColor(0.0f, 0.8f, 1.0f);
                                // Top-Left
                                Drawn.drawColoredRect(screenX, screenY, len, th, cornerColor, 0.8f * opacity);
                                Drawn.drawColoredRect(screenX, screenY, th, len, cornerColor, 0.8f * opacity);
                                // Top-Right
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - len, screenY, len, th, cornerColor,
                                        0.8f * opacity);
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - th, screenY, th, len, cornerColor,
                                        0.8f * opacity);
                                // Bottom-Left
                                Drawn.drawColoredRect(screenX, screenY + TILE_PIXEL_SIZE - th, len, th, cornerColor,
                                        0.8f * opacity);
                                Drawn.drawColoredRect(screenX, screenY + TILE_PIXEL_SIZE - len, th, len, cornerColor,
                                        0.8f * opacity);
                                // Bottom-Right
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - len, screenY + TILE_PIXEL_SIZE - th,
                                        len, th, cornerColor, 0.8f * opacity);
                                Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - th, screenY + TILE_PIXEL_SIZE - len,
                                        th, len, cornerColor, 0.8f * opacity);
                                break;

                            case CLASSIC:
                            default:
                                // Clásico
                                drawGrhIndex(grhTrans,
                                        screenX,
                                        screenY,
                                        null);
                                break;
                        }
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    // REMOVED: checkEffectCeiling() - Automatic roof fading based on underCeiling
    // (legacy client feature)
    // Roofs now only hide via manual toggle (Options/Shortcut) as this is an editor
    // tool

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

        // El overlay del viewport necesita renderizarse dentro de un contexto ImGui
        renderViewportOverlay();

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

            float viewportX = imgui.ImGui.getMainViewport().getWorkPosX();
            float viewportY = imgui.ImGui.getMainViewport().getWorkPosY();

            for (int y = camera.getMinY(); y <= camera.getMaxY(); y++) {
                camera.setScreenX(camera.getMinXOffset() - TILE_BUFFER_SIZE);
                for (int x = camera.getMinX(); x <= camera.getMaxX(); x++) {
                    if (mapData[x][y] == null) {
                        camera.incrementScreenX();
                        continue;
                    }

                    if (mapData[x][y].getTrigger() > 0 && renderSettings.getShowTriggers()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = String.valueOf(mapData[x][y].getTrigger());
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = viewportX + screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = viewportY + screenY + (TILE_PIXEL_SIZE - 28) / 2;

                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        drawList.addText(textX, textY, 0xFFFFFFFF, idText);
                    }

                    if (mapData[x][y].getParticleIndex() > 0 && renderSettings.getShowParticles()) {
                        int screenX = POS_SCREEN_X + camera.getScreenX() * TILE_PIXEL_SIZE + pixelOffsetX;
                        int screenY = POS_SCREEN_Y + camera.getScreenY() * TILE_PIXEL_SIZE + pixelOffsetY;

                        String idText = "P" + mapData[x][y].getParticleIndex();
                        float textWidth = imgui.ImGui.calcTextSize(idText).x;
                        float textX = viewportX + screenX + (TILE_PIXEL_SIZE - textWidth) / 2;
                        float textY = viewportY + screenY + (TILE_PIXEL_SIZE + 4) / 2;

                        drawList.addText(textX + 1, textY + 1, 0xFF000000, idText);
                        drawList.addText(textX, textY, 0xFFFFFF00, idText);
                    }
                    camera.incrementScreenX();
                }
                camera.incrementScreenY();
            }
        }
    }

    private void renderClipboardGhost(int pixelOffsetX, int pixelOffsetY) {
        if (!EditorController.INSTANCE.isPasteModeActive())
            return;

        Clipboard clip = Clipboard.getInstance();
        if (clip.isEmpty())
            return;

        int mx = (int) MouseListener.getX() - POS_SCREEN_X;
        int my = (int) MouseListener.getY() - POS_SCREEN_Y;
        int tx = EditorInputManager.getTileMouseX(mx);
        int ty = EditorInputManager.getTileMouseY(my);

        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;

        Clipboard.PasteSettings settings = clip.getSettings();

        // Efecto pulsante para el fantasma (0.5 a 0.8)
        float pulse = (float) (Math.sin(org.argentumforge.engine.utils.Time.getRunningTime() * 5.0f) + 1.0f) / 2.0f; // 0..1
        float alpha = 0.5f + (pulse * 0.3f);

        // Tinte Cyan para indicar modo de inserción
        RGBColor ghostColor = new RGBColor(0.7f, 1.0f, 1.0f);

        for (Clipboard.ClipboardItem item : clip.getItems()) {
            int targetX = tx + item.offsetX;
            int targetY = ty + item.offsetY;

            // Calcular posición en pantalla relativa a la cámara
            int screenX = POS_SCREEN_X + (targetX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                    * TILE_PIXEL_SIZE + pixelOffsetX;
            int screenY = POS_SCREEN_Y + (targetY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                    * TILE_PIXEL_SIZE + pixelOffsetY;

            if (item.type == Selection.EntityType.TILE) {
                // Dibujar capas
                if (item.layers != null) {
                    for (int i = 1; i <= 4; i++) {
                        if (i <= settings.layers.length && settings.layers[i - 1] && item.layers[i] > 0) {
                            drawGhostGrh(item.layers[i], screenX, screenY, alpha, ghostColor);
                        }
                    }
                }
                // Renderizar objetos asociados al tile
                if (settings.objects && item.objIndex > 0) {
                    ObjData objData = objs.get(item.objIndex);
                    if (objData != null) {
                        drawGhostGrh(objData.getGrhIndex(), screenX, screenY, alpha, ghostColor);
                    }
                }
                // Renderizar NPC es más complejo ya que clipboard guarda ID de NPC, no Grh
                // directo usualmente.
                // Si añadimos lógica para NPC aquí:
                /*
                 * if (settings.npc && item.npcIndex > 0) {
                 * // Npc data lookup...
                 * }
                 */

            } else if (item.type == Selection.EntityType.NPC && settings.npc) {
                // NPCs usan drawCharacter que ya maneja offsets internos usualmente,
                // pero si queremos consistencia con el ghost tint:
                drawCharacter(item.id, screenX, screenY, alpha, ghostColor);
            } else if (item.type == Selection.EntityType.OBJECT && settings.objects) {
                drawGhostGrh(item.id, screenX, screenY, alpha, ghostColor);
            }
        }
    }

    private void drawGhostGrh(int grhIndex, int x, int y, float alpha, RGBColor color) {
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;

        // Aplicar logica de centrado de Drawn.drawTexture
        if (grhData[grhIndex].getTileWidth() != 1)
            x = x - (int) (grhData[grhIndex].getTileWidth() * TILE_PIXEL_SIZE / 2) + TILE_PIXEL_SIZE / 2;
        if (grhData[grhIndex].getTileHeight() != 1)
            y = y - (int) (grhData[grhIndex].getTileHeight() * TILE_PIXEL_SIZE) + TILE_PIXEL_SIZE;

        drawGrhIndex(grhIndex, x, y, alpha, color);
    }

    private void renderSelectionHighlight(int pixelOffsetX, int pixelOffsetY) {
        // Mostrar cursor si hay selección activa o si estamos en modo Capturar (Pick)
        // de Superficie
        if (!selection.isActive() && org.argentumforge.engine.utils.editor.Surface.getInstance().getMode() != 3)
            return;

        int mx = (int) MouseListener.getX() - POS_SCREEN_X;
        int my = (int) MouseListener.getY() - POS_SCREEN_Y;
        int tx = EditorInputManager.getTileMouseX(mx);
        int ty = EditorInputManager.getTileMouseY(my);

        int screenX = POS_SCREEN_X + (tx - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                * TILE_PIXEL_SIZE + pixelOffsetX;
        int screenY = POS_SCREEN_Y + (ty - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                * TILE_PIXEL_SIZE + pixelOffsetY;

        float thickness = 2.0f;
        RGBColor color = new RGBColor(0.3f, 0.6f, 1.0f);
        float alpha = 0.8f;

        // Top
        Drawn.drawColoredRect(screenX, screenY, TILE_PIXEL_SIZE, (int) thickness, color, alpha);
        // Bottom
        Drawn.drawColoredRect(screenX, screenY + TILE_PIXEL_SIZE - (int) thickness, TILE_PIXEL_SIZE, (int) thickness,
                color, alpha);
        // Left
        Drawn.drawColoredRect(screenX, screenY, (int) thickness, TILE_PIXEL_SIZE, color, alpha);
        // Right
        Drawn.drawColoredRect(screenX + TILE_PIXEL_SIZE - (int) thickness, screenY, (int) thickness, TILE_PIXEL_SIZE,
                color, alpha);
    }
}
