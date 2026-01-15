package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.NpcData;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para colocar o quitar NPCs.
 * Maneja tanto el dato del mapa como la instancia visual.
 */
public class NpcChangeCommand implements Command {
    private final int x, y;
    private final short oldNpcIndex;
    private final short newNpcIndex;

    public NpcChangeCommand(int x, int y, short oldNpcIndex, short newNpcIndex) {
        this.x = x;
        this.y = y;
        this.oldNpcIndex = oldNpcIndex;
        this.newNpcIndex = newNpcIndex;
    }

    @Override
    public void execute() {
        apply(newNpcIndex);
    }

    @Override
    public void undo() {
        apply(oldNpcIndex);
    }

    private void apply(short npcIndex) {
        // Borrar visual actual
        short currentChar = mapData[x][y].getCharIndex();
        if (currentChar != 0) {
            Character.eraseChar(currentChar);
        }

        // Actualizar dato mapa
        mapData[x][y].setNpcIndex(npcIndex);

        // Crear nueva visual si corresponde
        if (npcIndex > 0 && AssetRegistry.npcs.containsKey((int) npcIndex)) {
            NpcData data = AssetRegistry.npcs.get((int) npcIndex);
            for (short i = 1; i < GameData.charList.length; i++) {
                if (!GameData.charList[i].isActive()) {
                    Character.makeChar(i, data.getBody(), data.getHead(), Direction.DOWN, x, y, 0, 0, 0);
                    break;
                }
            }
        }
    }
}
