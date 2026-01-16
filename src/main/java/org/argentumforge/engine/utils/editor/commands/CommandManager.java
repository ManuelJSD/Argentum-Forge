package org.argentumforge.engine.utils.editor.commands;

import java.util.Stack;
import org.argentumforge.engine.utils.MapContext;

/**
 * Gestor de comandos que implementa la lógica de Deshacer/Rehacer.
 * Mantiene dos pilas para rastrear el historial de acciones del usuario.
 */
public class CommandManager {

    private static CommandManager instance;
    private static final int MAX_STACK_SIZE = 100;

    private CommandManager() {
    }

    public static synchronized CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }

    /**
     * Ejecuta un comando y lo añade a la pila de deshacer.
     * Limpia la pila de rehacer ya que se ha iniciado una nueva rama de acciones.
     *
     * @param command El comando a ejecutar.
     */
    public void executeCommand(Command command) {
        command.execute();
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context != null) {
            Stack<Command> undoStack = context.getUndoStack();
            undoStack.push(command);
            context.getRedoStack().clear();

            if (undoStack.size() > MAX_STACK_SIZE) {
                undoStack.remove(0);
            }
        }

        // Marcar el mapa como modificado
        org.argentumforge.engine.utils.MapManager.markAsModified();
    }

    /**
     * Deshace la última acción realizada.
     */
    public void undo() {
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context != null) {
            Stack<Command> undoStack = context.getUndoStack();
            if (!undoStack.isEmpty()) {
                Command command = undoStack.pop();
                command.undo();
                context.getRedoStack().push(command);

                // Marcar el mapa como modificado
                org.argentumforge.engine.utils.MapManager.markAsModified();
            }
        }
    }

    /**
     * Rehace la última acción deshecha.
     */
    public void redo() {
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context != null) {
            Stack<Command> redoStack = context.getRedoStack();
            if (!redoStack.isEmpty()) {
                Command command = redoStack.pop();
                command.execute();
                context.getUndoStack().push(command);

                // Marcar el mapa como modificado
                org.argentumforge.engine.utils.MapManager.markAsModified();
            }
        }
    }

    public boolean canUndo() {
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        return context != null && !context.getUndoStack().isEmpty();
    }

    public boolean canRedo() {
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        return context != null && !context.getRedoStack().isEmpty();
    }

    /**
     * Limpia el historial de comandos.
     * Útil al cargar un mapa nuevo.
     */
    public void clearHistory() {
        MapContext context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context != null) {
            context.getUndoStack().clear();
            context.getRedoStack().clear();
        }
    }
}
