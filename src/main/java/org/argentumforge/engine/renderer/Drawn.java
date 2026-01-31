package org.argentumforge.engine.renderer;

import org.argentumforge.engine.utils.inits.GrhInfo;

import static org.argentumforge.engine.Engine.batch;
import static org.argentumforge.engine.scenes.Camera.TILE_PIXEL_SIZE;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;
import static org.argentumforge.engine.utils.Time.deltaTime;
import org.argentumforge.engine.Engine;

/**
 * <p>
 * Clase utilitaria central para el sistema de renderizado que proporciona
 * metodos de dibujo para todos los elementos visuales.
 * <p>
 * Esta clase estatica contiene los metodos principales que permiten dibujar en
 * la pantalla de renderizado, ofreciendo funciones
 * para renderizar texturas, figuras geometricas y otros elementos graficos.
 * <p>
 * Modernizada para eliminar el modo inmediato (OpenGL Legacy) y utilizar
 * exclusivamente {@link BatchRenderer} para un pipeline grafico eficiente y
 * portable (preparado para OpenGL Core / GLES).
 */

public final class Drawn {

    /**
     * @param: grh_index  = Numero de indice de grafico del GrhData
     * @param: x,         y: posicion eje x e y de la pantalla.
     * @param: src_width, src_height: size de recorte
     * @param: sX,        sY: posicion de recorte
     * @param: blend:     efecto blend
     * @param: alpha:     efecto de transparencia (0 a 1)
     * @param: color:     objeto que contiene valores RGB como punto flotante (0 a
     *                    1).
     *                    Se encargara de guardar la textura en la grafica y
     *                    prepararla para su dibujado (en pocas palabras).
     */
    public static void geometryBoxRender(int grh_index, int x, int y, int src_width, int src_height, float sX, float sY,
            boolean blend, float alpha, RGBColor color) {
        geometryBoxRender(grh_index, x, y, src_width, src_height, sX, sY, blend, alpha, color, 1.0f, 1.0f, 0.0f);
    }

    public static void geometryBoxRender(int grh_index, int x, int y, int src_width, int src_height, float sX, float sY,
            boolean blend, float alpha, RGBColor color, float scaleX, float scaleY, float skewX) {
        if (grhData == null)
            return;
        final Texture texture = Surface.INSTANCE.getTexture(grhData[grh_index].getFileNum());
        float globalScale = org.argentumforge.engine.scenes.Camera.getZoomScale();

        float drawWidth = src_width * globalScale * scaleX;
        float drawHeight = src_height * globalScale * scaleY;

        float heightDiff = drawHeight - (src_height * globalScale);

        batch.draw(texture, x, y - heightDiff, sX, sY, src_width, src_height, drawWidth, drawHeight, skewX, blend,
                alpha,
                color);
    }

    /**
     * Lo mismo pero con una textura ya cargada.
     */
    public static void geometryBoxRender(Texture texture, int x, int y, int src_width, int src_height, float sX,
            float sY, boolean blend, float alpha, RGBColor color) {
        geometryBoxRender(texture, x, y, src_width, src_height, sX, sY, 0.0f, blend, alpha, color);
    }

    public static void geometryBoxRender(Texture texture, int x, int y, int src_width, int src_height, float sX,
            float sY, float skewX, boolean blend, float alpha, RGBColor color) {
        float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();
        batch.draw(texture, x, y, sX, sY, src_width, src_height, src_width * scale, src_height * scale, skewX, blend,
                alpha, color);
    }

    /**
     * Dibuja una textura en la pantalla
     */
    public static void drawTexture(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color) {
        drawTexture(grh, x, y, center, animate, blend, alpha, color, 1.0f, 1.0f, 0.0f);
    }

    public static void drawTexture(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color, float scaleX, float scaleY) {
        drawTexture(grh, x, y, center, animate, blend, alpha, color, scaleX, scaleY, 0.0f);
    }

    public static void drawTexture(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color, float scaleX, float scaleY, float skewX) {
        if (grhData == null)
            return;
        if (grh.getGrhIndex() <= 0 || grh.getGrhIndex() >= grhData.length || grhData[grh.getGrhIndex()] == null
                || grhData[grh.getGrhIndex()].getNumFrames() == 0)
            return;
        if (animate && grh.isStarted()) {
            grh.update(deltaTime);
        }

        int gIdx2 = grh.getGrhIndex();
        if (gIdx2 >= grhData.length || grhData[gIdx2] == null)
            return;
        final int currentGrhIndex = grhData[gIdx2].getFrame((int) (grh.getFrameCounter()));

        if (currentGrhIndex <= 0 || currentGrhIndex >= grhData.length || grhData[currentGrhIndex] == null)
            return;

        if (center) {
            if (grhData[currentGrhIndex].getTileWidth() != 1)
                x = x - (int) (grhData[currentGrhIndex].getTileWidth() * TILE_PIXEL_SIZE / 2) + TILE_PIXEL_SIZE / 2;
            if (grhData[currentGrhIndex].getTileHeight() != 1)
                y = y - (int) (grhData[currentGrhIndex].getTileHeight() * TILE_PIXEL_SIZE) + TILE_PIXEL_SIZE;
        }

        if (currentGrhIndex == 0 || grhData[currentGrhIndex].getFileNum() == 0)
            return;

        geometryBoxRender(currentGrhIndex, x, y,
                grhData[currentGrhIndex].getPixelWidth(),
                grhData[currentGrhIndex].getPixelHeight(),
                grhData[currentGrhIndex].getsX(),
                grhData[currentGrhIndex].getsY(), blend, alpha, color, scaleX, scaleY, skewX);
    }

