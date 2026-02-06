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
    }
}
