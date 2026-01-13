package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.game.models.Character;
import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Gestor de estado del editor para la manipulación de NPCs en el mapa.
 * 
 * Implementa el patrón Singleton para mantener una única instancia del estado
 * de edición de NPCs (modo de edición y número de NPC seleccionado).
 */
public class Npc {

    private static volatile Npc instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = quitar
    private int npcNumber;

    private Npc() {
        this.mode = 0;
        this.npcNumber = 0;
    }

    /**
     * Obtiene la instancia única del gestor de edición de NPCs.
     */
    public static Npc getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Npc();
                }
            }
        }
        return instance;
    }

    /**
     * Resetea la instancia del Singleton.
     */
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getNpcNumber() {
        return npcNumber;
    }

    public void setNpcNumber(int npcNumber) {
        this.npcNumber = npcNumber;
    }

    /**
     * Ejecuta la acción de edición (colocar o quitar) en las coordenadas dadas.
     * 
     * @param x Coordenada X del mapa.
     * @param y Coordenada Y del mapa.
     */
    public void npc_edit(int x, int y) {
        switch (mode) {
            case 1:
                place(x, y);
                break;
            case 2:
                remove(x, y);
                break;
            default:
                break;
        }
    }

    /**
     * Coloca el NPC seleccionado en las coordenadas especificadas.
     * 
     * @param x Coordenada X del mapa.
     * @param y Coordenada Y del mapa.
     */
    private void place(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {

            // Si ya hay un personaje ahí, lo borramos primero
            if (mapData[x][y].getCharIndex() != 0) {
                Character.eraseChar(mapData[x][y].getCharIndex());
            }

            // Actualizamos el dato del mapa (para persistencia)
            mapData[x][y].setNpcIndex((short) npcNumber);

            // Creamos la instancia visual si hay un NPC seleccionado
            if (npcNumber > 0 && org.argentumforge.engine.utils.AssetRegistry.npcs.containsKey(npcNumber)) {
                org.argentumforge.engine.utils.inits.NpcData data = org.argentumforge.engine.utils.AssetRegistry.npcs
                        .get(npcNumber);

                // Buscamos un slot libre en charList
                for (short i = 1; i < org.argentumforge.engine.utils.GameData.charList.length; i++) {
                    if (!org.argentumforge.engine.utils.GameData.charList[i].isActive()) {
                        Character.makeChar(i, data.getBody(), data.getHead(),
                                org.argentumforge.engine.game.models.Direction.DOWN, x, y, 0, 0, 0);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Quita cualquier NPC de las coordenadas especificadas.
     * 
     * @param x Coordenada X del mapa.
     * @param y Coordenada Y del mapa.
     */
    private void remove(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {

            // Borramos la instancia visual si existe
            short charIndex = mapData[x][y].getCharIndex();
            if (charIndex != 0) {
                Character.eraseChar(charIndex);
            }

            // Limpiamos el dato del mapa
            mapData[x][y].setNpcIndex((short) 0);
        }
    }
}
