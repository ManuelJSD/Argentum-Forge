package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.imperialArmour;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.IMPERIAL_ARMOUR;

public class ImperialArmourCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 2, getCommandUsage(IMPERIAL_ARMOUR));
        requireInteger(commandContext, 0, "armor_id");
        requireInteger(commandContext, 1, "object_id");

        int armorId = Integer.parseInt(commandContext.getArgument(0));
        short objId = Short.parseShort(commandContext.getArgument(1));

        imperialArmour(armorId, objId);
    }

}
