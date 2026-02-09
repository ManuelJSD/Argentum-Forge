package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.utils.editor.MapValidator;

import imgui.type.ImBoolean;
import java.util.List;

public class FMapValidator extends Form {

    private List<MapValidator.ValidationError> currentErrors = null;
    private final ImBoolean open = new ImBoolean(true);
    private final imgui.type.ImInt inputObjType = new imgui.type.ImInt(0);

    public FMapValidator() {
        this.currentErrors = MapValidator.validateCurrentMap();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(600, 400, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.title"), open,
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking)) {

            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.rescan"))) {
                this.currentErrors = MapValidator.validateCurrentMap();
            }

            ImGui.sameLine();
            if (ImGui.treeNode(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.config"))) {
                ImGui.text(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.ignore"));

                ImGui.inputInt(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.objtype"), inputObjType);
                ImGui.sameLine();
                if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.add"))) {
                    Options.INSTANCE.getIgnoredObjTypes().add(inputObjType.get());
                    Options.INSTANCE.save(); // Salvar cambios
                }

                ImGui.separator();

                // Mostrar tags removibles
                java.util.List<Integer> sortedTypes = new java.util.ArrayList<>(Options.INSTANCE.getIgnoredObjTypes());
                java.util.Collections.sort(sortedTypes);

                for (Integer typeId : sortedTypes) {
                    ImGui.bulletText(String
                            .format(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.item.type"), typeId));
                    ImGui.sameLine();
                    if (ImGui.smallButton(
                            org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.symbol.clear") + "##" + typeId)) {
                        Options.INSTANCE.getIgnoredObjTypes().remove(typeId);
                        Options.INSTANCE.save();
                    }
                }
                ImGui.treePop();
            }

            ImGui.separator();

            if (currentErrors == null || currentErrors.isEmpty()) {
                ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f,
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.noerrors"));
            } else {
                ImGui.text(String.format(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.errors"),
                        currentErrors.size()));

                if (ImGui.beginTable("ErrorsTable", 4,
                        ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY)) {

                    ImGui.tableSetupColumn(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.type"),
                            ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.pos"),
                            ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.desc"),
                            ImGuiTableColumnFlags.WidthFixed, 300.0f);
                    ImGui.tableSetupColumn(org.argentumforge.engine.i18n.I18n.INSTANCE.get("validator.action"),
                            ImGuiTableColumnFlags.WidthFixed, 80.0f);
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
                        if (ImGui.tableNextColumn()) {
                            if (ImGui.button(org.argentumforge.engine.i18n.I18n.INSTANCE.get("common.go") + "##"
                                    + error.x + "_" + error.y)) {
                                org.argentumforge.engine.game.User.INSTANCE.teleport(error.x, error.y);
                                // Camera operates in pixels, assuming 32x32 tiles.
                                // We center the camera on the tile.
                                org.argentumforge.engine.game.User.INSTANCE.getUserPos().setX(error.x);
                                org.argentumforge.engine.game.User.INSTANCE.getUserPos().setY(error.y);
                            }
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
