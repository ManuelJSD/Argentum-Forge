package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.engine.game.console.Console;
import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

/**
 * Limpia la consola.
 */

public class ClearCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        Console.INSTANCE.clearConsole();
    }

}
