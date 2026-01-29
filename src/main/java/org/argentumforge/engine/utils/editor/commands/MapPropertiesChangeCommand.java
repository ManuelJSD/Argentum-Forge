package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;

import org.argentumforge.engine.utils.inits.MapProperties;

/**
 * Comando para gestionar cambios en las propiedades generales del mapa.
 */
public class MapPropertiesChangeCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.map_props");
    }

    private final MapProperties oldProperties;
    private final MapProperties newProperties;

    /**
     * Crea un nuevo comando de cambio de propiedades.
     * 
     * @param context  Contexto del mapa.
     * @param oldProps Copia de las propiedades antes del cambio.
     * @param newProps Copia de las propiedades después del cambio.
     */
    public MapPropertiesChangeCommand(org.argentumforge.engine.utils.MapContext context, MapProperties oldProps,
            MapProperties newProps) {
        super(context);
        this.oldProperties = oldProps;
        this.newProperties = newProps;
    }

    @Override
    public void execute() {
        applyProperties(newProperties);
    }

    @Override
    public void undo() {
        applyProperties(oldProperties);
    }

    private void applyProperties(MapProperties source) {
        var mapProperties = context.getMapProperties();
        if (mapProperties == null)
            return;

        mapProperties.setName(source.getName());
        mapProperties.setMusicIndex(source.getMusicIndex());
        mapProperties.setMagiaSinEfecto(source.getMagiaSinEfecto());
        mapProperties.setNoEncriptarMP(source.getNoEncriptarMP());
        mapProperties.setPlayerKiller(source.getPlayerKiller());
        mapProperties.setRestringir(source.getRestringir());
        mapProperties.setBackup(source.getBackup());
        mapProperties.setZona(source.getZona());
        mapProperties.setTerreno(source.getTerreno());
    }
}
