package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.sentinelCode;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.SENTINEL_CODE;

public class SentinelCodeCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(SENTINEL_CODE));
        requireInteger(commandContext, 0, "code");
        int code = Integer.parseInt(commandContext.getArgument(0));
        sentinelCode(code);
    }

}
