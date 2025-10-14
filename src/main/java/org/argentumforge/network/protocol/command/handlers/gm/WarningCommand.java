package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.warning;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.WARNING;

public class WarningCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(WARNING));
        requireString(commandContext, 0, "player");
        requireString(commandContext, 1, "reason");
        String player = commandContext.getArgument(0);
        String reason = commandContext.getArgument(1);
        warning(player, reason);
    }

}
