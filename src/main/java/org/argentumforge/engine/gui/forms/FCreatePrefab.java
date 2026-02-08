package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImString;
import org.argentumforge.engine.gui.DialogManager;
import org.argentumforge.engine.utils.editor.PrefabManager;
import org.argentumforge.engine.utils.editor.Selection;
import org.argentumforge.engine.utils.editor.models.Prefab;
import org.argentumforge.engine.utils.editor.models.Prefab.PrefabFeatures;
import org.argentumforge.engine.i18n.I18n;

import java.util.List;

/**
 * Diálogo modal para crear un nuevo prefabricado a partir de la selección
 * actual.
 */
public class FCreatePrefab extends Form {

    private final ImString name = new ImString(64);
    private final ImString category = new ImString("General", 64);

    // Opciones
    private final ImBoolean optLayer1 = new ImBoolean(true);
    private final ImBoolean optLayer2 = new ImBoolean(true);
    private final ImBoolean optLayer3 = new ImBoolean(true);
    private final ImBoolean optLayer4 = new ImBoolean(true);
    private final ImBoolean optBlock = new ImBoolean(true);
    private final ImBoolean optTriggers = new ImBoolean(true);
    private final ImBoolean optObjects = new ImBoolean(true);
    private final ImBoolean optNpcs = new ImBoolean(true);
    private final ImBoolean optParticles = new ImBoolean(true);

    private final ImBoolean pOpen = new ImBoolean(true);
    private boolean firstFrame = true;

    @Override
    public void render() {
        if (firstFrame) {
            ImGui.openPopup(I18n.INSTANCE.get("prefab.create.title"));
            firstFrame = false;
        }

        // Centro de pantalla
        imgui.ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getCenterX(), viewport.getCenterY(), imgui.flag.ImGuiCond.Appearing, 0.5f,
                0.5f);

        if (ImGui.beginPopupModal(I18n.INSTANCE.get("prefab.create.title"), pOpen, ImGuiWindowFlags.AlwaysAutoResize)) {

            ImGui.text(I18n.INSTANCE.get("prefab.create.desc"));
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

            ImGui.spacing();
            ImGui.text(I18n.INSTANCE.get("prefab.include"));

            // Capas
            ImGui.checkbox(I18n.INSTANCE.get("grhlib.layer") + " 1", optLayer1);
            ImGui.sameLine();
            ImGui.checkbox(I18n.INSTANCE.get("grhlib.layer") + " 2", optLayer2);
            ImGui.sameLine();
            ImGui.checkbox(I18n.INSTANCE.get("grhlib.layer") + " 3", optLayer3);
            ImGui.sameLine();
            ImGui.checkbox(I18n.INSTANCE.get("grhlib.layer") + " 4", optLayer4);

            ImGui.separator();
            ImGui.checkbox(I18n.INSTANCE.get("prefab.block"), optBlock);
            ImGui.checkbox(I18n.INSTANCE.get("prefab.trigger"), optTriggers);
            ImGui.sameLine();
            ImGui.checkbox(I18n.INSTANCE.get("prefab.object"), optObjects);
            ImGui.checkbox(I18n.INSTANCE.get("prefab.npc"), optNpcs);
            ImGui.sameLine();
            ImGui.checkbox(I18n.INSTANCE.get("prefab.particle"), optParticles);

            ImGui.separator();

            if (ImGui.button(I18n.INSTANCE.get("prefab.save"), 100, 30)) {
                if (name.get().isEmpty()) {
                    DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                            I18n.INSTANCE.get("prefab.error.nameRequired"));
                } else {
                    savePrefab();
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

    private void savePrefab() {
        Selection selection = Selection.getInstance();
        List<Selection.SelectedEntity> entities = selection.getSelectedEntities();

        if (entities.isEmpty()) {
            DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                    I18n.INSTANCE.get("prefab.error.noSelection"));
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (Selection.SelectedEntity se : entities) {
            if (se.x < minX)
                minX = se.x;
            if (se.y < minY)
                minY = se.y;
            if (se.x > maxX)
                maxX = se.x;
            if (se.y > maxY)
                maxY = se.y;
        }

        int width = (maxX - minX) + 1;
        int height = (maxY - minY) + 1;

        if (width <= 0 || height <= 0) {
            DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                    I18n.INSTANCE.get("prefab.error.invalidSelection"));
            return;
        }

        PrefabFeatures features = new PrefabFeatures();
        features.layer1 = optLayer1.get();
        features.layer2 = optLayer2.get();
        features.layer3 = optLayer3.get();
        features.layer4 = optLayer4.get();
        features.block = optBlock.get();
        features.triggers = optTriggers.get();
        features.objects = optObjects.get();
        features.npcs = optNpcs.get();
        features.particles = optParticles.get();

        Prefab prefab = PrefabManager.createPrefabFromMap(
                name.get(),
                category.get(),
                minX, minY, width, height,
                features);

        if (prefab != null) {
            boolean success = PrefabManager.getInstance().savePrefab(prefab);
            if (success) {
                DialogManager.getInstance().showInfo(I18n.INSTANCE.get("msg.complete"),
                        I18n.INSTANCE.get("prefab.success.save"));
            } else {
                DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                        I18n.INSTANCE.get("prefab.error.saveFile"));
            }
        } else {
            DialogManager.getInstance().showError(I18n.INSTANCE.get("msg.error"),
                    I18n.INSTANCE.get("prefab.error.extract"));
        }
    }
}
