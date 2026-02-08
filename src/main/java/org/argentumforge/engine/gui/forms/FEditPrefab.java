package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.utils.editor.PrefabManager;
import org.argentumforge.engine.utils.editor.models.Prefab;
import org.argentumforge.engine.i18n.I18n;

import java.util.List;

/**
 * Diálogo modal para editar los metadatos de un prefab existente.
 */
public class FEditPrefab extends Form {

    private final Prefab prefab;
    private final ImString name = new ImString(64);
    private final ImString category = new ImString(64);

    private final ImBoolean pOpen = new ImBoolean(true);
    private boolean firstFrame = true;

    public FEditPrefab(Prefab prefab) {
        this.prefab = prefab;
        if (prefab != null) {
            this.name.set(prefab.getName());
            this.category.set(prefab.getCategory());
        }
    }

    @Override
    public void render() {
        if (prefab == null) {
            this.close();
            return;
        }

        if (firstFrame) {
            ImGui.openPopup(I18n.INSTANCE.get("prefab.edit.title"));
            firstFrame = false;
        }

        // Centro de pantalla
        imgui.ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), imgui.flag.ImGuiCond.Appearing, 0.5f,
                0.5f);

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("prefab.edit.title"), pOpen, ImGuiWindowFlags.AlwaysAutoResize)) {

            ImGui.text(I18n.INSTANCE.get("prefab.edit.desc"));
            ImGui.separator();

            ImGui.alignTextToFramePadding();
            ImGui.text(I18n.INSTANCE.get("prefab.field.name"));
            ImGui.sameLine();
            ImGui.inputText("##name", name, ImGuiInputTextFlags.AutoSelectAll);

            ImGui.alignTextToFramePadding();
            ImGui.text(I18n.INSTANCE.get("prefab.field.category"));
            ImGui.sameLine();

            // Sugerencias de categorías existentes
            List<String> categories = PrefabManager.getInstance().getCategories();
            if (ImGui.beginCombo("##category", category.get())) {
                for (String cat : categories) {
                    boolean isSelected = category.get().equals(cat);
                    if (ImGui.selectable(cat, isSelected)) {
                        category.set(cat);
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }
            ImGui.sameLine();
            ImGui.text(I18n.INSTANCE.get("prefab.field.category.new"));
            ImGui.inputText("##categoryInput", category);

            ImGui.separator();

            if (ImGui.button(I18n.INSTANCE.get("prefab.saveChanges"), 120, 30)) {
                if (name.get().isEmpty()) {
                    DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                            I18n.INSTANCE.get("prefab.error.nameRequired"));
                } else {
                    saveChanges();
                    ImGui.closeCurrentPopup();
                    this.close();
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("prefab.cancel"), 100, 30)) {
                ImGui.closeCurrentPopup();
                this.close();
            }

            ImGui.endPopup();
        } else {
            if (!pOpen.get()) {
                this.close();
            }
        }
    }

    private void saveChanges() {
        boolean success = PrefabManager.getInstance().updatePrefab(prefab, name.get(), category.get());

        if (success) {
            // No es necesario mostrar mensaje de éxito intrusivo, la lista se actualizará
            // sola
            // DialogManager.getInstance().showInfo("Éxito", "Prefabricado actualizado.");
        } else {
            DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                    I18n.INSTANCE.get("prefab.error.update"));
        }
    }
}
