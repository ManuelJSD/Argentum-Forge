package org.argentumforge.engine.listeners;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.game.EditorController;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FOptions;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.utils.MapFileUtils;
import imgui.ImGui;
import static org.argentumforge.engine.game.console.FontStyle.REGULAR;
import static org.lwjgl.glfw.GLFW.*;

/**
 * Centraliza la gestión de atajos de teclado del editor.
 */
public class ShortcutManager {

    private static final ShortcutManager INSTANCE = new ShortcutManager();

    public static ShortcutManager getInstance() {
        return INSTANCE;
    }

    private ShortcutManager() {
    }

    /**
     * Procesa los atajos de teclado. Debe llamarse en cada frame desde la escena
     * activa.
     */
    public void update() {
        // Si ImGui está capturando texto (ej. en buscadores) o estamos configurando
        // teclas, no procesamos atajos
        if (ImGui.getIO().getWantTextInput() || Key.checkIsBinding()
                || ImGUISystem.INSTANCE.isFormVisible("FBindKeys")) {
            return;
        }

        // Combinaciones con CONTROL
        if (KeyHandler.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || KeyHandler.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            handleControlShortcuts();
        } else {
            // Teclas simples (Solo si no hay Ctrl presionado para evitar conflictos)
            handleGlobalShortcuts();
        }

        // ESCAPE (Siempre verificado para cancelar modos)
        if (KeyHandler.isKeyJustPressed(GLFW_KEY_ESCAPE)) {
            if (EditorController.INSTANCE.isPasteModeActive()) {
                EditorController.INSTANCE.setPasteModeActive(false);
                Console.INSTANCE.addMsgToConsole("Modo Pegar cancelado.", REGULAR, new RGBColor(1f, 1f, 1f));
            }
        }
    }

