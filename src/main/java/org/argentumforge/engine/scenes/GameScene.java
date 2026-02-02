package org.argentumforge.engine.scenes;

import org.argentumforge.engine.Window;
import org.argentumforge.engine.game.*;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FMain;

import org.argentumforge.engine.listeners.MouseListener;
import org.tinylog.Logger;
import org.argentumforge.engine.listeners.EditorInputManager;
import org.argentumforge.engine.renderer.RenderSettings;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.editor.Npc;
import org.argentumforge.engine.utils.editor.Obj;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.Engine;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.NpcData;
import org.argentumforge.engine.utils.inits.ObjData;

import org.argentumforge.engine.renderer.Drawn;
import org.argentumforge.engine.renderer.MapRenderer;

import static org.argentumforge.engine.utils.Time.timerTicksPerFrame;

import static org.argentumforge.engine.game.IntervalTimer.INT_SENTRPU;

import static org.argentumforge.engine.renderer.Drawn.drawGrhIndex;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.*;
import static org.argentumforge.engine.utils.AssetRegistry.*;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.utils.editor.Selection.SelectedEntity;

/**
 * Escena principal del editor.
 * Maneja el renderizado del mapa, la lógica de las herramientas y la entrada
 * del usuario.
 */

public final class GameScene extends Scene {

    /** Temporizador para controlar el intervalo de actualización de posición. */
    private final IntervalTimer intervalToUpdatePos = new IntervalTimer(INT_SENTRPU);

    /** Instancia del usuario actual (singleton). */
    private final User user = User.INSTANCE;

    /** Sistema de clima y color de ambiente. */
    private Weather weather;

    /** Acumulador de desplazamiento en X para animaciones suaves de movimiento. */
    private float offSetCounterX = 0;

    /** Acumulador de desplazamiento en Y para animaciones suaves de movimiento. */
    private float offSetCounterY = 0;

    /** Renderizador de las capas del mapa. */
    private MapRenderer mapRenderer;

    /** Gestor de entrada del editor. */
    private EditorInputManager inputManager;

    /** Formulario principal de la interfaz de usuario. */
    private FMain frmMain;

    /** Editor de superficies. */
    private Surface surface;

    /** Editor de NPCs. */
    private Npc npc;

    /** Editor de Objetos. */
    private Obj obj;

    /** Herramienta de seleccion y movimiento. */
    private Selection selection;

    private Texture whiteTexture;

    /**
     * Inicializa los componentes de la escena del juego.
     * Configura el tipo de escena de retorno, el clima, los editores y añade el
     * formulario principal a ImGui.
     */
    @Override
    public void init() {
        super.init();

        canChangeTo = SceneType.GAME_SCENE;
        weather = Weather.INSTANCE;
        frmMain = new FMain();
        surface = Surface.getInstance();
        npc = Npc.getInstance();
        obj = Obj.getInstance();
        selection = Selection.getInstance();

        mapRenderer = new MapRenderer(camera);
        inputManager = new EditorInputManager(camera);

        whiteTexture = org.argentumforge.engine.renderer.Surface.INSTANCE.getWhiteTexture();

        ImGUISystem.INSTANCE.addFrm(frmMain);
    }

