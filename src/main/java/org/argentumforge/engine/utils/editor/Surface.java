package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Clase singleton para gestionar la edición de superficies (capas de suelo) en
 * el mapa.
 * Permite insertar o eliminar gráficos (GRH) en cualquiera de las 4 capas del
 * mapa.
 */
public class Surface {

    private static volatile Surface instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = borrar, 3 = capturar (pick)
    private int surfaceIndex;
    private int layer;

    private Surface() {
        this.mode = 0;
        this.surfaceIndex = 1;
        this.layer = 1;
    }

    public static Surface getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Surface();
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

    public int getSurfaceIndex() {
        return surfaceIndex;
    }

    public void setSurfaceIndex(int surfaceIndex) {
        this.surfaceIndex = surfaceIndex;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public void surface_edit(int x, int y) {
        if (mapData == null || x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length) {
            return;
        }

        switch (mode) {
            case 1: // Insertar
                this.insert(x, y);
                break;

            case 2: // Eliminar
                this.delete(x, y);
                break;
            case 3: // Capturar (pick)
                this.pick(x, y);
                break;

            default:
                break;
        }
    }

    private void insert(int x, int y) {
        short oldGrh = mapData[x][y].getLayer(layer).getGrhIndex();
        if (oldGrh == (short) surfaceIndex)
            return;

        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                new org.argentumforge.engine.utils.editor.commands.TileChangeCommand(x, y, layer, oldGrh,
                        (short) surfaceIndex));
    }

    private void delete(int x, int y) {
        short oldGrh = mapData[x][y].getLayer(layer).getGrhIndex();
        short targetGrh = (short) (layer == 1 ? 1 : 0);

        if (oldGrh == targetGrh)
            return;

        org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                new org.argentumforge.engine.utils.editor.commands.TileChangeCommand(x, y, layer, oldGrh, targetGrh));
    }

    private void pick(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int grhIdx = mapData[x][y].getLayer(layer).getGrhIndex();
            if (grhIdx > 0) {
                this.surfaceIndex = grhIdx;
                this.mode = 1; // Volvemos a modo insertar con el nuevo índice
            }
        }
    }

}
