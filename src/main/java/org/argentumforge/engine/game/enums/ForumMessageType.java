package org.argentumforge.engine.game.enums;

/**
 * Tipos de mensajes del foro.
 * Basado en el enum eForumMsgType de VB6.
 */
public enum ForumMessageType {
    /**
     * Mensajes normales
     */
    GENERAL(0),
    CAOS(1),
    REAL(2),
    
    /**
     * Mensajes fijos (sticky)
     */
    GENERAL_STICKY(10),
    CAOS_STICKY(11),
    REAL_STICKY(12);
    
    private final int value;
    
    ForumMessageType(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    /**
     * Obtiene el tipo de mensaje a partir de su valor numérico.
     */
    public static ForumMessageType fromValue(int value) {
        for (ForumMessageType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return GENERAL; // Valor por defecto
    }
}
