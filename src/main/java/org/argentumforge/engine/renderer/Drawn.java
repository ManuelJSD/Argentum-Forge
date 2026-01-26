package org.argentumforge.engine.renderer;

import org.argentumforge.engine.utils.inits.GrhInfo;

import static org.argentumforge.engine.Engine.renderer;
import static org.argentumforge.engine.scenes.Camera.TILE_PIXEL_SIZE;
import static org.argentumforge.engine.utils.AssetRegistry.grhData;
import static org.argentumforge.engine.utils.Time.deltaTime;

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

        /*
            TODO: Hago esto porque sino tengo que leer 3kk de lineas de codigo para precargar cada textura que se desea
            dibujar.

            En fin, hay que optimizar nuestro TextureManager para liberar la memoria y quitar texturas que no seran
            dibujadas. Esto del TextureManager y la precarga se debe hacer porque en OpenGL Moderno, tenemos que
            prepararlas antes de dibujar porque sino se glitchean las texturas.
         */
        TextureManager.requestTexture(grhData[grh_index].getFileNum()); // TODO: test

        geometryBoxRender(grh_index, x, y, src_width, src_height, sX, sY, blend, alpha, color, 1.0f, 1.0f);
    }

    public static void geometryBoxRender(int grh_index, int x, int y, int src_width, int src_height, float sX, float sY,
            boolean blend, float alpha, RGBColor color, float scaleX, float scaleY) {

        // hay que precargar todas las texturas xd....
        TextureManager.requestTexture(grh_index); // test

        Texture tex = TextureManager.getTexture(grhData[grh_index].getFileNum());
        if (tex == null) return; // todavía no está lista

        float globalScale = org.argentumforge.engine.scenes.Camera.getZoomScale();

        // Ajustar Y para que el escalado sea desde abajo (pivot bottom-center o
        // bottom-left)
        // Por defecto batch.draw dibuja desde top-left o bottom-left? Depende de la
        // coord system.
        // En este engine parece (0,0) top-left.
        // Si escalo Y > 1.0, el sprite crece hacia abajo.
        // Si queremos respiración (crecer hacia arriba desde los pies), necesitamos
        // ajustar Y.
        // El ajuste sería: y - (height * globalScale * scaleY - height * globalScale)
        // O más simple: drawHeight = src_height * globalScale * scaleY.

        float drawWidth = src_width * globalScale * scaleX;
        float drawHeight = src_height * globalScale * scaleY;

        // Ajuste para centrado horizontal si escala X cambia (opcional, por ahora
        // asumimos X=1.0)
        // Ajuste para pivote vertical (pies):
        // Si scaleY cambia, la imagen crece. Si crece hacia abajo y la posicion (x,y)
        // es top-left, los pies bajan.
        // Queremos que los pies se mantengan fijos.
        // Normalmente (x,y) es top-left del tile/sprite.
        // Si el sprite es mas alto que el tile (personajes altos), se dibuja hacia
        // arriba?
        // En `drawTexture` hay logica de centrado `center`.

        // Dado que no quiero romper todo, aplicaré el scale tal cual y veré si flota.
        // Si flota, corregiré en Character.java ajustando Y.

        // Corrección de pivote inferior (asumiendo Y crece hacia abajo en pantalla y
        // dibujamos desde top-left):
        // Si scaleY > 1, height aumenta.
        // Para que los pies queden igual, debemos subir Y (restar) la diferencia de
        // altura.
        float heightDiff = drawHeight - (src_height * globalScale);

        // PERO 'y' aquí ya viene calculado desde drawTexture.
        // Aplicaré el scale directo.

        renderer.draw(tex, x, y - heightDiff, sX, sY, src_width, src_height, drawWidth, drawHeight, blend, alpha, color);
    }

    /**
     * Lo mismo pero con una textura ya cargada.
     */
    public static void geometryBoxRender(Texture texture, int x, int y, int src_width, int src_height, float sX,
            float sY, boolean blend, float alpha, RGBColor color) {

        float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();

        if (texture == null) return;
        renderer.draw(texture, x, y, sX, sY, src_width, src_height, src_width * scale, src_height * scale, blend, alpha, color);
    }

    /**
     * Dibuja una textura en la pantalla
     */
    public static void drawTexture(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color) {
        drawTexture(grh, x, y, center, animate, blend, alpha, color, 1.0f, 1.0f);
    }

    public static void drawTexture(GrhInfo grh, int x, int y, boolean center, boolean animate, boolean blend,
            float alpha, RGBColor color, float scaleX, float scaleY) {
        if (grh.getGrhIndex() == 0 || grhData[grh.getGrhIndex()].getNumFrames() == 0)
            return;
        if (animate && grh.isStarted()) {
            grh.setFrameCounter(
                    grh.getFrameCounter() + (deltaTime * grhData[grh.getGrhIndex()].getNumFrames() / grh.getSpeed()));
            if (grh.getFrameCounter() > grhData[grh.getGrhIndex()].getNumFrames()) {
                grh.setFrameCounter((grh.getFrameCounter() % grhData[grh.getGrhIndex()].getNumFrames()) + 1);
                if (grh.getLoops() != -1) {
                    if (grh.getLoops() > 0)
                        grh.setLoops(grh.getLoops() - 1);
                    else
                        grh.setStarted(false);
                }
            }
        }

        final int currentGrhIndex = grhData[grh.getGrhIndex()].getFrame((int) (grh.getFrameCounter()));

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
                grhData[currentGrhIndex].getsY(), blend, alpha, color, scaleX, scaleY);
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


}
