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

            ImGui.beginChild("HistoryList", 0, 0, true);

            Stack<Command> undoStack = context.getUndoStack();
            Stack<Command> redoStack = context.getRedoStack();

            // Show Undo items (Past)
            // Stored in stack: [0: old, ..., N: recent]
            // We want to show: Recent -> Old
            for (int i = undoStack.size() - 1; i >= 0; i--) {
                Command cmd = undoStack.get(i);
                String label = String.format("%d. %s##undo%d", i + 1, cmd.getName(), i);

                if (ImGui.selectable(label, false)) {
                    // Undo until this command (exclusive? or inclusive?)
                    // If I click index 5, I want to undo from Top down to 5?
                    // Usually history view shows state *after* command.
                    // Simply: to go back to state BEFORE command i, we must undo (size - i) times.
                    // To go back to state AFTER command i (so current state is command i), we undo
                    // (size - 1 - i) times.

                    // Let's implement simpler logic: Undo until this item is stripped?
                    // Or Undo multiple times.

                    int undoCount = undoStack.size() - i;
                    for (int k = 0; k < undoCount; k++) {
                        CommandManager.getInstance().undo();
                    }
                }
            }

            if (!undoStack.isEmpty() && !redoStack.isEmpty()) {
                ImGui.separator();
            }

            // Show Redo items (Future)
            // Stored in stack: [0: next, ..., N: far future]
            // We want to show: Next -> Far Future
            for (int i = 0; i < redoStack.size(); i++) {
                Command cmd = redoStack.get(i);
                String label = String.format("%s (Redo)##redo%d", cmd.getName(), i);

                ImGui.pushStyleColor(imgui.flag.ImGuiCol.Text, 0.5f, 0.5f, 0.5f, 1.0f);
                if (ImGui.selectable(label, false)) {
                    // Redo until this command is executed
                    int redoCount = i + 1;
                    for (int k = 0; k < redoCount; k++) {
                        CommandManager.getInstance().redo();
                    }
                }
                ImGui.popStyleColor();
            }

            ImGui.endChild();
        }
        ImGui.end();
    }
}
