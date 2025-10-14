package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

import static org.argentumforge.engine.utils.GameData.charList;

public class SetInvisibleHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(4)) return;
        buffer.readByte();

        charList[buffer.readInteger()].setInvisible(buffer.readBoolean());
        Logger.debug("handleSetInvisible Cargado!");
    }

}
