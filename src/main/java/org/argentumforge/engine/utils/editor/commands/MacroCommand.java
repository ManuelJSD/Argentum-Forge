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

    public boolean isEmpty() {
        return commands.isEmpty();
    }
}
