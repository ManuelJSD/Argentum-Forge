package org.argentumforge.engine.gui;

import imgui.ImGui;
import imgui.ImGuiStyle;
import imgui.flag.ImGuiCol;

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

    public enum StyleType {
        CLASSIC,
        DARK,
        LIGHT,
        MODERN
    }

    /**
     * Aplica un estilo basado en el tipo seleccionado.
     */
    public static void applyStyle(StyleType type) {
        switch (type) {
            case CLASSIC -> ImGui.styleColorsClassic();
            case DARK -> ImGui.styleColorsDark();
            case LIGHT -> ImGui.styleColorsLight();
            case MODERN -> applyModernStyle();
        }
    }

    /**
     * Aplica un estilo moderno y profesional a ImGui.
     * Configura colores oscuros, bordes redondeados y un espaciado equilibrado.
     */
    public static void applyModernStyle() {
        ImGuiStyle style = ImGui.getStyle();

        // Limpiar estilos previos (basarse en dark para empezar)
        ImGui.styleColorsDark();

        // --- Configuración de Colores (Formato ABGR) ---
        // Window & Background
        style.setColor(ImGuiCol.WindowBg, rgba(30, 30, 30, 240));
        style.setColor(ImGuiCol.ChildBg, rgba(35, 35, 35, 0));
        style.setColor(ImGuiCol.PopupBg, rgba(40, 40, 40, 245));
        style.setColor(ImGuiCol.Border, rgba(60, 60, 60, 120));
        style.setColor(ImGuiCol.BorderShadow, rgba(0, 0, 0, 0));

        // Frame
        style.setColor(ImGuiCol.FrameBg, rgba(45, 45, 45, 255));
        style.setColor(ImGuiCol.FrameBgHovered, rgba(60, 60, 60, 255));
        style.setColor(ImGuiCol.FrameBgActive, rgba(70, 70, 70, 255));

        // Titles
        style.setColor(ImGuiCol.TitleBg, rgba(25, 25, 25, 255));
        style.setColor(ImGuiCol.TitleBgActive, rgba(35, 35, 35, 255));
        style.setColor(ImGuiCol.TitleBgCollapsed, rgba(20, 20, 20, 150));

        // Tabs
        style.setColor(ImGuiCol.Tab, rgba(35, 35, 35, 255));
        style.setColor(ImGuiCol.TabHovered, rgba(60, 60, 60, 255));
        style.setColor(ImGuiCol.TabActive, rgba(50, 50, 50, 255));
        style.setColor(ImGuiCol.TabUnfocused, rgba(30, 30, 30, 255));
        style.setColor(ImGuiCol.TabUnfocusedActive, rgba(35, 35, 35, 255));

        // Buttons
        style.setColor(ImGuiCol.Button, rgba(55, 55, 55, 255));
        style.setColor(ImGuiCol.ButtonHovered, rgba(75, 75, 75, 255));
        style.setColor(ImGuiCol.ButtonActive, rgba(90, 90, 90, 255));

        // Headers
        style.setColor(ImGuiCol.Header, rgba(45, 45, 45, 255));
        style.setColor(ImGuiCol.HeaderHovered, rgba(60, 60, 60, 255));
        style.setColor(ImGuiCol.HeaderActive, rgba(70, 70, 70, 255));

        // Accent Colors (Using primary/accent definitions)
        style.setColor(ImGuiCol.CheckMark, COLOR_ACCENT);
        style.setColor(ImGuiCol.SliderGrab, rgba(100, 100, 100, 255));
        style.setColor(ImGuiCol.SliderGrabActive, COLOR_PRIMARY);
        style.setColor(ImGuiCol.SeparatorHovered, COLOR_PRIMARY);
        style.setColor(ImGuiCol.SeparatorActive, COLOR_PRIMARY);
        style.setColor(ImGuiCol.ResizeGrip, rgba(80, 80, 80, 255));
        style.setColor(ImGuiCol.ResizeGripHovered, COLOR_PRIMARY);
        style.setColor(ImGuiCol.ResizeGripActive, COLOR_PRIMARY);

        // --- Configuración de Estilo (Bordes y Espaciado) ---
        style.setWindowRounding(6.0f);
        style.setChildRounding(4.0f);
        style.setFrameRounding(4.0f);
        style.setPopupRounding(4.0f);
        style.setScrollbarRounding(12.0f);
        style.setGrabRounding(4.0f);
        style.setTabRounding(4.0f);

        style.setFramePadding(6.0f, 4.0f);
        style.setItemSpacing(8.0f, 6.0f);
        style.setIndentSpacing(22.0f);
        style.setScrollbarSize(14.0f);

        style.setWindowBorderSize(1.0f);
        style.setChildBorderSize(1.0f);
        style.setPopupBorderSize(1.0f);
        style.setFrameBorderSize(0.0f);
        style.setTabBorderSize(0.0f);
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
