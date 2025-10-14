package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.commentServerLog;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.COMMENT_SERVER_LOG;

public class CommentServerLogCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, UNLIMITED_ARGUMENTS, getCommandUsage(COMMENT_SERVER_LOG));
        requireValidString(commandContext, "comment", REGEX);
        String comment = commandContext.argumentsRaw().trim();
        commentServerLog(comment);
    }

}
