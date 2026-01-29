package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.AssetRegistry;

import org.argentumforge.engine.utils.inits.ObjData;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Gestor de estado del editor para la manipulación de objetos en el mapa.
 * 
 * Implementa el patrón Singleton para mantener una única instancia del estado
 * de edición de objetos (modo de edición y objeto seleccionado).
 */
public class Obj {

    private static volatile Obj instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = quitar, 3 = capturar (pick)
    private int objNumber;

    private Obj() {
        this.mode = 0;
        this.objNumber = 0;
    }

    /**
     * Obtiene la instancia única del gestor de edición de objetos.
     */
    public static Obj getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Obj();
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

    public int getObjNumber() {
        return objNumber;
    }

    public void setObjNumber(int objNumber) {
        this.objNumber = objNumber;
    }

    /**
     * Ejecuta la acción de edición (colocar o quitar) en las coordenadas dadas.
     * 
     * @param x Coordenada X del mapa.
     * @param y Coordenada Y del mapa.
     */
    public void obj_edit(int x, int y) {
        switch (mode) {
            case 1:
                place(x, y);
                break;
            case 2:
                remove(x, y);
                break;
            case 3:
                pick(x, y);
                break;
            default:
                break;
        }
    }

    /**
     * Coloca el objeto seleccionado en las coordenadas especificadas.
     * 
     * @param x Coordenada X del mapa.
     * @param y Coordenada Y del mapa.
     */
    private void place(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            if (AssetRegistry.objs != null && AssetRegistry.objs.containsKey(objNumber)) {
                ObjData data = AssetRegistry.objs.get(objNumber);
                int oldGrh = mapData[x][y].getObjGrh().getGrhIndex();
                int newGrh = data.getGrhIndex();

                if (oldGrh == newGrh)
                    return;

                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                        new org.argentumforge.engine.utils.editor.commands.ObjChangeCommand(
                                org.argentumforge.engine.utils.GameData.getActiveContext(), x, y, oldGrh, newGrh));
            }
        }
    }

    private void remove(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int oldGrh = mapData[x][y].getObjGrh().getGrhIndex();
            if (oldGrh == 0)
                return;

            org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(
                    new org.argentumforge.engine.utils.editor.commands.ObjChangeCommand(
                            org.argentumforge.engine.utils.GameData.getActiveContext(), x, y, oldGrh, 0));
        }
    }

    /**
     * Captura el ID del objeto en las coordenadas dadas.
     */
    private void pick(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int grhIdx = mapData[x][y].getObjGrh().getGrhIndex();
            if (grhIdx > 0) {
                // Buscamos el ObjNumber que use este GrhIndex
                for (org.argentumforge.engine.utils.inits.ObjData data : AssetRegistry.objs.values()) {
                    if (data.getGrhIndex() == grhIdx) {
                        this.objNumber = data.getNumber();
                        this.mode = 1; // Volver a modo colocar
                        break;
                    }
                }
            }
        }
    }
}
