package org.argentumforge.engine.utils.editor.commands;

import java.util.Map;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para registrar y revertir cambios de bloqueos.
 * Soporta cambios en múltiples tiles (útil para pinceles).
 */
public class BlockChangeCommand implements Command {
    private final Map<TilePos, Boolean> oldStates;
    private final boolean newState;

    public static record TilePos(int x, int y) {
    }

    public BlockChangeCommand(Map<TilePos, Boolean> oldStates, boolean newState) {
        this.oldStates = oldStates;
        this.newState = newState;
    }

    @Override
    public void execute() {
        for (TilePos pos : oldStates.keySet()) {
            mapData[pos.x][pos.y].setBlocked(newState);
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<TilePos, Boolean> entry : oldStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setBlocked(entry.getValue());
        }
    }
}
