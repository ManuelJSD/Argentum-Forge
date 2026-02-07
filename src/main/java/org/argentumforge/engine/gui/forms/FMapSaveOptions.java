package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapManager;
import org.argentumforge.engine.utils.MapManager.MapSaveOptions;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.MapFormat;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.gui.FileDialog;
import org.argentumforge.engine.game.Options;
import java.io.File;

public class FMapSaveOptions extends Form {

    private final MapSaveOptions options;
    private final ImInt preset = new ImInt(3); // 3 = Custom
    private final ImInt version = new ImInt(1);
    private final ImBoolean header = new ImBoolean(true);
    private final ImBoolean longIndices = new ImBoolean(false);

    // Callback to execute after successful save
    private Runnable onSuccess;
    // Callback to execute if cancelled
    private Runnable onCancel;

    public FMapSaveOptions(MapSaveOptions initialOptions, Runnable onSuccess, Runnable onCancel) {
        this.options = initialOptions != null ? initialOptions : MapSaveOptions.standard();
        this.onSuccess = onSuccess;
        this.onCancel = onCancel;

        // Load initial state
        this.version.set(this.options.getVersion());
        this.header.set(this.options.isIncludeHeader());
        this.longIndices.set(this.options.isUseLongIndices());

        detectPreset();
    }

    public FMapSaveOptions(MapSaveOptions initialOptions, Runnable onSuccess) {
        this(initialOptions, onSuccess, null);
    }

    private void detectPreset() {
        if (options.getFormatType() == MapManager.MapFormatType.V1_LEGACY) {
            preset.set(3); // V1 Legacy
        } else if (version.get() == 1 && !longIndices.get() && header.get()) {
            preset.set(0); // Standard
        } else if (version.get() == 1 && longIndices.get() && header.get()) {
            preset.set(1); // Extended
        } else if (version.get() == 136 && longIndices.get() && header.get()) {
            preset.set(2); // AOLibre
        } else {
            preset.set(4); // Custom
        }
    }

    private void applyPreset(int idx) {
        if (idx == 0) { // Standard
            options.setFormatType(MapManager.MapFormatType.V2_STANDARD);
            version.set(1);
            header.set(true);
            longIndices.set(false);
        } else if (idx == 1) { // Extended
            options.setFormatType(MapManager.MapFormatType.V2_STANDARD);
            version.set(1);
            header.set(true);
            longIndices.set(true);
        } else if (idx == 2) { // AOLibre
            options.setFormatType(MapManager.MapFormatType.V2_STANDARD);
            version.set(136);
            header.set(true);
            longIndices.set(true);
        } else if (idx == 3) { // V1 Legacy
            options.setFormatType(MapManager.MapFormatType.V1_LEGACY);
            version.set(1);
            header.set(true);
            longIndices.set(false);
        }
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(400, 300, imgui.flag.ImGuiCond.Appearing);
        int flags = ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoDocking;

        if (ImGui.begin("Opciones de Guardado", flags)) {

            ImGui.text("Preajuste:");
            String[] presets = {
                    I18n.INSTANCE.get("map.save.format.standard"),
                    I18n.INSTANCE.get("map.save.format.extended"),
                    I18n.INSTANCE.get("map.save.format.aolibre"),
                    "V1 Legacy (0.99z/0.11.2)",
                    I18n.INSTANCE.get("map.save.option.custom")
            };

            if (ImGui.combo("##preset", preset, presets)) {
                applyPreset(preset.get());
            }

            ImGui.separator();

            ImGui.text("Versión:");
            if (ImGui.inputInt("##version", version)) {
                if (version.get() < 0)
                    version.set(0);
                if (version.get() > 32767)
                    version.set(32767);
                preset.set(4); // To Custom
            }

            ImGui.dummy(0, 5);

            if (ImGui.checkbox("Incluir Cabecera (273 bytes)", header)) {
                preset.set(4); // To Custom
            }

            ImGui.dummy(0, 5);
            ImGui.text("Tamaño de Índices:");
            if (ImGui.radioButton("Integer (2 Bytes)", !longIndices.get())) {
                longIndices.set(false);
                preset.set(4); // To Custom
            }
            ImGui.sameLine();
            if (ImGui.radioButton("Long (4 Bytes)", longIndices.get())) {
                longIndices.set(true);
                preset.set(4); // To Custom
            }

            ImGui.dummy(0, 20);
            ImGui.separator();
            ImGui.dummy(0, 10);

            // Buttons
            float width = ImGui.getWindowWidth();
            float btnWidth = 130;
            float btnHeight = 30;
            float spacing = 20;

            ImGui.setCursorPosX((width - (btnWidth * 2 + spacing)) / 2);

            // Save Button (Blue)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.2f, 0.4f, 0.8f, 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, 0.3f, 0.5f, 0.9f, 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonActive, 0.1f, 0.3f, 0.7f, 1.0f);
            if (ImGui.button("Guardar", btnWidth, btnHeight)) {
                requestSave();
            }
            ImGui.popStyleColor(3);

            ImGui.sameLine(0, spacing);

            // Cancel Button (Red)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0.8f, 0.2f, 0.2f, 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonHovered, 0.9f, 0.3f, 0.3f, 1.0f);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.ButtonActive, 0.7f, 0.1f, 0.1f, 1.0f);
            if (ImGui.button("Cancelar", btnWidth, btnHeight)) {
                if (onCancel != null)
                    onCancel.run();
                this.close();
            }
            ImGui.popStyleColor(3);

            ImGui.end();
        }
    }

    private void requestSave() {
        // Update Options object
        options.setVersion((short) version.get());
        options.setIncludeHeader(header.get());
        options.setUseLongIndices(longIndices.get());

        // Close FIRST to remove from UI stack, then open File Dialog
        this.close();

        // Execute File Dialog logic immediately
        String lastPath = Options.INSTANCE.getLastMapPath();
        if (lastPath == null || lastPath.isEmpty()) {
            lastPath = new File(".").getAbsolutePath() + File.separator;
        } else {
            if (!lastPath.endsWith(File.separator)) {
                lastPath += File.separator;
            }
        }

        // Prioritize Context format, fallback to Global
        MapFormat targetFormat = MapManager.getActiveFormat();
        MapContext context = GameData.getActiveContext();
        if (context != null && context.getMapFormat() != null) {
            targetFormat = context.getMapFormat();
        }

        String ext = targetFormat.getExtension();
        String desc = targetFormat.getDescription();
        String filter = "*" + ext;

        String selectedFile = FileDialog.showSaveDialog(
                I18n.INSTANCE.get("menu.file.saveAs"),
                lastPath + "mapa" + ext,
                desc + " (" + filter + ")",
                filter);

        if (selectedFile != null) {
            if (!selectedFile.toLowerCase().endsWith(ext)) {
                selectedFile += ext;
            }

            Options.INSTANCE.setLastMapPath(new File(selectedFile).getParent());
            Options.INSTANCE.save();

            // Perform Save
            GameData.saveMap(selectedFile, options);

            // Trigger Callback
            if (onSuccess != null) {
                onSuccess.run();
            }
        } else {
            // User cancelled the file dialog
            if (onCancel != null)
                onCancel.run();
        }
    }
}
