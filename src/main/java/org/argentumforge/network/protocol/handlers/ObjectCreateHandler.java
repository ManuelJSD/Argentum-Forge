package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

public class ObjectCreateHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(5)) return;
        buffer.readByte();

        int x = buffer.readByte();
        int y = buffer.readByte();
        short grhIndex = buffer.readInteger();

        mapData[x][y].getObjGrh().setGrhIndex(grhIndex);
        initGrh(mapData[x][y].getObjGrh(), mapData[x][y].getObjGrh().getGrhIndex(), true);
    }

}
