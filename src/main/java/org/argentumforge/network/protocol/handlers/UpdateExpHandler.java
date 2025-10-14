package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

public class UpdateExpHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(5)) return;
        buffer.readByte();
        // Get data and update
        User.INSTANCE.setUserExp(buffer.readLong());
    }

}
