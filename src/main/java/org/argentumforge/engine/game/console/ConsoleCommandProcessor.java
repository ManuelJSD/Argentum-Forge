package org.argentumforge.engine.game.console;

import org.argentumforge.engine.game.console.Console.MessageType;
import org.argentumforge.engine.gui.ImGUISystem;
import org.argentumforge.engine.gui.forms.FHelpCommands;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ConsoleCommandProcessor {

    private static final List<ConsoleCommand> commands = new ArrayList<>();

    static {
        register("/help", "command.help.desc", () -> {
            Console.INSTANCE.addMsgToConsole("--- Comandos Disponibles ---", MessageType.INFO);
            for (ConsoleCommand cmd : commands) {
                // Idealmente, aquí también se traduciría, pero para la consola rápida
                // mostramos el nombre
                Console.INSTANCE.addMsgToConsole(cmd.name(), MessageType.INFO);
            }
            ImGUISystem.INSTANCE.show(new FHelpCommands());
        });

        register("/clear", "command.clear.desc", Console.INSTANCE::clearConsole);

        register("/time", "command.time.desc", () -> {
            Console.INSTANCE.addMsgToConsole(java.time.LocalTime.now().toString(), MessageType.INFO);
        });

        register("/quit", "command.quit.desc", () -> {
            org.argentumforge.engine.Engine.closeClient();
        });
    }

    public static void register(String name, String descriptionKey, Runnable action) {
        commands.add(new ConsoleCommand(name, descriptionKey, action));
        commands.sort(Comparator.comparing(ConsoleCommand::name));
    }

    public static void process(String input) {
        String commandName = input.split(" ")[0];

        for (ConsoleCommand cmd : commands) {
            if (cmd.name().equalsIgnoreCase(commandName)) {
                try {
                    cmd.action().run();
                } catch (Exception e) {
                    Console.INSTANCE.addMsgToConsole("Error ejecutando comando: " + e.getMessage(), MessageType.ERROR);
                    e.printStackTrace();
                }
                return;
            }
        }

        Console.INSTANCE.addMsgToConsole("Comando no reconocido: " + commandName, MessageType.WARNING);
    }

    public static List<ConsoleCommand> getCommands() {
        return commands;
    }
}
