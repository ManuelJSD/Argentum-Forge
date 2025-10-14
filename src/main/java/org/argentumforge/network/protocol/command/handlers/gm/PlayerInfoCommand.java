package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.playerInfo;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.PLAYER_INFO;

public class PlayerInfoCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(PLAYER_INFO));
        String player = commandContext.getArgument(0);
        playerInfo(player);
    }

}
