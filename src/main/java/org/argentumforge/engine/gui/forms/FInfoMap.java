package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapProperties;

/**
 * Ventana de edición para las propiedades generales del mapa actual.
 * <p>
 * Permite visualizar y modificar en tiempo real los metadatos cargados
 * desde el archivo .dat del mapa.
 */
public final class FInfoMap extends Form {

    private final ImString mapName = new ImString(100);
    private final ImInt musicNum = new ImInt();
    private final ImBoolean magiaSinEfecto = new ImBoolean();
    private final ImBoolean noEncriptarMP = new ImBoolean();
    private final ImBoolean pk = new ImBoolean();
    private final ImBoolean restringir = new ImBoolean();
    private final ImInt backupMap = new ImInt();
    private final ImString zona = new ImString(100);
    private final ImString terreno = new ImString(100);

    /** Referencia a las propiedades cargadas para detectar cambios. */
    private MapProperties lastProps;

    public FInfoMap() {
        refreshData();
    }

    /**
     * Sincroniza los campos de ImGui con las propiedades actuales en GameData.
     */
    private void refreshData() {
        MapProperties props = GameData.mapProperties;
        if (props == null)
            return;

        mapName.set(props.getName());
        musicNum.set(props.getMusicIndex());
        magiaSinEfecto.set(props.getMagiaSinEfecto() == 1);
        noEncriptarMP.set(props.getNoEncriptarMP() == 1);
        pk.set(props.getPlayerKiller() == 1);
        restringir.set(props.getRestringir() == 1);
        backupMap.set(props.getBackup());
        zona.set(props.getZona());
        terreno.set(props.getTerreno());
    }

    @Override
    public void render() {
        // Auto-refresh si cambia la instancia de propiedades (nuevo mapa cargado)
        if (lastProps != GameData.mapProperties) {
            refreshData();
        }

        ImGui.setNextWindowSize(350, 300, ImGuiCond.Always);
        if (ImGui.begin("Información del Mapa", ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            if (ImGui.inputText("Nombre", mapName)) {
                GameData.mapProperties.setName(mapName.get());
            }

            if (ImGui.inputInt("Música #", musicNum)) {
                GameData.mapProperties.setMusicIndex(musicNum.get());
            }

            ImGui.separator();

            if (ImGui.checkbox("Magia Sin Efecto", magiaSinEfecto)) {
                GameData.mapProperties.setMagiaSinEfecto(magiaSinEfecto.get() ? 1 : 0);
            }

            if (ImGui.checkbox("No Encriptar MP", noEncriptarMP)) {
                GameData.mapProperties.setNoEncriptarMP(noEncriptarMP.get() ? 1 : 0);
            }

            if (ImGui.checkbox("Player Killer (PK)", pk)) {
                GameData.mapProperties.setPlayerKiller(pk.get() ? 1 : 0);
            }

            if (ImGui.checkbox("Restringir", restringir)) {
                GameData.mapProperties.setRestringir(restringir.get() ? 1 : 0);
            }

            ImGui.separator();

            if (ImGui.inputInt("Backup Map", backupMap)) {
                GameData.mapProperties.setBackup(backupMap.get());
            }

            // ComboBox para Zona
            String[] zonas = { "CIUDAD", "CAMPO", "DUNGEON" };
            String currentZona = GameData.mapProperties.getZona();
            if (ImGui.beginCombo("Zona", currentZona)) {
                for (String z : zonas) {
                    boolean isSelected = currentZona.equalsIgnoreCase(z);
                    if (ImGui.selectable(z, isSelected)) {
                        GameData.mapProperties.setZona(z);
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }

            // ComboBox para Terreno
            String[] terrenos = { "NIEVE", "DESIERTO", "BOSQUE" };
            String currentTerreno = GameData.mapProperties.getTerreno();
            if (ImGui.beginCombo("Terreno", currentTerreno)) {
                for (String t : terrenos) {
                    boolean isSelected = currentTerreno.equalsIgnoreCase(t);
                    if (ImGui.selectable(t, isSelected)) {
                        GameData.mapProperties.setTerreno(t);
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }

            ImGui.dummy(0, 10);
            ImGui.separator();

            if (ImGui.button("Cerrar", ImGui.getWindowWidth() - 15, 25)) {
                this.close();
            }

            ImGui.end();
        }
    }
}
