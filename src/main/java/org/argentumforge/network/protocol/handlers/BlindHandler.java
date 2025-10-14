package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class BlindHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        //UserCiego = True
        Logger.debug("handleBlind Cargado! - FALTA TERMINAR!");
    }

}
