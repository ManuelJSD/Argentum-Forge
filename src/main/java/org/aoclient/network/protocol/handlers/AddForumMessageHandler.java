package org.aoclient.network.protocol.handlers;

import org.aoclient.engine.game.managers.ForumManager;
import org.aoclient.network.PacketBuffer;
import org.tinylog.Logger;

public class AddForumMessageHandler implements PacketHandler {

    @Override
    public void handle(PacketBuffer buffer) {
        if (buffer.checkBytes(8)) return;
        PacketBuffer tempBuffer = new PacketBuffer();
        tempBuffer.copy(buffer);
        tempBuffer.readByte();

        int forumType = tempBuffer.readByte();
        String title = tempBuffer.readCp1252String();
        String author = tempBuffer.readCp1252String();
        String message = tempBuffer.readCp1252String();

        ForumManager forumManager = ForumManager.getInstance();
        
        // Si el foro no está limpio, lo limpiamos
        if (!forumManager.isClean()) {
            forumManager.clearForums();
            forumManager.setClean(true);
        }

        // Determinar la alineación y si es un anuncio basado en el tipo de foro
        int alignment = getForumAlignment(forumType);
        boolean isSticky = isAnnouncement(forumType);
        
        // Agregar el mensaje al foro
        forumManager.addPost(alignment, title, author, message, isSticky);

        buffer.copy(tempBuffer);
        Logger.debug("Mensaje del foro agregado - Título: {}, Autor: {}", title, author);
    }

    /**
     * Obtiene la alineación del foro basado en el tipo.
     * Basado en la función ForumAlignment de VB6.
     * @param forumType Tipo de foro
     * @return 0 para General, 1 para Caos, 2 para Real
     */
    private int getForumAlignment(int forumType) {
        // Limpiar el bit alto para obtener el tipo base (0=General, 1=Caos, 2=Real)
        int baseType = forumType & 0x0F; // Usamos solo los 4 bits bajos
        
        switch (baseType) {
            case 1:  // CAOS
                return 1;
            case 2:  // REAL
                return 2;
            case 0:  // GENERAL
            default:
                return 0; // General por defecto
        }
    }

    /**
     * Determina si el tipo de foro es un anuncio (sticky).
     * Basado en la función EsAnuncio de VB6.
     * @param forumType Tipo de foro
     * @return true si es un mensaje sticky, false en caso contrario
     */
    private boolean isAnnouncement(int forumType) {
        // Verificar si el tipo de foro es uno de los tipos sticky
        // Según el enum ForumMessageType, los valores 10, 11 y 12 son sticky
        return forumType == 10 || forumType == 11 || forumType == 12;
    }
}
