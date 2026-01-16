package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.MapValidator;

import imgui.type.ImBoolean;
import java.util.List;

public class FMapValidator extends Form {

    private List<MapValidator.ValidationError> currentErrors = null;
    private final ImBoolean open = new ImBoolean(true);

    public FMapValidator() {
        this.currentErrors = MapValidator.validateCurrentMap();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(600, 400, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin("Validación de Mapa", open, ImGuiWindowFlags.NoCollapse)) {

            if (ImGui.button("Re-Escanear Mapa")) {
                this.currentErrors = MapValidator.validateCurrentMap();
            }

            ImGui.separator();

            if (currentErrors == null || currentErrors.isEmpty()) {
                ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f, "¡No se encontraron errores en el mapa!");
            } else {
                ImGui.text("Errores encontrados: " + currentErrors.size());

                if (ImGui.beginTable("ErrorsTable", 4,
                        ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY)) {

                    ImGui.tableSetupColumn("Tipo", ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn("Posición", ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn("Descripción", ImGuiTableColumnFlags.WidthFixed, 300.0f);
                    ImGui.tableSetupColumn("Acción", ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableHeadersRow();

                    for (MapValidator.ValidationError error : currentErrors) {
                        ImGui.tableNextRow();

                        // Column 0: Type
                        ImGui.tableNextColumn();
                        if ("ERROR".equals(error.type)) {
                            ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, error.type);
                        } else {
                            ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, error.type);
                        }

                        // Column 1: Pos
                        ImGui.tableNextColumn();
                        ImGui.text(error.x + "," + error.y);

                        // Column 2: Desc
                        ImGui.tableNextColumn();
                        ImGui.text(error.description);

                        // Column 3: Action
                        ImGui.tableNextColumn();
                        if (ImGui.button("Ir##" + error.x + "_" + error.y)) {
                            // Teleport camera logic
                            // Camera operates in pixels, assuming 32x32 tiles.
                            // We center the camera on the tile.
                            org.argentumforge.engine.game.User.INSTANCE.getUserPos().setX(error.x);
                            org.argentumforge.engine.game.User.INSTANCE.getUserPos().setY(error.y);
                        }
                    }

                    ImGui.endTable();
                }
            }
        }
        ImGui.end();

        // Handle window close manually if needed, or rely on ImGui close button logic
        // managing the list in ImGUISystem.
        if (!open.get()) {
            ImGUISystem.INSTANCE.deleteFrmArray(this);
        }
    }
}
