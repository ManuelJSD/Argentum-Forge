package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.kickCouncil;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.KICK_COUNCIL;

public class KickCouncilCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(KICK_COUNCIL));
        String player = commandContext.getArgument(0);
        kickCouncil(player);
    }

}
