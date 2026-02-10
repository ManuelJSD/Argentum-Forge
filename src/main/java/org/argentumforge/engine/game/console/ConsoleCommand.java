package org.argentumforge.engine.game.console;

/**
 * Representa un comando de consola.
 *
 * @param name           El nombre del comando (ej: "/help").
 * @param descriptionKey La clave de traducción para la descripción.
 * @param action         La acción a ejecutar.
 */
import java.util.function.Consumer;

/**
 * Representa un comando de consola con soporte para argumentos.
 *
 * @param name           El nombre del comando (ej: "/help").
 * @param descriptionKey La clave de traducción para la descripción.
 * @param action         La acción a ejecutar, recibiendo un array de
 *                       argumentos.
 */
public record ConsoleCommand(String name, String descriptionKey, Consumer<String[]> action) {
}
