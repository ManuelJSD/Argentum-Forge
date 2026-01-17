package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiInputTextFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.editor.Transfer;
import org.argentumforge.engine.i18n.I18n;

import static org.argentumforge.engine.utils.GameData.mapData;

/**
 * Formulario de edición de traslados (teleports) en el mapa.
 * <p>
 * Permite configurar destinos de traslados, insertar y quitar traslados,
 * y activar la unión manual con mapas adyacentes.
 */
public class FTransferEditor extends Form {

    private final Transfer transfer;

    // Campos de entrada para coordenadas de destino
    private final ImInt inputMap = new ImInt(1);
    private final ImInt inputX = new ImInt(50);
    private final ImInt inputY = new ImInt(50);

    // Checkbox para unión manual
    private boolean manualUnionEnabled = false;

    public FTransferEditor() {
        this.transfer = Transfer.getInstance();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(320, 310, imgui.flag.ImGuiCond.Once);
        ImGui.begin(I18n.INSTANCE.get("editor.transfer.title"), imgui.flag.ImGuiWindowFlags.NoResize);

        // === SECCIÓN: DESTINO ===
        ImGui.text(I18n.INSTANCE.get("editor.transfer.destination"));
        ImGui.separator();
        ImGui.spacing();

        // Campo Mapa
        ImGui.text(I18n.INSTANCE.get("editor.transfer.map"));
        ImGui.sameLine(80);
        ImGui.setNextItemWidth(150);
        if (ImGui.inputInt("##MapInput", inputMap, 1, 10, ImGuiInputTextFlags.None)) {
            // Validar rango
            if (inputMap.get() < 1)
                inputMap.set(1);
            if (inputMap.get() > 9000)
                inputMap.set(9000);
            updateDestination();
        }

        // Campo X
        ImGui.text("X:");
        ImGui.sameLine(80);
        ImGui.setNextItemWidth(150);
        if (ImGui.inputInt("##XInput", inputX, 1, 10, ImGuiInputTextFlags.None)) {
            // Validar rango
            if (inputX.get() < 1)
                inputX.set(1);
            if (inputX.get() > 100)
                inputX.set(100);
            updateDestination();
        }

        // Campo Y
        ImGui.text("Y:");
        ImGui.sameLine(80);
        ImGui.setNextItemWidth(150);
        if (ImGui.inputInt("##YInput", inputY, 1, 10, ImGuiInputTextFlags.None)) {
            // Validar rango
            if (inputY.get() < 1)
                inputY.set(1);
            if (inputY.get() > 100)
                inputY.set(100);
            updateDestination();
        }

        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // === SECCIÓN: MODO ===
        ImGui.text(I18n.INSTANCE.get("editor.transfer.mode"));
        ImGui.spacing();

        // Botón Insertar Traslado
        boolean isInserting = transfer.isActive() && transfer.getMode() == 1;
        if (isInserting) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF00AA00); // Verde oscuro
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0xFF00CC00); // Verde claro
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0xFF008800); // Verde más oscuro
        }

        if (ImGui.button(I18n.INSTANCE.get("editor.transfer.insert"), 280, 30)) {
            if (isInserting) {
                // Desactivar
                transfer.setActive(false);
            } else {
                // Activar modo insertar
                transfer.setActive(true);
                transfer.setMode(1);
                updateDestination();
            }
        }

        if (isInserting) {
            ImGui.popStyleColor(3);
        }

        ImGui.spacing();

        // Botón Quitar Traslado
        boolean isRemoving = transfer.isActive() && transfer.getMode() == 0;
        if (isRemoving) {
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF0000AA); // Rojo oscuro
            ImGui.pushStyleColor(ImGuiCol.ButtonHovered, 0xFF0000CC); // Rojo claro
            ImGui.pushStyleColor(ImGuiCol.ButtonActive, 0xFF000088); // Rojo más oscuro
        }

        if (ImGui.button(I18n.INSTANCE.get("editor.transfer.remove"), 280, 30)) {
            if (isRemoving) {
                // Desactivar
                transfer.setActive(false);
            } else {
                // Activar modo quitar
                transfer.setActive(true);
                transfer.setMode(0);
            }
        }

        if (isRemoving) {
            ImGui.popStyleColor(3);
        }

        ImGui.spacing();
        ImGui.separator();
        ImGui.spacing();

        // === SECCIÓN: OPCIONES AVANZADAS ===
        ImGui.text(I18n.INSTANCE.get("editor.transfer.options"));
        ImGui.spacing();

        // Checkbox Unión Manual
        if (ImGui.checkbox(I18n.INSTANCE.get("editor.transfer.manualUnion"), manualUnionEnabled)) {
            manualUnionEnabled = !manualUnionEnabled;
            transfer.setManualUnion(manualUnionEnabled);
        }

        if (ImGui.isItemHovered()) {
            ImGui.setTooltip(I18n.INSTANCE.get("editor.transfer.manualUnionTooltip"));
        }

        ImGui.sameLine(160);
        if (ImGui.button(I18n.INSTANCE.get("editor.transfer.autoUnion"))) {
            org.argentumforge.engine.gui.ImGUISystem.INSTANCE.show(new FAutoUnion());
        }

        ImGui.spacing();

        // Información de estado
        if (transfer.isActive()) {
            ImGui.spacing();
            ImGui.separator();
            ImGui.spacing();

            if (transfer.getMode() == 1) {
                ImGui.textColored(0f, 1f, 0f, 1f, I18n.INSTANCE.get("editor.transfer.mode.insert"));
                ImGui.text(I18n.INSTANCE.get("editor.transfer.destInfo", transfer.getDestinationMap(),
                        transfer.getDestinationX(), transfer.getDestinationY()));
                if (manualUnionEnabled) {
                    ImGui.textColored(1f, 1f, 0f, 1f, I18n.INSTANCE.get("editor.transfer.manualUnionActive"));
                }
            } else {
                ImGui.textColored(1f, 0f, 0f, 1f, I18n.INSTANCE.get("editor.transfer.mode.remove"));
            }
        }

        ImGui.end();
    }

    /**
     * Actualiza las coordenadas de destino en el Transfer manager.
     */
    private void updateDestination() {
        transfer.setDestination(inputMap.get(), inputX.get(), inputY.get());
    }

    /**
     * Actualiza los campos de entrada con las coordenadas capturadas.
     * Llamado externamente cuando se capturan coordenadas con clic derecho.
     */
    public void updateInputFields(int map, int x, int y) {
        inputMap.set(map);
        inputX.set(x);
        inputY.set(y);
        updateDestination();
    }
}