    /**
     * Actualiza la lógica y renderiza la escena.
     * Maneja la actualización del clima, los temporizadores, el desplazamiento
     * suave del personaje
     * y delega el renderizado del mapa a {@link #renderScreen}.
     */
    @Override
    public void render() {

        if (!visible || charList == null || mapData == null)
            return;

        weather.update();
        intervalToUpdatePos.update();

        // Update user walking speed based on options
        int charIndex = user.getUserCharIndex();
        if (charIndex >= 0 && charIndex < charList.length && charList[charIndex] != null) {
            int targetSpeed = user.isWalkingmode()
                    ? options.getMoveSpeedWalk()
                    : options.getMoveSpeedNormal();
            charList[charIndex].setWalkingSpeed(targetSpeed);
        }

        if (user.isUserMoving()) {
            try {
                // Movement logic refactored to use Percentage (0.0 to 1.0) instead of raw
                // pixels
                // This ensures stability when TILE_PIXEL_SIZE changes (Zoom)

                float zoomScale = Camera.getZoomScale();
                int userIdx = user.getUserCharIndex();

                // Safety check for valid character in move loop
                if (userIdx < 0 || userIdx >= charList.length || charList[userIdx] == null) {
                    user.setUserMoving(false);
                    return;
                }

                float speedPixels = (charList[userIdx].getWalkingSpeed() * zoomScale) * timerTicksPerFrame;
                float progressParams = speedPixels / TILE_PIXEL_SIZE;

                // Calculate completion threshold based on distance (multiplier)
                float totalDistance = Math.max(Math.abs(user.getAddToUserPos().getX()),
                        Math.abs(user.getAddToUserPos().getY()));
                if (totalDistance == 0)
                    totalDistance = 1.0f; // Safety

                if (user.getAddToUserPos().getX() != 0) {
                    offSetCounterX += progressParams;
                }
                if (user.getAddToUserPos().getY() != 0) {
                    offSetCounterY += progressParams;
                }

                if (offSetCounterX >= totalDistance || offSetCounterY >= totalDistance) {
                    user.getAddToUserPos().setX(0);
                    user.getAddToUserPos().setY(0);
                    offSetCounterX = 0;
                    offSetCounterY = 0;
                    user.setUserMoving(false);
                }
            } catch (Exception e) {
                Logger.error(e, "Error in GameScene movement logic");
                user.setUserMoving(false);
            }
        } else {
            offSetCounterX = 0;
            offSetCounterY = 0;
        }

        // Calculate Pixel Offset for Renderer: -1 * Direction * (Progress * TileSize)
        // Note: Progress is now offSetCounter (0 to totalDistance)
        int pixelOffsetX = 0;
        int pixelOffsetY = 0;

        if (user.getAddToUserPos().getX() != 0) {
            pixelOffsetX = (int) (-1 * Math.signum(user.getAddToUserPos().getX()) * (offSetCounterX * TILE_PIXEL_SIZE));
        }
        if (user.getAddToUserPos().getY() != 0) {
            pixelOffsetY = (int) (-1 * Math.signum(user.getAddToUserPos().getY()) * (offSetCounterY * TILE_PIXEL_SIZE));
        }

        // Correct formula: (UserPos - AddToUserPos) gives the starting tile.
        // We render from Start towards End using pixelOffset.
        renderScreen(user.getUserPos().getX() - user.getAddToUserPos().getX(),
                user.getUserPos().getY() - user.getAddToUserPos().getY(),
                pixelOffsetX, pixelOffsetY);

        // Update Auto-save
        org.argentumforge.engine.utils.editor.AutoSaveManager.getInstance().update();
    }

    /**
     * Escucha los eventos del mouse.
     */
    @Override
    public void mouseEvents() {
        inputManager.updateMouse();
    }

    /**
     * Escucha los eventos del teclado.
     */
    @Override
    public void keyEvents() {
        inputManager.updateKeys();
    }

    /**
     * Renderiza los overlays de ImGui delegando al MapRenderer.
     */
    public void renderImGuiOverlays() {
        int pixelOffsetX = 0;
        int pixelOffsetY = 0;

        if (user.getAddToUserPos().getX() != 0) {
            pixelOffsetX = (int) (-1 * user.getAddToUserPos().getX() * (offSetCounterX * TILE_PIXEL_SIZE));
        }
        if (user.getAddToUserPos().getY() != 0) {
            pixelOffsetY = (int) (-1 * user.getAddToUserPos().getY() * (offSetCounterY * TILE_PIXEL_SIZE));
        }

        mapRenderer.renderImGuiOverlays(pixelOffsetX, pixelOffsetY);
    }

    /**
     * Cierre de la escena.
     */
    @Override
    public void close() {
        visible = false;
    }

