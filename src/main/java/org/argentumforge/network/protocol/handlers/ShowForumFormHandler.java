package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FForum;
import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class ShowForumFormHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        int privilegios = buffer.readByte();
        int canPostSticky = buffer.readByte();

        //frmForo.Privilegios = data.ReadByte
        //    frmForo.CanPostSticky = data.ReadByte
        //
        //    If Not MirandoForo Then
        //        frmForo.Show , //FrmMain
        //    End If

        ImGUISystem.INSTANCE.show(new FForum(privilegios, canPostSticky));

        Logger.debug("handleShowForumForm Cargado! - FALTA TERMINAR!");
    }

}
