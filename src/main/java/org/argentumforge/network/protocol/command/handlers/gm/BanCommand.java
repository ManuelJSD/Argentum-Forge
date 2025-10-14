package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.ban;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.BAN;

public class BanCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(BAN));
        requireString(commandContext, 0, "player");
        requireString(commandContext, 1, "reason");

        String player = commandContext.getArgument(0);
        String reason = commandContext.getArgument(1);

        ban(player, reason);
    }

}
