package org.argentumforge.engine.listeners;

import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FOptions;
import org.argentumforge.engine.game.models.Key;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.utils.MapFileUtils;
import imgui.ImGui;
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
        // Si ImGui está capturando texto (ej. en buscadores), no procesamos atajos
        // globales
        if (ImGui.getIO().getWantTextInput()) {
            return;
        }

        // Combinaciones con CONTROL
        if (KeyHandler.isKeyPressed(GLFW_KEY_LEFT_CONTROL) || KeyHandler.isKeyPressed(GLFW_KEY_RIGHT_CONTROL)) {
            handleControlShortcuts();
        } else {
            // Teclas simples (Solo si no hay Ctrl presionado para evitar conflictos)
            handleGlobalShortcuts();
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
        final Key key = Key.getKey(KeyHandler.getLastKeyPressed());
        if (key != null && KeyHandler.isActionKeyJustPressed(key)) {
            switch (key) {
                case DEBUG_SHOW:
                    ImGUISystem.INSTANCE.setShowDebug(!ImGUISystem.INSTANCE.isShowDebug());
                    break;
                case SHOW_OPTIONS:
                    ImGUISystem.INSTANCE.show(new FOptions());
                    break;
                case TOGGLE_WALKING_MODE:
                    User.INSTANCE.setWalkingmode(!User.INSTANCE.isWalkingmode());
                    break;
                case TOGGLE_GRID:
                    Options.INSTANCE.getRenderSettings()
                            .setShowGrid(!Options.INSTANCE.getRenderSettings().isShowGrid());
                    Options.INSTANCE.save();
                    break;
                case TOOL_BRUSH:
                    org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1);
                    org.argentumforge.engine.utils.editor.Surface.getInstance()
                            .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.BRUSH);
                    org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Pincel",
                            org.argentumforge.engine.game.console.FontStyle.REGULAR,
                            new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
                    break;
                case TOOL_BUCKET:
                    org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1);
                    org.argentumforge.engine.utils.editor.Surface.getInstance()
                            .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.BUCKET);
                    org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                            "Herramienta: Cubo de Relleno",
                            org.argentumforge.engine.game.console.FontStyle.REGULAR,
                            new org.argentumforge.engine.renderer.RGBColor(1f, 1f, 1f));
                    break;
                case TOOL_ERASER:
                    org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(2);
                    org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole("Herramienta: Borrar",
                            org.argentumforge.engine.game.console.FontStyle.REGULAR,
                            new org.argentumforge.engine.renderer.RGBColor(1f, 0.5f, 0.5f));
                    break;
                case TOOL_PICK:
                    org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(3);
                    org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                            "Herramienta: Capturar (Pick)",
                            org.argentumforge.engine.game.console.FontStyle.REGULAR,
                            new org.argentumforge.engine.renderer.RGBColor(0.5f, 1f, 0.5f));
                    break;
                case TOOL_MAGIC_WAND:
                    org.argentumforge.engine.utils.editor.Surface.getInstance().setMode(1); // Modo edicion
                    org.argentumforge.engine.utils.editor.Surface.getInstance()
                            .setToolMode(org.argentumforge.engine.utils.editor.Surface.ToolMode.MAGIC_WAND);
                    org.argentumforge.engine.game.console.Console.INSTANCE.addMsgToConsole(
                            "Herramienta: Varita Mágica",
                            org.argentumforge.engine.game.console.FontStyle.REGULAR,
                            new org.argentumforge.engine.renderer.RGBColor(0.8f, 0.4f, 1f));
                    break;
                case GOTO_POS:
                    ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FGoTo());
                    break;
                case MAP_PROPERTIES:
                    ImGUISystem.INSTANCE.show(new org.argentumforge.engine.gui.forms.FInfoMap());
                    break;
                default:
                    break;
            }
        }
    }
}
