package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

public class UpdateStaHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();

        User.INSTANCE.setUserMinSTA(buffer.readInteger());
    }

}
