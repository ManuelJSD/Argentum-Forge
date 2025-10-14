package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.citizenMessage;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.CITIZEN_MSG;

public class CitizenMessageCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, UNLIMITED_ARGUMENTS, getCommandUsage(CITIZEN_MSG));
        requireValidString(commandContext, "message", REGEX);
        String message = commandContext.argumentsRaw().trim();
        citizenMessage(message);
    }

}
