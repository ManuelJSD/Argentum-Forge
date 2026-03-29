package org.argentumforge.engine.game.console;

import org.argentumforge.engine.game.console.Console.MessageType;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FHelpCommands;
import org.argentumforge.engine.game.User;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.scenes.Camera;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.i18n.I18n;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

public class ConsoleCommandProcessor {

    private static final List<ConsoleCommand> commands = new ArrayList<>();

    static {
        // --- System Commands ---
        register("/help", "command.help.desc", args -> {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.help.header"), MessageType.INFO);
            for (ConsoleCommand cmd : commands) {
                Console.INSTANCE.addMsgToConsole(cmd.name(), MessageType.INFO);
            }
            ImGUISystem.INSTANCE.show(new FHelpCommands());
        });

        register("/clear", "command.clear.desc", args -> Console.INSTANCE.clearConsole());

        register("/reloadgrh", "command.reloadgrh.desc", args -> {
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.reloadgrh.start"), Console.MessageType.INFO);
            try {
                org.argentumforge.engine.utils.editor.GrhLibraryManager.getInstance().load();
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.reloadgrh.success"), Console.MessageType.INFO);
            } catch (Exception e) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.reloadgrh.error") + e.getMessage(), Console.MessageType.ERROR);
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
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.goto.usage"), MessageType.ERROR);
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
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.goto.success") + x + ", " + y, MessageType.INFO);
            } catch (NumberFormatException e) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.goto.invalid"), MessageType.ERROR);
            }
        });

        register("/center", "command.center.desc", args -> {
            User.INSTANCE.teleport(50, 50);
            Camera cam = ((org.argentumforge.engine.scenes.GameScene) org.argentumforge.engine.Engine.getCurrentScene())
                    .getCamera();
            if (cam != null)
                cam.update(50, 50);
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.center.done"), MessageType.INFO);
        });

        register("/resetzoom", "command.resetzoom.desc", args -> {
            Camera.setTileSize(32);
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.resetzoom.done"), MessageType.INFO);
        });

        // --- Visuals Commands ---

        register("/grid", "command.grid.desc", args -> {
            boolean state = !Options.INSTANCE.getRenderSettings().isShowGrid();
            Options.INSTANCE.getRenderSettings().setShowGrid(state);
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get(state ? "console.cmd.grid.on" : "console.cmd.grid.off"), MessageType.INFO);
        });

        register("/blocks", "command.blocks.desc", args -> {
            boolean state = !Options.INSTANCE.getRenderSettings().getShowBlock();
            Options.INSTANCE.getRenderSettings().setShowBlock(state);
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get(state ? "console.cmd.blocks.on" : "console.cmd.blocks.off"), MessageType.INFO);
        });

        register("/layers", "command.layers.desc", args -> {
            boolean[] layers = Options.INSTANCE.getRenderSettings().getShowLayer();
            boolean newState = !layers[1]; // Toggle based on layer 2
            layers[1] = newState;
            layers[2] = newState;
            layers[3] = newState;
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get(newState ? "console.cmd.layers.on" : "console.cmd.layers.off"),
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

                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.mapinfo.map") + mapNum + " - " + ctx.getMapName(), MessageType.INFO);
                Console.INSTANCE.addMsgToConsole("Tamaño: " + GameData.X_MAX_MAP_SIZE + "x" + GameData.Y_MAX_MAP_SIZE,
                        MessageType.INFO);
                Console.INSTANCE.addMsgToConsole("Música ID: " + ctx.getMapProperties().getMusicIndex(),
                        MessageType.INFO);
            } else {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.mapinfo.nomap"), MessageType.WARNING);
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
            Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.screenshot.done"), MessageType.INFO);
        });

        register("/theme", "command.theme.desc", args -> {
            if (args.length < 1) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.theme.usage"), MessageType.ERROR);
                return;
            }
            String themeName = args[0].toUpperCase();
            Options.INSTANCE.setVisualTheme(themeName);
            Console.INSTANCE.addMsgToConsole(
                    I18n.INSTANCE.get("console.cmd.theme.changed") + themeName, MessageType.INFO);
        });

        register("/fill", "command.fill.desc", args -> {
            if (args.length < 2) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.fill.usage"), MessageType.ERROR);
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
                    Console.INSTANCE.addMsgToConsole(String.format(I18n.INSTANCE.get("console.cmd.fill.done"), layer, grh), MessageType.INFO);
                }
            } catch (Exception e) {
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.fill.error"), MessageType.ERROR);
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
                Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.clearblocks.done"), MessageType.INFO);
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
                    Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.error") + e.getMessage(), MessageType.ERROR);
                    org.tinylog.Logger.error(e, "Error inesperado procesando comando: " + commandName);
                }
                return;
            }
        }

        Console.INSTANCE.addMsgToConsole(I18n.INSTANCE.get("console.cmd.unknown") + commandName, MessageType.WARNING);
    }

    public static List<ConsoleCommand> getCommands() {
        return commands;
    }
}
