package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.i18n.I18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Comando que encapsula una optimización masiva del mapa.
 * Almacena el estado previo y posterior de los tiles modificados para permitir
 * Undo/Redo.
 */
public class MassOptimizeCommand extends AbstractCommand {

    private final Map<Integer, TileState> beforeStates = new HashMap<>();
    private final Map<Integer, TileState> afterStates = new HashMap<>();

    public MassOptimizeCommand(MapContext context) {
        super(context);
    }

    /**
     * Registra un cambio en un tile específico.
     * 
     * @param x       Coordenada X del tile.
     * @param y       Coordenada Y del tile.
     * @param oldData Datos originales del tile (antes del cambio).
     * @param newData Datos nuevos del tile (después del cambio).
     */
    public void addChange(int x, int y, MapData oldData, MapData newData) {
        int key = (x << 16) | y; // Simple key based on coordinates (assuming 100x100 map)
        if (!beforeStates.containsKey(key)) {
            beforeStates.put(key, new TileState(oldData));
        }
        afterStates.put(key, new TileState(newData));
    }

    @Override
    public void execute() {
        applyStates(afterStates);
    }

    @Override
    public void undo() {
        applyStates(beforeStates);
    }

    private void applyStates(Map<Integer, TileState> states) {
        MapData[][] map = context.getMapData();
        if (map == null)
            return;

        for (Map.Entry<Integer, TileState> entry : states.entrySet()) {
            int key = entry.getKey();
            int x = (key >> 16) & 0xFFFF;
            int y = key & 0xFFFF;

            if (x >= 1 && x < map.length && y >= 1 && y < map[0].length) {
                entry.getValue().applyTo(map[x][y]);
            }
        }
    }

    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.optimize");
    }

    @Override
    public int[] getAffectedBounds() {
        // Optimización masiva, devolvemos null para indicar que afecta a todo el mapa
        // potencialmente
        // o calculamos los bounds si es necesario, pero suele ser todo el mapa.
        return null;
    }

    public boolean hasChanges() {
        return !beforeStates.isEmpty();
    }

    /**
     * Instantánea del estado de un tile.
     */
    private static class TileState {
        boolean blocked;
        int trigger;
        int exitMap, exitX, exitY;
        int npcIndex;
        int objIndex, objAmount;
        int particleIndex;
        int[] layerGrhs = new int[5]; // 1-4 used
        int objGrhIndex;

        public TileState(MapData data) {
            this.blocked = data.getBlocked();
            this.trigger = data.getTrigger();
            this.exitMap = data.getExitMap();
            this.exitX = data.getExitX();
            this.exitY = data.getExitY();
            this.npcIndex = data.getNpcIndex();
            this.objIndex = data.getObjIndex();
            this.objAmount = data.getObjAmount();
            this.particleIndex = data.getParticleIndex();

            for (int i = 1; i <= 4; i++) {
                this.layerGrhs[i] = data.getLayer(i).getGrhIndex();
            }
            this.objGrhIndex = data.getObjGrh().getGrhIndex();
        }

        public void applyTo(MapData data) {
            data.setBlocked(this.blocked);
            data.setTrigger(this.trigger);
            data.setExitMap(this.exitMap);
            data.setExitX(this.exitX);
            data.setExitY(this.exitY);
            data.setNpcIndex(this.npcIndex);
            data.setObjIndex(this.objIndex);
            data.setObjAmount(this.objAmount);
            data.setParticleIndex(this.particleIndex);

            for (int i = 1; i <= 4; i++) {
                data.getLayer(i).setGrhIndex(this.layerGrhs[i]);
                org.argentumforge.engine.utils.GameData.initGrh(data.getLayer(i), this.layerGrhs[i], true);
            }

            data.getObjGrh().setGrhIndex(this.objGrhIndex);
            org.argentumforge.engine.utils.GameData.initGrh(data.getObjGrh(), this.objGrhIndex, true);
        }
    }
}
