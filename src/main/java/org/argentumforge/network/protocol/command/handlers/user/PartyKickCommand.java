package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.partyKick;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.PARTY_KICK;

public class PartyKickCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(PARTY_KICK));
        String player = commandContext.getArgument(0);
        partyKick(player);
    }

}
