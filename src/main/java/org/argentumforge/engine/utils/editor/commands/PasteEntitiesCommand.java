package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.Selection;
import java.util.ArrayList;
import java.util.List;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Comando para pegar múltiples entidades desde el portapapeles.
 */
public class PasteEntitiesCommand implements Command {
    private final List<Command> commands = new ArrayList<>();

    public PasteEntitiesCommand(List<Clipboard.ClipboardItem> items, int destX, int destY) {
        if (mapData == null)
            return;

        for (Clipboard.ClipboardItem item : items) {
            int tx = destX + item.offsetX;
            int ty = destY + item.offsetY;

            if (tx < 0 || tx >= mapData.length || ty < 0 || ty >= mapData[0].length)
                continue;

            if (item.type == Selection.EntityType.NPC) {
                short oldNpc = mapData[tx][ty].getNpcIndex();
                commands.add(new NpcChangeCommand(tx, ty, oldNpc, (short) item.id));
            } else if (item.type == Selection.EntityType.OBJECT) {
                int oldObj = mapData[tx][ty].getObjGrh().getGrhIndex();
                commands.add(new ObjChangeCommand(tx, ty, oldObj, item.id));
            } else if (item.type == Selection.EntityType.TILE && item.layers != null) {
                short[] oldLayers = new short[5];
                for (int i = 1; i <= 4; i++) {
                    oldLayers[i] = mapData[tx][ty].getLayer(i).getGrhIndex();
                }
                commands.add(new SurfaceChangeCommand(tx, ty, oldLayers, item.layers));
            }
        }
    }

    @Override
    public void execute() {
        for (Command cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
