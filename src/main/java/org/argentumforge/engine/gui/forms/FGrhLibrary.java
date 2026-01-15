package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.argentumforge.engine.utils.editor.GrhLibraryManager;
import org.argentumforge.engine.utils.editor.models.GrhCategory;
import org.argentumforge.engine.utils.editor.models.GrhIndexRecord;

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

    private GrhCategory selectedCategory = null;
    private GrhIndexRecord selectedRecord = null;
    private final ImBoolean pOpen = new ImBoolean(true);

    @Override
    public void render() {
        ImGui.setNextWindowSize(700, 500, imgui.flag.ImGuiCond.Always);
        if (ImGui.begin("Gestion de Biblioteca GRH", pOpen, ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            ImGui.columns(2, "LibraryColumns", true);
            ImGui.setColumnWidth(0, 250);

            // Columna Izquierda: Categorías
            ImGui.text("Categorías");
            if (ImGui.beginChild("CategoriesList", 0, 300, true)) {
                List<GrhCategory> categories = GrhLibraryManager.getInstance().getCategories();
                for (GrhCategory cat : categories) {
                    if (ImGui.selectable(cat.getName(), selectedCategory == cat)) {
                        selectedCategory = cat;
                        selectedRecord = null;
                        syncCatEditField();
                    }
                }
            }
            ImGui.endChild();

            ImGui.pushItemWidth(100);
            if (ImGui.inputText("##newCat", newCategoryName)) {
            }
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button("Nueva")) {
                if (!newCategoryName.get().isEmpty()) {
                    GrhLibraryManager.getInstance().getCategories().add(new GrhCategory(newCategoryName.get()));
                    newCategoryName.set("");
                    GrhLibraryManager.getInstance().save();
                }
            }
            ImGui.sameLine();
            if (ImGui.button("Eliminar")) {
                if (selectedCategory != null) {
                    GrhLibraryManager.getInstance().getCategories().remove(selectedCategory);
                    selectedCategory = null;
                    selectedRecord = null;
                    GrhLibraryManager.getInstance().save();
                }
            }

            if (selectedCategory != null) {
                ImGui.text("Renombrar:");
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
                ImGui.text("Registros en: " + selectedCategory.getName());
                if (ImGui.beginChild("RecordsList", 0, 150, true)) {
                    for (GrhIndexRecord rec : selectedCategory.getRecords()) {
                        if (ImGui.selectable(rec.getName() + " [" + rec.getGrhIndex() + "]", selectedRecord == rec)) {
                            selectedRecord = rec;
                            syncEditFields();
                        }
                    }
                }
                ImGui.endChild();

                if (ImGui.button("Añadir Registro")) {
                    GrhIndexRecord newRec = new GrhIndexRecord("Nuevo Registro", 0);
                    selectedCategory.addRecord(newRec);
                    selectedRecord = newRec;
                    syncEditFields();
                    GrhLibraryManager.getInstance().save();
                }
                ImGui.sameLine();
                if (ImGui.button("Subir") && selectedRecord != null) {
                    int idx = selectedCategory.getRecords().indexOf(selectedRecord);
                    if (idx > 0) {
                        selectedCategory.getRecords().remove(idx);
                        selectedCategory.getRecords().add(idx - 1, selectedRecord);
                        GrhLibraryManager.getInstance().save();
                    }
                }
                ImGui.sameLine();
                if (ImGui.button("Bajar") && selectedRecord != null) {
                    int idx = selectedCategory.getRecords().indexOf(selectedRecord);
                    if (idx < selectedCategory.getRecords().size() - 1) {
                        selectedCategory.getRecords().remove(idx);
                        selectedCategory.getRecords().add(idx + 1, selectedRecord);
                        GrhLibraryManager.getInstance().save();
                    }
                }

                if (selectedRecord != null) {
                    ImGui.separator();
                    ImGui.text("Editar: " + selectedRecord.getName());

                    if (ImGui.inputText("Nombre", editRecordName))
                        selectedRecord.setName(editRecordName.get());
                    if (ImGui.inputInt("GRH Index", editRecordGrh))
                        selectedRecord.setGrhIndex(editRecordGrh.get());
                    if (ImGui.inputInt("Capa (1-4)", editRecordLayer))
                        selectedRecord.setLayer(editRecordLayer.get());
                    if (ImGui.checkbox("Auto-Bloqueo", editRecordAutoBlock))
                        selectedRecord.setAutoBlock(editRecordAutoBlock.get());
                    // Mosaico
                    ImGui.text("Mosaico:");
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

                    if (ImGui.button("Guardar Cambios")) {
                        GrhLibraryManager.getInstance().save();
                    }
                    ImGui.sameLine();
                    if (ImGui.button("Eliminar Registro")) {
                        selectedCategory.getRecords().remove(selectedRecord);
                        selectedRecord = null;
                        GrhLibraryManager.getInstance().save();
                    }

                    // Preview del GRH
                    if (selectedRecord.getGrhIndex() > 0) {
                        ImGui.separator();
                        ImGui.text("Preview:");
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
                ImGui.text("Selecciona una categoría");
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
