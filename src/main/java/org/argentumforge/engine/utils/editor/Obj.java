package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.ObjData;

import static org.argentumforge.engine.utils.GameData.initGrh;
import static org.argentumforge.engine.utils.GameData.mapData;

public class Obj {

    private static volatile Obj instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = quitar
    private int objNumber;

    private Obj() {
        this.mode = 0;
        this.objNumber = 0;
    }

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

    public void obj_edit(int x, int y) {
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

    private void place(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            if (GameData.objs != null && GameData.objs.containsKey(objNumber)) {
                ObjData data = GameData.objs.get(objNumber);
                mapData[x][y].getObjGrh().setGrhIndex(0); // Reset before init
                initGrh(mapData[x][y].getObjGrh(), (short) data.getGrhIndex(), false);
            }
        }
    }

    private void remove(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            mapData[x][y].getObjGrh().setGrhIndex(0);
        }
    }
}
