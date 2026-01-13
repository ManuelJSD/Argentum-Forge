package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Clase singleton para gestionar la edición de bloqueos en el mapa.
 * Permite activar/desactivar bloqueos en tiles específicos del mapa.
 */
public class Block {

    private static volatile Block instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = bloquear, 2 = desbloquear, 3 = invertir
    private int brushSize = 1; // 1, 3, 5...

    private Block() {
        this.mode = 0;
    }

    public static Block getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Block();
                }
            }
        }
        return instance;
    }

    // Para testing
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    /**
     * Edita el estado de bloqueo de un tile en las coordenadas especificadas.
     * 
     * @param x coordenada X del tile
     * @param y coordenada Y del tile
     */
    public void block_edit(int x, int y) {
        if (mode <= 0)
            return;

        int offset = brushSize / 2;
        for (int i = x - offset; i <= x + offset; i++) {
            for (int j = y - offset; j <= y + offset; j++) {
                if (i >= 0 && i < mapData.length && j >= 0 && j < mapData[0].length && mapData[i][j] != null) {
                    if (mode == 1) {
                        mapData[i][j].setBlocked(true);
                    } else if (mode == 2) {
                        mapData[i][j].setBlocked(false);
                    } else if (mode == 3) {
                        mapData[i][j].setBlocked(!mapData[i][j].getBlocked());
                    }
                }
            }
        }
    }

    /**
     * Bloquea un tile en las coordenadas especificadas.
     */
    private void block(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            mapData[x][y].setBlocked(true);
        }
    }

    /**
     * Desbloquea un tile en las coordenadas especificadas.
     */
    private void unblock(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            mapData[x][y].setBlocked(false);
        }
    }

    /**
     * Bloquea los bordes del mapa según la lógica original de VB6.
     * Basado en un área de visión estándar de 13x11 (o similar configurable).
     */
    public void blockBorders() {
        if (mapData == null)
            return;

        // Tamaños configurados del cliente (viewport)
        int clientWidth = org.argentumforge.engine.utils.GameData.options.getClientWidth();
        int clientHeight = org.argentumforge.engine.utils.GameData.options.getClientHeight();

        int minXBorder = clientWidth / 2;
        int maxXBorder = mapData.length - (clientWidth / 2) - 1;
        int minYBorder = clientHeight / 2;
        int maxYBorder = mapData[0].length - (clientHeight / 2) - 1;

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    if (x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder) {
                        mapData[x][y].setBlocked(true);
                    }
                }
            }
        }
    }

    /**
     * Limpia los bloqueos de los bordes del mapa.
     */
    public void unblockBorders() {
        if (mapData == null)
            return;

        int clientWidth = org.argentumforge.engine.utils.GameData.options.getClientWidth();
        int clientHeight = org.argentumforge.engine.utils.GameData.options.getClientHeight();

        int minXBorder = clientWidth / 2;
        int maxXBorder = mapData.length - (clientWidth / 2) - 1;
        int minYBorder = clientHeight / 2;
        int maxYBorder = mapData[0].length - (clientHeight / 2) - 1;

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    if (x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder) {
                        mapData[x][y].setBlocked(false);
                    }
                }
            }
        }
    }

    /**
     * Bloquea todos los tiles del mapa.
     */
    public void blockAll() {
        if (mapData == null)
            return;
        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    mapData[x][y].setBlocked(true);
                }
            }
        }
    }

    /**
     * Desbloquea todos los tiles del mapa.
     */
    public void unblockAll() {
        if (mapData == null)
            return;
        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    mapData[x][y].setBlocked(false);
                }
            }
        }
    }
}
