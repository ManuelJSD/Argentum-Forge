package org.argentumforge.engine.gui;

/**
 * Sistema centralizado de colores y estilos para la UI.
 * <p>
 * Define la paleta de colores estándar para mantener consistencia visual
 * en toda la aplicación. Los colores están definidos en formato ABGR
 * (0xAABBGGRR)
 * para compatibilidad con ImGui.
 * <p>
 * Uso:
 * 
 * <pre>
 * ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
 * ImGui.button("Mi Botón");
 * ImGui.popStyleColor();
 * </pre>
 */
public class Theme {

    // ========== Functional Colors ==========

    /** Color primario para acciones principales (Azul Material: #2196F3) */
    public static final int COLOR_PRIMARY = 0xFFF39621;

    /** Color de acento para estados activos/toggles (Verde Material: #4CAF50) */
    public static final int COLOR_ACCENT = 0xFF50AF4C;

    /** Color para acciones destructivas o errores (Rojo Material: #F44336) */
    public static final int COLOR_DANGER = 0xFF3643F4;

    /** Color para advertencias (Amarillo Material: #FFC107) */
    public static final int COLOR_WARNING = 0xFF07C1FF;

    // ========== Text Colors ==========

    /** Texto estándar (Blanco) */
    public static final int COLOR_TEXT = 0xFFFFFFFF;

    /** Texto deshabilitado o secundario (Gris Claro: #B0B0B0) */
    public static final int COLOR_TEXT_DIM = 0xFFB0B0B0;

    /** Texto muy tenue para hints (Gris Oscuro: #808080) */
    public static final int COLOR_TEXT_HINT = 0xFF808080;

    // ========== Background Colors ==========

    /** Fondo transparente */
    public static final int TRANSPARENT = 0x00000000;

    /** Fondo de panel oscuro (#2D2D2D) */
    public static final int BG_PANEL = 0xFF2D2D2D;

    // ========== Utility Methods ==========

    /**
     * Convierte un color RGBA (0-255) a formato ABGR para ImGui.
     * 
     * @param r Componente rojo (0-255)
     * @param g Componente verde (0-255)
     * @param b Componente azul (0-255)
     * @param a Componente alpha (0-255)
     * @return Color en formato ABGR
     */
    public static int rgba(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Convierte un color RGB (0-255) a formato ABGR con alpha completo.
     * 
     * @param r Componente rojo (0-255)
     * @param g Componente verde (0-255)
     * @param b Componente azul (0-255)
     * @return Color en formato ABGR con alpha = 255
     */
    public static int rgb(int r, int g, int b) {
        return rgba(r, g, b, 255);
    }

    /**
     * Aplica transparencia a un color existente.
     * 
     * @param color Color base en formato ABGR
     * @param alpha Nuevo valor de alpha (0.0f = transparente, 1.0f = opaco)
     * @return Color con alpha modificado
     */
    public static int withAlpha(int color, float alpha) {
        int a = (int) (alpha * 255.0f);
        return (color & 0x00FFFFFF) | (a << 24);
    }
}
