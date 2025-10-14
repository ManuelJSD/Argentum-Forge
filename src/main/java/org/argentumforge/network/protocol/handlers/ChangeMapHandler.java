package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.Rain;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.bLluvia;

public class ChangeMapHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(5)) return;
        buffer.readByte();

        short userMap = buffer.readInteger();
        User.INSTANCE.setUserMap(userMap);

        // Once on-the-fly editor is implemented check for map version before loading....
        // For now we just drop it
        buffer.readInteger();

        GameData.loadMap(userMap);

        if (!bLluvia[userMap] && Rain.INSTANCE.isRaining()) Rain.INSTANCE.stopRainingSoundLoop();

    }

}
