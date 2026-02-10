package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCond;
import org.argentumforge.engine.game.console.ConsoleCommand;
import org.argentumforge.engine.game.console.ConsoleCommandProcessor;
import org.argentumforge.engine.i18n.I18n;

public class FHelpCommands extends Form {

    public FHelpCommands() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(400, 300, ImGuiCond.FirstUseEver);

        String title = I18n.INSTANCE.get("help.commands.title");
        if (ImGui.begin(title, ImGuiWindowFlags.NoCollapse)) {

            if (ImGui.beginTable("CommandsTable", 2,
                    ImGuiTableFlags.Borders | ImGuiTableFlags.RowBg | ImGuiTableFlags.Resizable)) {

                ImGui.tableSetupColumn(I18n.INSTANCE.get("help.commands.column.command"));
                ImGui.tableSetupColumn(I18n.INSTANCE.get("help.commands.column.description"));
                ImGui.tableHeadersRow();

                for (ConsoleCommand cmd : ConsoleCommandProcessor.getCommands()) {
                    ImGui.tableNextRow();

                    ImGui.tableNextColumn();
                    ImGui.textColored(0.2f, 0.8f, 1.0f, 1.0f, cmd.name());

                    ImGui.tableNextColumn();
                    ImGui.textWrapped(I18n.INSTANCE.get(cmd.descriptionKey()));
                }

                ImGui.endTable();
            }

            ImGui.separator();
            ImGui.dummy(0, 10);

            if (ImGui.button(I18n.INSTANCE.get("common.close"), -1, 30)) {
                close();
            }
        }
        ImGui.end();
    }
}
