package org.argentumforge.engine.gui.forms;

import org.argentumforge.engine.utils.MapContext;

/**
 * Interface for UI components that edit a specific map context.
 */
public interface IMapEditor {
    /**
     * Sets the active map context for this editor.
     * 
     * @param context The map context to edit, or null to disable editing.
     */
    void setContext(MapContext context);
}
