package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.removeChaos;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.REMOVE_CHAOS;

public class RemoveChaosCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(REMOVE_CHAOS));
        requireString(commandContext, 0, "player");
        String player = commandContext.getArgument(0);
        removeChaos(player);
    }

}
