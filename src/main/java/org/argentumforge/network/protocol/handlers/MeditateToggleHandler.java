package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class MeditateToggleHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        //UserMeditar = Not UserMeditar
        Logger.debug("handleMeditateToggle Cargado! - FALTA TERMINAR!");
    }

}
