package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

public class LoggedHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        User.INSTANCE.setUserConected(true);
    }

}
