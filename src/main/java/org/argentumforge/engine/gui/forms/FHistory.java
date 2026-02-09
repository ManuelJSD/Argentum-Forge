package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.commands.Command;
import org.argentumforge.engine.utils.editor.commands.CommandManager;

import java.util.Stack;

public class FHistory extends Form {
    private static FHistory instance;
    private final String title;

    private FHistory() {
        this.title = I18n.INSTANCE.get("history.title");
    }

    public static FHistory getInstance() {
        if (instance == null) {
            instance = new FHistory();
        }
        return instance;
    }

    @Override
    public void render() {
        ImBoolean pOpen = new ImBoolean(true);
        ImGui.setNextWindowSizeConstraints(300, 400, Float.MAX_VALUE, Float.MAX_VALUE);

        if (ImGui.begin(title, pOpen, ImGuiWindowFlags.None)) {
            if (!pOpen.get()) {
                this.close();
            }

            MapContext context = GameData.getActiveContext();
            if (context == null) {
                ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, I18n.INSTANCE.get("msg.noActiveContext"));
                ImGui.end();
                return;
            }

            if (ImGui.button(I18n.INSTANCE.get("menu.edit.undo"))) {
                CommandManager.getInstance().undo();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("menu.edit.redo"))) {
                CommandManager.getInstance().redo();
            }

            ImGui.separator();

            ImGui.text(I18n.INSTANCE.get("history.title"));

            CommandManager cmdManager = CommandManager.getInstance();
            cmdManager.setHoveredCommand(null); // Reset each frame

            Stack<Command> undoStack = context.getUndoStack();
            Stack<Command> redoStack = context.getRedoStack();
            int savedSize = context.getSavedUndoStackSize();

            // Status indicator for current state
            if (savedSize == undoStack.size()) {
                ImGui.textColored(0.0f, 1.0f, 0.0f, 1.0f, I18n.INSTANCE.get("history.state.saved"));
            } else {
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, I18n.INSTANCE.get("history.state.unsaved"));
            }

            ImGui.beginChild("HistoryList", 0, 0, true);

            // Show Undo items (Past)
            for (int i = undoStack.size() - 1; i >= 0; i--) {
                Command cmd = undoStack.get(i);
                boolean isSavedPoint = (i + 1 == savedSize);
                String label = String.format("%d. %s%s##undo%d", i + 1, cmd.getName(),
                        (isSavedPoint ? " " + I18n.INSTANCE.get("history.tag.saved") : ""), i);

                if (ImGui.selectable(label, false)) {
                    int undoCount = undoStack.size() - i;
                    for (int k = 0; k < undoCount; k++) {
                        cmdManager.undo();
                    }
                }
                if (ImGui.isItemHovered()) {
                    cmdManager.setHoveredCommand(cmd);
                }
            }

            if (!undoStack.isEmpty() && !redoStack.isEmpty()) {
                ImGui.separator();
            }

            // Show Redo items (Future)
            for (int i = 0; i < redoStack.size(); i++) {
                Command cmd = redoStack.get(i);
                boolean isSavedPoint = (undoStack.size() + i + 1 == savedSize);
                String label = String.format("%s%s%s##redo%d", cmd.getName(), I18n.INSTANCE.get("history.tag.redo"),
                        (isSavedPoint ? " " + I18n.INSTANCE.get("history.tag.saved") : ""),
                        i);

                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
                if (ImGui.selectable(label, false)) {
                    int redoCount = i + 1;
                    for (int k = 0; k < redoCount; k++) {
                        cmdManager.redo();
                    }
                }
                if (ImGui.isItemHovered()) {
                    cmdManager.setHoveredCommand(cmd);
                }
                ImGui.popStyleColor();
            }

            ImGui.endChild();
        }
        ImGui.end();
    }
}
