package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.Selection;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para pegar múltiples entidades desde el portapapeles.
 */
public class PasteEntitiesCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.paste");
    }

    private final List<Command> commands = new ArrayList<>();

    public PasteEntitiesCommand(org.argentumforge.engine.utils.MapContext context, List<Clipboard.ClipboardItem> items,
            int destX, int destY) {
        super(context);
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        for (Clipboard.ClipboardItem item : items) {
            int tx = destX + item.offsetX;
            int ty = destY + item.offsetY;

            if (tx < 0 || tx >= mapData.length || ty < 0 || ty >= mapData[0].length)
                continue;

            if (item.type == Selection.EntityType.NPC) {
                int oldNpc = mapData[tx][ty].getNpcIndex();
                commands.add(new NpcChangeCommand(context, tx, ty, oldNpc, item.id));
            } else if (item.type == Selection.EntityType.OBJECT) {
                int oldObj = mapData[tx][ty].getObjGrh().getGrhIndex();
                commands.add(new ObjChangeCommand(context, tx, ty, oldObj, item.id));
            } else if (item.type == Selection.EntityType.TILE && item.layers != null) {
                int[] oldLayers = new int[5];
                for (int i = 1; i <= 4; i++) {
                    oldLayers[i] = mapData[tx][ty].getLayer(i).getGrhIndex();
                }
                commands.add(new SurfaceChangeCommand(context, tx, ty, oldLayers, item.layers));
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
