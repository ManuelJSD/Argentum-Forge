package org.argentumforge.engine.utils.inits;

/**
 * Representa los datos de una celda individual (tile) del mapa.
 * <p>
 * {@code MapData} almacena la información de las 4 capas de gráficos,
 * el índice del NPC u objeto presente, bloqueos, triggers, información
 * de traslados (exits) y el ítem depositado en el suelo.
 *
 * @see GrhInfo
 */
public final class MapData {

    private final GrhInfo[] layer = new GrhInfo[5];
    private short charIndex;
    private GrhInfo objGrh;
    private int npcIndex;
    private boolean blocked;
    private int trigger;

    // Tile Exit
    private int exitMap;
    private int exitX;
    private int exitY;

    // Object Info
    private int objIndex;
    private int objAmount;

    public MapData() {
        layer[1] = new GrhInfo();
        layer[2] = new GrhInfo();
        layer[3] = new GrhInfo();
        layer[4] = new GrhInfo();
        objGrh = new GrhInfo();
    }

    public GrhInfo getLayer(int index) {
        return layer[index];
    }

    public void setLayer(int index, GrhInfo layer) {
        this.layer[index] = layer;
    }

    public short getCharIndex() {
        return charIndex;
    }

    public void setCharIndex(int charIndex) {
        this.charIndex = (short) charIndex;
    }

    public GrhInfo getObjGrh() {
        return objGrh;
    }

    public void setObjGrh(GrhInfo objGrh) {
        this.objGrh = objGrh;
    }

    public int getNpcIndex() {
        return npcIndex;
    }

    public void setNpcIndex(int npcIndex) {
        this.npcIndex = npcIndex;
    }

    public boolean getBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public int getTrigger() {
        return trigger;
    }

    public void setTrigger(int trigger) {
        this.trigger = trigger;
    }

    public int getExitMap() {
        return exitMap;
    }

    public void setExitMap(int exitMap) {
        this.exitMap = exitMap;
    }

    public int getExitX() {
        return exitX;
    }

    public void setExitX(int exitX) {
        this.exitX = exitX;
    }

    public int getExitY() {
        return exitY;
    }

    public void setExitY(int exitY) {
        this.exitY = exitY;
    }

    public int getObjIndex() {
        return objIndex;
    }

    public void setObjIndex(int objIndex) {
        this.objIndex = objIndex;
    }

    public int getObjAmount() {
        return objAmount;
    }

    public void setObjAmount(int objAmount) {
        this.objAmount = objAmount;
    }

}
