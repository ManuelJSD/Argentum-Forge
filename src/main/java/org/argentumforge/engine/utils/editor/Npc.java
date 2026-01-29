package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.MapContext;

/**
 * Gestor de estado del editor para la manipulación de NPCs en el mapa.
 * 
 * Implementa el patrón Singleton para mantener una única instancia del estado
 * de edición de NPCs (modo de edición y número de NPC seleccionado).
 */
public class Npc {

    private static volatile Npc instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = quitar, 3 = capturar (pick)
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
    public void npc_edit(MapContext context, int x, int y) {
        switch (mode) {
            case 1:
                place(context, x, y);
                break;
            case 2:
                remove(context, x, y);
                break;
            case 3:
                pick(context, x, y);
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
    private void place(MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int oldNpc = mapData[x][y].getNpcIndex();
            if (oldNpc == npcNumber)
                return;

            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.NpcChangeCommand(
                            context, x, y, oldNpc,
                            npcNumber));
        }
    }

    private void remove(MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int oldNpc = mapData[x][y].getNpcIndex();
            if (oldNpc == 0)
                return;

            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.NpcChangeCommand(
                            context, x, y, oldNpc, 0));
        }
    }

    /**
     * Captura el ID del NPC en las coordenadas dadas.
     */
    private void pick(MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int npcIdx = mapData[x][y].getNpcIndex();
            if (npcIdx > 0) {
                this.npcNumber = npcIdx;
                // Una vez capturado, volvemos a modo colocar con ese NPC
                this.mode = 1;
            }
        }
    }
}
