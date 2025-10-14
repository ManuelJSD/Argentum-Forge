package org.argentumforge.network.protocol.handlers.gm;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FSpawnList;
import org.argentumforge.network.PacketBuffer;
import org.argentumforge.network.protocol.handlers.PacketHandler;

public class SpawnListHandler implements PacketHandler {

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
        ImGUISystem.INSTANCE.show(new FSpawnList(creatureList));

        buffer.copy(tempBuffer);
    }

}
