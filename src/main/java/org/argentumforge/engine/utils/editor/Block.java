package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Clase singleton para gestionar la edición de bloqueos en el mapa.
 * Permite activar/desactivar bloqueos en tiles específicos del mapa.
 */
public class Block {

    private static volatile Block instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = bloquear, 2 = desbloquear
    private boolean showBlocks; // Para visualizar los bloqueos en el mapa

    private Block() {
        this.mode = 0;
        this.showBlocks = false;
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

    public boolean isShowBlocks() {
        return showBlocks;
    }

    public void setShowBlocks(boolean showBlocks) {
        this.showBlocks = showBlocks;
    }

    /**
     * Edita el estado de bloqueo de un tile en las coordenadas especificadas.
     * @param x coordenada X del tile
     * @param y coordenada Y del tile
     */
    public void block_edit(int x, int y) {
        switch (mode) {
            case 1: // Bloquear
                this.block(x, y);
                break;

            case 2: // Desbloquear
                this.unblock(x, y);
                break;

            default:
                break;
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
}
