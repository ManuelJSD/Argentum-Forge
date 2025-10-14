package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.playerStats;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.PLAYER_STATS;

public class PlayerStatsCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(PLAYER_STATS));
        String player = commandContext.getArgument(0);
        playerStats(player);
    }

}
