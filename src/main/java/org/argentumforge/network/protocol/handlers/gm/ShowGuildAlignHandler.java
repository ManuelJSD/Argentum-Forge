package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class ShowGuildAlignHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        //frmEligeAlineacion.Show vbModeless, //FrmMain
    }

}
