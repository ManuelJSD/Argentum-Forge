package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Selection;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para mover múltiples entidades en una sola operación atómica.
 */
public class BulkEntityMoveCommand implements Command {

    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.bulk_entity_move");
    }

    private static class MoveData {
        int srcX, srcY, destX, destY;
        Selection.EntityType type;
        int id;

        MoveData(int srcX, int srcY, int destX, int destY, Selection.EntityType type, int id) {
            this.srcX = srcX;
            this.srcY = srcY;
            this.destX = destX;
            this.destY = destY;
            this.type = type;
            this.id = id;
        }
    }

    private final List<MoveData> moves = new ArrayList<>();
    private final List<MoveEntityCommand> subCommands = new ArrayList<>();

    public BulkEntityMoveCommand(List<Selection.SelectedEntity> entities, int anchorDestX, int anchorDestY) {
        for (Selection.SelectedEntity se : entities) {
            int nx = anchorDestX + se.offsetX;
            int ny = anchorDestY + se.offsetY;
            if (se.x != nx || se.y != ny) {
                moves.add(new MoveData(se.x, se.y, nx, ny, se.type, se.id));
                subCommands.add(new MoveEntityCommand(se.x, se.y, nx, ny, se.type, se.id));
            }
        }
    }

    @Override
    public void execute() {
        for (MoveEntityCommand cmd : subCommands) {
            cmd.execute();
        }
        updateSelectionState(true);
    }

    @Override
    public void undo() {
        for (int i = subCommands.size() - 1; i >= 0; i--) {
            subCommands.get(i).undo();
        }
        updateSelectionState(false);
    }

    private void updateSelectionState(boolean forward) {
        List<Selection.SelectedEntity> sel = Selection.getInstance().getSelectedEntities();
        for (MoveData m : moves) {
            int oldX = forward ? m.srcX : m.destX;
            int newX = forward ? m.destX : m.srcX;
            int oldY = forward ? m.srcY : m.destY;
            int newY = forward ? m.destY : m.srcY;

            for (Selection.SelectedEntity se : sel) {
                if (se.x == oldX && se.y == oldY && se.type == m.type && se.id == m.id) {
                    se.x = newX;
                    se.y = newY;
                    break;
                }
            }
        }
    }
}
