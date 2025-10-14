package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.bet;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.BET;

public class BetCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        if (user.isDead()) {
            showError("You are dead!");
            return;
        }
        requireArguments(commandContext, 1, getCommandUsage(BET));
        requireShort(commandContext, 0, "amount");

        short amount = Short.parseShort(commandContext.getArgument(0));

        bet(amount);
    }

}
