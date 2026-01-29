package org.argentumforge.engine.renderer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL11.*;

/**
 * Clase Batch Renderer <br>
 * <br>
 *
 * Que es mas rapido? Hacer dibujos 1x1 o tener toda la info necesaria para
 * dibujar de una y una sola vez? <br>
 * <br>
 *
 * Esto es lo que hace el batch rendering, antes de dibujar empieza a colocar
 * cada textura en su posicion
 * con su recorte, blend, color, etc. Para luego al momento de dibujar solo
 * tenga que hacerlo sabiendo donde se encuentra
 * cada textura y como tiene que estar.
 */
public class BatchRenderer {

    /**
     * Toda la informacion de una quad con una textura.
     */
    private static class Quad {
        float x, y, srcWidth, srcHeight, destWidth, destHeight;
        float srcX, srcY;
        float texWidth, texHeight;
        float r1, g1, b1, a1; // Vertex 0
        float r2, g2, b2, a2; // Vertex 1
        float r3, g3, b3, a3; // Vertex 2
        float r4, g4, b4, a4; // Vertex 3
        float skewX; // Displacement for top vertices
        boolean blend;
        Texture texture;
    }

    private final List<Quad> quads = new ArrayList<>();
    private int activeQuads = 0;

    // Búfer para datos de vértices: pos(2), tex(2), color(4) = 8 floats por vértice
    // 4 vértices por Quad = 32 floats por Quad
    private static final int STRIDE = 8;
    private static final int QUAD_SIZE = 4 * STRIDE;
    private FloatBuffer vertexBuffer;
    private int maxQuads = 1000;

