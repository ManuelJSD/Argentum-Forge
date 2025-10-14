package org.argentumforge.network.protocol.handlers;

import org.argentumforge.network.PacketBuffer;

/**
 * Representa un contrato que define la accion de manejar los paquetes del servidor.
 */

public interface PacketHandler {

    void handle(PacketBuffer buffer);

}
