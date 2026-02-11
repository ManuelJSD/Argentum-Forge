package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import imgui.flag.ImGuiTreeNodeFlags;
import org.argentumforge.engine.gui.PreviewUtils;
import org.argentumforge.engine.utils.editor.Surface;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.GrhData;
import org.argentumforge.engine.renderer.Texture;
import org.argentumforge.engine.gui.Theme;
import org.argentumforge.engine.gui.widgets.UIComponents;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.PrefabManager;
import org.argentumforge.engine.utils.editor.models.Prefab;
import org.argentumforge.engine.utils.editor.Selection;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.awt.Desktop;
import org.tinylog.Logger;

import org.argentumforge.engine.gui.DialogManager;
import java.util.List;
import java.util.ArrayList;

import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.FileDialog;

import org.argentumforge.engine.utils.editor.GrhLibraryManager;
import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;
import org.argentumforge.engine.utils.MapContext;

import java.util.stream.Collectors;

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
    private final ImString searchPrefab = new ImString(64);
    private final ImBoolean prefabListViewMode = new ImBoolean(false);

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
                        // Sincronizar paginación si no hay búsqueda activa para mostrar el tile
                        // capturado
                        if (searchGrh.get() == 0 && selectedGrhIndex > 0) {
                            currentPage = (selectedGrhIndex - 1) / ITEMS_PER_PAGE;
                        }
                    }

                    drawTileGrid();
                    ImGui.endTabItem();
                }

                if (ImGui.beginTabItem(I18n.INSTANCE.get("editor.surface.library"))) {
                    if (ImGui.button(I18n.INSTANCE.get("editor.surface.editLib"), ImGui.getContentRegionAvailX(), 25)) {
                        ImGUISystem.INSTANCE.show(new FGrhLibrary());
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

                if (ImGui.beginTabItem(I18n.INSTANCE.get("editor.surface.prefabs"))) {
                    drawPrefabsTab();
                    ImGui.endTabItem();
                }
                ImGui.endTabBar();
            }
        }
        ImGui.end();
    }

    private void drawPrefabsTab() {
        ImGui.pushStyleColor(ImGuiCol.Button, Theme.COLOR_ACCENT);
        // Habilitar si hay entidades seleccionadas (ya sea por área o individualmente)
        boolean canCreate = Selection.getInstance().isActive()
                && !Selection.getInstance().getSelectedEntities().isEmpty();
        if (!canCreate && ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.INSTANCE.get("prefab.create.tooltip"));
        }

        if (canCreate) {
            if (ImGui.button(I18n.INSTANCE.get("prefab.new"), ImGui.getContentRegionAvailX() / 2 - 5, 25)) {
                ImGUISystem.INSTANCE.show(new FCreatePrefab());
            }
        } else {
            ImGui.beginDisabled();
            ImGui.button(I18n.INSTANCE.get("prefab.new"), ImGui.getContentRegionAvailX() / 2 - 5, 25);
            ImGui.endDisabled();
        }
        ImGui.popStyleColor();

        ImGui.sameLine();

        // Import Button
        if (ImGui.button(I18n.INSTANCE.get("prefab.import"), (ImGui.getContentRegionAvailX() / 2) - 5, 25)) {
            try {
                // Usar TinyFileDialogs vía wrapper seguro para evitar bloqueos de Swing/LWJGL
                String result = FileDialog.showOpenDialog(
                        I18n.INSTANCE.get("prefab.import"),
                        new File("assets/prefabs/").getAbsolutePath() + File.separator,
                        "JSON Files",
                        "*.json");

                if (result != null) {
                    File source = new File(result);
                    File dest = new File("assets/prefabs/" + source.getName());

                    try {
                        Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        PrefabManager.getInstance().loadPrefabs(); // Recargar
                        DialogManager.getInstance().showInfo(I18n.INSTANCE.get("prefab.import"),
                                I18n.INSTANCE.get("prefab.import.success", source.getName()));
                    } catch (IOException e) {
                        Logger.error(e);
                        DialogManager.getInstance().showError(I18n.INSTANCE.get("prefab.import"),
                                I18n.INSTANCE.get("prefab.import.error"));
                    }
                }
            } catch (Exception e) {
                Logger.error(e);
            }
        }

        ImGui.sameLine();

        // Open Folder Button (Icono o Texto "Abrir")
        if (ImGui.button(I18n.INSTANCE.get("prefab.openFolder"), ImGui.getContentRegionAvailX(), 25)) {
            try {
                Desktop.getDesktop().open(new File("assets/prefabs/"));
            } catch (IOException e) {
                Logger.error(e);
            }
        }

        ImGui.separator();

        ImGui.text(I18n.INSTANCE.get("prefab.search") + ":");
        ImGui.sameLine();
        ImGui.inputText("##searchPrefab", searchPrefab);

        if (ImGui.checkbox(I18n.INSTANCE.get("prefab.listMode"), prefabListViewMode)) {
            // Toggle
        }

        PrefabManager pm = PrefabManager.getInstance();
        List<String> categories = pm.getCategories();

        String filter = searchPrefab.get().toLowerCase();

        if (ImGui.beginChild("PrefabsList")) {
            for (String cat : categories) {
                List<Prefab> prefabs = pm.getPrefabsByCategory(cat);

                // Filtrar lista
                if (!filter.isEmpty()) {
                    prefabs = prefabs.stream()
                            .filter(p -> p.getName().toLowerCase().contains(filter))
                            .collect(Collectors.toList());
                }

                if (prefabs.isEmpty())
                    continue;

                if (ImGui.collapsingHeader(cat, ImGuiTreeNodeFlags.DefaultOpen)) {
                    if (prefabListViewMode.get()) {
                        // VISTA DE LISTA
                        if (ImGui.beginTable("ListPrefab_" + cat, 1)) {
                            for (Prefab p : prefabs) {
                                ImGui.tableNextColumn();
                                ImGui.pushID(p.getName());

                                // Small preview?
                                float previewSize = 32;
                                float cursorX = ImGui.getCursorPosX();
                                float cursorY = ImGui.getCursorPosY();

                                PreviewUtils.drawPrefab(p, previewSize, previewSize);

                                ImGui.setCursorPos(cursorX + previewSize + 5, cursorY + 5);
                                if (ImGui.selectable(p.getName(), false, 0, 0, previewSize)) {
                                    pm.pastePrefab(p);
                                }

                                if (ImGui.isItemHovered()) {
                                    int bgCol = (Theme.COLOR_ACCENT & 0x00FFFFFF) | (0x40 << 24);
                                    ImGui.getWindowDrawList().addRectFilled(
                                            ImGui.getItemRectMinX(), ImGui.getItemRectMinY(),
                                            ImGui.getItemRectMaxX(), ImGui.getItemRectMaxY(),
                                            bgCol);
                                    ImGui.getWindowDrawList().addRect(
                                            ImGui.getItemRectMinX(), ImGui.getItemRectMinY(),
                                            ImGui.getItemRectMaxX(), ImGui.getItemRectMaxY(),
                                            Theme.COLOR_ACCENT);
                                }

                                // Context Menu
                                if (ImGui.beginPopupContextItem()) {
                                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.prefab.edit"))) {
                                        ImGUISystem.INSTANCE.show(new FEditPrefab(p));
                                    }
                                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.prefab.delete"))) {
                                        DialogManager.getInstance().showConfirm(
                                                I18n.INSTANCE.get("editor.prefab.delete.title"),
                                                String.format(I18n.INSTANCE.get("editor.prefab.delete.confirm"),
                                                        p.getName()),
                                                () -> {
                                                    if (pm.deletePrefab(p)) {
                                                        DialogManager.getInstance().showInfo(
                                                                I18n.INSTANCE.get("common.deleted"),
                                                                I18n.INSTANCE.get("editor.prefab.delete.success"));
                                                    } else {
                                                        DialogManager.getInstance().showError(
                                                                I18n.INSTANCE.get("common.error"),
                                                                I18n.INSTANCE.get("editor.prefab.delete.error"));
                                                    }
                                                }, () -> {
                                                });
                                    }
                                    ImGui.endPopup();
                                }
                                ImGui.popID();
                            }
                            ImGui.endTable();
                        }
                    } else {
                        // VISTA DE GRILLA
                        int columns = Math.max(1, (int) (ImGui.getContentRegionAvailX() / 100)); // ~100px per item

                        if (ImGui.beginTable("GridPrefab_" + cat, columns)) {
                            for (Prefab p : prefabs) {
                                ImGui.tableNextColumn();
                                ImGui.pushID(p.getName());

                                // Diseño de tarjeta (Card)
                                float previewHeight = 64; // Altura fija para preview

                                ImGui.beginGroup();

                                // 1. Preview
                                float startX = ImGui.getCursorPosX();
                                float startY = ImGui.getCursorPosY();

                                // Dibujar preview
                                PreviewUtils.drawPrefab(p, 64, previewHeight);

                                // 2. Nombre (Limitado visualmente)
                                String name = p.getName();
                                if (name.length() > 12)
                                    name = name.substring(0, 9) + "...";

                                // Centrar texto
                                float textW = ImGui.calcTextSize(name).x;
                                float centeredTextX = startX + (64 - textW) / 2;
                                if (centeredTextX < startX)
                                    centeredTextX = startX;

                                ImGui.setCursorPosY(startY + previewHeight + 2); // Forzar posición Y abajo
                                ImGui.setCursorPosX(centeredTextX);
                                ImGui.text(name);

                                ImGui.endGroup();

                                // Lógica de botón invisible que cubre todo el grupo
                                float groupW = ImGui.getItemRectSizeX();
                                float groupH = ImGui.getItemRectSizeY();

                                ImGui.setCursorPos(startX, startY);
                                if (ImGui.invisibleButton("##btn", groupW, groupH)) {
                                    pm.pastePrefab(p);
                                }

                                boolean hovered = ImGui.isItemHovered();

                                // Context Menu
                                if (ImGui.beginPopupContextItem()) {
                                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.prefab.edit"))) {
                                        ImGUISystem.INSTANCE.show(new FEditPrefab(p));
                                    }
                                    if (ImGui.menuItem(I18n.INSTANCE.get("editor.prefab.delete"))) {
                                        DialogManager.getInstance().showConfirm(
                                                I18n.INSTANCE.get("editor.prefab.delete.title"),
                                                String.format(I18n.INSTANCE.get("editor.prefab.delete.confirm"),
                                                        p.getName()),
                                                () -> {
                                                    if (pm.deletePrefab(p)) {
                                                        DialogManager.getInstance().showInfo(
                                                                I18n.INSTANCE.get("common.deleted"),
                                                                I18n.INSTANCE.get("editor.prefab.delete.success"));
                                                    } else {
                                                        DialogManager.getInstance().showError(
                                                                I18n.INSTANCE.get("common.error"),
                                                                I18n.INSTANCE.get("editor.prefab.delete.error"));
                                                    }
                                                }, () -> {
                                                });
                                    }
                                    ImGui.endPopup();
                                }

                                // Dibujar efectos si hover
                                if (hovered) {
                                    float minX = ImGui.getItemRectMinX();
                                    float minY = ImGui.getItemRectMinY();
                                    float maxX = ImGui.getItemRectMaxX();
                                    float maxY = ImGui.getItemRectMaxY();

                                    // Fondo semi-transparente
                                    int bgCol = (Theme.COLOR_ACCENT & 0x00FFFFFF) | (0x40 << 24); // ~25% alpha
                                    ImGui.getWindowDrawList().addRectFilled(minX, minY, maxX, maxY, bgCol);

                                    // Borde resaltado
                                    ImGui.getWindowDrawList().addRect(minX, minY, maxX, maxY, Theme.COLOR_ACCENT);

                                    ImGui.setTooltip(String.format(I18n.INSTANCE.get("prefab.tooltip"), p.getWidth(),
                                            p.getHeight()));
                                }

                                ImGui.popID();
                            }
                            ImGui.endTable();
                        }
                    }
                }
            }
        }
        ImGui.endChild();
    }

    private void drawLibraryTab() {
        GrhLibraryManager lib = GrhLibraryManager
                .getInstance();
        List<GrhCategory> categories = lib.getCategories();

        if (ImGui.beginChild("LibraryChild")) {
            float windowWidth = ImGui.getContentRegionAvailX();
            int columns = Math.max(1, (int) (windowWidth / (TILE_SIZE + 10)));

            for (GrhCategory cat : categories) {
                if (ImGui.collapsingHeader(cat.getName())) {
                    if (libraryVisualMode.get()) {
                        if (ImGui.beginTable("GridCat_" + cat.getName(), columns, ImGuiTableFlags.SizingFixedFit)) {
                            for (GrhIndexRecord rec : cat.getRecords()) {
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
                        for (GrhIndexRecord rec : cat.getRecords()) {
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

    private void selectRecord(GrhIndexRecord rec) {
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

    private void showRecordTooltip(GrhIndexRecord rec) {
        ImGui.beginTooltip();
        ImGui.text(rec.getName() + " (ID: " + rec.getGrhIndex() + ")");
        if (rec.getWidth() > 1 || rec.getHeight() > 1) {
            ImGui.text(I18n.INSTANCE.get("common.mosaic") + " " + rec.getWidth() + "x" + rec.getHeight());
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
            if (ImGui.button(I18n.INSTANCE.get("common.symbol.clear")))
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
                        ImGui.text(I18n.INSTANCE.get("common.grh") + " " + i);
                        ImGui.text(I18n.INSTANCE.get("common.size") + " " + data.getPixelWidth() + "x"
                                + data.getPixelHeight());
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