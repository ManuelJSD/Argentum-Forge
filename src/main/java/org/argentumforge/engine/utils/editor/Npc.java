package org.argentumforge.engine.utils.editor;

import static org.argentumforge.engine.utils.GameData.mapData;

public class Npc {

    private static volatile Npc instance;
    private static final Object lock = new Object();

    private int mode; // 0 = ninguno, 1 = colocar, 2 = quitar
    private int npcNumber;

    private Npc() {
        this.mode = 0;
        this.npcNumber = 0;
    }

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

    private void place(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            mapData[x][y].setNpcIndex((short) npcNumber);
        }
    }

    private void remove(int x, int y) {
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            mapData[x][y].setNpcIndex((short) 0);
        }
    }
}
