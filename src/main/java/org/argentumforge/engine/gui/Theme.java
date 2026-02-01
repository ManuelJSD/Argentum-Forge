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
        MODERN,
        MATRIX,
        DRACULA,
        CHERRY
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
            case MATRIX -> applyMatrixStyle();
            case DRACULA -> applyDraculaStyle();
            case CHERRY -> applyCherryStyle();
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

    public static void applyMatrixStyle() {
        ImGuiStyle style = ImGui.getStyle();
        ImGui.styleColorsDark();

        int black = rgba(0, 0, 0, 255);
        int darkGreen = rgba(0, 50, 0, 255);
        int green = rgba(0, 255, 0, 255);
        int brightGreen = rgba(50, 255, 50, 255);
        int text = rgba(200, 255, 200, 255);

        style.setColor(ImGuiCol.Text, text);
        style.setColor(ImGuiCol.WindowBg, rgba(0, 10, 0, 240));
        style.setColor(ImGuiCol.ChildBg, rgba(0, 0, 0, 0));
        style.setColor(ImGuiCol.PopupBg, rgba(0, 20, 0, 250));
        style.setColor(ImGuiCol.Border, green);
        style.setColor(ImGuiCol.FrameBg, darkGreen);
        style.setColor(ImGuiCol.FrameBgHovered, rgba(0, 80, 0, 255));
        style.setColor(ImGuiCol.FrameBgActive, rgba(0, 100, 0, 255));
        style.setColor(ImGuiCol.TitleBg, black);
        style.setColor(ImGuiCol.TitleBgActive, darkGreen);
        style.setColor(ImGuiCol.TitleBgCollapsed, black);
        style.setColor(ImGuiCol.MenuBarBg, black);
        style.setColor(ImGuiCol.ScrollbarBg, black);
        style.setColor(ImGuiCol.ScrollbarGrab, darkGreen);
        style.setColor(ImGuiCol.ScrollbarGrabHovered, green);
        style.setColor(ImGuiCol.ScrollbarGrabActive, brightGreen);
        style.setColor(ImGuiCol.CheckMark, green);
        style.setColor(ImGuiCol.SliderGrab, green);
        style.setColor(ImGuiCol.SliderGrabActive, brightGreen);
        style.setColor(ImGuiCol.Button, darkGreen);
        style.setColor(ImGuiCol.ButtonHovered, rgba(0, 120, 0, 255));
        style.setColor(ImGuiCol.ButtonActive, rgba(0, 150, 0, 255));
        style.setColor(ImGuiCol.Header, darkGreen);
        style.setColor(ImGuiCol.HeaderHovered, rgba(0, 100, 0, 255));
        style.setColor(ImGuiCol.HeaderActive, green);
        style.setColor(ImGuiCol.Separator, darkGreen);
        style.setColor(ImGuiCol.SeparatorHovered, green);
        style.setColor(ImGuiCol.SeparatorActive, brightGreen);
        style.setColor(ImGuiCol.ResizeGrip, darkGreen);
        style.setColor(ImGuiCol.ResizeGripHovered, green);
        style.setColor(ImGuiCol.ResizeGripActive, brightGreen);
        style.setColor(ImGuiCol.Tab, darkGreen);
        style.setColor(ImGuiCol.TabHovered, green);
        style.setColor(ImGuiCol.TabActive, rgba(0, 100, 0, 255));
        style.setColor(ImGuiCol.TabUnfocused, rgba(0, 30, 0, 255));
        style.setColor(ImGuiCol.TabUnfocusedActive, rgba(0, 60, 0, 255));
        style.setColor(ImGuiCol.PlotLines, green);
        style.setColor(ImGuiCol.PlotLinesHovered, brightGreen);
        style.setColor(ImGuiCol.PlotHistogram, green);
        style.setColor(ImGuiCol.PlotHistogramHovered, brightGreen);
        style.setColor(ImGuiCol.TextSelectedBg, rgba(0, 255, 0, 100));

        style.setWindowBorderSize(1.0f);
        style.setFrameBorderSize(1.0f);
        style.setPopupBorderSize(1.0f);
    }

    public static void applyDraculaStyle() {
        ImGuiStyle style = ImGui.getStyle();
        ImGui.styleColorsDark();

        int bg = rgba(40, 42, 54, 255);
        int currentLine = rgba(68, 71, 90, 255);
        int fg = rgba(248, 248, 242, 255);
        int comment = rgba(98, 114, 164, 255);
        int green = rgba(80, 250, 123, 255);
        int pink = rgba(255, 121, 198, 255);
        int purple = rgba(189, 147, 249, 255);

        style.setColor(ImGuiCol.WindowBg, bg);
        style.setColor(ImGuiCol.Text, fg);
        style.setColor(ImGuiCol.Border, currentLine);
        style.setColor(ImGuiCol.FrameBg, currentLine);
        style.setColor(ImGuiCol.FrameBgHovered, comment);
        style.setColor(ImGuiCol.FrameBgActive, comment);
        style.setColor(ImGuiCol.TitleBg, rgba(30, 30, 40, 255));
        style.setColor(ImGuiCol.TitleBgActive, currentLine);
        style.setColor(ImGuiCol.MenuBarBg, bg);
        style.setColor(ImGuiCol.CheckMark, green);
        style.setColor(ImGuiCol.SliderGrab, purple);
        style.setColor(ImGuiCol.SliderGrabActive, pink);
        style.setColor(ImGuiCol.Button, currentLine);
        style.setColor(ImGuiCol.ButtonHovered, comment);
        style.setColor(ImGuiCol.ButtonActive, purple);
        style.setColor(ImGuiCol.Header, currentLine);
        style.setColor(ImGuiCol.HeaderHovered, comment);
        style.setColor(ImGuiCol.HeaderActive, purple);
        style.setColor(ImGuiCol.Separator, currentLine);
        style.setColor(ImGuiCol.ResizeGrip, currentLine);
        style.setColor(ImGuiCol.ResizeGripHovered, pink);
        style.setColor(ImGuiCol.ResizeGripActive, pink);
        style.setColor(ImGuiCol.Tab, currentLine);
        style.setColor(ImGuiCol.TabHovered, comment);
        style.setColor(ImGuiCol.TabActive, purple);
        style.setColor(ImGuiCol.TabUnfocused, bg);
        style.setColor(ImGuiCol.TabUnfocusedActive, currentLine);
    }

    public static void applyCherryStyle() {
        ImGuiStyle style = ImGui.getStyle();
        ImGui.styleColorsDark();

        int bg = rgba(15, 0, 5, 245); // Dark cherry background
        int text = rgba(255, 200, 200, 255); // Pale pink text
        int frame = rgba(60, 0, 20, 255); // Dark red frame
        int active = rgba(180, 0, 60, 255); // Bright cherry red
        int hover = rgba(120, 0, 40, 255);

        style.setColor(ImGuiCol.WindowBg, bg);
        style.setColor(ImGuiCol.Text, text);
        style.setColor(ImGuiCol.Border, active);
        style.setColor(ImGuiCol.FrameBg, frame);
        style.setColor(ImGuiCol.FrameBgHovered, hover);
        style.setColor(ImGuiCol.FrameBgActive, active);
        style.setColor(ImGuiCol.TitleBg, frame);
        style.setColor(ImGuiCol.TitleBgActive, active);
        style.setColor(ImGuiCol.CheckMark, active);
        style.setColor(ImGuiCol.SliderGrab, active);
        style.setColor(ImGuiCol.SliderGrabActive, rgba(255, 50, 100, 255));
        style.setColor(ImGuiCol.Button, frame);
        style.setColor(ImGuiCol.ButtonHovered, hover);
        style.setColor(ImGuiCol.ButtonActive, active);
        style.setColor(ImGuiCol.Header, frame);
        style.setColor(ImGuiCol.HeaderHovered, hover);
        style.setColor(ImGuiCol.HeaderActive, active);
        style.setColor(ImGuiCol.Separator, active);
        style.setColor(ImGuiCol.Tab, frame);
        style.setColor(ImGuiCol.TabHovered, hover);
        style.setColor(ImGuiCol.TabActive, active);
        style.setColor(ImGuiCol.TabUnfocused, rgba(40, 0, 10, 255));
        style.setColor(ImGuiCol.TabUnfocusedActive, frame);
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
