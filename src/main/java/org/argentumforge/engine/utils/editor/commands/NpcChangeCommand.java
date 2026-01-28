package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
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
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.npc");
    }

    private final int x, y;
    private final int oldNpcIndex;
    private final int newNpcIndex;

    public NpcChangeCommand(int x, int y, int oldNpcIndex, int newNpcIndex) {
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

    private void apply(int npcIndex) {
        // Borrar visual actual
        short currentChar = GameData.mapData[x][y].getCharIndex();
        if (currentChar != 0) {
            Character.eraseChar(currentChar);
        }

        // Actualizar dato mapa
        mapData[x][y].setNpcIndex(npcIndex);

        // Create nueva visual si corresponde
        if (npcIndex > 0 && AssetRegistry.npcs.containsKey(npcIndex)) {
            NpcData data = AssetRegistry.npcs.get(npcIndex);
            for (short i = 1; i < GameData.charList.length; i++) {
                if (!GameData.charList[i].isActive()) {
                    Character.makeChar(i, data.getBody(), data.getHead(), Direction.DOWN, x, y, 0, 0, 0);
                    break;
                }
            }
        }
    }

    @Override
    public int[] getAffectedBounds() {
        return new int[] { x, y, x, y };
    }
}
