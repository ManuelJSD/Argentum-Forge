package org.argentumforge.engine.game.console;

import org.argentumforge.engine.game.console.Console.MessageType;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FHelpCommands;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.GameData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ConsoleCommandProcessor {

    private static final List<ConsoleCommand> commands = new ArrayList<>();

    static {
        // --- System Commands ---
        register("/help", "command.help.desc", args -> {
            Console.INSTANCE.addMsgToConsole("--- Comandos Disponibles ---", MessageType.INFO);
            for (ConsoleCommand cmd : commands) {
                Console.INSTANCE.addMsgToConsole(cmd.name(), MessageType.INFO);
            }
            ImGUISystem.INSTANCE.show(new FHelpCommands());
        });

        register("/clear", "command.clear.desc", args -> Console.INSTANCE.clearConsole());

        register("/reloadgrh", "command.reloadgrh.desc", args -> {
            Console.INSTANCE.addMsgToConsole("Recargando GRH Library...", Console.MessageType.INFO);
            try {
                org.argentumforge.engine.utils.editor.GrhLibraryManager.getInstance().load();
                Console.INSTANCE.addMsgToConsole("GRH Library recargada exitosamente.", Console.MessageType.INFO);
            } catch (Exception e) {
                Console.INSTANCE.addMsgToConsole("Error al recargar GRH: " + e.getMessage(), Console.MessageType.ERROR);
            }
        });

        register("/time", "command.time.desc", args -> {
            Console.INSTANCE.addMsgToConsole(java.time.LocalTime.now().toString(), MessageType.INFO);
        });

        register("/quit", "command.quit.desc", args -> {
            org.argentumforge.engine.Engine.closeClient();
        });

        // --- Navigation Commands ---
        register("/goto", "command.goto.desc", args -> {
            if (args.length < 2) {
                Console.INSTANCE.addMsgToConsole("Uso: /goto <x> <y>", MessageType.ERROR);
                return;
            }
            try {
                int x = Integer.parseInt(args[0]);
                int y = Integer.parseInt(args[1]);
                User.INSTANCE.teleport(x, y);
                Camera cam = ((org.argentumforge.engine.scenes.GameScene) org.argentumforge.engine.Engine
                        .getCurrentScene()).getCamera();
                if (cam != null)
                    cam.update(x, y);
                Console.INSTANCE.addMsgToConsole("Teletransportado a " + x + ", " + y, MessageType.INFO);
            } catch (NumberFormatException e) {
                Console.INSTANCE.addMsgToConsole("Coordenadas inválidas.", MessageType.ERROR);
            }
        });

        register("/center", "command.center.desc", args -> {
            User.INSTANCE.teleport(50, 50);
            Camera cam = ((org.argentumforge.engine.scenes.GameScene) org.argentumforge.engine.Engine.getCurrentScene())
                    .getCamera();
            if (cam != null)
                cam.update(50, 50);
            Console.INSTANCE.addMsgToConsole("Centrado en 50, 50", MessageType.INFO);
        });

        register("/resetzoom", "command.resetzoom.desc", args -> {
            Camera.setTileSize(32);
            Console.INSTANCE.addMsgToConsole("Zoom restablecido (32px).", MessageType.INFO);
        });

        // --- Visuals Commands ---

        register("/grid", "command.grid.desc", args -> {
            boolean state = !Options.INSTANCE.getRenderSettings().isShowGrid();
            Options.INSTANCE.getRenderSettings().setShowGrid(state);
            Console.INSTANCE.addMsgToConsole("Grilla " + (state ? "Activada" : "Desactivada"), MessageType.INFO);
        });

        register("/blocks", "command.blocks.desc", args -> {
            boolean state = !Options.INSTANCE.getRenderSettings().getShowBlock();
            Options.INSTANCE.getRenderSettings().setShowBlock(state);
            Console.INSTANCE.addMsgToConsole("Bloqueos " + (state ? "Visibles" : "Ocultos"), MessageType.INFO);
        });

        register("/layers", "command.layers.desc", args -> {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            boolean newState = !layers[1]; // Toggle based on layer 2
            layers[1] = newState;
            layers[2] = newState;
            layers[3] = newState;
            Console.INSTANCE.addMsgToConsole("Capas superiores " + (newState ? "Visibles" : "Ocultas"),
                    MessageType.INFO);
        });

        // --- Info Commands ---
        register("/mapinfo", "command.mapinfo.desc", args -> {
            var ctx = GameData.getActiveContext();
            if (ctx != null) {
                String mapNum = "N/A";
                try {
                    String name = new java.io.File(ctx.getFilePath()).getName();
                    if (name.toLowerCase().startsWith("mapa")) {
                        mapNum = name.substring(4, name.lastIndexOf('.'));
                    }
                } catch (Exception ignored) {
                }

                Console.INSTANCE.addMsgToConsole("Mapa: " + mapNum + " - " + ctx.getMapName(), MessageType.INFO);
                Console.INSTANCE.addMsgToConsole("Tamaño: " + GameData.X_MAX_MAP_SIZE + "x" + GameData.Y_MAX_MAP_SIZE,
                        MessageType.INFO);
                Console.INSTANCE.addMsgToConsole("Música ID: " + ctx.getMapProperties().getMusicIndex(),
                        MessageType.INFO);
            } else {
                Console.INSTANCE.addMsgToConsole("No hay mapa cargado.", MessageType.WARNING);
            }
        });

        register("/stats", "command.stats.desc", args -> {
            var ctx = GameData.getActiveContext();
            if (ctx != null) {
                long npcCount = java.util.Arrays.stream(ctx.getMapData()).flatMap(java.util.Arrays::stream)
                        .filter(t -> t.getCharIndex() > 0).count();
                long objCount = java.util.Arrays.stream(ctx.getMapData()).flatMap(java.util.Arrays::stream)
                        .filter(t -> t.getObjGrh().getGrhIndex() > 0).count();
                Console.INSTANCE.addMsgToConsole("NPCs: " + npcCount + " | Objetos: " + objCount, MessageType.INFO);
            }
        });

        // --- System/Edit Commands ---
        register("/screenshot", "command.screenshot.desc", args -> {
            org.argentumforge.engine.utils.ScreenshotHandler.takeScreenshot();
            Console.INSTANCE.addMsgToConsole("Capturando pantalla...", MessageType.INFO);
        });

        register("/theme", "command.theme.desc", args -> {
            if (args.length < 1) {
                Console.INSTANCE.addMsgToConsole("Uso: /theme [DARK|LIGHT|MODERN|CLASSIC]", MessageType.ERROR);
                return;
            }
            String themeName = args[0].toUpperCase();
            Options.INSTANCE.setVisualTheme(themeName);
            Console.INSTANCE.addMsgToConsole(
                    "Tema cambiado a " + themeName + " (Requiere reinicio para aplicar totalmente)", MessageType.INFO);
        });

        register("/fill", "command.fill.desc", args -> {
            if (args.length < 2) {
                Console.INSTANCE.addMsgToConsole("Uso: /fill <capa 1-4> <grhIndex>", MessageType.ERROR);
                return;
            }
            try {
                int layer = Integer.parseInt(args[0]);
                int grh = Integer.parseInt(args[1]);
                var ctx = GameData.getActiveContext();
                if (ctx != null && layer >= 1 && layer <= 4) {
                    for (int x = 1; x <= 100; x++) {
                        for (int y = 1; y <= 100; y++) {
                            ctx.getMapData()[x][y].getLayer(layer).setGrhIndex(grh);
                        }
                    }
                    Console.INSTANCE.addMsgToConsole("Capa " + layer + " rellenada con Grh " + grh, MessageType.INFO);
                }
            } catch (Exception e) {
                Console.INSTANCE.addMsgToConsole("Error en argumentos.", MessageType.ERROR);
            }
        });

        register("/clearblocks", "command.clearblocks.desc", args -> {
            var ctx = GameData.getActiveContext();
            if (ctx != null) {
                for (int x = 1; x <= 100; x++) {
                    for (int y = 1; y <= 100; y++) {
                        ctx.getMapData()[x][y].setBlocked(false);
                    }
                }
                Console.INSTANCE.addMsgToConsole("Bloqueos eliminados.", MessageType.INFO);
            }
        });

    }

    public static void register(String name, String descriptionKey, Consumer<String[]> action) {
        commands.add(new ConsoleCommand(name, descriptionKey, action));
        commands.sort(Comparator.comparing(ConsoleCommand::name));
    }

    public static void process(String input) {
        String[] parts = input.split(" ");
        String commandName = parts[0];
        String[] args = new String[parts.length - 1];
        if (parts.length > 1) {
            System.arraycopy(parts, 1, args, 0, parts.length - 1);
        }

        for (ConsoleCommand cmd : commands) {
            if (cmd.name().equalsIgnoreCase(commandName)) {
                try {
                    cmd.action().accept(args);
                } catch (Exception e) {
                    Console.INSTANCE.addMsgToConsole("Error ejecutando comando: " + e.getMessage(), MessageType.ERROR);
                    e.printStackTrace();
                }
                return;
            }
        }

        Console.INSTANCE.addMsgToConsole("Desconocido: " + commandName, MessageType.WARNING);
    }

    public static List<ConsoleCommand> getCommands() {
        return commands;
    }
}
