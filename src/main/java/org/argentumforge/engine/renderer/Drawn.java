package org.argentumforge.engine.renderer;

import org.argentumforge.engine.utils.inits.GrhInfo;

import static org.argentumforge.engine.Engine.batch;
import static org.argentumforge.engine.scenes.Camera.TILE_PIXEL_SIZE;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;
import static org.argentumforge.engine.utils.Time.deltaTime;
import org.argentumforge.engine.Engine;

import static org.lwjgl.opengl.GL11.*;

/**
 * <p>
 * Clase utilitaria central para el sistema de renderizado que proporciona
 * metodos de dibujo para todos los elementos visuales.
 * <p>
 * Esta clase estatica contiene los metodos principales que permiten dibujar en
 * la pantalla de renderizado, ofreciendo funciones
 * para renderizar texturas, figuras geometricas, lineas, rectangulos y otros
 * elementos graficos con diferentes propiedades y
 * transformaciones.
 * <p>
 * Implementa la funcionalidad de bajo nivel para el renderizado mediante
 * OpenGL, encapsulando las operaciones complejas en
 * metodos faciles de usar. Maneja aspectos como la aplicacion de efectos de
 * mezcla (blending), transparencia, colores y
 * transformaciones de coordenadas.
 * <p>
 * Es utilizada por todos los subsistemas graficos del juego para dibujar desde
 * elementos de la interfaz de usuario hasta
 * personajes, objetos del mapa y efectos visuales. La mayor parte del
 * renderizado visible pasa por los metodos de esta clase.
 * <p>
 * El dibujado especifico de personajes se encuentra en la clase
 * {@link Character}, el de la interfaz de usuario en
 * {@code ElementGUI} y los textos en la clase {@code FontText}.
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
            // Check if animations are disabled in options
            if (org.argentumforge.engine.game.Options.INSTANCE.getRenderSettings().isDisableAnimations()) {
                // If disabled, just ensure we are using the first frame logic or just not
                // updating frame counter
                // But we still want to draw the current frame.
                // If we don't update frame counter, it stays static.
            } else {
                int gIdx = grh.getGrhIndex();
                if (gIdx >= grhData.length || grhData[gIdx] == null)
                    return;
                grh.setFrameCounter(
                        grh.getFrameCounter() + (deltaTime * grhData[gIdx].getNumFrames() / grh.getSpeed()));
                if (grh.getFrameCounter() > grhData[gIdx].getNumFrames()) {
                    grh.setFrameCounter((grh.getFrameCounter() % grhData[gIdx].getNumFrames()) + 1);
                    if (grh.getLoops() != -1) {
                        if (grh.getLoops() > 0)
                            grh.setLoops(grh.getLoops() - 1);
                        else
                            grh.setStarted(false);
                    }
                }
            }
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
        // Usar blend=false para GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA (transparencia
        // estándar)
        batch.draw(texture, x, y, 0, 0, texture.getTex_width(), texture.getTex_height(),
                width, height, false,
                r1, g1, b1, a1,
                r2, g2, b2, a2,
                r3, g3, b3, a3,
                r4, g4, b4, a4);
    }

    /**
     * Dibuja un efecto de viñeta (oscurecimiento de bordes) sobre toda la pantalla.
     * Mapeo de Vértices: V1=Top-Left, V0=Bottom-Left, V2=Top-Right, V3=Bottom-Right
     */
    public static void drawVignette(int width, int height, float intensity) {
        Texture white = Surface.INSTANCE.getWhiteTexture();
        int vSize = (int) (Math.min(width, height) * 0.4f);

        // Top: Oscuro arriba (V1, V2), Transparente abajo (V0, V3)
        drawGradient(white, 0, 0, width, vSize,
                0, 0, 0, 0, // V0 (Bottom-Left)
                0, 0, 0, intensity, // V1 (Top-Left)
                0, 0, 0, intensity, // V2 (Top-Right)
                0, 0, 0, 0 // V3 (Bottom-Right)
        );

        // Bottom: Transparente arriba (V1, V2), Oscuro abajo (V0, V3)
        drawGradient(white, 0, height - vSize, width, vSize,
                0, 0, 0, intensity, // V0 (Bottom-Left)
                0, 0, 0, 0, // V1 (Top-Left)
                0, 0, 0, 0, // V2 (Top-Right)
                0, 0, 0, intensity // V3 (Bottom-Right)
        );

        // Left: Oscuro izquierda (V0, V1), Transparente derecha (V2, V3)
        drawGradient(white, 0, 0, vSize, height,
                0, 0, 0, intensity, // V0 (Bottom-Left)
                0, 0, 0, intensity, // V1 (Top-Left)
                0, 0, 0, 0, // V2 (Top-Right)
                0, 0, 0, 0 // V3 (Bottom-Right)
        );

        // Right: Transparente izquierda (V0, V1), Oscuro derecha (V2, V3)
        drawGradient(white, width - vSize, 0, vSize, height,
                0, 0, 0, 0, // V0 (Bottom-Left)
                0, 0, 0, 0, // V1 (Top-Left)
                0, 0, 0, intensity, // V2 (Top-Right)
                0, 0, 0, intensity // V3 (Bottom-Right)
        );
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
     * Dibujado sin batch (lo hago para el frmCrearPersonaje).
     * ===============================================================
     */

    /**
     * Dibuja una textura en la pantalla
     */
    public static void drawTextureNoBatch(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color) {
        if (grhData == null)
            return;
        if (grh.getGrhIndex() <= 0 || grh.getGrhIndex() >= grhData.length || grhData[grh.getGrhIndex()] == null
                || grhData[grh.getGrhIndex()].getNumFrames() == 0)
            return;
        if (animate && grh.isStarted()) {
            // Check if animations are disabled
            if (org.argentumforge.engine.game.Options.INSTANCE.getRenderSettings().isDisableAnimations()) {
                // Do nothing, effectively freezing the animation
            } else {
                int gIdx = grh.getGrhIndex();
                if (gIdx >= grhData.length || grhData[gIdx] == null)
                    return;
                grh.setFrameCounter(
                        grh.getFrameCounter() + (deltaTime * grhData[gIdx].getNumFrames() / grh.getSpeed()));
                if (grh.getFrameCounter() > grhData[gIdx].getNumFrames()) {
                    grh.setFrameCounter((grh.getFrameCounter() % grhData[gIdx].getNumFrames()) + 1);
                    if (grh.getLoops() != -1) {
                        if (grh.getLoops() > 0)
                            grh.setLoops(grh.getLoops() - 1);
                        else
                            grh.setStarted(false);
                    }
                }
            }
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

        geometryBoxRenderNoBatch(currentGrhIndex, x, y,
                grhData[currentGrhIndex].getPixelWidth(),
                grhData[currentGrhIndex].getPixelHeight(),
                grhData[currentGrhIndex].getsX(),
                grhData[currentGrhIndex].getsY(), blend, alpha, color);
    }

    /**
     * Dibujamos sin animacion
     */
    public static void drawGrhIndexNoBatch(int grhIndex, int x, int y, RGBColor color) {
        if (color == null)
            color = new RGBColor(1.0f, 1.0f, 1.0f);
        geometryBoxRenderNoBatch(grhIndex, x, y,
                grhData[grhIndex].getPixelWidth(),
                grhData[grhIndex].getPixelHeight(),
                grhData[grhIndex].getsX(),
                grhData[grhIndex].getsY(), false, 1.0f, color);
    }

    public static void geometryBoxRenderNoBatch(int grh_index, int x, int y, int src_width, int src_height, float sX,
            float sY, boolean blend, float alpha, RGBColor color) {
        if (grhData == null)
            return;
        if (blend)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        final Texture texture = Surface.INSTANCE.getTexture(grhData[grh_index].getFileNum());
        final float src_right = sX + src_width;
        final float src_bottom = sY + src_height;

        texture.bind();
        glBegin(GL_QUADS);

        {
            float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();
            float dWidth = src_width * scale;
            float dHeight = src_height * scale;

            // 0----0
            // | |
            // 1----0
            glColor4f(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            glTexCoord2f(sX / texture.getTex_width(), (src_bottom) / texture.getTex_height());
            glVertex2d(x, y + dHeight);

            // 1----0
            // | |
            // 0----0
            glColor4f(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            glTexCoord2f(sX / texture.getTex_width(), sY / texture.getTex_height());
            glVertex2d(x, y);

            // 0----1
            // | |
            // 0----0
            glColor4f(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            glTexCoord2f((src_right) / texture.getTex_width(), sY / texture.getTex_height());
            glVertex2d(x + dWidth, y);

            // 0----0
            // | |
            // 0----1
            glColor4f(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            glTexCoord2f((src_right) / texture.getTex_width(), (src_bottom) / texture.getTex_height());
            glVertex2d(x + dWidth, y + dHeight);
        }

        texture.unbind();
        glEnd();

        if (blend)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void geometryBoxRenderGUI(Texture texture, int x, int y, float alpha) {
        texture.bind();
        glBegin(GL_QUADS);

        {
            // 0----0
            // | |,
            // 1----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(0, 1);
            glVertex2d(x, y + texture.getTex_height());

            // 1----0
            // | |
            // 0----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(0, 0);
            glVertex2d(x, y);

            // 0----1
            // | |
            // 0----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(1, 0);
            glVertex2d(x + texture.getTex_width(), y);

            // 0----0
            // | |
            // 0----1
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(1, 1);
            glVertex2d(x + texture.getTex_width(), y + texture.getTex_height());
        }

        texture.unbind();
        glEnd();
    }

    /**
     * Versión sobrecargada que permite especificar ancho y alto personalizados.
     * Útil para escalar texturas a diferentes tamaños de ventana.
     */
    public static void geometryBoxRenderGUI(Texture texture, int x, int y, int width, int height, float alpha) {
        texture.bind();
        glBegin(GL_QUADS);

        {
            // 0----0
            // | |
            // 1----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(0, 1);
            glVertex2d(x, y + height);

            // 1----0
            // | |
            // 0----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(0, 0);
            glVertex2d(x, y);

            // 0----1
            // | |
            // 0----0
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(1, 0);
            glVertex2d(x + width, y);

            // 0----0
            // | |
            // 0----1
            glColor4f(1.0f, 1.0f, 1.0f, alpha);
            glTexCoord2f(1, 1);
            glVertex2d(x + width, y + height);
        }

        texture.unbind();
        glEnd();
    }

    /**
     * Dibuja un rectángulo de color sólido sin textura.
     * Útil para overlays y efectos visuales.
     */
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