    private void handleControlShortcuts() {
        // Deshacer: Ctrl + Z
        if (KeyHandler.isKeyJustPressed(GLFW_KEY_Z)) {
            CommandManager.getInstance().undo();
        }
        // Rehacer: Ctrl + Y
        else if (KeyHandler.isKeyJustPressed(GLFW_KEY_Y)) {
            CommandManager.getInstance().redo();
        }
        // Reset Zoom: Ctrl + 0
        else if (KeyHandler.isKeyJustPressed(GLFW_KEY_0)) {
            Camera.setTileSize(32);
        }
        // Copiar: Ctrl + C (Ahora configurable)
        else if (KeyHandler.isActionKeyJustPressed(Key.COPY)) {
            EditorController.INSTANCE.copySelection();
        }
        // Cortar: Ctrl + X (Ahora configurable)
        else if (KeyHandler.isActionKeyJustPressed(Key.CUT)) {
            EditorController.INSTANCE.cutSelection();
        }
        // Pegar: Ctrl + V / Ctrl + Shift + V (Ahora configurable)
        else if (KeyHandler.isActionKeyJustPressed(Key.PASTE)
                || KeyHandler.isActionKeyJustPressed(Key.PASTE_ADVANCED)) {
            boolean isShiftPressed = KeyHandler.isKeyPressed(GLFW_KEY_LEFT_SHIFT)
                    || KeyHandler.isKeyPressed(GLFW_KEY_RIGHT_SHIFT);

            // Si las teclas son diferentes, respetamos el bindeo exacto
            if (Key.PASTE.getKeyCode() != Key.PASTE_ADVANCED.getKeyCode()) {
                if (KeyHandler.isActionKeyJustPressed(Key.PASTE_ADVANCED)) {
                    ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FPasteAdvanced());
                } else {
                    EditorController.INSTANCE.pasteSelection();
                }
            } else {
                // Si son la misma tecla (por defecto 'V'), usamos Shift para diferenciar
                if (isShiftPressed) {
                    ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FPasteAdvanced());
                } else {
                    EditorController.INSTANCE.pasteSelection();
                }
            }
        }
        // Guardar: Ctrl + S / Ctrl + Shift + S
        else if (KeyHandler.isKeyJustPressed(GLFW_KEY_S)) {
            if (KeyHandler.isKeyPressed(GLFW_KEY_LEFT_SHIFT) || KeyHandler.isKeyPressed(GLFW_KEY_RIGHT_SHIFT)) {
                MapFileUtils.saveMapAs();
            } else {
                MapFileUtils.quickSaveMap();
            }
        }
        // Nuevo Mapa: Ctrl + N
        else if (KeyHandler.isActionKeyJustPressed(Key.NEW_MAP)) {
            org.argentumforge.engine.game.EditorController.INSTANCE.newMap();
        }
        // Abrir Mapa: Ctrl + O
        else if (KeyHandler.isActionKeyJustPressed(Key.OPEN_MAP)) {
            org.argentumforge.engine.game.EditorController.INSTANCE.loadMapAction();
        }
    }

    private void handleGlobalShortcuts() {
        // Usando el sistema de bindeos para teclas de acción (Debug, etc)
        // Verificar cada tecla de atajo independientemente para permitir bindeos
        // duplicados
        if (KeyHandler.isActionKeyJustPressed(Key.DEBUG_SHOW)) {
            ImGUISystem.INSTANCE.setShowDebug(!ImGUISystem.INSTANCE.isShowDebug());
        }
        // Eliminar selección (Configurable)
        if (KeyHandler.isActionKeyJustPressed(Key.DELETE)) {
            EditorController.INSTANCE.deleteSelection();
        }
        if (KeyHandler.isActionKeyJustPressed(Key.SHOW_OPTIONS)) {
            ImGUISystem.INSTANCE.show(new FOptions());
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_WALKING_MODE)) {
            User.INSTANCE.setWalkingmode(!User.INSTANCE.isWalkingmode());
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_GRID)) {
            Options.INSTANCE.getRenderSettings().setShowGrid(!Options.INSTANCE.getRenderSettings().isShowGrid());
            Options.INSTANCE.save();
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_PHOTO_MODE)) {
            Options.INSTANCE.getRenderSettings()
                    .setPhotoModeActive(!Options.INSTANCE.getRenderSettings().isPhotoModeActive());
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_VIEWPORT)) {
            Options.INSTANCE.getRenderSettings()
                    .setShowViewportOverlay(!Options.INSTANCE.getRenderSettings().isShowViewportOverlay());
            Options.INSTANCE.save();
        }

        // Capas (Toggles)
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_LAYER_1)) {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            layers[0] = !layers[0];
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Capa 1: " + (layers[0] ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_LAYER_2)) {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            layers[1] = !layers[1];
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Capa 2: " + (layers[1] ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_LAYER_3)) {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            layers[2] = !layers[2];
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Capa 3: " + (layers[2] ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_LAYER_4)) {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            layers[3] = !layers[3];
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Capa 4: " + (layers[3] ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }

        // Visibilidad (Toggles)
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_BLOCKS)) {
            boolean current = Options.INSTANCE.getRenderSettings().getShowBlock();
            Options.INSTANCE.getRenderSettings().setShowBlock(!current);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Bloqueos: " + (!current ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_TRANSFERS)) {
            boolean current = Options.INSTANCE.getRenderSettings().getShowMapTransfer();
            Options.INSTANCE.getRenderSettings().setShowMapTransfer(!current);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Traslados: " + (!current ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_TRIGGERS)) {
            boolean current = Options.INSTANCE.getRenderSettings().getShowTriggers();
            Options.INSTANCE.getRenderSettings().setShowTriggers(!current);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Triggers: " + (!current ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_NPCS)) {
            boolean current = Options.INSTANCE.getRenderSettings().getShowNPCs();
            Options.INSTANCE.getRenderSettings().setShowNPCs(!current);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("NPCs: " + (!current ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOGGLE_OBJECTS)) {
            boolean current = Options.INSTANCE.getRenderSettings().getShowOJBs();
            Options.INSTANCE.getRenderSettings().setShowOJBs(!current);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                    "Objetos: " + (!current ? "ON" : "OFF"),
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }

        if (KeyHandler.isActionKeyJustPressed(Key.TOOL_BRUSH)) {
            org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1);
            org.argentumforge.engine.utils.editor.Surface.getInstance()
                    .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.BRUSH);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Pincel",
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOOL_BUCKET)) {
            org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1);
            org.argentumforge.engine.utils.editor.Surface.getInstance()
                    .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.BUCKET);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Cubo de Relleno",
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOOL_ERASER)) {
            org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(2);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Borrar",
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(1f, 0.5f, 0.5f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOOL_PICK)) {
            org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(3);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Capturar (Pick)",
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(0.5f, 1f, 0.5f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TOOL_MAGIC_WAND)) {
            org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1);
            org.argentumforge.engine.utils.editor.Surface.getInstance()
                    .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.MAGIC_WAND);
            org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Varita Mágica",
                    org.argentumforge.engine.game.console.FontStyle.REGULAR,
                    new org.argentumforge.engine.renderer.RGBColor(0.8f, 0.4f, 1f));
        }
        if (KeyHandler.isActionKeyJustPressed(Key.GOTO_POS)) {
            ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FGoTo());
        }
        if (KeyHandler.isActionKeyJustPressed(Key.MAP_PROPERTIES)) {
            ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FInfoMap());
        }
        if (KeyHandler.isActionKeyJustPressed(Key.TAKE_SCREENSHOT)) {
            org.argentumforge.engine.utils.ScreenshotUtils.takeScreenshot();
        }

        if (KeyHandler.isActionKeyJustPressed(Key.VALIDATE_MAP)) {
            if (org.argentumforge.engine.utils.GameData.getActiveContext() != null) {
                ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FMapValidator());
            } else {
                org.argentumforge.engine.gui.DialogManager.getInstance().showInfo("Mapa",
                        org.argentumforge.engine.i18n.I18n.INSTANCE.get("msg.noActiveMap"));
            }
        }

        if (KeyHandler.isActionKeyJustPressed(Key.ZOOM_IN)) {
            Camera.setTileSize(Math.min(128, Camera.TILE_PIXEL_SIZE + 16));
        }

        if (KeyHandler.isActionKeyJustPressed(Key.ZOOM_OUT)) {
            Camera.setTileSize(Math.max(16, Camera.TILE_PIXEL_SIZE - 16));
        }
    }
}
