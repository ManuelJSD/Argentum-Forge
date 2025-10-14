package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.guildKick;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.GUILD_KICK;

public class GuildKickCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(GUILD_KICK));
        String player = commandContext.getArgument(0);
        guildKick(player);
    }

}
