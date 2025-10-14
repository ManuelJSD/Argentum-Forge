package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.chaosArmour;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.CHAOS_ARMOUR;

public class ChaosArmourCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(CHAOS_ARMOUR));
        requireInteger(commandContext, 0, "armor");
        requireShort(commandContext, 1, "object");

        int armor = Integer.parseInt(commandContext.getArgument(0));
        short object = Short.parseShort(commandContext.getArgument(1));

        chaosArmour(armor, object);
    }

}
