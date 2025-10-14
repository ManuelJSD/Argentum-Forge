package org.argentumforge.network.protocol.handlers;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FTrainer;
import org.argentumforge.network.PacketBuffer;

public class TrainerCreatureListHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(3)) return;
        PacketBuffer tempBuffer = new PacketBuffer();
        tempBuffer.copy(buffer);
        tempBuffer.readByte();

        String creatureListString = tempBuffer.readCp1252String();

        // Dividimos por el carácter nulo
        String[] creatureList = creatureListString.split("\0");

        // Mostramos la lista en el GUI
        ImGUISystem.INSTANCE.show(new FTrainer(creatureList));

        buffer.copy(tempBuffer);
    }

}
