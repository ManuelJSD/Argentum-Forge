package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.Dialogs;
import org.argentumforge.network.PacketBuffer;

public class RemoveDialogsHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        Dialogs.removeAllDialogs();
    }

}
