package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.models.Direction;
import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.game.models.Character.refreshAllChars;

public class ForceCharMoveHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(2)) return;
        buffer.readByte();

        Direction direction = Direction.values()[buffer.readByte() - 1];
        short userCharIndex = User.INSTANCE.getUserCharIndex();
        User.INSTANCE.moveCharbyHead(userCharIndex, direction);
        User.INSTANCE.moveScreen(direction);

        refreshAllChars();
    }

}
