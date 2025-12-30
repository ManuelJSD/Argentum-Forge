package org.argentumforge.engine.utils.inits;

/**
 * Representa las propiedades generales de un mapa del juego.
 * <p>
 * Esta clase almacena la información proveniente del archivo .dat asociado al
 * mapa,
 * como su nombre, si es zona segura, música de fondo, etc.
 */
public final class MapProperties {

    private String name = "Mapa Nuevo";
    private int musicIndex = 0;
    private int magiaSinEfecto = 0;
    private int noEncriptarMP = 0;
    private int playerKiller = 1;
    private int Restringir = 0;
    private int Backup = 1;
    private String zona = "CIUDAD";
    private String Terreno = "BOSQUE";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMusicIndex() {
        return musicIndex;
    }

    public void setMusicIndex(int musicIndex) {
        this.musicIndex = musicIndex;
    }

    public int getMagiaSinEfecto() {
        return magiaSinEfecto;
    }

    public void setMagiaSinEfecto(int magiaSinEfecto) {
        this.magiaSinEfecto = magiaSinEfecto;
    }

    public int getNoEncriptarMP() {
        return noEncriptarMP;
    }

    public void setNoEncriptarMP(int noEncriptarMP) {
        this.noEncriptarMP = noEncriptarMP;
    }

    public int getPlayerKiller() {
        return playerKiller;
    }

    public void setPlayerKiller(int playerKiller) {
        this.playerKiller = playerKiller;
    }

    public int getRestringir() {
        return Restringir;
    }

    public void setRestringir(int restringir) {
        Restringir = restringir;
    }

    public int getBackup() {
        return Backup;
    }

    public void setBackup(int backup) {
        Backup = backup;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getTerreno() {
        return Terreno;
    }

    public void setTerreno(String terreno) {
        Terreno = terreno;
    }
}
