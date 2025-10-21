package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

public class Surface {
    private static final Surface instance = new Surface();
    private int mode;
    private int surfaceIndex;
    private int layer;

    private Surface() {
        this.mode = 0;
        this.surfaceIndex = 1;
        this.layer = 1;
    }

    public static Surface getInstance() {
        return instance;
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
        switch (mode) {
            case 1: //Insertar
                this.insert(x,y);
                break;

            case 2: //Eliminar
                this.delete(x,y);
                break;

            default:
                break;
        }
    }

    private void insert(int x, int y) {
        mapData[x][y].getLayer(layer).setGrhIndex(surfaceIndex);
        mapData[x][y].setLayer(layer, initGrh(mapData[x][y].getLayer(layer), mapData[x][y].getLayer(layer).getGrhIndex(), true));
    }

    private void delete(int x, int y) {
        if (layer == 1) {
            mapData[x][y].getLayer(layer).setGrhIndex(1);
        } else {
            mapData[x][y].getLayer(layer).setGrhIndex(0);
        }
        mapData[x][y].setLayer(layer, initGrh(mapData[x][y].getLayer(layer), mapData[x][y].getLayer(layer).getGrhIndex(), true));
    }

}
