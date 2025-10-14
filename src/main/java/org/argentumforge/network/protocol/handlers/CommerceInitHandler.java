package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.game.User;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FComerce;
import org.argentumforge.network.PacketBuffer;

public class CommerceInitHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        // comerciando = true
        User.INSTANCE.setUserComerciando(true);
        ImGUISystem.INSTANCE.show(new FComerce());
    }

}
