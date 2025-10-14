package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;
import org.tinylog.Logger;

public class ShowMOTDEditionFormHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        PacketBuffer tempBuffer = new PacketBuffer();
        tempBuffer.copy(buffer);
        tempBuffer.readByte();

        String txtMotd = tempBuffer.readCp1252String();

        //frmCambiaMotd.txtMotd.Text = Buffer.ReadASCIIString()
        //    frmCambiaMotd.Show , //FrmMain
        //

        buffer.copy(tempBuffer);
        Logger.debug("handleShowMOTDEditionForm Cargado! - FALTA TERMINAR!");
    }

}
