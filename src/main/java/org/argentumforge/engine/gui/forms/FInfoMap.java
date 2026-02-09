package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapProperties;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.audio.Sound;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.commands.MapPropertiesChangeCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    private boolean showMusicSelector = false;
    private List<File> musicFiles = new ArrayList<>();
    private List<File> filteredMusicFiles = new ArrayList<>();
    private final ImString musicFilter = new ImString(50);

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
            lastProps = GameData.mapProperties;
        }

        ImGui.setNextWindowSize(350, 300, ImGuiCond.Always);
        if (ImGui.begin(I18n.INSTANCE.get("map.info.title"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoDocking)) {

            // Name
            if (ImGui.inputText(I18n.INSTANCE.get("map.info.name"), mapName)) {
                // Editando...
            }
            if (ImGui.isItemDeactivatedAfterEdit()) {
                applyChange(p -> p.setName(mapName.get()));
            }

            ImGui.pushItemWidth(ImGui.getWindowWidth() * 0.5f);
            if (ImGui.inputInt(I18n.INSTANCE.get("map.info.music"), musicNum)) {
                // Editando...
            }
            if (ImGui.isItemDeactivatedAfterEdit()) {
                applyChange(p -> p.setMusicIndex(musicNum.get()));
            }
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button("...")) {
                scanMusicFiles();
                showMusicSelector = true;
            }

            if (showMusicSelector) {
                renderMusicSelector();
            }

            ImGui.separator();

            if (ImGui.checkbox(I18n.INSTANCE.get("map.info.noMagic"), magiaSinEfecto)) {
                applyChange(p -> p.setMagiaSinEfecto(magiaSinEfecto.get() ? 1 : 0));
            }

            if (ImGui.checkbox(I18n.INSTANCE.get("map.info.noEncrypt"), noEncriptarMP)) {
                applyChange(p -> p.setNoEncriptarMP(noEncriptarMP.get() ? 1 : 0));
            }

            if (ImGui.checkbox(I18n.INSTANCE.get("map.info.pk"), pk)) {
                applyChange(p -> p.setPlayerKiller(pk.get() ? 1 : 0));
            }

            if (ImGui.checkbox(I18n.INSTANCE.get("map.info.restrict"), restringir)) {
                applyChange(p -> p.setRestringir(restringir.get() ? 1 : 0));
            }

            ImGui.separator();

            if (ImGui.inputInt(I18n.INSTANCE.get("map.info.backup"), backupMap)) {
                // Editando...
            }
            if (ImGui.isItemDeactivatedAfterEdit()) {
                applyChange(p -> p.setBackup(backupMap.get()));
            }

            // ComboBox para Zona
            String[] zonas = { "CIUDAD", "CAMPO", "DUNGEON" };
            String currentZona = GameData.mapProperties.getZona();
            if (ImGui.beginCombo(I18n.INSTANCE.get("map.info.zone"), currentZona)) {
                for (String z : zonas) {
                    boolean isSelected = currentZona.equalsIgnoreCase(z);
                    if (ImGui.selectable(z, isSelected)) {
                        zona.set(z);
                        applyChange(p -> p.setZona(z));
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
            if (ImGui.beginCombo(I18n.INSTANCE.get("map.info.terrain"), currentTerreno)) {
                for (String t : terrenos) {
                    boolean isSelected = currentTerreno.equalsIgnoreCase(t);
                    if (ImGui.selectable(t, isSelected)) {
                        terreno.set(t);
                        applyChange(p -> p.setTerreno(t));
                    }
                    if (isSelected) {
                        ImGui.setItemDefaultFocus();
                    }
                }
                ImGui.endCombo();
            }

            ImGui.dummy(0, 10);
            ImGui.separator();

            if (ImGui.button(I18n.INSTANCE.get("common.close"), ImGui.getWindowWidth() - 15, 25)) {
                this.close();
            }

            ImGui.end();
        }
    }

    private void applyChange(java.util.function.Consumer<MapProperties> changeAction) {
        if (GameData.mapProperties == null)
            return;

        // Clone OLD state
        MapProperties oldProps = cloneProperties(GameData.mapProperties);

        // Clone NEW state (initially same as old)
        MapProperties newProps = cloneProperties(GameData.mapProperties);

        // Apply change to NEW state object
        changeAction.accept(newProps);

        // Execute Command (this will apply newProps to GameData.mapProperties)
        // Ejecutar comando
        CommandManager.getInstance()
                .executeCommand(new MapPropertiesChangeCommand(GameData.getActiveContext(), oldProps, newProps));

        // Refresh UI from the (now updated) GameData
        refreshData();
    }

    private MapProperties cloneProperties(MapProperties src) {
        MapProperties dest = new MapProperties();
        dest.setName(src.getName());
        dest.setMusicIndex(src.getMusicIndex());
        dest.setMagiaSinEfecto(src.getMagiaSinEfecto());
        dest.setNoEncriptarMP(src.getNoEncriptarMP());
        dest.setPlayerKiller(src.getPlayerKiller());
        dest.setRestringir(src.getRestringir());
        dest.setBackup(src.getBackup());
        dest.setZona(src.getZona());
        dest.setTerreno(src.getTerreno());
        return dest;
    }

    private void scanMusicFiles() {
        String path = Options.INSTANCE.getMusicPath();
        File dir = new File(path);
        musicFiles.clear();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".mp3") || lower.endsWith(".ogg") || lower.endsWith(".wav")
                        || lower.endsWith(".mid") || lower.endsWith(".midi");
            });
            if (files != null) {
                for (File f : files)
                    musicFiles.add(f);
            }
        }
        updateFilteredMusic();
    }

    private void updateFilteredMusic() {
        String filter = musicFilter.get().toLowerCase();
        filteredMusicFiles = musicFiles.stream()
                .filter(f -> f.getName().toLowerCase().contains(filter))
                .collect(Collectors.toList());
    }

    private void renderMusicSelector() {
        ImGui.setNextWindowSize(300, 400, ImGuiCond.FirstUseEver);
        ImBoolean pOpen = new ImBoolean(true);
        if (ImGui.begin(I18n.INSTANCE.get("dialog.music.title"), pOpen, ImGuiWindowFlags.NoCollapse)) {
            if (!pOpen.get()) {
                showMusicSelector = false;
            }

            if (ImGui.inputText(I18n.INSTANCE.get("common.filter"), musicFilter)) {
                updateFilteredMusic();
            }

            ImGui.beginChild("MusicList", 0, -60, true);
            for (File f : filteredMusicFiles) {
                boolean isSelected = musicNum.get() == getIndexFromFilename(f.getName());
                if (ImGui.selectable(f.getName(), isSelected)) {
                    int idx = getIndexFromFilename(f.getName());
                    musicNum.set(idx);
                    GameData.mapProperties.setMusicIndex(idx);
                }
            }
            ImGui.endChild();

            ImGui.separator();

            String selectedName = getFilenameFromIndex(musicNum.get());
            ImGui.text(I18n.INSTANCE.get("dialog.music.selected")
                    + (selectedName.isEmpty() ? musicNum.get() : selectedName));

            if (ImGui.button(I18n.INSTANCE.get("common.play"))) {
                if (!selectedName.isEmpty()) {
                    Sound.stopMusic();
                    Sound.playMusic(selectedName);
                    Sound.playMusic(selectedName);
                }
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.stop"))) {
                Sound.stopMusic();
                Sound.stopMusic();
            }

            ImGui.end();
        } else {
            showMusicSelector = false;
        }
    }

    private int getIndexFromFilename(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isDigit(c))
                sb.append(c);
            else
                break;
        }
        try {
            return sb.length() > 0 ? Integer.parseInt(sb.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getFilenameFromIndex(int index) {
        for (File f : musicFiles) {
            if (getIndexFromFilename(f.getName()) == index)
                return f.getName();
        }
        return "";
    }
}
