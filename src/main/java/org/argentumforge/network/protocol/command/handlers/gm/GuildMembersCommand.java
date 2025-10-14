package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.guildMembers;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.GUILD_MEMBERS;

public class GuildMembersCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(GUILD_MEMBERS));
        String guild = commandContext.getArgument(0);
        guildMembers(guild);
    }

}
