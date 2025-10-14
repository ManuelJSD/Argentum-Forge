package org.argentumforge.network.protocol.command.handlers.user;

import org.argentumforge.network.protocol.command.core.CommandContext;
import org.argentumforge.network.protocol.command.core.CommandException;
import org.argentumforge.network.protocol.command.handlers.BaseCommandHandler;

import static org.argentumforge.network.protocol.Protocol.desc;
import static org.argentumforge.network.protocol.command.metadata.GameCommand.DESC;

public class DescCommand extends BaseCommandHandler {

    @Override
    public void handle(CommandContext commandContext) throws CommandException {
        requireArguments(commandContext, UNLIMITED_ARGUMENTS, getCommandUsage(DESC)); // TODO Podria verificarse en el proceso de comandos ya que la mayoria de comandos tiene argumentos
        /* Me parece que si en la descripcion hay espacios al principio o al final, el servidor los elimina, por que desde aca, el
         * argumento en crudo (getArgumentsRaw()) permite los espacios. Ademas, el servidor tambien valida si la descripcion tiene
         * caracteres invalidos, lo cual este diseño es bastente inconsistente. */
        desc(commandContext.argumentsRaw());
    }

}
