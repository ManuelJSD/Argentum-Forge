package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.argentumforge.engine.gui.PreviewUtils;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;
import java.util.List;
import java.util.ArrayList;

import org.argentumforge.engine.utils.MapContext;

/**
 * Editor de Superficies Unificado.
 * Combina los controles de capa y modo con una paleta visual de tiles (GRHs).
 */
public class FSurfaceEditor extends Form implements IMapEditor {

    private final ImInt searchGrh = new ImInt(0);
    private int selectedGrhIndex = -1;
    private final ImInt selectedLayer = new ImInt(0);
    private final List<Integer> capas = new ArrayList<>(List.of(1, 2, 3, 4));
    private final ImInt selectedBrushSize = new ImInt(1);
    private final float[] scatterDensityArr = { 0.3f };
    private final ImBoolean useMosaicChecked = new ImBoolean(true);
    private final ImBoolean libraryVisualMode = new ImBoolean(true);

    // Configuración de la rejilla
    private static final int TILE_SIZE = 48;
    private static final int ITEMS_PER_PAGE = 100;
    private int currentPage = 0;
    private final ImInt pageInput = new ImInt(1);

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

        selectedBrushSize.set(surface.getBrushSize());
        scatterDensityArr[0] = surface.getScatterDensity();
    }

    @Override
    public void setContext(MapContext context) {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(350, 600, ImGuiCond.FirstUseEver);

        if (ImGui.begin(I18n.INSTANCE.get("editor.surface"), ImGuiWindowFlags.None)) {
            drawToolSettings();
            ImGui.separator();
            drawLayerAndModeControls();
            ImGui.separator();

            if (ImGui.beginTabBar("SurfaceTabs")) {
                if (ImGui.beginTabItem(I18n.INSTANCE.get("editor.surface.palette"))) {
                    drawSearchAndPagination();
                    ImGui.separator();

                    if (surface.getSurfaceIndex() != selectedGrhIndex && surface.getMode() != 3) {
                        selectedGrhIndex = surface.getSurfaceIndex();
                    }

                    drawTileGrid();
                    ImGui.endTabItem();
                }

                if (ImGui.beginTabItem(I18n.INSTANCE.get("editor.surface.library"))) {
                    if (ImGui.button(I18n.INSTANCE.get("editor.surface.editLib"), ImGui.getContentRegionAvailX(), 25)) {
                        IM_GUI_SYSTEM.show(new FGrhLibrary());
                    }
                    ImGui.separator();

                    useMosaicChecked.set(surface.isUseMosaic());
                    if (ImGui.checkbox(I18n.INSTANCE.get("editor.surface.mosaic"), useMosaicChecked)) {
                        surface.setUseMosaic(useMosaicChecked.get());
                    }
                    ImGui.sameLine();
                    ImGui.checkbox(I18n.INSTANCE.get("common.visualMode"), libraryVisualMode);
                    ImGui.separator();

                    drawLibraryTab();
                    ImGui.endTabItem();
                }
                ImGui.endTabBar();
            }
        }
        ImGui.end();
    }

    private void drawLibraryTab() {
        org.argentumforge.engine.utils.editor.GrhLibraryManager lib = org.argentumforge.engine.utils.editor.GrhLibraryManager
                .getInstance();
        List<org.argentumforge.engine.utils.editor.models.GrhCategory> categories = lib.getCategories();

        if (ImGui.beginChild("LibraryChild")) {
            float windowWidth = ImGui.getContentRegionAvailX();
            int columns = Math.max(1, (int) (windowWidth / (TILE_SIZE + 10)));

            for (org.argentumforge.engine.utils.editor.models.GrhCategory cat : categories) {
                if (ImGui.collapsingHeader(cat.getName())) {
                    if (libraryVisualMode.get()) {
                        if (ImGui.beginTable("GridCat_" + cat.getName(), columns, ImGuiTableFlags.SizingFixedFit)) {
                            for (org.argentumforge.engine.utils.editor.models.GrhIndexRecord rec : cat.getRecords()) {
                                ImGui.tableNextColumn();
                                ImGui.pushID(rec.getGrhIndex());

                                int currentIdx = rec.getGrhIndex();
                                GrhData data = AssetRegistry.grhData[currentIdx];

                                if (data != null) {
                                    if (data.getNumFrames() > 1) {
                                        currentIdx = data.getFrame(1);
                                        data = AssetRegistry.grhData[currentIdx];
                                    }

                                    if (data != null && data.getFileNum() > 0) {
                                        Texture tex = org.argentumforge.engine.renderer.Surface.INSTANCE
                                                .getTexture(data.getFileNum());
                                        boolean isSelected = (selectedGrhIndex == rec.getGrhIndex());

                                        if (isSelected)
                                            ImGui.pushStyleColor(ImGuiCol.Border, Theme.COLOR_ACCENT);

                                        if (tex != null) {
                                            float u0 = data.getsX() / (float) tex.getTex_width();
                                            float v0 = (data.getsY() + data.getPixelHeight())
                                                    / (float) tex.getTex_height();
                                            float u1 = (data.getsX() + data.getPixelWidth())
                                                    / (float) tex.getTex_width();
                                            float v1 = data.getsY() / (float) tex.getTex_height();

                                            if (ImGui.imageButton("##libTile_" + rec.getGrhIndex(), (long) tex.getId(),
                                                    (float) TILE_SIZE, (float) TILE_SIZE, u0, v1, u1, v0)) {
                                                selectRecord(rec);
                                            }
                                        }
                                        if (isSelected)
                                            ImGui.popStyleColor();

                                        if (ImGui.isItemHovered()) {
                                            showRecordTooltip(rec);
                                        }
                                    }
                                }
                                ImGui.popID();
                            }
                            ImGui.endTable();
                        }
                    } else {
                        for (org.argentumforge.engine.utils.editor.models.GrhIndexRecord rec : cat.getRecords()) {
                            boolean isSelected = (selectedGrhIndex == rec.getGrhIndex());
                            if (ImGui.selectable(rec.getName() + " (GRH: " + rec.getGrhIndex() + ")", isSelected)) {
                                selectRecord(rec);
                            }
                            if (ImGui.isItemHovered()) {
                                showRecordTooltip(rec);
                            }
                        }
                    }
                }
            }
        }
        ImGui.endChild();
    }

    private void selectRecord(org.argentumforge.engine.utils.editor.models.GrhIndexRecord rec) {
        selectedGrhIndex = rec.getGrhIndex();
        surface.setSurfaceIndex(rec.getGrhIndex());
        surface.setLayer(rec.getLayer());
        surface.setAutoBlock(rec.isAutoBlock());
        surface.setMosaicWidth(rec.getWidth());
        surface.setMosaicHeight(rec.getHeight());
        surface.setMode(1);

        for (int i = 0; i < capas.size(); i++) {
            if (capas.get(i) == rec.getLayer()) {
                selectedLayer.set(i);
                break;
            }
        }
    }

    private void showRecordTooltip(org.argentumforge.engine.utils.editor.models.GrhIndexRecord rec) {
        ImGui.beginTooltip();
        ImGui.text(rec.getName() + " (ID: " + rec.getGrhIndex() + ")");
        if (rec.getWidth() > 1 || rec.getHeight() > 1) {
            ImGui.text("Mosaic: " + rec.getWidth() + "x" + rec.getHeight());
            PreviewUtils.drawGrhMosaic(rec.getGrhIndex(), rec.getWidth(), rec.getHeight(), 128, 128);
        } else {
            PreviewUtils.drawGrh(rec.getGrhIndex(), 2.0f);
        }
        ImGui.endTooltip();
    }

    private void drawLayerAndModeControls() {
        ImGui.text(I18n.INSTANCE.get("editor.surface.layerActive") + ":");
        ImGui.sameLine();
        String[] labels = capas.stream().map(n -> I18n.INSTANCE.get("menu.view.layer") + " " + n)
                .toArray(String[]::new);
        ImGui.pushItemWidth(120);
        if (ImGui.combo("##capasCombo", selectedLayer, labels, labels.length)) {
            surface.setLayer(capas.get(selectedLayer.get()));
        }
        ImGui.popItemWidth();

        ImGui.spacing();
        int currentMode = surface.getMode();

        if (UIComponents.toggleButton(I18n.INSTANCE.get("common.selection"), currentMode == 0)) {
            surface.setMode(0);
            selectedGrhIndex = -1;
        }

        ImGui.sameLine();
        if (UIComponents.toggleButton(I18n.INSTANCE.get("common.insert"), currentMode == 1)) {
            if (selectedGrhIndex > 0) {
                surface.setMode(currentMode == 1 ? 0 : 1);
                if (surface.getMode() == 1)
                    surface.setSurfaceIndex(selectedGrhIndex);
            }
        }

        ImGui.sameLine();
        if (currentMode == 2)
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        if (ImGui.button(I18n.INSTANCE.get("common.delete"))) {
            surface.setMode(currentMode == 2 ? 0 : 2);
        }
        if (currentMode == 2)
            ImGui.popStyleColor();

        ImGui.sameLine();
        if (UIComponents.toggleButton(I18n.INSTANCE.get("common.pick"), currentMode == 3)) {
            surface.setMode(currentMode == 3 ? 0 : 3);
        }
    }

    private void drawToolSettings() {
        ImGui.text(I18n.INSTANCE.get("editor.surface.tool") + ":");
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.brush"),
                surface.getToolMode() == Surface.ToolMode.BRUSH)) {
            surface.setToolMode(Surface.ToolMode.BRUSH);
        }
        ImGui.sameLine();
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.bucket"),
                surface.getToolMode() == Surface.ToolMode.BUCKET)) {
            surface.setToolMode(Surface.ToolMode.BUCKET);
        }

        ImGui.spacing();
        if (surface.getToolMode() == Surface.ToolMode.BRUSH) {
            ImGui.text(I18n.INSTANCE.get("editor.surface.shape") + ":");
            if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.square"),
                    surface.getBrushShape() == Surface.BrushShape.SQUARE)) {
                surface.setBrushShape(Surface.BrushShape.SQUARE);
            }
            ImGui.sameLine();
            if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.circle"),
                    surface.getBrushShape() == Surface.BrushShape.CIRCLE)) {
                surface.setBrushShape(Surface.BrushShape.CIRCLE);
            }
            ImGui.sameLine();
            if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.scatter"),
                    surface.getBrushShape() == Surface.BrushShape.SCATTER)) {
                surface.setBrushShape(Surface.BrushShape.SCATTER);
            }

            if (ImGui.sliderInt(I18n.INSTANCE.get("editor.surface.size"), selectedBrushSize.getData(), 1, 15)) {
                surface.setBrushSize(selectedBrushSize.get());
            }

            if (surface.getBrushShape() == Surface.BrushShape.SCATTER) {
                if (ImGui.sliderFloat(I18n.INSTANCE.get("editor.surface.density"), scatterDensityArr, 0.05f, 1.0f)) {
                    surface.setScatterDensity(scatterDensityArr[0]);
                }
            }
        }
    }

    private void drawSearchAndPagination() {
        if (AssetRegistry.grhData == null)
            return;

        ImGui.text(I18n.INSTANCE.get("editor.surface.search") + ":");
        ImGui.sameLine();
        ImGui.pushItemWidth(80);
        if (ImGui.inputInt("##search", searchGrh)) {
            if (searchGrh.get() < 0)
                searchGrh.set(0);
            if (searchGrh.get() > AssetRegistry.maxGrhCount)
                searchGrh.set(AssetRegistry.maxGrhCount);
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
        int totalGrhs = AssetRegistry.maxGrhCount;
        int maxPages = Math.max(0, (totalGrhs - 1) / ITEMS_PER_PAGE);

        ImGui.text(I18n.INSTANCE.get("editor.surface.page") + ":");
        ImGui.sameLine();
        ImGui.pushItemWidth(40);
        pageInput.set(currentPage + 1);
        if (ImGui.inputInt("##pageInput", pageInput, 0, 0)) {
            int newPage = pageInput.get() - 1;
            if (newPage < 0)
                newPage = 0;
            if (newPage > maxPages)
                newPage = maxPages;
            currentPage = newPage;
        }
        ImGui.popItemWidth();
        ImGui.sameLine();
        ImGui.text("/ " + (maxPages + 1));

        ImGui.sameLine();
        if (ImGui.button("<") && currentPage > 0)
            currentPage--;
        ImGui.sameLine();
        if (ImGui.button(">") && currentPage < maxPages)
            currentPage++;
    }

    private void drawTileGrid() {
        if (AssetRegistry.grhData == null)
            return;

        if (ImGui.beginChild("TileGridChild", 0, 0, true)) {
            float windowWidth = ImGui.getContentRegionAvailX();
            int columns = Math.max(1, (int) (windowWidth / (TILE_SIZE + 10)));

            int totalGrhs = AssetRegistry.maxGrhCount;
            int start = searchGrh.get() > 0 ? searchGrh.get() : (currentPage * ITEMS_PER_PAGE);
            if (start <= 0)
                start = 1;
            int end = searchGrh.get() > 0 ? start + 1 : Math.min(start + ITEMS_PER_PAGE, totalGrhs);

            if (ImGui.beginTable("GridTable", columns, ImGuiTableFlags.SizingFixedFit)) {
                for (int i = start; i < end; i++) {
                    if (i <= 0 || i >= totalGrhs)
                        continue;

                    int currentIdx = i;
                    GrhData data = AssetRegistry.grhData[i];
                    if (data == null)
                        continue;

                    if (data.getNumFrames() > 1) {
                        currentIdx = data.getFrame(1);
                        data = AssetRegistry.grhData[currentIdx];
                    }

                    if (data == null || data.getFileNum() == 0)
                        continue;

                    ImGui.tableNextColumn();
                    ImGui.pushID(i);

                    Texture tex = org.argentumforge.engine.renderer.Surface.INSTANCE.getTexture(data.getFileNum());
                    boolean isSelected = (selectedGrhIndex == i);

                    if (isSelected)
                        ImGui.pushStyleColor(ImGuiCol.Border, Theme.COLOR_ACCENT);

                    // Usar un color de fondo de botón ligeramente más claro para que los tiles
                    // negros/transparentes
                    // se vean como "slots" y no como vacíos.
                    ImGui.pushStyleColor(ImGuiCol.Button, Theme.rgba(50, 50, 55, 255));

                    if (tex != null) {
                        float u0 = data.getsX() / (float) tex.getTex_width();
                        float v0 = (data.getsY() + data.getPixelHeight()) / (float) tex.getTex_height();
                        float u1 = (data.getsX() + data.getPixelWidth()) / (float) tex.getTex_width();
                        float v1 = data.getsY() / (float) tex.getTex_height();

                        if (ImGui.imageButton("##surfaceTile_" + i, (long) tex.getId(), (float) TILE_SIZE,
                                (float) TILE_SIZE, u0, v1, u1, v0)) {
                            selectedGrhIndex = i;
                            surface.setSurfaceIndex(i);
                            surface.setMode(1);
                        }
                    } else {
                        if (ImGui.button("?", (float) TILE_SIZE, (float) TILE_SIZE)) {
                            selectedGrhIndex = i;
                            surface.setSurfaceIndex(i);
                        }
                    }

                    ImGui.popStyleColor(); // Pop Button Color

                    if (isSelected)
                        ImGui.popStyleColor(); // Pop Border Color

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
            ImGui.endChild();
        }
    }
}