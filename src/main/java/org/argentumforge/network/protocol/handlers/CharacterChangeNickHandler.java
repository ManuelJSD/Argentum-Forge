package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;

import static org.argentumforge.engine.utils.GameData.charList;

public class CharacterChangeNickHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(5)) return;
        buffer.readByte();
        charList[buffer.readInteger()].setName(buffer.readCp1252String());
    }

}