    /**
     * Coordina el proceso completo de renderizado de la pantalla.
     * Actualiza la cámara y renderiza todas las capas del mapa en orden, seguido de
     * los diálogos,
     * techos, overlays de bloqueos y efectos climáticos.
     *
     * @param tileX        Posición X en tiles (epicentro de la cámara)
     * @param tileY        Posición Y en tiles (epicentro de la cámara)
     * @param pixelOffsetX Desplazamiento fino en X (píxeles)
     * @param pixelOffsetY Desplazamiento fino en Y (píxeles)
     */
    private void renderScreen(int tileX, int tileY, int pixelOffsetX, int pixelOffsetY) {
        camera.update(tileX, tileY);

        RenderSettings renderSettings = options.getRenderSettings();

        mapRenderer.render(pixelOffsetX, pixelOffsetY);

        renderGrid(renderSettings, pixelOffsetX, pixelOffsetY);
        renderEditorPreviews(pixelOffsetX, pixelOffsetY);
        renderCommandHighlight(pixelOffsetX, pixelOffsetY);
    }

    /**
     * Renderiza una previsualización de lo que el usuario está a punto de colocar.
     */
    private void renderEditorPreviews(int pixelOffsetX, int pixelOffsetY) {
        if (!EditorInputManager.inGameArea())
            return;

        int mouseX = (int) MouseListener.getX() - POS_SCREEN_X;
        int mouseY = (int) MouseListener.getY() - POS_SCREEN_Y;
        int tileX = EditorInputManager.getTileMouseX(mouseX);
        int tileY = EditorInputManager.getTileMouseY(mouseY);

        // Previsualizar Superficies
        if (surface.getMode() == 1 && surface.getSurfaceIndex() > 0) {
            int half = surface.getBrushSize() / 2;
            float alpha = options.getRenderSettings().getGhostOpacity();

            if (surface.isUseMosaic() && (surface.getMosaicWidth() > 1 || surface.getMosaicHeight() > 1)) {
                // Modo Estampado
                for (int dx = 0; dx < surface.getMosaicWidth(); dx++) {
                    for (int dy = 0; dy < surface.getMosaicHeight(); dy++) {
                        int mapX = tileX + dx;
                        int mapY = tileY + dy;
                        drawPreviewGrh((short) (surface.getSurfaceIndex() + (dy * surface.getMosaicWidth()) + dx), mapX,
                                mapY, pixelOffsetX, pixelOffsetY, alpha);
                    }
                }
            } else {
                // Modo Pincel Normal
                for (int i = tileX - half; i <= tileX + half; i++) {
                    for (int j = tileY - half; j <= tileY + half; j++) {
                        if (surface.getBrushShape() == Surface.BrushShape.CIRCLE
                                && (Math.pow(i - tileX, 2) + Math.pow(j - tileY, 2) > Math.pow(half, 2)))
                            continue;

                        short targetGrh = (short) surface.getSurfaceIndex();
                        if (surface.getMosaicWidth() > 1 || surface.getMosaicHeight() > 1) {
                            targetGrh = (short) (surface.getSurfaceIndex()
                                    + ((j % surface.getMosaicHeight()) * surface.getMosaicWidth())
                                    + (i % surface.getMosaicWidth()));
                        }
                        drawPreviewGrh(targetGrh, i, j, pixelOffsetX, pixelOffsetY, alpha);
                    }
                }
            }
        }

        // Previsualizar NPCs
        if (npc.getMode() == 1 && npc.getNpcNumber() > 0) {
            NpcData data = AssetRegistry.npcs.get(npc.getNpcNumber());
            if (data != null) {
                int screenX = POS_SCREEN_X
                        + (tileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetX;
                int screenY = POS_SCREEN_Y
                        + (tileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetY;
                Character.drawCharacterGhost(data.getBody(), data.getHead(),
                        screenX, screenY, options.getRenderSettings().getGhostOpacity(), weather.getWeatherColor());
            }
        }

        // Renderizar Resaltado de Selección
        if (selection.isActive() && !selection.getSelectedEntities().isEmpty()) {
            for (SelectedEntity se : selection.getSelectedEntities()) {
                int sTileX = se.x;
                int sTileY = se.y;

                int screenX = POS_SCREEN_X
                        + (sTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetX;
                int screenY = POS_SCREEN_Y
                        + (sTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                        + pixelOffsetY;

                Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, TILE_PIXEL_SIZE, TILE_PIXEL_SIZE, true,
                        0.2f,
                        new RGBColor(0.0f, 1.0f, 0.0f));
            }
        }

        // Previsualizar Objetos
        if (obj.getMode() == 1 && obj.getObjNumber() > 0) {
            ObjData data = AssetRegistry.objs.get(obj.getObjNumber());
            if (data != null) {
                drawPreviewGrh((short) data.getGrhIndex(), tileX, tileY, pixelOffsetX, pixelOffsetY,
                        options.getRenderSettings().getGhostOpacity());
            }
        }

        // Previsualizar Arrastre (Seleccion Múltiple)
        if (selection.isDragging()) {
            for (SelectedEntity se : selection.getSelectedEntities()) {
                int dragTileX = tileX + se.offsetX;
                int dragTileY = tileY + se.offsetY;

                if (se.type == Selection.EntityType.NPC) {
                    NpcData data = AssetRegistry.npcs.get(se.id);
                    if (data != null) {
                        int screenX = POS_SCREEN_X
                                + (dragTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                                        * TILE_PIXEL_SIZE
                                + pixelOffsetX;
                        int screenY = POS_SCREEN_Y
                                + (dragTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                                        * TILE_PIXEL_SIZE
                                + pixelOffsetY;
                        Character.drawCharacterGhost(data.getBody(),
                                data.getHead(),
                                screenX, screenY, options.getRenderSettings().getGhostOpacity(),
                                weather.getWeatherColor());
                    }
                } else if (se.type == Selection.EntityType.OBJECT) {
                    drawPreviewGrh((short) se.id, dragTileX, dragTileY, pixelOffsetX, pixelOffsetY,
                            options.getRenderSettings().getGhostOpacity());
                } else if (se.type == Selection.EntityType.TILE) {
                    // Si arrastramos un TILE, deberíamos ver sus capas?
                    // En Selection.tryGrab para TILE id es 0.
                    // Si queremos fantasmas de tiles arrastrados, necesitaríamos los datos de capas
                    // en SelectedEntity.
                    // Para simplificar ahora, dibujamos un recuadro fantasma.
                    int screenX = POS_SCREEN_X
                            + (dragTileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE)
                                    * TILE_PIXEL_SIZE
                            + pixelOffsetX;
                    int screenY = POS_SCREEN_Y
                            + (dragTileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE)
                                    * TILE_PIXEL_SIZE
                            + pixelOffsetY;
                    Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, TILE_PIXEL_SIZE, TILE_PIXEL_SIZE,
                            true,
                            0.3f, new RGBColor(1.0f, 1.0f, 1.0f));
                }
            }
        }

        // Renderizar Cuadro de Selección (Marquee)
        if (selection.isAreaSelecting()) {
            int x1 = selection.getMarqueeStartX();
            int y1 = selection.getMarqueeStartY();
            int x2 = selection.getMarqueeEndX();
            int y2 = selection.getMarqueeEndY();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);

            int screenX = POS_SCREEN_X
                    + (minX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                    + pixelOffsetX;
            int screenY = POS_SCREEN_Y
                    + (minY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                    + pixelOffsetY;
            int width = (maxX - minX + 1) * TILE_PIXEL_SIZE;
            int height = (maxY - minY + 1) * TILE_PIXEL_SIZE;

            Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, width, height, true, 0.3f,
                    new RGBColor(0.2f, 0.5f, 1.0f));
        }
    }

    /**
     * Resalta el área afectada por el comando que el usuario está sobrevolando en
     * el
     * historial.
     */
    private void renderCommandHighlight(int pixelOffsetX, int pixelOffsetY) {
        org.argentumforge.engine.utils.editor.commands.Command hovered = org.argentumforge.engine.utils.editor.commands.CommandManager
                .getInstance().getHoveredCommand();
        if (hovered == null)
            return;

        int[] bounds = hovered.getAffectedBounds();
        if (bounds == null)
            return;

        int minX = bounds[0];
        int minY = bounds[1];
        int maxX = bounds[2];
        int maxY = bounds[3];

        int screenX = POS_SCREEN_X
                + (minX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetX;
        int screenY = POS_SCREEN_Y
                + (minY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetY;
        int width = (maxX - minX + 1) * TILE_PIXEL_SIZE;
        int height = (maxY - minY + 1) * TILE_PIXEL_SIZE;

        Engine.batch.draw(whiteTexture, screenX, screenY, 0, 0, 1, 1, width, height, true, 0.4f,
                new RGBColor(1.0f, 1.0f, 0.0f));
    }

    private void drawPreviewGrh(short grhIndex, int tileX, int tileY, int pixelOffsetX, int pixelOffsetY, float alpha) {
        if (mapData == null || tileX < 0 || tileX >= mapData.length || tileY < 0 || tileY >= mapData[0].length)
            return;

        int screenX = POS_SCREEN_X
                + (tileX - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetX;
        int screenY = POS_SCREEN_Y
                + (tileY - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * TILE_PIXEL_SIZE
                + pixelOffsetY;

        // Aplicar centrado para GRHs grandes (mismo sistema que Drawn.drawTexture)
        if (grhIndex > 0 && grhIndex < grhData.length && grhData[grhIndex] != null) {
            if (grhData[grhIndex].getTileWidth() != 1.0f) {
                screenX = screenX - (int) (grhData[grhIndex].getTileWidth() * TILE_PIXEL_SIZE / 2)
                        + TILE_PIXEL_SIZE / 2;
            }
            if (grhData[grhIndex].getTileHeight() != 1.0f) {
                screenY = screenY - (int) (grhData[grhIndex].getTileHeight() * TILE_PIXEL_SIZE) + TILE_PIXEL_SIZE;
            }
        }

        drawGrhIndex(grhIndex, screenX, screenY, alpha,
                weather.getWeatherColor());
    }

    /**
     * Renderiza una rejilla visual sobre el mapa.
     */
    private void renderGrid(RenderSettings renderSettings, int pixelOffsetX, int pixelOffsetY) {
        if (!renderSettings.isShowGrid())
            return;

        float[] colorArr = renderSettings.getGridColor();
        RGBColor gridColorObj = new RGBColor(colorArr[0], colorArr[1], colorArr[2]);
        float gridAlpha = colorArr[3];

        boolean showMajor = renderSettings.isShowMajorGrid();
        int majorInterval = renderSettings.getGridMajorInterval();
        float[] majorColorArr = renderSettings.getGridMajorColor();
        RGBColor majorColorObj = new RGBColor(majorColorArr[0], majorColorArr[1], majorColorArr[2]);
        float majorAlpha = majorColorArr[3];

        int tileSize = Camera.TILE_PIXEL_SIZE;

        // Calcular los límites de la pantalla en píxeles relativos al mapa
        int startX = POS_SCREEN_X + (camera.getMinXOffset() - TILE_BUFFER_SIZE) * tileSize + pixelOffsetX;
        int startY = POS_SCREEN_Y + (camera.getMinYOffset() - TILE_BUFFER_SIZE) * tileSize + pixelOffsetY;

        int endX = startX + (camera.getMaxX() - camera.getMinX() + 1 + TILE_BUFFER_SIZE * 2) * tileSize;
        int endY = startY + (camera.getMaxY() - camera.getMinY() + 1 + TILE_BUFFER_SIZE * 2) * tileSize;

        // Limitar al área de renderizado de la ventana
        int minY = Math.max(POS_SCREEN_Y, startY);
        int maxY = Math.min(POS_SCREEN_Y + Window.SCREEN_HEIGHT, endY);
        int minX = Math.max(POS_SCREEN_X, startX);
        int maxX = Math.min(POS_SCREEN_X + Window.SCREEN_WIDTH, endX);

        // Límites efectivos del mapa en pantalla (píxeles reales donde hay tiles)
        int mapPixelMinX = POS_SCREEN_X
                + (Camera.XMinMapSize - camera.getMinX() + camera.getMinXOffset() - TILE_BUFFER_SIZE) * tileSize
                + pixelOffsetX;
        int mapPixelMaxX = mapPixelMinX + (Camera.XMaxMapSize - Camera.XMinMapSize + 1) * tileSize;
        int mapPixelMinY = POS_SCREEN_Y
                + (Camera.YMinMapSize - camera.getMinY() + camera.getMinYOffset() - TILE_BUFFER_SIZE) * tileSize
                + pixelOffsetY;
        int mapPixelMaxY = mapPixelMinY + (Camera.YMaxMapSize - Camera.YMinMapSize + 1) * tileSize;

        // Área de recorte final: Donde la pantalla y el mapa se intersectan
        int clipMinX = Math.max(minX, mapPixelMinX);
        int clipMaxX = Math.min(maxX, mapPixelMaxX);
        int clipMinY = Math.max(minY, mapPixelMinY);
        int clipMaxY = Math.min(maxY, mapPixelMaxY);

        float currentGridAlpha = gridAlpha;
        if (renderSettings.isAdaptiveGrid()) {
            if (tileSize <= 12) {
                currentGridAlpha = 0;
            } else if (tileSize < 32) {
                // Fade out linearly from 32px down to 12px
                currentGridAlpha *= (tileSize - 12) / 20.0f;
            }
        }

        // Draw minor grid only if alpha > 0
        if (currentGridAlpha > 0) {
            // Dibujar líneas verticales
            int tileX = camera.getMinX() - TILE_BUFFER_SIZE;
            for (int x = startX; x <= endX; x += tileSize) {
                if (x >= clipMinX && x <= clipMaxX) {
                    boolean isMajor = showMajor && (tileX % majorInterval == 0);
                    if (!isMajor) {
                        Drawn.drawColoredRect(x, clipMinY, 1, clipMaxY - clipMinY, gridColorObj, currentGridAlpha);
                    }
                }
                tileX++;
            }

            // Dibujar líneas horizontales
            int tileY = camera.getMinY() - TILE_BUFFER_SIZE;
            for (int y = startY; y <= endY; y += tileSize) {
                if (y >= clipMinY && y <= clipMaxY) {
                    boolean isMajor = showMajor && (tileY % majorInterval == 0);
                    if (!isMajor) {
                        Drawn.drawColoredRect(clipMinX, y, clipMaxX - clipMinX, 1, gridColorObj, currentGridAlpha);
                    }
                }
                tileY++;
            }
        }

        // Always draw major grid if enabled
        if (showMajor) {
            float currentMajorAlpha = majorAlpha;
            if (renderSettings.isAdaptiveGrid() && tileSize < 12) {
                // Subtle fade for major grid when zooming out extremely far
                currentMajorAlpha *= Math.max(0.3f, tileSize / 12.0f);
            }

            int tileX = camera.getMinX() - TILE_BUFFER_SIZE;
            for (int x = startX; x <= endX; x += tileSize) {
                if (x >= clipMinX && x <= clipMaxX) {
                    if (tileX % majorInterval == 0) {
                        Drawn.drawColoredRect(x, clipMinY, 2, clipMaxY - clipMinY, majorColorObj, currentMajorAlpha);
                    }
                }
                tileX++;
            }

            int tileY = camera.getMinY() - TILE_BUFFER_SIZE;
            for (int y = startY; y <= endY; y += tileSize) {
                if (y >= clipMinY && y <= clipMaxY) {
                    if (tileY % majorInterval == 0) {
                        Drawn.drawColoredRect(clipMinX, y, clipMaxX - clipMinX, 2, majorColorObj, currentMajorAlpha);
                    }
                }
                tileY++;
            }
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}
