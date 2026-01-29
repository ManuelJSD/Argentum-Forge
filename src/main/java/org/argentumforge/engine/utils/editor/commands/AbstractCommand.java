package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.utils.MapContext;

/**
 * Clase base para comandos que operan sobre un contexto de mapa específico.
 * Asegura que el comando afecte al mapa correcto incluso si el contexto activo
 * cambia.
 */
public abstract class AbstractCommand implements Command {

    protected final MapContext context;

    public AbstractCommand(MapContext context) {
        this.context = context;
    }

    public MapContext getContext() {
        return context;
    }
}
