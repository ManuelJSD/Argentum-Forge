package org.argentumforge.engine.utils;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.inits.MapProperties;
import org.argentumforge.engine.utils.editor.commands.Command;
import java.util.Stack;

/**
 * Representa el contexto de un mapa abierto en el editor.
 * Encapsula los datos del mapa, propiedades y entidades.
 */
public class MapContext {
    private String filePath;
    private MapData[][] mapData;
    private MapProperties mapProperties;
    private Character[] charList;
    private boolean modified = false;
    private short lastChar = 0;
    private MapManager.MapSaveOptions saveOptions = MapManager.MapSaveOptions.standard();
    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();
    private int savedUndoStackSize = 0;

    // Persistent Camera/User Position
    private int savedUserX = 50;
    private int savedUserY = 50;

    public MapContext(String filePath, MapData[][] mapData, MapProperties mapProperties, Character[] charList) {
        this.filePath = filePath;
        this.mapData = mapData;
        this.mapProperties = mapProperties;
        this.charList = charList;
    }

    public int getSavedUserX() {
        return savedUserX;
    }

    public void setSavedUserX(int x) {
        this.savedUserX = x;
    }

    public int getSavedUserY() {
        return savedUserY;
    }

    public void setSavedUserY(int y) {
        this.savedUserY = y;
    }

    public MapManager.MapSaveOptions getSaveOptions() {
        return saveOptions;
    }

    public void setSaveOptions(MapManager.MapSaveOptions saveOptions) {
        this.saveOptions = saveOptions;
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

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public short getLastChar() {
        return lastChar;
    }

    public void setLastChar(short lastChar) {
        this.lastChar = lastChar;
    }

    public Stack<Command> getUndoStack() {
        return undoStack;
    }

    public Stack<Command> getRedoStack() {
        return redoStack;
    }

    public int getSavedUndoStackSize() {
        return savedUndoStackSize;
    }

    public void setSavedUndoStackSize(int savedUndoStackSize) {
        this.savedUndoStackSize = savedUndoStackSize;
    }
}
