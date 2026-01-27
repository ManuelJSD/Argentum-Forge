package org.argentumforge.engine.utils.editor.commands;

import java.util.Map;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para registrar y revertir cambios en las partículas de múltiples
 * tiles.
 */
public class ParticleChangeCommand implements Command {
    private final Map<TilePos, Integer> oldStates;
    private final Map<TilePos, Integer> newStates;

    public static record TilePos(int x, int y) {
    }

    public ParticleChangeCommand(Map<TilePos, Integer> oldStates, Map<TilePos, Integer> newStates) {
        this.oldStates = oldStates;
        this.newStates = newStates;
    }

    @Override
    public void execute() {
        for (Map.Entry<TilePos, Integer> entry : newStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setParticleIndex(entry.getValue());
        }
    }

    @Override
    public void undo() {
        for (Map.Entry<TilePos, Integer> entry : oldStates.entrySet()) {
            mapData[entry.getKey().x][entry.getKey().y].setParticleIndex(entry.getValue());
        }
    }
}
