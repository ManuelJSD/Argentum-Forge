package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.depositGold;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.DEPOSIT_GOLD;

public class DepositGoldCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        if (user.isDead()) {
            showError("You are dead!");
            return;
        }
        requireArguments(commandContext, 1, getCommandUsage(DEPOSIT_GOLD));
        requireInteger(commandContext, 0, "amount");

        int amount = Integer.parseInt(commandContext.getArgument(0));

        if (amount > user.getUserGLD()) {
            showError("You don't have enough gold!");
            return;
        }

        depositGold(amount);
    }

}
