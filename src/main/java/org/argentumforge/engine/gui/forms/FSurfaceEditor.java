package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.gui.PreviewUtils;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.renderer.Texture;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de Superficies Unificado.
 * Combina los controles de capa y modo con una paleta visual de tiles (GRHs).
 */
public class FSurfaceEditor extends Form {

    private final ImInt searchGrh = new ImInt(0);
    private int selectedGrhIndex = -1;
    private final ImInt selectedLayer = new ImInt(0);
    private final List<Integer> capas = new ArrayList<>(List.of(1, 2, 3, 4));

    // Configuración de la rejilla
    private static final int TILE_SIZE = 48;
    private static final int ITEMS_PER_PAGE = 100;
    private int currentPage = 0;

    private Surface surface;

    public FSurfaceEditor() {
        this.surface = Surface.getInstance();
        this.selectedGrhIndex = surface.getSurfaceIndex();

        // Sincronizar capa inicial
        for (int i = 0; i < capas.size(); i++) {
            if (capas.get(i) == surface.getLayer()) {
                selectedLayer.set(i);
                break;
            }
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(350, 600, ImGuiCond.FirstUseEver);

        // Ventana principal del editor de superficies
        if (ImGui.begin("Editor de Superficies", ImGuiWindowFlags.None)) {

            drawLayerAndModeControls();
            ImGui.separator();

            drawSearchAndPagination();
            ImGui.separator();

            // Sincronizar selección si cambió externamente (por pick)
            if (surface.getSurfaceIndex() != selectedGrhIndex && surface.getMode() != 3) {
                selectedGrhIndex = surface.getSurfaceIndex();
            }

            drawTileGrid();
        }
        ImGui.end();
    }

    private void drawLayerAndModeControls() {
        // Selector de Capas
        ImGui.text("Capa Activa:");
        ImGui.sameLine();
        String[] labels = capas.stream().map(n -> "Capa " + n).toArray(String[]::new);
        ImGui.pushItemWidth(120);
        if (ImGui.combo("##capasCombo", selectedLayer, labels, labels.length)) {
            surface.setLayer(capas.get(selectedLayer.get()));
        }
        ImGui.popItemWidth();

        ImGui.spacing();

        // Modos de Edición
        int activeColor = 0xFF00FF00; // Verde activo
        int currentMode = surface.getMode();

        // Botón Seleccionar (Vacio)
        if (currentMode == 0)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("Seleccion")) {
            surface.setMode(0);
            selectedGrhIndex = -1;
        }
        if (currentMode == 0)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Insertar
        if (currentMode == 1)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("Insertar")) {
            if (selectedGrhIndex > 0) {
                surface.setMode(1);
                surface.setSurfaceIndex(selectedGrhIndex);
            }
        }
        if (currentMode == 1)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Borrar
        if (currentMode == 2)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("Borrar")) {
            surface.setMode(2);
        }
        if (currentMode == 2)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // Botón Capturar (Pick)
        if (currentMode == 3)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("Capturar")) {
            surface.setMode(3);
        }
        if (currentMode == 3)
            ImGui.popStyleColor();
    }

    private void drawSearchAndPagination() {
        if (AssetRegistry.grhData == null)
            return;

        // Buscador
        ImGui.text("Buscar ID:");
        ImGui.sameLine();
        ImGui.pushItemWidth(80);
        if (ImGui.inputInt("##search", searchGrh)) {
            if (searchGrh.get() < 0)
                searchGrh.set(0);
            if (searchGrh.get() >= AssetRegistry.grhData.length)
                searchGrh.set(AssetRegistry.grhData.length - 1);
            if (searchGrh.get() > 0)
                currentPage = 0;
        }
        ImGui.popItemWidth();

        if (searchGrh.get() > 0) {
            ImGui.sameLine();
            if (ImGui.button("X"))
                searchGrh.set(0);
        }

        ImGui.spacing();

        // Paginación
        int totalGrhs = AssetRegistry.grhData.length;
        int maxPages = (totalGrhs / ITEMS_PER_PAGE);

        ImGui.text("Pag: " + (currentPage + 1) + "/" + (maxPages + 1));
        ImGui.sameLine();
        if (ImGui.button("<") && currentPage > 0)
            currentPage--;
        ImGui.sameLine();
        if (ImGui.button(">") && currentPage < maxPages)
            currentPage++;
    }

    private void drawTileGrid() {
        if (AssetRegistry.grhData == null) {
            ImGui.textColored(1f, 0f, 0f, 1f, "grhData no cargado");
            return;
        }

        if (ImGui.beginChild("TileGridChild", 0, 0, true)) {
            float windowWidth = ImGui.getContentRegionAvailX();
            int columns = Math.max(1, (int) (windowWidth / (TILE_SIZE + 10)));

            int totalGrhs = AssetRegistry.grhData.length;
            int start = searchGrh.get() > 0 ? searchGrh.get() : (currentPage * ITEMS_PER_PAGE);
            if (start <= 0)
                start = 1;
            int end = searchGrh.get() > 0 ? start + 1 : Math.min(start + ITEMS_PER_PAGE, totalGrhs);

            if (ImGui.beginTable("GridTable", columns, ImGuiTableFlags.SizingFixedFit)) {
                for (int i = start; i < end; i++) {
                    if (i <= 0 || i >= totalGrhs)
                        continue;
                    GrhData data = AssetRegistry.grhData[i];
                    if (data == null || data.getFileNum() == 0)
                        continue;

                    ImGui.tableNextColumn();
                    ImGui.pushID(i);

                    int currentGrh = data.getNumFrames() > 1 ? data.getFrame(0) : i;
                    GrhData frameData = AssetRegistry.grhData[currentGrh];
                    Texture tex = org.argentumforge.engine.renderer.Surface.INSTANCE.getTexture(frameData.getFileNum());

                    boolean isSelected = (selectedGrhIndex == i);
                    if (isSelected)
                        ImGui.pushStyleColor(ImGuiCol.Border, 1f, 1f, 0f, 1f);

                    if (tex != null) {
                        float u0 = frameData.getsX() / (float) tex.getTex_width();
                        float v0 = (frameData.getsY() + frameData.getPixelHeight()) / (float) tex.getTex_height();
                        float u1 = (frameData.getsX() + frameData.getPixelWidth()) / (float) tex.getTex_width();
                        float v1 = frameData.getsY() / (float) tex.getTex_height();

                        if (ImGui.imageButton(tex.getId(), (float) TILE_SIZE, (float) TILE_SIZE, u0, v1, u1, v0)) {
                            selectedGrhIndex = i;
                            surface.setSurfaceIndex(i);
                            surface.setMode(1); // Auto-activar insertar al seleccionar
                        }
                    } else {
                        if (ImGui.button("?", (float) TILE_SIZE, (float) TILE_SIZE)) {
                            selectedGrhIndex = i;
                            surface.setSurfaceIndex(i);
                        }
                    }

                    if (isSelected)
                        ImGui.popStyleColor();

                    if (ImGui.isItemHovered()) {
                        ImGui.beginTooltip();
                        ImGui.text("GRH: " + i);
                        ImGui.text("Size: " + data.getPixelWidth() + "x" + data.getPixelHeight());
                        PreviewUtils.drawGrh(i, 2.0f);
                        ImGui.endTooltip();
                    }

                    ImGui.popID();
                }
                ImGui.endTable();
            }
        }
        ImGui.endChild();
    }
}