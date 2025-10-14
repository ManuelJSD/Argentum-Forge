package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.Rain;
import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

public class RainToggleHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        int userX = User.INSTANCE.getUserPos().getX();
        int userY = User.INSTANCE.getUserPos().getY();
        if (User.INSTANCE.inMapBounds(userX, userY)) return;

        User.INSTANCE.setUnderCeiling(User.INSTANCE.checkUnderCeiling());

        if (Rain.INSTANCE.isRaining()) {
            Rain.INSTANCE.setRainValue(false);
            Rain.INSTANCE.stopRainingSoundLoop();
            Rain.INSTANCE.playEndRainSound();
        } else Rain.INSTANCE.setRainValue(true);

    }

}
