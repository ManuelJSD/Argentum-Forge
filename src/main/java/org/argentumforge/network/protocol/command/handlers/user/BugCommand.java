package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.bug;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.BUG;

public class BugCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, UNLIMITED_ARGUMENTS, getCommandUsage(BUG));
        requireValidString(commandContext, "description", REGEX);
        String description = commandContext.argumentsRaw().trim();
        bug(description);
    }

}
