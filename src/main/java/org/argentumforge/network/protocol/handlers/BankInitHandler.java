package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FBank;
import org.argentumforge.network.PacketBuffer;

public class BankInitHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        int bankGold = buffer.readLong();
        User.INSTANCE.setUserComerciando(true);
        ImGUISystem.INSTANCE.show(new FBank(bankGold));
    }

}
