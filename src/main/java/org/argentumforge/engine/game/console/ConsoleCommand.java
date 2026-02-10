package org.argentumforge.engine.game.console;

/**
 * Representa un comando de consola.
 *
 * @param name           El nombre del comando (ej: "/help").
 * @param descriptionKey La clave de traducción para la descripción.
 * @param action         La acción a ejecutar.
 */
public record ConsoleCommand(String name, String descriptionKey, Runnable action) {
}
