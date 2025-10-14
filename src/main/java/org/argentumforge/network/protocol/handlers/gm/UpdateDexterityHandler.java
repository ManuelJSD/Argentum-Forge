package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class UpdateDexterityHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(2)) return;
        buffer.readByte();
        User.INSTANCE.setUserDext(buffer.readByte());
    }

}
