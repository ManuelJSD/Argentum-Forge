package org.argentumforge;

import org.argentumforge.engine.Engine;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Clase principal que actua como punto de entrada de la aplicacion.
 * <p>
 * Esta clase contiene el metodo main que sirve como punto de inicio para la
 * ejecucion del cliente. Su unica
 * responsabilidad es instanciar el motor grafico (Engine) y ejecutar su metodo
 * {@code start()}, iniciando asi toda la
 * secuencia de arranque del juego.
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