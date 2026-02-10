package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.utils.editor.MapValidator;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.game.User;

import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.flag.ImGuiCond;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

public class FMapValidator extends Form {

    private List<MapValidator.ValidationError> currentErrors = null;
    private final ImBoolean open = new ImBoolean(true);
    private final ImInt inputObjType = new ImInt(0);

    public FMapValidator() {
        this.currentErrors = MapValidator.validateCurrentMap();
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(600, 400, ImGuiCond.FirstUseEver);

        if (ImGui.begin(I18n.INSTANCE.get("validator.title"), open,
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoDocking)) {

            if (ImGui.button(I18n.INSTANCE.get("validator.rescan"))) {
                this.currentErrors = MapValidator.validateCurrentMap();
            }

            ImGui.sameLine();
            if (ImGui.treeNode(I18n.INSTANCE.get("validator.config"))) {
                ImGui.text(I18n.INSTANCE.get("validator.ignore"));

                ImGui.inputInt(I18n.INSTANCE.get("validator.objtype"), inputObjType);
                ImGui.sameLine();
                if (ImGui.button(I18n.INSTANCE.get("validator.add"))) {
                    Options.INSTANCE.getIgnoredObjTypes().add(inputObjType.get());
                    Options.INSTANCE.save(); // Salvar cambios
                }

                ImGui.separator();

                // Mostrar tags removibles
                List<Integer> sortedTypes = new ArrayList<>(Options.INSTANCE.getIgnoredObjTypes());
                Collections.sort(sortedTypes);

                for (Integer typeId : sortedTypes) {
                    ImGui.bulletText(String
                            .format(I18n.INSTANCE.get("validator.item.type"), typeId));
                    ImGui.sameLine();
                    if (ImGui.smallButton(
                            I18n.INSTANCE.get("common.symbol.clear") + "##" + typeId)) {
                        Options.INSTANCE.getIgnoredObjTypes().remove(typeId);
                        Options.INSTANCE.save();
                    }
                }
                ImGui.treePop();
            }

            ImGui.separator();

            if (currentErrors == null || currentErrors.isEmpty()) {
                ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f,
                        I18n.INSTANCE.get("validator.noerrors"));
            } else {
                ImGui.text(String.format(I18n.INSTANCE.get("validator.errors"),
                        currentErrors.size()));

                if (ImGui.beginTable("ErrorsTable", 4,
                        ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.ScrollY)) {

                    ImGui.tableSetupColumn(I18n.INSTANCE.get("validator.type"),
                            ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn(I18n.INSTANCE.get("validator.pos"),
                            ImGuiTableColumnFlags.WidthFixed, 80.0f);
                    ImGui.tableSetupColumn(I18n.INSTANCE.get("validator.desc"),
                            ImGuiTableColumnFlags.WidthFixed, 300.0f);
                    ImGui.tableSetupColumn(I18n.INSTANCE.get("validator.action"),
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
                            if (ImGui.button(I18n.INSTANCE.get("common.go") + "##"
                                    + error.x + "_" + error.y)) {
                                User.INSTANCE.teleport(error.x, error.y);
                                // Camera operates in pixels, assuming 32x32 tiles.
                                // We center the camera on the tile.
                                User.INSTANCE.getUserPos().setX(error.x);
                                User.INSTANCE.getUserPos().setY(error.y);
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
