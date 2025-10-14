package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FPanelGM;
import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class ShowGMPanelFormHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        buffer.readByte();
        ImGUISystem.INSTANCE.show(new FPanelGM());
    }

}
