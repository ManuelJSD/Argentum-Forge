package org.argentumforge.engine.utils;

import org.argentumforge.engine.utils.inits.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Registro centralizado de todos los recursos (assets) cargados en memoria.
 * 
 * Esta clase actúa como un almacén global para los datos estáticos del juego,
 * separando la persistencia y el almacenamiento de la lógica de procesamiento.
 * Centraliza el acceso a NPCs, objetos, animaciones y equipamiento.
 */
public final class AssetRegistry {

    private AssetRegistry() {
        // Clase de utilidad
    }

    // --- Datos de Personajes y Equipamiento ---
    /** Datos de animaciones de cuerpos. */
    public static BodyData[] bodyData;
    /** Datos de gráficos de cabezas. */
    public static HeadData[] headData;
    /** Datos de gráficos de cascos. */
    public static HeadData[] helmetsData;
    /** Datos de gráficos de armas. */
    public static WeaponData[] weaponData;
    /** Datos de gráficos de escudos. */
    public static ShieldData[] shieldData;

    // --- Datos Visuales y Animaciones ---
    /** Datos de efectos visuales (FXs). */
    public static FxData[] fxData;
    /** Datos de definiciones de gráficos (GRH). */
    public static GrhData[] grhData;
    /** Mapa de colores asignados a GRHs para el minimapa. */
    public static Map<Integer, Integer> minimapColors = new HashMap<>();

    // --- Definiciones de Entidades ---
    /** Diccionario de definiciones de NPCs indexado por ID. */
    public static Map<Integer, NpcData> npcs = new HashMap<>();
    /** Diccionario de definiciones de Objetos indexado por ID. */
    public static Map<Integer, ObjData> objs = new HashMap<>();

    /**
     * Limpia todas las colecciones y libera las referencias a los datos cargados.
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
