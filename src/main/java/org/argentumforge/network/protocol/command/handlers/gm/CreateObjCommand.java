package org.argentumforge.network.protocol.command.handlers.gm;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.createObj;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.CREATE_OBJ;

/**
 * Al crear el hacha (objeto #3 del obj.dat del servidor VB6), se indica GrhIndex=505 pero el grafico real es el 16037 en
 * graphics-descompressed del cliente Java.
 */

public class CreateObjCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, 1, getCommandUsage(CREATE_OBJ));
        requireInteger(commandContext, 0, "object_id");
        int objId = Integer.parseInt(commandContext.getArgument(0));
        createObj(objId);
    }

}
