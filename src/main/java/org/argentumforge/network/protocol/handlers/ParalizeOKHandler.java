package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.charList;

public class ParalizeOKHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        charList[User.INSTANCE.getUserCharIndex()].setParalizado(!charList[User.INSTANCE.getUserCharIndex()].isParalizado());
    }

}
