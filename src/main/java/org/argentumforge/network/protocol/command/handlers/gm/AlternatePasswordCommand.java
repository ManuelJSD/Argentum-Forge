package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.alternatePassword;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.ALTERNATE_PASSWORD;

public class AlternatePasswordCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(ALTERNATE_PASSWORD));
        requireString(commandContext, 0, "player_without_password");
        requireString(commandContext, 1, "player_with_password");

        String playerWithoutPassword = commandContext.getArgument(0);
        String playerWithPassword = commandContext.getArgument(1);

        alternatePassword(playerWithoutPassword, playerWithPassword);
    }

}
