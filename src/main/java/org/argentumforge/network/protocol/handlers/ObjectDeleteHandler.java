package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.mapData;

public class ObjectDeleteHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();

        int x = buffer.readByte();
        int y = buffer.readByte();

        mapData[x][y].getObjGrh().setGrhIndex((short) 0);
    }

}
