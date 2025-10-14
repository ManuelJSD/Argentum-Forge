package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.systemMessage;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.SYSTEM_MESSAGE;

public class SystemMessageCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, UNLIMITED_ARGUMENTS, getCommandUsage(SYSTEM_MESSAGE));
        requireValidString(commandContext, "message", REGEX);
        String message = commandContext.argumentsRaw().trim();
        systemMessage(message);
    }

}
