package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.teleportNearToPlayer;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.TELEPORT_NEAR_TO_PLAYER;

public class TeleportNearToPlayerCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(TELEPORT_NEAR_TO_PLAYER));
        String player = commandContext.getArgument(0);
        teleportNearToPlayer(player);
    }

}
