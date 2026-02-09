package org.argentumforge.engine.gui.widgets;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImString;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.i18n.I18n;

/**
 * Biblioteca de componentes UI reutilizables.
 * <p>
 * Proporciona componentes estandarizados para mantener consistencia
 * visual y reducir duplicación de código.
 */
public class UIComponents {

    /**
     * Dibuja un botón toggle que muestra visualmente su estado activo/inactivo.
     * 
     * @param label    Texto del botón
     * @param isActive Estado actual del botón
     * @param width    Ancho del botón
     * @param height   Alto del botón
     * @return true si el botón fue presionado
     */
    public static boolean toggleButton(String label, boolean isActive, float width, float height) {
        if (isActive) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
        }

        boolean clicked = ImGui.button(label, width, height);

        if (isActive) {
            ImGui.popStyleColor();
        }

        return clicked;
    }

    /**
     * Dibuja un botón toggle con tamaño automático.
     * 
     * @param label    Texto del botón
     * @param isActive Estado actual del botón
     * @return true si el botón fue presionado
     */
    public static boolean toggleButton(String label, boolean isActive) {
        return toggleButton(label, isActive, 0, 0);
    }

    /**
     * Dibuja un botón de acción destructiva (eliminar, borrar, etc).
     * 
     * @param label  Texto del botón
     * @param width  Ancho del botón
     * @param height Alto del botón
     * @return true si el botón fue presionado
     */
    public static boolean dangerButton(String label, float width, float height) {
        ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        boolean clicked = ImGui.button(label, width, height);
        ImGui.popStyleColor();
        return clicked;
    }

    /**
     * Dibuja controles de paginación estándar.
     * 
     * @param currentPage Página actual (0-indexed)
     * @param maxPage     Página máxima (0-indexed)
     * @param id          ID único para los botones
     * @return Nueva página seleccionada, o -1 si no cambió
     */
    public static int paginationControls(int currentPage, int maxPage, String id) {
        ImGui.text(I18n.INSTANCE.get("common.page") + ": " + (currentPage + 1) + "/" + (maxPage + 1));
        ImGui.sameLine();

        int newPage = currentPage;

        if (ImGui.button("<##prev" + id) && currentPage > 0) {
            newPage = currentPage - 1;
        }
        ImGui.sameLine();
        if (ImGui.button(">##next" + id) && currentPage < maxPage) {
            newPage = currentPage + 1;
        }

        return newPage != currentPage ? newPage : -1;
    }

    /**
     * Dibuja una barra de búsqueda estándar.
     * 
     * @param searchString ImString que contiene el texto de búsqueda
     * @param hint         Texto de placeholder
     * @param id           ID único para el input
     * @return true si el texto cambió
     */
    public static boolean searchBar(ImString searchString, String hint, String id) {
        ImGui.pushItemWidth(-1);
        boolean changed = ImGui.inputTextWithHint("##search" + id, hint, searchString);
        ImGui.popItemWidth();

        if (!searchString.isEmpty()) {
            ImGui.sameLine();
            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.symbol.clear") + "##clear" + id)) {
                searchString.set("");
                changed = true;
            }
        }

        return changed;
    }

    /**
     * Muestra un modal de confirmación simple.
     * 
     * @param modalId   ID único del modal
     * @param title     Título del modal
     * @param message   Mensaje a mostrar
     * @param onConfirm Callback a ejecutar si se confirma
     */
    public static void confirmDialog(String modalId, String title, String message, Runnable onConfirm) {
        // Force center alignment for the modal
        imgui.ImVec2 center = ImGui.getMainViewport().getCenter();
        ImGui.setNextWindowPos(center.x, center.y, imgui.flag.ImGuiCond.Appearing, 0.5f, 0.5f);

        if (ImGui.beginPopupModal(modalId, ImGuiWindowFlags.AlwaysAutoResize)) {
            ImGui.text(message);
            ImGui.spacing();

            if (ImGui.button(I18n.INSTANCE.get("common.yes"), 120, 0)) {
                if (onConfirm != null) {
                    onConfirm.run();
                }
                ImGui.closeCurrentPopup();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.no"), 120, 0)) {
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
        }
    }

    /**
     * Dibuja un separador con texto.
     * 
     * @param text Texto a mostrar en el separador
     */
    public static void separatorWithText(String text) {
        ImGui.spacing();
        ImGui.separator();
        ImGui.text(text);
        ImGui.separator();
        ImGui.spacing();
    }
}
