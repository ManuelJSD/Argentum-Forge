package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class AddSlotsHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        int maxInventorySlots = buffer.readByte();
    }

}
