package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Selection;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para eliminar múltiples entidades en una sola operación atómica.
 */
public class DeleteEntitiesCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.delete_entity");
    }

    private final List<Command> commands = new ArrayList<>();

    public DeleteEntitiesCommand(org.argentumforge.engine.utils.MapContext context,
            List<Selection.SelectedEntity> entities) {
        super(context);
        for (Selection.SelectedEntity se : entities) {
            if (se.type == Selection.EntityType.NPC) {
                commands.add(new NpcChangeCommand(context, se.x, se.y, se.id, 0));
            } else if (se.type == Selection.EntityType.OBJECT) {
                commands.add(new ObjChangeCommand(context, se.x, se.y, se.id, 0));
            } else if (se.type == Selection.EntityType.TILE) {
                int[] oldLayers = new int[5];
                int[] newLayers = new int[5]; // Todo a 0
                for (int i = 1; i <= 4; i++) {
                    oldLayers[i] = context.getMapData()[se.x][se.y].getLayer(i)
                            .getGrhIndex();
                    newLayers[i] = 0;
                }
                commands.add(new SurfaceChangeCommand(context, se.x, se.y, oldLayers, newLayers));
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
        // Deshacer en orden inverso
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
