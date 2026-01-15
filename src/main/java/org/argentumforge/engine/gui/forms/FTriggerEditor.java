package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.type.ImString;
import org.argentumforge.engine.utils.editor.Trigger;
import org.argentumforge.engine.utils.editor.TriggerManager;
import org.argentumforge.engine.utils.editor.models.TriggerData;

import java.util.List;

import static org.argentumforge.engine.gui.ImGUISystem.INSTANCE;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public class FTriggerEditor extends Form {

    private final TriggerManager manager;
    private final Trigger tool;
    private int selectedIndex = -1;
    private final ImString editName = new ImString(64);

    public FTriggerEditor() {
        this.manager = TriggerManager.getInstance();
        this.tool = Trigger.getInstance();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(460, 350, imgui.flag.ImGuiCond.Once);
        ImGui.begin("Editor de Triggers", imgui.flag.ImGuiWindowFlags.NoResize);

        // --- COLUMNA IZQUIERDA: Lista y Gestión ---
        ImGui.beginGroup();

        // 1. Lista
        ImGui.text("Lista de Triggers:");
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
        ImGui.text("Gestión:");
        if (ImGui.button("Nuevo Trigger", 220, 25)) {
            manager.addTrigger("Nuevo Trigger");
        }

        boolean hasSelection = selectedIndex >= 0 && selectedIndex < triggers.size();

        if (hasSelection) {
            ImGui.setNextItemWidth(220); // Fijar ancho para evitar desplazamiento de columna derecha
            ImGui.inputText("##EditName", editName, ImGuiInputTextFlags.None);
            if (ImGui.button("Renombrar", 108, 0)) {
                TriggerData t = triggers.get(selectedIndex);
                manager.updateTrigger(t.getId(), editName.get());
            }
            ImGui.sameLine();
            if (ImGui.button("Eliminar", 108, 0)) {
                TriggerData t = triggers.get(selectedIndex);
                manager.removeTrigger(t.getId());
                selectedIndex = -1;
                editName.set("");
                if (tool.isActive() && tool.getSelectedTriggerId() == t.getId()) {
                    tool.setActive(false);
                }
            }
        } else {
            ImGui.textDisabled("(Selecciona para editar)");
        }

        ImGui.endGroup();

        ImGui.sameLine();
        ImGui.dummy(10, 0); // Espaciado entre columnas
        ImGui.sameLine();

        // --- COLUMNA DERECHA: Herramienta ---
        ImGui.beginGroup();
        ImGui.text("Herramienta:");

        ImGui.spacing();

        // Estado
        boolean isPlacing = tool.isActive() && tool.getSelectedTriggerId() > 0;
        boolean isErasing = tool.isActive() && tool.getSelectedTriggerId() == 0;

        if (isPlacing) {
            ImGui.textColored(0f, 1f, 0f, 1f, "MODO: COLOCAR");
            ImGui.textColored(0f, 1f, 0f, 1f, "ID: " + tool.getSelectedTriggerId());
        } else if (isErasing) {
            ImGui.textColored(1f, 0f, 0f, 1f, "MODO: BORRADOR");
        } else {
            ImGui.textDisabled("Inactiva");
        }

        ImGui.spacing();

        // Boton Colocar (Toggle logic)
        if (hasSelection) {
            boolean active = isPlacing && tool.getSelectedTriggerId() == triggers.get(selectedIndex).getId();
            if (active) {
                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0xFF00FF00); // Verde
            }
            if (ImGui.button("Colocar", 160, 30)) {
                if (active) {
                    tool.setActive(false);
                } else {
                    tool.setActive(true);
                    tool.setSelectedTriggerId(triggers.get(selectedIndex).getId());
                }
            }
            if (active)
                ImGui.popStyleColor();
        } else {
            ImGui.beginDisabled();
            ImGui.button("Colocar", 160, 30);
            ImGui.endDisabled();
        }

        ImGui.spacing();

        // Boton Borrador (Toggle logic)
        if (isErasing) {
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0xFF0000FF); // Rojo
        }
        if (ImGui.button("Borrador", 160, 30)) {
            if (isErasing) {
                tool.setActive(false);
            } else {
                tool.setActive(true);
                tool.setSelectedTriggerId(0);
            }
        }
        if (isErasing)
            ImGui.popStyleColor();

        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // --- Herramientas de Pincel ---
        ImGui.text("Forma:");
        if (ImGui.radioButton("Cuadrado", tool.getBrushShape() == Trigger.BrushShape.SQUARE)) {
            tool.setBrushShape(Trigger.BrushShape.SQUARE);
        }
        ImGui.sameLine();
        if (ImGui.radioButton("Circulo", tool.getBrushShape() == Trigger.BrushShape.CIRCLE)) {
            tool.setBrushShape(Trigger.BrushShape.CIRCLE);
        }

        ImGui.spacing();
        ImGui.text("Tamaño:");

        int currentBrush = tool.getBrushSize();
        int activeColor = 0xFF00FF00;

        // 1x1
        if (currentBrush == 1)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, activeColor);
        if (ImGui.button("1x1", 48, 25))
            tool.setBrushSize(1);
        if (currentBrush == 1)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // 3x3
        if (currentBrush == 3)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, activeColor);
        if (ImGui.button("3x3", 48, 25))
            tool.setBrushSize(3);
        if (currentBrush == 3)
            ImGui.popStyleColor();

        ImGui.sameLine();

        // 5x5
        if (currentBrush == 5)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, activeColor);
        if (ImGui.button("5x5", 48, 25))
            tool.setBrushSize(5);
        if (currentBrush == 5)
            ImGui.popStyleColor();

        ImGui.endGroup();

        ImGui.end();
    }
}
