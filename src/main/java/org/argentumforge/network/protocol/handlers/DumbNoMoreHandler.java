package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class DumbNoMoreHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        // userEstupido = false;
        Logger.debug("handleDumbNoMore Cargado! - FALTA TERMINAR!");
    }

}
