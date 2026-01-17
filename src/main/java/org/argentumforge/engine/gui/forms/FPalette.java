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
import org.argentumforge.engine.i18n.I18n;

/**
 * Formulario que muestra una paleta visual de todos los GRHs disponibles.
 * Permite buscar y seleccionar gráficos de forma intuitiva mediante una
 * rejilla.
 */
public class FPalette extends Form {

    private final ImInt searchGrh = new ImInt(0);
    private int selectedGrh = -1;
    private static final int TILE_SIZE = 48;
    private static final int ITEMS_PER_PAGE = 100;
    private int currentPage = 0;

    public FPalette() {
        // Inicializar con el seleccionado en Surface si existe
        this.selectedGrh = Surface.getInstance().getSurfaceIndex();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(320, 500, ImGuiCond.FirstUseEver);

        // El error de assertion ocurre si no se llama a End() cuando Begin() retorna
        // false (ventana colapsada)
        boolean isOpen = ImGui.begin(I18n.INSTANCE.get("editor.palette.title"), ImGuiWindowFlags.None);

        if (isOpen) {
            drawSearch();
            ImGui.separator();

            drawPagination();
            ImGui.separator();

            drawGrid();
        }

        ImGui.end(); // Siempre cerrar el stack de la ventana
    }

    private void drawSearch() {
        if (ImGui.button(I18n.INSTANCE.get("editor.palette.selectEmpty"))) {
            selectedGrh = -1;
            Surface.getInstance().setSurfaceIndex(1); // Tile vacio por defecto
            Surface.getInstance().setMode(0); // Modo seleccion/neutro
        }
        ImGui.sameLine();
        ImGui.text(I18n.INSTANCE.get("editor.palette.searchId"));
        ImGui.sameLine();
        ImGui.pushItemWidth(100);
        if (ImGui.inputInt("##search", searchGrh)) {
            if (searchGrh.get() < 0)
                searchGrh.set(0);
            if (AssetRegistry.grhData != null && searchGrh.get() >= AssetRegistry.grhData.length) {
                searchGrh.set(AssetRegistry.grhData.length - 1);
            }
            if (searchGrh.get() > 0)
                currentPage = 0;
        }
        ImGui.popItemWidth();

        if (searchGrh.get() > 0) {
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("editor.palette.clear"))) {
                searchGrh.set(0);
            }
        }
    }

    private void drawPagination() {
        if (AssetRegistry.grhData == null)
            return;

        int totalGrhs = AssetRegistry.grhData.length;
        int maxPages = (totalGrhs / ITEMS_PER_PAGE);

        ImGui.text(I18n.INSTANCE.get("editor.palette.page") + ": " + (currentPage + 1) + " / " + (maxPages + 1));
        ImGui.sameLine();
        if (ImGui.button(I18n.INSTANCE.get("editor.palette.previousPage")) && currentPage > 0) {
            currentPage--;
        }
        ImGui.sameLine();
        if (ImGui.button(I18n.INSTANCE.get("editor.palette.nextPage")) && currentPage < maxPages) {
            currentPage++;
        }
    }

    private void drawGrid() {
        if (AssetRegistry.grhData == null) {
            ImGui.textColored(1f, 0f, 0f, 1f, I18n.INSTANCE.get("editor.palette.loadError"));
            return;
        }

        // Child window para que el scroll solo afecte a la rejilla
        if (ImGui.beginChild("TileGridChild", 0, 0, true)) {
            float windowWidth = ImGui.getContentRegionAvailX();
            int columns = Math.max(1, (int) (windowWidth / (TILE_SIZE + 10)));

            int totalGrhs = AssetRegistry.grhData.length;
            int start = searchGrh.get() > 0 ? searchGrh.get() : (currentPage * ITEMS_PER_PAGE);
            if (start <= 0)
                start = 1;
            int end = searchGrh.get() > 0 ? start + 1 : Math.min(start + ITEMS_PER_PAGE, totalGrhs);

            // Usamos tablas para una alineacion perfecta
            if (ImGui.beginTable("GridTable", columns, ImGuiTableFlags.SizingFixedFit)) {
                for (int i = start; i < end; i++) {
                    if (i <= 0 || i >= totalGrhs)
                        continue;
                    org.argentumforge.engine.utils.inits.GrhData data = AssetRegistry.grhData[i];
                    if (data == null || data.getFileNum() == 0)
                        continue;

                    ImGui.tableNextColumn();
                    ImGui.pushID(i);

                    int currentGrh = data.getNumFrames() > 1 ? data.getFrame(0) : i;
                    org.argentumforge.engine.utils.inits.GrhData frameData = AssetRegistry.grhData[currentGrh];
                    org.argentumforge.engine.renderer.Texture tex = org.argentumforge.engine.renderer.Surface.INSTANCE
                            .getTexture(frameData.getFileNum());

                    boolean isSelected = (selectedGrh == i);
                    if (isSelected) {
                        ImGui.pushStyleColor(ImGuiCol.Border, 1f, 1f, 0f, 1f); // Amarillo
                    }

                    if (tex != null) {
                        float u0 = frameData.getsX() / (float) tex.getTex_width();
                        float v0 = (frameData.getsY() + frameData.getPixelHeight()) / (float) tex.getTex_height();
                        float u1 = (frameData.getsX() + frameData.getPixelWidth()) / (float) tex.getTex_width();
                        float v1 = frameData.getsY() / (float) tex.getTex_height();

                        if (ImGui.imageButton(tex.getId(), (float) TILE_SIZE, (float) TILE_SIZE, u0, v1, u1, v0)) {
                            selectedGrh = i;
                            Surface.getInstance().setSurfaceIndex(i);
                        }
                    } else {
                        if (ImGui.button("?", (float) TILE_SIZE, (float) TILE_SIZE)) {
                            selectedGrh = i;
                            Surface.getInstance().setSurfaceIndex(i);
                        }
                    }

                    if (isSelected) {
                        ImGui.popStyleColor();
                    }

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
