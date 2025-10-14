package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.SOSShowList;
import static org.argentumforge.network.protocol.Protocol.showServerForm;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.SHOW;

public class ShowCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(SHOW));
        requireString(commandContext, 0, "option");
        String option = commandContext.getArgument(0);
        switch (option.toUpperCase()) {
            case "SOS" -> SOSShowList();
            case "INT" -> showServerForm();
            default -> showError("Valid options: SOS, INT");
        }
    }

}
