package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.playerBank;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.PLAYER_BANK;

public class PlayerBankCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(PLAYER_BANK));
        String player = commandContext.getArgument(0);
        playerBank(player);
    }

}
