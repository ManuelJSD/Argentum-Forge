package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import java.util.ArrayList;
import java.util.List;

/**
 * Permite agrupar múltiples comandos en una sola acción atómica para Undo/Redo.
 */
public class MacroCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.macro");
    }

    private final List<Command> commands = new ArrayList<>();

    public void addCommand(Command command) {
        commands.add(command);
    }

    @Override
    public void execute() {
        for (Command command : commands) {
            command.execute();
        }
    }

    @Override
    public void undo() {
        // Deshacer en orden inverso
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }

    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public int[] getAffectedBounds() {
        if (commands.isEmpty())
            return null;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        boolean hasBounds = false;

        for (Command cmd : commands) {
            int[] bounds = cmd.getAffectedBounds();
            if (bounds != null) {
                hasBounds = true;
                if (bounds[0] < minX)
                    minX = bounds[0];
                if (bounds[1] < minY)
                    minY = bounds[1];
                if (bounds[2] > maxX)
                    maxX = bounds[2];
                if (bounds[3] > maxY)
                    maxY = bounds[3];
            }
        }

        if (!hasBounds)
            return null;
        return new int[] { minX, minY, maxX, maxY };
    }
}