    /**
     * Dibuja una textura con un gradiente de color por vértice.
     */
    public static void drawGradient(Texture texture, int x, int y, int width, int height,
            float r1, float g1, float b1, float a1,
            float r2, float g2, float b2, float a2,
            float r3, float g3, float b3, float a3,
            float r4, float g4, float b4, float a4) {
        batch.draw(texture, x, y, 0, 0, texture.getTex_width(), texture.getTex_height(),
                width, height, false,
                r1, g1, b1, a1,
                r2, g2, b2, a2,
                r3, g3, b3, a3,
                r4, g4, b4, a4);
    }

    /**
     * Dibuja un efecto de viñeta (oscurecimiento de bordes).
     */
    public static void drawVignette(int width, int height, float intensity) {
        Texture white = Surface.INSTANCE.getWhiteTexture();
        int vSize = (int) (Math.min(width, height) * 0.4f);

        // Top
        drawGradient(white, 0, 0, width, vSize,
                0, 0, 0, 0, // V0 (Bottom-Left)
                0, 0, 0, intensity, // V1 (Top-Left)
                0, 0, 0, intensity, // V2 (Top-Right)
                0, 0, 0, 0 // V3 (Bottom-Right)
        );

        // Bottom
        drawGradient(white, 0, height - vSize, width, vSize,
                0, 0, 0, intensity,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, intensity);

        // Left
        drawGradient(white, 0, 0, vSize, height,
                0, 0, 0, intensity,
                0, 0, 0, intensity,
                0, 0, 0, 0,
                0, 0, 0, 0);

        // Right
        drawGradient(white, width - vSize, 0, vSize, height,
                0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0, intensity,
                0, 0, 0, intensity);
    }

    public static void drawRect(int x, int y, int width, int height, float r, float g, float b, float a) {
        batch.draw(Surface.INSTANCE.getWhiteTexture(), x, y, 0, 0, 1, 1, width, height, false, a,
                new RGBColor(r, g, b));
    }

    /**
     * Dibujamos sin animacion
     */
    public static void drawGrhIndex(int grhIndex, int x, int y, RGBColor color) {
        drawGrhIndex(grhIndex, x, y, 1.0f, color);
    }

    /**
     * Dibujamos sin animacion con opacidad ajustable
     */
    public static void drawGrhIndex(int grhIndex, int x, int y, float alpha, RGBColor color) {
        if (grhData == null)
            return;
        if (grhIndex <= 0 || grhIndex >= grhData.length || grhData[grhIndex] == null)
            return;
        if (color == null)
            color = new RGBColor(1.0f, 1.0f, 1.0f);
        geometryBoxRender(grhIndex, x, y,
                grhData[grhIndex].getPixelWidth(),
                grhData[grhIndex].getPixelHeight(),
                grhData[grhIndex].getsX(),
                grhData[grhIndex].getsY(), false, alpha, color);
    }

    /**
     * ===============================================================
     * Métodos Legacy redirigidos al BatchRenderer
     * ===============================================================
     */

    public static void drawTextureNoBatch(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color) {
        // Redirigir a la implementación con Batch, ya que el modo inmediato es obsoleto
        drawTexture(grh, x, y, center, animate, blend, alpha, color);
    }

    public static void drawGrhIndexNoBatch(int grhIndex, int x, int y, RGBColor color) {
        drawGrhIndex(grhIndex, x, y, 1.0f, color);
    }

    @Deprecated(forRemoval = true)
    public static void geometryBoxRenderNoBatch(int grh_index, int x, int y, int src_width, int src_height, float sX,
            float sY, boolean blend, float alpha, RGBColor color) {
        // Redirigido a la versión con Batch
        geometryBoxRender(grh_index, x, y, src_width, src_height, sX, sY, blend, alpha, color);
    }

    public static void geometryBoxRenderGUI(Texture texture, int x, int y, float alpha) {
        geometryBoxRenderGUI(texture, x, y, texture.getTex_width(), texture.getTex_height(), alpha);
    }

    /**
     * Versión GUI optimizada con BatchRenderer
     */
    public static void geometryBoxRenderGUI(Texture texture, int x, int y, int width, int height, float alpha) {
        // En GUI no usamos escala de cámara, dibujamos directo en coords de pantalla
        batch.draw(texture, x, y, 0, 0, texture.getTex_width(), texture.getTex_height(),
                width, height, 0.0f, true, alpha, new RGBColor(1, 1, 1));
    }

    /**
     * Dibuja un rectángulo coloreado en pantalla utilizando el sistema de batching.
     */
    public static void drawColoredRect(int x, int y, int width, int height, RGBColor color, float alpha) {
        if (color == null) {
            color = new RGBColor(1.0f, 1.0f, 1.0f);
        }
        Engine.batch.draw(Surface.INSTANCE.getWhiteTexture(), x, y, 0, 0, 1, 1, width, height, true, alpha, color);
    }

}
