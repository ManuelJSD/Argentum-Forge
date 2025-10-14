package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.Dialogs;
import org.argentumforge.network.PacketBuffer;

public class RemoveCharDialogHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();
        Dialogs.removeDialog(buffer.readInteger());
    }

}
