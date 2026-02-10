package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.type.ImString;
import org.argentumforge.engine.utils.editor.Trigger;
import org.argentumforge.engine.utils.editor.TriggerManager;
import org.argentumforge.engine.utils.editor.models.TriggerData;
import org.argentumforge.engine.i18n.I18n;

import java.util.List;

import org.argentumforge.engine.utils.MapContext;

public class FTriggerEditor extends Form implements IMapEditor {

    private final TriggerManager manager;
    private final Trigger tool;
    private int selectedIndex = -1;
    private final ImString editName = new ImString(64);

    public FTriggerEditor() {
        this.manager = TriggerManager.getInstance();
        this.tool = Trigger.getInstance();
    }

    @Override
    public void setContext(MapContext context) {
        // Context not needed in this editor
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(460, 350, ImGuiCond.Once);
        ImGui.begin(I18n.INSTANCE.get("editor.trigger"), ImGuiWindowFlags.NoResize);

        // --- COLUMNA IZQUIERDA: Lista y Gestión ---
        ImGui.beginGroup();

        // 1. Lista
        ImGui.text(I18n.INSTANCE.get("editor.trigger.list"));
        ImGui.beginChild("TriggerList", 220, 180, true);
        List<TriggerData> triggers = manager.getTriggers();
        for (int i = 0; i < triggers.size(); i++) {
            TriggerData t = triggers.get(i);
            String label = t.getId() + " - " + t.getName();
            boolean isSelected = (selectedIndex == i);

            if (ImGui.selectable(label, isSelected, ImGuiSelectableFlags.None)) {
                selectedIndex = i;
                editName.set(t.getName());
                // Si la herramienta de colocar estaba activa, actualizamos el ID a lo que
                // seleccionamos
                if (tool.isActive() && tool.getSelectedTriggerId() != 0) {
                    tool.setSelectedTriggerId(t.getId());
                }
            }
        }
        ImGui.endChild();

        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // 2. Gestión (Debajo de la lista)
        ImGui.text(I18n.INSTANCE.get("editor.trigger.management"));
        if (ImGui.button(I18n.INSTANCE.get("editor.trigger.new"), 220, 25)) {
            manager.addTrigger(I18n.INSTANCE.get("editor.trigger.defaultName"));
        }

        boolean hasSelection = selectedIndex >= 0 && selectedIndex < triggers.size();

        if (hasSelection) {
            ImGui.setNextItemWidth(220); // Fijar ancho para evitar desplazamiento de columna derecha
            ImGui.inputText("##EditName", editName, ImGuiInputTextFlags.None);
            if (ImGui.button(I18n.INSTANCE.get("editor.trigger.rename"), 108, 0)) {
                TriggerData t = triggers.get(selectedIndex);
                manager.updateTrigger(t.getId(), editName.get());
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.delete"), 108, 0)) {
                TriggerData t = triggers.get(selectedIndex);
                manager.removeTrigger(t.getId());
                selectedIndex = -1;
                editName.set("");
                if (tool.isActive() && tool.getSelectedTriggerId() == t.getId()) {
                    tool.setActive(false);
                }
            }
        } else {
            ImGui.textDisabled(I18n.INSTANCE.get("editor.trigger.editPrompt"));
        }

        ImGui.endGroup();

        ImGui.sameLine();
        ImGui.dummy(10, 0); // Espaciado entre columnas
        ImGui.sameLine();

        // --- COLUMNA DERECHA: Herramienta ---
        ImGui.beginGroup();
        ImGui.text(I18n.INSTANCE.get("editor.trigger.tool"));

        ImGui.spacing();

        // Estado
        boolean isPlacing = tool.isActive() && tool.getSelectedTriggerId() > 0;
        boolean isErasing = tool.isActive() && tool.getSelectedTriggerId() == 0;

        if (isPlacing) {
            ImGui.textColored(0f, 1f, 0f, 1f, I18n.INSTANCE.get("editor.trigger.mode.place"));
            ImGui.textColored(0f, 1f, 0f, 1f, I18n.INSTANCE.get("common.id") + ": " + tool.getSelectedTriggerId());
        } else if (isErasing) {
            ImGui.textColored(1f, 0f, 0f, 1f, I18n.INSTANCE.get("editor.trigger.mode.erase"));
        } else {
            ImGui.textDisabled(I18n.INSTANCE.get("editor.trigger.mode.inactive"));
        }

        ImGui.spacing();

        // Boton Colocar (Toggle logic)
        if (hasSelection) {
            boolean active = isPlacing && tool.getSelectedTriggerId() == triggers.get(selectedIndex).getId();
            if (active) {
                ImGui.pushStyleColor(ImGuiCol.Button, 0xFF00FF00); // Verde
            }
            if (ImGui.button(I18n.INSTANCE.get("editor.trigger.place"), 160, 30)) {
                if (active) {
                    tool.setActive(false);
                } else {
                    tool.setMode(1);
                    tool.setSelectedTriggerId(triggers.get(selectedIndex).getId());
                }
            }
            if (active)
                ImGui.popStyleColor();
        } else {
            ImGui.beginDisabled();
            ImGui.button(I18n.INSTANCE.get("editor.trigger.place"), 160, 30);
            ImGui.endDisabled();
        }

        ImGui.spacing();

        // Boton Borrador (Toggle logic)
        if (isErasing) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF0000FF); // Rojo
        }
        if (ImGui.button(I18n.INSTANCE.get("editor.trigger.eraser"), 160, 30)) {
            if (isErasing) {
                tool.setActive(false);
            } else {
                tool.setMode(1);
                tool.setSelectedTriggerId(0);
            }
        }
        if (isErasing)
            ImGui.popStyleColor();

        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // --- Herramientas de Pincel ---
        ImGui.text(I18n.INSTANCE.get("editor.block.shape"));
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.square"),
                tool.getBrushShape() == Trigger.BrushShape.SQUARE)) {
            tool.setBrushShape(Trigger.BrushShape.SQUARE);
        }
        ImGui.sameLine();
        if (ImGui.radioButton(I18n.INSTANCE.get("editor.surface.circle"),
                tool.getBrushShape() == Trigger.BrushShape.CIRCLE)) {
            tool.setBrushShape(Trigger.BrushShape.CIRCLE);
        }

        ImGui.spacing();
        ImGui.text(I18n.INSTANCE.get("editor.surface.size") + ":");

        int currentBrush = tool.getBrushSize();
        int activeColor = 0xFF00FF00;

        // 1x1
        if (currentBrush == 1)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("1x1", 48, 25))
            tool.setBrushSize(1);
        if (currentBrush == 1)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // 3x3
        if (currentBrush == 3)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("3x3", 48, 25))
            tool.setBrushSize(3);
        if (currentBrush == 3)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // 5x5
        if (currentBrush == 5)
            ImGui.pushStyleColor(ImGuiCol.Button, activeColor);
        if (ImGui.button("5x5", 48, 25))
            tool.setBrushSize(5);
        if (currentBrush == 5)
            ImGui.popStyleColor();

        ImGui.endGroup();

        ImGui.end();
    }
}
