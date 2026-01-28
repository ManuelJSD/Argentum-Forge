package org.argentumforge;

import org.argentumforge.engine.Engine;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Punto de entrada de la aplicación.
 * Inicializa el manejo de errores global y arranca el motor gráfico
 * ({@link Engine}).
 */

public class Main {

    public static void main(String[] args) {
        // Establecer manejador de excepciones global para mostrar errores visualmente
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            showErrorDialog(throwable);
        });

        try {
            new Engine().start();
        } catch (Throwable t) {
            showErrorDialog(t);
        }
    }

    private static void showErrorDialog(Throwable t) {
        t.printStackTrace(); // Imprimir en consola para depuración
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        System.err.println("CRITICAL ERROR: " + t.getMessage());
        System.err.println(sw.toString());

        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("crash.log"),
                    "CRITICAL ERROR: " + t.getMessage() + "\n" + sw.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(1);
    }

}