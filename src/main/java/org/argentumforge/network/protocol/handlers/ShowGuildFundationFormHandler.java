package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class ShowGuildFundationFormHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        //CreandoClan = True
        //    frmGuildFoundation.Show , //FrmMain
        Logger.debug("handleShowGuildFundationForm Cargado! - FALTA TERMINAR!");
    }

}
