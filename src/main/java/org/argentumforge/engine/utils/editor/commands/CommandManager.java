package org.argentumforge.engine.utils.editor.commands;

import java.util.Stack;

/**
 * Gestor de comandos que implementa la lógica de Deshacer/Rehacer.
 * Mantiene dos pilas para rastrear el historial de acciones del usuario.
 */
public class CommandManager {

    private static CommandManager instance;
    private static final int MAX_STACK_SIZE = 100;

    private final Stack<Command> undoStack = new Stack<>();
    private final Stack<Command> redoStack = new Stack<>();

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
        undoStack.push(command);
        redoStack.clear();

        if (undoStack.size() > MAX_STACK_SIZE) {
            undoStack.remove(0);
        }
    }

    /**
     * Deshace la última acción realizada.
     */
    public void undo() {
        if (!undoStack.isEmpty()) {
            Command command = undoStack.pop();
            command.undo();
            redoStack.push(command);
        }
    }

    /**
     * Rehace la última acción deshecha.
     */
    public void redo() {
        if (!redoStack.isEmpty()) {
            Command command = redoStack.pop();
            command.execute();
            undoStack.push(command);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Limpia el historial de comandos.
     * Útil al cargar un mapa nuevo.
     */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
    }
}
