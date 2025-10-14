package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.acceptRoyalCouncil;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.ACCEPT_ROYAL_COUNCIL;

public class AcceptRoyalCouncilCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(ACCEPT_ROYAL_COUNCIL));
        String player = commandContext.getArgument(0);
        acceptRoyalCouncil(player);
    }

}