    public BatchRenderer() {
        vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * QUAD_SIZE);
    }

    private void ensureCapacity(int quadsNeeded) {
        if (quadsNeeded > maxQuads) {
            maxQuads = quadsNeeded + 500;
            vertexBuffer = BufferUtils.createFloatBuffer(maxQuads * QUAD_SIZE);
        }
    }

    /**
     * Vaciamos nuestro array de quads con texturas para que se prepare a dibujar
     * una nueva imagen.
     */
    public void begin() {
        activeQuads = 0;
    }

    /**
     * Preparamos las cosas para el dibujado, creando o reutilizando un quad con
     * su informacion de textura, recorte, pos de recorte
     * color y demas...
     */
    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, boolean blend, float alpha, RGBColor color) {
        draw(texture, x, y, srcX, srcY, srcWidth, srcHeight, destWidth, destHeight, 0.0f, blend, alpha, color);
    }

    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, float skewX, boolean blend, float alpha, RGBColor color) {

        Quad quad;
        if (activeQuads < quads.size()) {
            quad = quads.get(activeQuads);
        } else {
            quad = new Quad();
            quads.add(quad);
        }
        activeQuads++;

        quad.x = x;
        quad.y = y;
        quad.srcWidth = srcWidth;
        quad.srcHeight = srcHeight;
        quad.destWidth = destWidth;
        quad.destHeight = destHeight;
        quad.srcX = srcX;
        quad.srcY = srcY;
        quad.skewX = skewX;
        quad.texWidth = texture.getTex_width();
        quad.texHeight = texture.getTex_height();
        quad.r1 = quad.r2 = quad.r3 = quad.r4 = color.getRed();
        quad.g1 = quad.g2 = quad.g3 = quad.g4 = color.getGreen();
        quad.b1 = quad.b2 = quad.b3 = quad.b4 = color.getBlue();
        quad.a1 = quad.a2 = quad.a3 = quad.a4 = alpha;
        quad.texture = texture;
        quad.blend = blend;
    }

    /**
     * Dibuja una textura con colores independientes por cada vértice (gradiente).
     */
    public void draw(Texture texture, float x, float y, float srcX, float srcY, float srcWidth, float srcHeight,
            float destWidth, float destHeight, boolean blend,
            float r1, float g1, float b1, float a1,
            float r2, float g2, float b2, float a2,
            float r3, float g3, float b3, float a3,
            float r4, float g4, float b4, float a4) {

        Quad quad;
        if (activeQuads < quads.size()) {
            quad = quads.get(activeQuads);
        } else {
            quad = new Quad();
            quads.add(quad);
        }
        activeQuads++;

        quad.x = x;
        quad.y = y;
        quad.srcWidth = srcWidth;
        quad.srcHeight = srcHeight;
        quad.destWidth = destWidth;
        quad.destHeight = destHeight;
        quad.srcX = srcX;
        quad.srcY = srcY;
        quad.texWidth = texture.getTex_width();
        quad.texHeight = texture.getTex_height();
        quad.r1 = r1;
        quad.g1 = g1;
        quad.b1 = b1;
        quad.a1 = a1;
        quad.r2 = r2;
        quad.g2 = g2;
        quad.b2 = b2;
        quad.a2 = a2;
        quad.r3 = r3;
        quad.g3 = g3;
        quad.b3 = b3;
        quad.a3 = a3;
        quad.r4 = r4;
        quad.g4 = g4;
        quad.b4 = b4;
        quad.a4 = a4;
        quad.texture = texture;
        quad.blend = blend;
    }

    /**
     * Recorre nuestro array de quads y dibuja todas las texturas optimizadamente.
     */
    public void end() {
        if (activeQuads == 0)
            return;

        ensureCapacity(activeQuads);

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);

        Texture lastTexture = null;
        int batchStart = 0;
        int quadsInBatch = 0;
        boolean lastBlend = false;

        for (int i = 0; i <= activeQuads; i++) {
            Quad quad = (i < activeQuads) ? quads.get(i) : null;

            // Detectar cambio de estado (textura o blend) o fin de lista
            if (i == activeQuads || (lastTexture != null && (lastTexture != quad.texture || lastBlend != quad.blend))) {
                // Dibujar lote acumulado
                if (quadsInBatch > 0) {
                    renderBatch(batchStart, quadsInBatch, lastTexture, lastBlend);
                }
                batchStart = i;
                quadsInBatch = 0;
            }

            if (quad != null) {
                fillQuadData(i, quad);
                lastTexture = quad.texture;
                lastBlend = quad.blend;
                quadsInBatch++;
            }
        }

        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);
    }

    private void fillQuadData(int index, Quad quad) {
        int offset = index * QUAD_SIZE;

        float u0 = quad.srcX / quad.texWidth;
        float v0 = (quad.srcY + quad.srcHeight) / quad.texHeight;
        float u1 = (quad.srcX + quad.srcWidth) / quad.texWidth;
        float v1 = quad.srcY / quad.texHeight;

        // Vértice 0 (Bottom-Left en coordenadas de mapa/GL invertido)
        // Antes tenía skewX aquí, pero y+destHeight es el fondo del sprite (pies)
        vertexBuffer.put(offset + 0, quad.x);
        vertexBuffer.put(offset + 1, quad.y + quad.destHeight);
        vertexBuffer.put(offset + 2, u0);
        vertexBuffer.put(offset + 3, v0);
        vertexBuffer.put(offset + 4, quad.r1);
        vertexBuffer.put(offset + 5, quad.g1);
        vertexBuffer.put(offset + 6, quad.b1);
        vertexBuffer.put(offset + 7, quad.a1);

        // Vértice 1 (Top-Left)
        // Aplicamos skewX aquí para que la cabeza se incline pero los pies queden fijos
        vertexBuffer.put(offset + 8, quad.x + quad.skewX);
        vertexBuffer.put(offset + 9, quad.y);
        vertexBuffer.put(offset + 10, u0);
        vertexBuffer.put(offset + 11, v1);
        vertexBuffer.put(offset + 12, quad.r2);
        vertexBuffer.put(offset + 13, quad.g2);
        vertexBuffer.put(offset + 14, quad.b2);
        vertexBuffer.put(offset + 15, quad.a2);

        // Vértice 2 (Top-Right)
        vertexBuffer.put(offset + 16, quad.x + quad.destWidth + quad.skewX);
        vertexBuffer.put(offset + 17, quad.y);
        vertexBuffer.put(offset + 18, u1);
        vertexBuffer.put(offset + 19, v1);
        vertexBuffer.put(offset + 20, quad.r3);
        vertexBuffer.put(offset + 21, quad.g3);
        vertexBuffer.put(offset + 22, quad.b3);
        vertexBuffer.put(offset + 23, quad.a3);

        // Vértice 3 (Bottom-Right)
        vertexBuffer.put(offset + 24, quad.x + quad.destWidth);
        vertexBuffer.put(offset + 25, quad.y + quad.destHeight);
        vertexBuffer.put(offset + 26, u1);
        vertexBuffer.put(offset + 27, v0);
        vertexBuffer.put(offset + 28, quad.r4);
        vertexBuffer.put(offset + 29, quad.g4);
        vertexBuffer.put(offset + 30, quad.b4);
        vertexBuffer.put(offset + 31, quad.a4);
    }

    private void renderBatch(int start, int count, Texture texture, boolean blend) {
        texture.bind();
        if (blend) {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);
        } else {
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        }

        vertexBuffer.position(start * QUAD_SIZE);

        // Pointer offset: pos(0-1), tex(2-3), color(4-7)
        glVertexPointer(2, GL_FLOAT, STRIDE * 4, vertexBuffer);

        vertexBuffer.position(start * QUAD_SIZE + 2);
        glTexCoordPointer(2, GL_FLOAT, STRIDE * 4, vertexBuffer);

        vertexBuffer.position(start * QUAD_SIZE + 4);
        glColorPointer(4, GL_FLOAT, STRIDE * 4, vertexBuffer);

        glDrawArrays(GL_QUADS, 0, count * 4);

        vertexBuffer.position(0);
    }

}