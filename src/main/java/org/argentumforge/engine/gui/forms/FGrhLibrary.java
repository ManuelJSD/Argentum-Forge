package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.argentumforge.engine.utils.editor.GrhLibraryManager;
import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;
import org.argentumforge.engine.i18n.I18n;

import java.util.List;

/**
 * Ventana de gestión de la Biblioteca de GRHs.
 */
public class FGrhLibrary extends Form {

    private final ImString newCategoryName = new ImString(64);
    private final ImString editCategoryName = new ImString(64);
    private final ImString editRecordName = new ImString(64);
    private final ImInt editRecordGrh = new ImInt(0);
    private final ImInt editRecordLayer = new ImInt(1);
    private final ImBoolean editRecordAutoBlock = new ImBoolean(false);
    private final ImInt editRecordWidth = new ImInt(1);
    private final ImInt editRecordHeight = new ImInt(1);
    private final ImString searchQuery = new ImString(64);

    private GrhCategory selectedCategory = null;
    private GrhIndexRecord selectedRecord = null;
    private final ImBoolean pOpen = new ImBoolean(true);

    @Override
    public void render() {
        ImGui.setNextWindowSize(700, 500, imgui.flag.ImGuiCond.Always);
        if (ImGui.begin(I18n.INSTANCE.get("grhlib.manageTitle"), pOpen,
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            ImGui.columns(2, "LibraryColumns", true);
            ImGui.setColumnWidth(0, 250);

            // Buscador Global
            ImGui.text(I18n.INSTANCE.get("common.search") + ":");
            ImGui.sameLine();
            if (ImGui.inputText("##search", searchQuery)) {
                // El filtrado ocurre en tiempo real en los bucles de renderizado
            }
            ImGui.separator();

            // Columna Izquierda: Categorías
            ImGui.text(I18n.INSTANCE.get("grhlib.categories"));
            if (ImGui.beginChild("CategoriesList", 0, 300, true)) {
                List<GrhCategory> categories = GrhLibraryManager.getInstance().getCategories();
                String filter = searchQuery.get().toLowerCase();
                for (GrhCategory cat : categories) {
                    // Si hay búsqueda, comprobamos si la categoría o alguno de sus registros
                    // coincide
                    boolean matches = filter.isEmpty() || cat.getName().toLowerCase().contains(filter);

                    // Si la categoría no coincide, vemos si alguno de sus hijos coincide
                    if (!matches && !filter.isEmpty()) {
                        for (GrhIndexRecord rec : cat.getRecords()) {
                            if (rec.getName().toLowerCase().contains(filter) ||
                                    String.valueOf(rec.getGrhIndex()).contains(filter)) {
                                matches = true;
                                break;
                            }
                        }
                    }

                    if (matches) {
                        if (ImGui.selectable(cat.getName(), selectedCategory == cat)) {
                            selectedCategory = cat;
                            selectedRecord = null;
                            syncCatEditField();
                        }
                    }
                }
            }
            ImGui.endChild();

            ImGui.pushItemWidth(100);
            if (ImGui.inputText("##newCat", newCategoryName)) {
            }
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("grhlib.new"))) {
                if (!newCategoryName.get().isEmpty()) {
                    GrhLibraryManager.getInstance().getCategories().add(new GrhCategory(newCategoryName.get()));
                    newCategoryName.set("");
                    GrhLibraryManager.getInstance().save();
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.delete"))) {
                if (selectedCategory != null) {
                    GrhLibraryManager.getInstance().getCategories().remove(selectedCategory);
                    selectedCategory = null;
                    selectedRecord = null;
                    GrhLibraryManager.getInstance().save();
                }
            }

            if (selectedCategory != null) {
                ImGui.text(I18n.INSTANCE.get("grhlib.rename"));
                ImGui.pushItemWidth(100);
                if (ImGui.inputText("##editCat", editCategoryName)) {
                    selectedCategory.setName(editCategoryName.get());
                    GrhLibraryManager.getInstance().save();
                }
                ImGui.popItemWidth();
            }

            ImGui.nextColumn();

            // Columna Derecha: Registros de la Categoría
            if (selectedCategory != null) {
                ImGui.text(I18n.INSTANCE.get("grhlib.records") + " en: " + selectedCategory.getName());
                if (ImGui.beginChild("RecordsList", 0, 150, true)) {
                    String filter = searchQuery.get().toLowerCase();
                    for (GrhIndexRecord rec : selectedCategory.getRecords()) {
                        boolean matches = filter.isEmpty() ||
                                rec.getName().toLowerCase().contains(filter) ||
                                String.valueOf(rec.getGrhIndex()).contains(filter);

                        if (matches) {
                            if (ImGui.selectable(rec.getName() + " [" + rec.getGrhIndex() + "]",
                                    selectedRecord == rec)) {
                                selectedRecord = rec;
                                syncEditFields();
                            }
                        }
                    }
                }
                ImGui.endChild();

                if (ImGui.button(I18n.INSTANCE.get("grhlib.addRecord"))) {
                    GrhIndexRecord newRec = new GrhIndexRecord("Nuevo Registro", 0);
                    selectedCategory.addRecord(newRec);
                    selectedRecord = newRec;
                    syncEditFields();
                    GrhLibraryManager.getInstance().save();
                }
                ImGui.sameLine();
                if (ImGui.button(I18n.INSTANCE.get("grhlib.moveUp")) && selectedRecord != null) {
                    int idx = selectedCategory.getRecords().indexOf(selectedRecord);
                    if (idx > 0) {
                        selectedCategory.getRecords().remove(idx);
                        selectedCategory.getRecords().add(idx - 1, selectedRecord);
                        GrhLibraryManager.getInstance().save();
                    }
                }
                ImGui.sameLine();
                if (ImGui.button(I18n.INSTANCE.get("grhlib.moveDown")) && selectedRecord != null) {
                    int idx = selectedCategory.getRecords().indexOf(selectedRecord);
                    if (idx < selectedCategory.getRecords().size() - 1) {
                        selectedCategory.getRecords().remove(idx);
                        selectedCategory.getRecords().add(idx + 1, selectedRecord);
                        GrhLibraryManager.getInstance().save();
                    }
                }

                if (selectedRecord != null) {
                    ImGui.separator();
                    ImGui.text(I18n.INSTANCE.get("common.edit") + ": " + selectedRecord.getName());

                    if (ImGui.inputText(I18n.INSTANCE.get("common.name"), editRecordName))
                        selectedRecord.setName(editRecordName.get());
                    if (ImGui.inputInt(I18n.INSTANCE.get("grhlib.grhIndex"), editRecordGrh))
                        selectedRecord.setGrhIndex(editRecordGrh.get());
                    if (ImGui.inputInt(I18n.INSTANCE.get("grhlib.layer"), editRecordLayer))
                        selectedRecord.setLayer(editRecordLayer.get());
                    if (ImGui.checkbox(I18n.INSTANCE.get("grhlib.autoBlock"), editRecordAutoBlock))
                        selectedRecord.setAutoBlock(editRecordAutoBlock.get());
                    // Mosaico
                    ImGui.text(I18n.INSTANCE.get("grhlib.mosaic") + ":");
                    ImGui.sameLine();
                    if (ImGui.button("-##W", 25, 25)) {
                        editRecordWidth.set(Math.max(1, editRecordWidth.get() - 1));
                        selectedRecord.setWidth(editRecordWidth.get());
                    }
                    ImGui.sameLine();
                    ImGui.text(String.valueOf(editRecordWidth.get()));
                    ImGui.sameLine();
                    if (ImGui.button("+##W", 25, 25)) {
                        editRecordWidth.set(editRecordWidth.get() + 1);
                        selectedRecord.setWidth(editRecordWidth.get());
                    }
                    ImGui.sameLine();
                    ImGui.text("W");

                    ImGui.sameLine();
                    if (ImGui.button("-##H", 25, 25)) {
                        editRecordHeight.set(Math.max(1, editRecordHeight.get() - 1));
                        selectedRecord.setHeight(editRecordHeight.get());
                    }
                    ImGui.sameLine();
                    ImGui.text(String.valueOf(editRecordHeight.get()));
                    ImGui.sameLine();
                    if (ImGui.button("+##H", 25, 25)) {
                        editRecordHeight.set(editRecordHeight.get() + 1);
                        selectedRecord.setHeight(editRecordHeight.get());
                    }
                    ImGui.sameLine();
                    ImGui.text("H");

                    if (ImGui.button(I18n.INSTANCE.get("grhlib.save"))) {
                        GrhLibraryManager.getInstance().save();
                    }
                    ImGui.sameLine();
                    if (ImGui.button(I18n.INSTANCE.get("grhlib.deleteRecord"))) {
                        selectedCategory.getRecords().remove(selectedRecord);
                        selectedRecord = null;
                        GrhLibraryManager.getInstance().save();
                    }

                    // Preview del GRH
                    if (selectedRecord.getGrhIndex() > 0) {
                        ImGui.separator();
                        ImGui.text(I18n.INSTANCE.get("editor.npc.preview") + ":");
                        if (selectedRecord.getWidth() > 1 || selectedRecord.getHeight() > 1) {
                            org.argentumforge.engine.gui.PreviewUtils.drawGrhMosaic(selectedRecord.getGrhIndex(),
                                    selectedRecord.getWidth(), selectedRecord.getHeight(), 128, 128);
                        } else {
                            org.argentumforge.engine.gui.PreviewUtils.drawGrhFit(selectedRecord.getGrhIndex(), 128,
                                    128);
                        }
                    }
                }
            } else {
                ImGui.text(I18n.INSTANCE.get("grhlib.selectCategory"));
            }

            ImGui.columns(1);
        }
        ImGui.end();

        if (!pOpen.get()) {
            this.close();
        }
    }

    private void syncCatEditField() {
        if (selectedCategory == null)
            return;
        editCategoryName.set(selectedCategory.getName());
    }

    private void syncEditFields() {
        if (selectedRecord == null)
            return;
        editRecordName.set(selectedRecord.getName());
        editRecordGrh.set(selectedRecord.getGrhIndex());
        editRecordLayer.set(selectedRecord.getLayer());
        editRecordAutoBlock.set(selectedRecord.isAutoBlock());
        editRecordWidth.set(selectedRecord.getWidth());
        editRecordHeight.set(selectedRecord.getHeight());
    }
}
