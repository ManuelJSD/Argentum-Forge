package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.Connection;
import org.tinylog.Logger;

import static org.argentumforge.engine.audio.Sound.playMusic;
import static org.argentumforge.engine.game.models.Character.eraseAllChars;

public class DisconnectHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();

        Connection.INSTANCE.disconnect();
        eraseAllChars();

        ImGUISystem.INSTANCE.closeAllFrms();

        Sound.clearSounds();

        /*
        'Hide main form
    //FrmMain.Visible = False

    'Stop audio
    Call Audio.StopWave
    //FrmMain.IsPlaying = PlayLoop.plNone

    'Show connection form
    frmConnect.Visible = True

    'Reset global vars
    UserDescansar = False
    UserParalizado = False
    pausa = False
    UserCiego = False
    UserMeditar = False
    UserNavegando = False
    bRain = False
    bFogata = False
    SkillPoints = 0
    Comerciando = False
    'new
    Traveling = False
    'Delete all kind of dialogs
    Call CleanDialogs

    'Reset some char variables...
    For i = 1 To LastChar
        charlist(i).invisible = False
    Next i


    For i = 1 To MAX_INVENTORY_SLOTS
        Call Inventario.SetItem(i, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "")
    Next i

    Call Audio.PlayMIDI("2.mid")
         */

        playMusic("2.ogg");
        Logger.debug("handleDisconnect CARGADO - FALTA TERMINAR!");
    }

}
