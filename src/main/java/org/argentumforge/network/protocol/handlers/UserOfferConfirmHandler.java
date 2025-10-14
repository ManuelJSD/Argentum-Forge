package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

public class UserOfferConfirmHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        // With frmComerciarUsu
        //        ' Now he can accept the offer or reject it
        //        .HabilitarAceptarRechazar True
        //
        //        .PrintCommerceMsg TradingUserName & " ha confirmado su oferta!", FontTypeNames.FONTTYPE_CONSE
        //    End With

        Logger.debug("handleUserOfferConfirm CARGADO - FALTA TERMINAR!");
    }

}
