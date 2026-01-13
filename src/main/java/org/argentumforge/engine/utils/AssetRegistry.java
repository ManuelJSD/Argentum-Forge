package org.argentumforge.engine.utils;

import org.argentumforge.engine.utils.inits.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Registro centralizado de todos los assets cargados del juego.
 * Esta clase separa el almacenamiento de datos de la lógica de GameData.
 */
public final class AssetRegistry {

    private AssetRegistry() {
        // Clase de utilidad
    }

    // --- Datos de Personajes y Equipamiento ---
    public static BodyData[] bodyData;
    public static HeadData[] headData;
    public static HeadData[] helmetsData;
    public static WeaponData[] weaponData;
    public static ShieldData[] shieldData;

    // --- Datos Visuales y Animaciones ---
    public static FxData[] fxData;
    public static GrhData[] grhData;
    public static Map<Integer, Integer> minimapColors = new HashMap<>();

    // --- Definiciones de Entidades ---
    public static Map<Integer, NpcData> npcs = new HashMap<>();
    public static Map<Integer, ObjData> objs = new HashMap<>();

    /**
     * Limpia todos los datos cargados.
     */
    public static void clear() {
        bodyData = null;
        headData = null;
        helmetsData = null;
        weaponData = null;
        shieldData = null;
        fxData = null;
        grhData = null;
        minimapColors.clear();
        npcs.clear();
        objs.clear();
    }
}
