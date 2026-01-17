package org.argentumforge.engine.scenes;

import org.argentumforge.engine.Window;

/**
 * La clase Camera gestiona la visualizacion del juego y el campo de vision del
 * usuario.
 * <p>
 * Esta clase contiene variables relacionadas con el campo de vision del juego,
 * incluyendo la distancia de dibujado, el tamano del
 * render en pantalla, el tamano de los tiles (32x32), y los bordes del mapa. Su
 * funcion principal es determinar que elementos del
 * mapa son visibles en la pantalla y calcular las coordenadas necesarias para
 * renderizar correctamente el mundo.
 * <p>
 * Ademas, proporciona el metodo update() que actualiza las variables
 * relacionadas con la distancia de dibujado segun la posicion
 * del usuario, permitiendo una visualizacion correcta mientras el personaje se
 * desplaza por el mapa. Esto es fundamental para
 * optimizar el rendimiento, ya que solo se renderizan los elementos que son
 * visibles para el jugador.
 * <p>
 * La camara tambien es utilizada en otras escenas para calcular correctamente
 * las posiciones de renderizado y controlar que
 * sectores del mapa se deben dibujar en cada momento.
 */

public final class Camera {

    public static int POS_SCREEN_X = 0;
    public static int POS_SCREEN_Y = 0;

    public static int TILE_PIXEL_SIZE = 32;
    public static final int TILE_BUFFER_SIZE = 7;

    // Rango maximo de la matriz del mapa.
    public static final int XMaxMapSize = 100;
    public static final int XMinMapSize = 1;
    public static final int YMaxMapSize = 100;
    public static final int YMinMapSize = 1;

    // Variables de pantalla dinámicas
    public static int HALF_WINDOW_TILE_WIDTH;
    public static int HALF_WINDOW_TILE_HEIGHT;

    // limites del mapa
    public static int minXBorder;
    public static int maxXBorder;
    public static int minYBorder;
    public static int maxYBorder;

    static {
        updateConstants();
    }

    public static void updateConstants() {
        int tilesW = Window.SCREEN_WIDTH / TILE_PIXEL_SIZE;
        int tilesH = Window.SCREEN_HEIGHT / TILE_PIXEL_SIZE;

        HALF_WINDOW_TILE_WIDTH = tilesW / 2;
        HALF_WINDOW_TILE_HEIGHT = tilesH / 2;

        // Centering logic:
        // 1. Remainder centering: centers the grid in the window if resolution is not
        // multiple of 32
        int remainderX = (Window.SCREEN_WIDTH % TILE_PIXEL_SIZE) / 2;
        int remainderY = (Window.SCREEN_HEIGHT % TILE_PIXEL_SIZE) / 2;

        // 2. Even tile count offset: If we have even tiles, center falls between tiles.
        // Shift left by half tile (-16px) to center the character tile.
        int evenOffsetX = (tilesW % 2 == 0) ? -(TILE_PIXEL_SIZE / 2) : 0;
        int evenOffsetY = (tilesH % 2 == 0) ? -(TILE_PIXEL_SIZE / 2) : 0;

        POS_SCREEN_X = remainderX + evenOffsetX;
        POS_SCREEN_Y = remainderY + evenOffsetY;

        minXBorder = XMinMapSize + HALF_WINDOW_TILE_WIDTH;
        maxXBorder = XMaxMapSize - HALF_WINDOW_TILE_WIDTH;
        minYBorder = YMinMapSize + HALF_WINDOW_TILE_HEIGHT;
        maxYBorder = YMaxMapSize - HALF_WINDOW_TILE_HEIGHT;
    }

    public static float getZoomScale() {
        return TILE_PIXEL_SIZE / 32.0f;
    }

    public static void setTileSize(int size) {
        if (size < 16)
            size = 16;
        if (size > 128)
            size = 128;
        TILE_PIXEL_SIZE = size;
        updateConstants();
    }

