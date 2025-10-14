package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.map;

public class OnlineMapCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        if (commandContext.hasArguments()) {
            requireInteger(commandContext, 0, "map");
            short mapNumber = Short.parseShort(commandContext.getArgument(0));
            map(mapNumber);
        } else map(user.getUserMap()); // Si no se proporciona argumento, usar el mapa actual del usuario
    }

}
