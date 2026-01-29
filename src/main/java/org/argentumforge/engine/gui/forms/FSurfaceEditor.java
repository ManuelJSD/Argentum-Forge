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

    private MapContext context; // Contexto activo inyectado

    private final ImInt searchGrh = new ImInt(0);
    private int selectedGrhIndex = -1;
    private final ImInt selectedLayer = new ImInt(0);
    private final List<Integer> capas = new ArrayList<>(List.of(1, 2, 3, 4));
    private final ImInt selectedBrushSize = new ImInt(1);
    private final float[] scatterDensityArr = { 0.3f };
    private final ImBoolean useMosaicChecked = new ImBoolean(true);

    // Configuración de la rejilla
    private static final int TILE_SIZE = 48;
    private static final int ITEMS_PER_PAGE = 100;
    private int currentPage = 0;

    private Surface surface;

    public FSurfaceEditor() {
        this.surface = Surface.getInstance();
        this.selectedGrhIndex = surface.getSurfaceIndex();

        // Inicializar contexto por defecto (temporal hasta que FMain inyecte)
        // Esto previene NPEs si FMain no llama a setContext inmediatamente
        this.context = org.argentumforge.engine.utils.GameData.getActiveContext();

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
        this.context = context;
        // Opcional: Si Surface dependiera del contexto para su estado interno,
        // actualizarlo aquí.
        // Surface es un singleton de *herramienta* (configuración de pincel), por lo
        // que no cambia por mapa,
        // pero las acciones que ejecuta sí dependen del contexto.
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(350, 600, ImGuiCond.FirstUseEver);

        // Ventana principal del editor de superficies
        if (ImGui.begin(I18n.INSTANCE.get("editor.surface"), ImGuiWindowFlags.None)) {

            drawToolSettings();
            ImGui.separator();
            drawLayerAndModeControls();
            ImGui.separator();

            if (ImGui.beginTabBar("SurfaceTabs")) {
                if (ImGui.beginTabItem(I18n.INSTANCE.get("editor.surface.palette"))) {
                    drawSearchAndPagination();
                    ImGui.separator();

                    // Sincronizar selección si cambió externamente (por pick)
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
            for (org.argentumforge.engine.utils.editor.models.GrhCategory cat : categories) {
                if (ImGui.collapsingHeader(cat.getName())) {
                    for (org.argentumforge.engine.utils.editor.models.GrhIndexRecord rec : cat.getRecords()) {
                        boolean isSelected = (selectedGrhIndex == rec.getGrhIndex());

                        if (ImGui.selectable(rec.getName() + " (GRH: " + rec.getGrhIndex() + ")", isSelected)) {
                            selectedGrhIndex = rec.getGrhIndex();
                            surface.setSurfaceIndex(rec.getGrhIndex());
                            surface.setLayer(rec.getLayer());
                            surface.setAutoBlock(rec.isAutoBlock());
                            surface.setMosaicWidth(rec.getWidth());
                            surface.setMosaicHeight(rec.getHeight());
                            surface.setMode(1);

                            // Actualizar combo de capa visualmente
                            for (int i = 0; i < capas.size(); i++) {
                                if (capas.get(i) == rec.getLayer()) {
                                    selectedLayer.set(i);
                                    break;
                                }
                            }
                        }

                        if (ImGui.isItemHovered()) {
                            ImGui.beginTooltip();
                            if (rec.getWidth() > 1 || rec.getHeight() > 1) {
                                ImGui.text("Mosaic: " + rec.getWidth() + "x" + rec.getHeight());
                                PreviewUtils.drawGrhMosaic(rec.getGrhIndex(), rec.getWidth(), rec.getHeight(), 128,
                                        128);
                            } else {
                                ImGui.text("GRH: " + rec.getGrhIndex());
                                PreviewUtils.drawGrh(rec.getGrhIndex(), 1.0f);
                            }
                            ImGui.endTooltip();
                        }
                    }
                }
            }
        }
        ImGui.endChild();
    }

    private void drawLayerAndModeControls() {
        // Selector de Capas
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

        // Modos de Edición
        int currentMode = surface.getMode();

        // Botón Seleccionar (Vacio)
        if (UIComponents.toggleButton(I18n.INSTANCE.get("common.selection"), currentMode == 0)) {
            surface.setMode(currentMode == 0 ? 0 : 0); // Always set to 0 when clicked
            selectedGrhIndex = -1;
        }

        ImGui.sameLine();

        // Botón Insertar
        if (UIComponents.toggleButton(I18n.INSTANCE.get("common.insert"), currentMode == 1)) {
            if (selectedGrhIndex > 0) {
                surface.setMode(currentMode == 1 ? 0 : 1);
                if (surface.getMode() == 1) {
                    surface.setSurfaceIndex(selectedGrhIndex);
                }
            }
        }

        ImGui.sameLine();

        // Botón Borrar (Destructivo - toggle)
        if (currentMode == 2) {
            ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_DANGER);
        }
        if (ImGui.button(I18n.INSTANCE.get("common.delete"))) {
            surface.setMode(currentMode == 2 ? 0 : 2);
        }
        if (currentMode == 2) {
            ImGui.popStyleColor();
        }

        ImGui.sameLine();

        // Botón Capturar (Pick)
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
        if (AssetRegistry.grhData == null) {
            ImGui.textColored(1f, 0f, 0f, 1f, "ERROR: grhData no cargado (NULL)");
            ImGui.text("Init Path: " + org.argentumforge.engine.game.Options.INSTANCE.getInitPath());
            ImGui.text("Dats Path: " + org.argentumforge.engine.game.Options.INSTANCE.getDatsPath());
            return;
        }

        // Buscador
        ImGui.text(I18n.INSTANCE.get("editor.surface.search") + ":");
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

        ImGui.text(I18n.INSTANCE.get("editor.surface.page") + ": " + (currentPage + 1) + "/" + (maxPages + 1));
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
                        ImGui.pushStyleColor(ImGuiCol.Border, Theme.COLOR_ACCENT);

                    if (tex != null) {
                        float u0 = frameData.getsX() / (float) tex.getTex_width();
                        float v0 = (frameData.getsY() + frameData.getPixelHeight()) / (float) tex.getTex_height();
                        float u1 = (frameData.getsX() + frameData.getPixelWidth()) / (float) tex.getTex_width();
                        float v1 = frameData.getsY() / (float) tex.getTex_height();

                        if (ImGui.imageButton("##surfaceTile_" + i, (long) tex.getId(), (float) TILE_SIZE,
                                (float) TILE_SIZE,
                                u0, v1, u1, v0)) {
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