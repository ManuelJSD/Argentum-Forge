package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.inits.MapProperties;

/**
 * Representa el contexto de un mapa abierto en el editor.
 * Encapsula los datos del mapa, propiedades y entidades.
 */
public class MapContext {
    private String filePath;
    private MapData[][] mapData;
    private MapProperties mapProperties;
    private Character[] charList;

    public MapContext(String filePath, MapData[][] mapData, MapProperties mapProperties, Character[] charList) {
        this.filePath = filePath;
        this.mapData = mapData;
        this.mapProperties = mapProperties;
        this.charList = charList;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public MapData[][] getMapData() {
        return mapData;
    }

    public MapProperties getMapProperties() {
        return mapProperties;
    }

    public Character[] getCharList() {
        return charList;
    }

    public String getMapName() {
        if (filePath == null || filePath.isEmpty())
            return "Sin Título";
        java.io.File file = new java.io.File(filePath);
        return file.getName();
    }
}
