package org.argentumforge.engine.game.managers;

import org.argentumforge.engine.game.models.Forum;

import java.util.ArrayList;
import java.util.List;

/**
 * Maneja la colección de mensajes del foro.
 */
public class ForumManager {
    // Límites de mensajes en el foro
    public static final int MAX_MESSAGES = 30;
    public static final int MAX_ANNOUNCEMENTS = 5;
    private static ForumManager instance;
    private final List<Forum> forumMessages;
    private boolean isClean;

    private ForumManager() {
        this.forumMessages = new ArrayList<>();
        this.isClean = true;
    }

    public static synchronized ForumManager getInstance() {
        if (instance == null) {
            instance = new ForumManager();
        }
        return instance;
    }

    /**
     * Limpia todos los mensajes del foro.
     */
    public void clearForums() {
        forumMessages.clear();
        isClean = true;
    }

    /**
     * Agrega un nuevo mensaje al foro.
     * @param alignment Alineación del mensaje
     * @param title Título del mensaje
     * @param author Autor del mensaje
     * @param message Contenido del mensaje
     * @param isSticky Si es un mensaje fijo (sticky)
     */
    /**
     * Agrega un nuevo mensaje al foro, respetando los límites de mensajes y anuncios.
     * @param alignment Alineación del mensaje
     * @param title Título del mensaje
     * @param author Autor del mensaje
     * @param message Contenido del mensaje
     * @param isSticky Si es un mensaje fijo (sticky)
     * @return true si el mensaje fue agregado, false si se alcanzó el límite
     */
    public boolean addPost(int alignment, String title, String author, String message, boolean isSticky) {
        System.out.println("Agregando mensaje al foro - Título: " + title + ", Sticky: " + isSticky + ", Alineación: " + alignment);
        
        // Verificar límite de mensajes
        if (forumMessages.size() >= MAX_MESSAGES) {
            // Si es un anuncio, verificar si podemos eliminar mensajes no sticky
            if (isSticky) {
                if (!removeOldestNonStickyMessage()) {
                    return false; // No se pudo hacer espacio para el nuevo mensaje
                }
            } else {
                return false; // No hay espacio para más mensajes normales
            }
        }

        // Verificar límite de anuncios si es un anuncio
        if (isSticky) {
            int stickyCount = countStickyMessages();
            if (stickyCount >= MAX_ANNOUNCEMENTS) {
                // Eliminar el anuncio más antiguo
                removeOldestStickyMessage();
            }
        }

        Forum forum = new Forum(alignment, title, author, message, isSticky);
        if (isSticky) {
            // Los mensajes sticky van al principio
            forumMessages.add(0, forum);
        } else {
            forumMessages.add(forum);
        }
        
        return true;
    }

    /**
     * Obtiene todos los mensajes del foro.
     * @return Lista de mensajes del foro
     */
    /**
     * Obtiene todos los mensajes del foro, asegurando que los mensajes sticky estén al principio.
     * @return Lista ordenada de mensajes del foro con los sticky primero
     */
    public List<Forum> getForumMessages() {
        // Devolvemos una copia ordenada con los mensajes sticky primero
        List<Forum> sortedList = new ArrayList<>(forumMessages);
        // Ordenamos para que los sticky estén primero (manteniendo el orden relativo)
        sortedList.sort((m1, m2) -> {
            if (m1.isSticky() && !m2.isSticky()) {
                return -1;
            } else if (!m1.isSticky() && m2.isSticky()) {
                return 1;
            }
            return 0;
        });
        return sortedList;
    }

    public boolean isClean() {
        return isClean;
    }

    public void setClean(boolean clean) {
        isClean = clean;
    }
    
    /**
     * Cuenta la cantidad de mensajes sticky en el foro.
     */
    private int countStickyMessages() {
        return (int) forumMessages.stream().filter(Forum::isSticky).count();
    }
    
    /**
     * Elimina el mensaje sticky más antiguo del foro.
     */
    private void removeOldestStickyMessage() {
        // Buscar desde el final hacia atrás para encontrar el sticky más antiguo
        for (int i = forumMessages.size() - 1; i >= 0; i--) {
            if (forumMessages.get(i).isSticky()) {
                forumMessages.remove(i);
                return;
            }
        }
    }
    
    /**
     * Elimina el mensaje no sticky más antiguo para hacer espacio.
     * @return true si se pudo eliminar un mensaje, false si no hay mensajes no sticky
     */
    private boolean removeOldestNonStickyMessage() {
        // Buscar desde el final hacia atrás para encontrar el primer mensaje no sticky
        for (int i = forumMessages.size() - 1; i >= 0; i--) {
            if (!forumMessages.get(i).isSticky()) {
                forumMessages.remove(i);
                return true;
            }
        }
        return false; // No hay mensajes no sticky para eliminar
    }
}
