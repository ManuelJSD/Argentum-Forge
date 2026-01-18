package org.argentumforge.engine.utils.editor.commands;

import java.util.Map;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para registrar y revertir cambios de bloqueos.
 * Soporta cambios en múltiples tiles (útil para pinceles).
 */
public class BlockChangeCommand implements Command {
    private final Map<TilePos, Boolean> oldStates;
    private final Map<TilePos, Boolean> newStates;

    public static record TilePos(int x, int y) {
    }

    public BlockChangeCommand(Map<TilePos, Boolean> oldStates, Map<TilePos, Boolean> newStates) {
        this.oldStates = oldStates;
        this.newStates = newStates;
    }

    @Override
    public void execute() {
        for (Map.Entry<TilePos, Boolean> entry : newStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setBlocked(entry.getValue());
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<TilePos, Boolean> entry : oldStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setBlocked(entry.getValue());
        }
    }
}
