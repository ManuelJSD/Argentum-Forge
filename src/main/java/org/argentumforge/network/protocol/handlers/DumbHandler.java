package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class DumbHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        //UserEstupido = True
        Logger.debug("handleDumb Cargado! - FALTA TERMINAR!");
    }

}
