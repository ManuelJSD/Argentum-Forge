package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.changeNick;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.CHANGE_NICK;

public class ChangeNickCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(CHANGE_NICK));
        requireString(commandContext, 0, "nick");
        requireString(commandContext, 1, "newNick");

        String nick = commandContext.getArgument(0);
        String newNick = commandContext.getArgument(1);

        changeNick(nick, newNick);

    }

}
