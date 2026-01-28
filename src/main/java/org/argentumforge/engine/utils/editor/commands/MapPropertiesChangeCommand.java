package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapProperties;

/**
 * Comando para gestionar cambios en las propiedades generales del mapa.
 */
public class MapPropertiesChangeCommand implements Command {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.map_props");
    }

    private final MapProperties oldProperties;
    private final MapProperties newProperties;

    /**
     * Crea un nuevo comando de cambio de propiedades.
     * 
     * @param oldProps Copia de las propiedades antes del cambio.
     * @param newProps Copia de las propiedades después del cambio.
     */
    public MapPropertiesChangeCommand(MapProperties oldProps, MapProperties newProps) {
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
        if (GameData.mapProperties == null)
            return;

        GameData.mapProperties.setName(source.getName());
        GameData.mapProperties.setMusicIndex(source.getMusicIndex());
        GameData.mapProperties.setMagiaSinEfecto(source.getMagiaSinEfecto());
        GameData.mapProperties.setNoEncriptarMP(source.getNoEncriptarMP());
        GameData.mapProperties.setPlayerKiller(source.getPlayerKiller());
        GameData.mapProperties.setRestringir(source.getRestringir());
        GameData.mapProperties.setBackup(source.getBackup());
        GameData.mapProperties.setZona(source.getZona());
        GameData.mapProperties.setTerreno(source.getTerreno());
    }
}
