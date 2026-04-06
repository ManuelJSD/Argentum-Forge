package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.game.models.Direction;

import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.inits.NpcData;

/**
 * Comando para mover una entidad (NPC u Objeto) de una posición a otra.
 */
public class MoveEntityCommand extends AbstractCommand {

    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.move_entity");
    }

    private final int srcX, srcY;
    private final int destX, destY;
    private final Selection.EntityType type;
    private final int id;

    public MoveEntityCommand(org.argentumforge.engine.utils.MapContext context, int srcX, int srcY, int destX,
            int destY, Selection.EntityType type, int id) {
        super(context);
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
        var mapData = context.getMapData();
        if (type == Selection.EntityType.NPC) {
            // Eliminar de origen
            int oldChar = mapData[x1][y1].getCharIndex();
            if (oldChar != 0)
                Character.eraseChar(oldChar);
            mapData[x1][y1].setNpcIndex(0);

            // Colocar en destino
            mapData[x2][y2].setNpcIndex(id);
            if (id > 0 && AssetRegistry.npcs.containsKey(id)) {
                NpcData data = AssetRegistry.npcs.get(id);
                var charList = context.getCharList();
                for (int i = 1; i < charList.length; i++) {
                    if (!charList[i].isActive()) {
                        // TODO: Character.makeChar likely still uses static GameData or needs
                        // refactoring.
                        // For now we assume it works or we should refactor it later.
                        Character.makeChar(i, data.getBody(), data.getHead(), Direction.DOWN, x2, y2, 0, 0, 0);
                        break;
                    }
                }
            }
        } else if (type == Selection.EntityType.OBJECT) {
            // Obtener datos del origen
            int objIndex = mapData[x1][y1].getObjIndex();
            int objAmount = mapData[x1][y1].getObjAmount();

            // Eliminar de origen
            mapData[x1][y1].getObjGrh().setGrhIndex(0);
            mapData[x1][y1].setObjIndex(0);
            mapData[x1][y1].setObjAmount(0);

            // Colocar en destino
            mapData[x2][y2].setObjIndex(objIndex);
            mapData[x2][y2].setObjAmount(objAmount);
            org.argentumforge.engine.utils.GameData.initGrh(mapData[x2][y2].getObjGrh(), id, false);
        }
    }
}
