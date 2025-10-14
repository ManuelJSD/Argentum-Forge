package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.charList;

public class UserCharIndexInServerHandler implements PacketHandler {
    
    private final User user = User.INSTANCE;

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();

        user.setUserCharIndex(buffer.readInteger());
        user.getUserPos().setX(charList[user.getUserCharIndex()].getPos().getX());
        user.getUserPos().setY(charList[user.getUserCharIndex()].getPos().getY());
        user.setUnderCeiling(user.checkUnderCeiling());
    }

}
