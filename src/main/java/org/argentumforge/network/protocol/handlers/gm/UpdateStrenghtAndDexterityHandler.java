package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class UpdateStrenghtAndDexterityHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();
        User.INSTANCE.setUserStrg(buffer.readByte());
        User.INSTANCE.setUserDext(buffer.readByte());
    }

}
