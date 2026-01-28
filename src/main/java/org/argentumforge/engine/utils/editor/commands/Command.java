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

    /**
     * Obtiene el nombre legible del comando para el historial.
     * 
     * @return Nombre localizado del comando.
     */
    String getName();

    /**
     * Obtiene los límites del área afectada por este comando.
     * 
     * @return Un array int[] {minX, minY, maxX, maxY} o null si no aplica.
     */
    default int[] getAffectedBounds() {
        return null;
    }
}