    private int screenminY, screenmaxY;
    private int screenminX, screenmaxX;
    private int minY, maxY;
    private int minX, maxX;
    private int screenX, screenY;
    private int minXOffset, minYOffset;

    public Camera() {

    }

    /**
     * @param tileX: Posicion X donde este parado nuestro usuario.
     * @param tileY: Posicion Y donde este parado nuestro usuario.
     *               Esta es toda la logica que se encontraba al principio del
     *               "RenderScreen", permite actualizar la distancia de
     *               dibujado segun la posicion en la que se encuentre el usuario.
     *               Esto sirve para el recorrido de la matriz del MapData, cada
     *               uno tiene distinto rango segun la capa que se va a dibujar.
     */
    public void update(int tileX, int tileY) {
        screenX = 0;
        screenY = 0;
        minXOffset = 0;
        minYOffset = 0;

        // esto es un rango de vision segun la pantalla y donde este parado el user
        screenminY = tileY - HALF_WINDOW_TILE_HEIGHT;
        screenmaxY = tileY + HALF_WINDOW_TILE_HEIGHT;
        screenminX = tileX - HALF_WINDOW_TILE_WIDTH;
        screenmaxX = tileX + HALF_WINDOW_TILE_WIDTH;

        // este es el rango minimo que se va a recorrer
        minY = screenminY - TILE_BUFFER_SIZE;
        maxY = screenmaxY + TILE_BUFFER_SIZE;
        minX = screenminX - TILE_BUFFER_SIZE;
        maxX = screenmaxX + TILE_BUFFER_SIZE;

        if (minY < XMinMapSize) {
            minYOffset = YMinMapSize - minY;
            minY = YMinMapSize;
        }

        if (maxY > YMaxMapSize)
            maxY = YMaxMapSize;

        if (minX < XMinMapSize) {
            minXOffset = XMinMapSize - minX;
            minX = XMinMapSize;
        }

        if (maxX > XMaxMapSize)
            maxX = XMaxMapSize;

        // Removed legacy decrement logic that caused off-center rendering
        // if (screenminY > YMinMapSize) screenminY--; ...
        // if (screenminX > XMinMapSize) screenminX--; ...
    }

    public int getScreenminY() {
        return screenminY;
    }

    public void setScreenminY(int screenminY) {
        this.screenminY = screenminY;
    }

    public int getScreenmaxY() {
        return screenmaxY;
    }

    public void setScreenmaxY(int screenmaxY) {
        this.screenmaxY = screenmaxY;
    }

    public int getScreenminX() {
        return screenminX;
    }

    public void setScreenminX(int screenminX) {
        this.screenminX = screenminX;
    }

    public int getScreenmaxX() {
        return screenmaxX;
    }

    public void setScreenmaxX(int screenmaxX) {
        this.screenmaxX = screenmaxX;
    }

    public int getMinY() {
        return minY;
    }

    public void setMinY(int minY) {
        this.minY = minY;
    }

    public int getMaxY() {
        return maxY;
    }

    public void setMaxY(int maxY) {
        this.maxY = maxY;
    }

    public int getMinX() {
        return minX;
    }

    public void setMinX(int minX) {
        this.minX = minX;
    }

    public int getMaxX() {
        return maxX;
    }

    public void setMaxX(int maxX) {
        this.maxX = maxX;
    }

    public int getScreenX() {
        return screenX;
    }

    public void setScreenX(int screenX) {
        this.screenX = screenX;
    }

    public void incrementScreenX() {
        this.screenX++;
    }

    public void incrementScreenY() {
        this.screenY++;
    }

    public int getScreenY() {
        return screenY;
    }

    public void setScreenY(int screenY) {
        this.screenY = screenY;
    }

    public int getMinXOffset() {
        return minXOffset;
    }

    public int getMinYOffset() {
        return minYOffset;
    }

}
