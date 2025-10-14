package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class StopWorkingHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        //With FontTypes(FontTypeNames.FONTTYPE_INFO)
        //        Call ShowConsoleMsg("¡Has terminado de trabajar!", .red, .green, .blue, .bold, .italic)
        //    End With
        //
        //    If //FrmMain.macrotrabajo.Enabled Then Call //FrmMain.DesactivarMacroTrabajo
    }

}
