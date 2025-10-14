package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.network.PacketBuffer;
import org.tinylog.Logger;

import static org.argentumforge.engine.utils.GameData.charList;

public class UpdateHPHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        buffer.readByte();

        User.INSTANCE.setUserMinHP(buffer.readInteger());

        charList[User.INSTANCE.getUserCharIndex()].setDead(User.INSTANCE.getUserMinHP() <= 0);

        //
        //    'Is the user alive??
        //    If UserMinHP = 0 Then
        //        UserEstado = 1
        //        If //FrmMain.TrainingMacro Then Call //FrmMain.DesactivarMacroHechizos
        //        If //FrmMain.macrotrabajo Then Call //FrmMain.DesactivarMacroTrabajo
        //    Else
        //        UserEstado = 0
        //    End If

        Logger.debug("handleUpdateHP CARGADO - FALTA DESACTIVAR MACROS DE TRABAJO!");
    }

}
