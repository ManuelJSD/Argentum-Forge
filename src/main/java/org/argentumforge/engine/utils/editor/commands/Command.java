package org.argentumforge.engine.utils.editor.commands;

/**
 * Interfaz base para todas las acciones editables en el mapa.
 * Permite ejecutar y deshacer cambios.
 */
public interface Command {
    /**
     * Ejecuta la acción o la vuelve a aplicar (Redo).
     */
    void execute();

    /**
     * Deshace la acción (Undo).
     */
    void undo();
}
