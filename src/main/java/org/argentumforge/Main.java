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
        String stackTrace = sw.toString();

        javax.swing.JOptionPane.showMessageDialog(null,
                "Se ha producido un error crítico en el editor:\n\n" + t.getMessage() +
                        "\n\nStack Trace:\n" + stackTrace.substring(0, Math.min(stackTrace.length(), 1000)),
                "Error Crítico - Argentum Forge",
                javax.swing.JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

}