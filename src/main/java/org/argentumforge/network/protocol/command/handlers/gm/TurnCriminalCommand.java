package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.turnCriminal;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.TURN_CRIMINAL;

public class TurnCriminalCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(TURN_CRIMINAL));
        String player = commandContext.getArgument(0);
        turnCriminal(player);
    }

}
