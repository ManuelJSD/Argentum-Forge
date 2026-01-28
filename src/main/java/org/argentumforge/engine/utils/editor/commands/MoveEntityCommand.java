package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.game.models.Direction;

import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.inits.NpcData;

import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para mover una entidad (NPC u Objeto) de una posición a otra.
 */
public class MoveEntityCommand implements Command {

    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.move_entity");
    }

    private final int srcX, srcY;
    private final int destX, destY;
    private final Selection.EntityType type;
    private final int id;

    public MoveEntityCommand(int srcX, int srcY, int destX, int destY, Selection.EntityType type, int id) {
        this.srcX = srcX;
        this.srcY = srcY;
        this.destX = destX;
        this.destY = destY;
        this.type = type;
        this.id = id;
    }

    @Override
    public void execute() {
        move(srcX, srcY, destX, destY);
    }

    @Override
    public void undo() {
        move(destX, destY, srcX, srcY);
    }

    private void move(int x1, int y1, int x2, int y2) {
        if (type == Selection.EntityType.NPC) {
            // Eliminar de origen
            short oldChar = mapData[x1][y1].getCharIndex();
            if (oldChar != 0)
                Character.eraseChar(oldChar);
            mapData[x1][y1].setNpcIndex((short) 0);

            // Colocar en destino
            mapData[x2][y2].setNpcIndex((short) id);
            if (id > 0 && AssetRegistry.npcs.containsKey(id)) {
                NpcData data = AssetRegistry.npcs.get(id);
                for (short i = 1; i < GameData.charList.length; i++) {
                    if (!GameData.charList[i].isActive()) {
                        Character.makeChar(i, data.getBody(), data.getHead(), Direction.DOWN, x2, y2, 0, 0, 0);
                        break;
                    }
                }
            }
        } else if (type == Selection.EntityType.OBJECT) {
            // Eliminar de origen
            mapData[x1][y1].getObjGrh().setGrhIndex(0);

            // Colocar en destino
            initGrh(mapData[x2][y2].getObjGrh(), (short) id, false);
        }
    }
}
