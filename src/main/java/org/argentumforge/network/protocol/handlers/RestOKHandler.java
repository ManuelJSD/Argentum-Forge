package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class RestOKHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        //UserDescansar = Not UserDescansar
        Logger.debug("handleCarpenterObjects Cargado! - FALTA TERMINAR!");
    }

}
