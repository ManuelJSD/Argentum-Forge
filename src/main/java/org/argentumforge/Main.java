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

        // Generar mensaje descriptivo según el tipo de error
        String userMessage = buildUserFriendlyMessage(t);
        String technicalDetails = sw.toString();

        String crashContent = buildCrashReport(userMessage, t, technicalDetails);

        System.err.println(crashContent);

        try {
            java.nio.file.Files.writeString(
                    java.nio.file.Path.of("crash.log"),
                    crashContent);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(1);
    }

    /**
     * Traduce excepciones técnicas a mensajes comprensibles para el usuario.
     * Cada causa conocida genera una explicación con pasos de solución.
     */
    private static String buildUserFriendlyMessage(Throwable t) {
        String message = t.getMessage() != null ? t.getMessage() : "";
        String className = t.getClass().getSimpleName();

        // --- NullPointerException con contexto descriptivo de JVM ---
        if (t instanceof NullPointerException) {
            if (message.contains("AssetRegistry.grhData")) {
                return "ERROR DE RECURSOS: No se pudieron cargar los datos gráficos (Graficos.ind).\n"
                        + "CAUSA PROBABLE: La ruta de archivos INIT está mal configurada o el archivo está corrupto/ausente.\n"
                        + "SOLUCIÓN: Verifique que la ruta de INIT en su perfil apunte a una carpeta que contenga 'Graficos.ind'.";
            }
            if (message.contains("AssetRegistry.bodyData")) {
                return "ERROR DE RECURSOS: No se pudieron cargar los datos de cuerpos (Cuerpos.ind / Personajes.ind).\n"
                        + "CAUSA PROBABLE: El archivo de cuerpos no se encontró en la ruta de INIT.\n"
                        + "SOLUCIÓN: Verifique que la carpeta INIT contenga 'Cuerpos.ind' o 'Personajes.ind'.";
            }
            if (message.contains("AssetRegistry.headData")) {
                return "ERROR DE RECURSOS: No se pudieron cargar los datos de cabezas (Cabezas.ind).\n"
                        + "CAUSA PROBABLE: El archivo de cabezas no se encontró en la ruta de INIT.\n"
                        + "SOLUCIÓN: Verifique que la carpeta INIT contenga 'Cabezas.ind'.";
            }
            if (message.contains("AssetRegistry")) {
                return "ERROR DE RECURSOS: Los datos del juego no se cargaron correctamente.\n"
                        + "CAUSA PROBABLE: La configuración de rutas (INIT/DATS) es incorrecta o los archivos están corruptos.\n"
                        + "SOLUCIÓN: Elimine su perfil y vuelva a configurar las rutas correctamente mediante el asistente.";
            }
            if (message.contains("mapData")) {
                return "ERROR DE MAPA: Los datos del mapa no están disponibles.\n"
                        + "CAUSA PROBABLE: El mapa no se cargó correctamente o se intentó renderizar antes de la carga.\n"
                        + "SOLUCIÓN: Intente reiniciar la aplicación. Si persiste, verifique que el archivo del mapa no esté corrupto.";
            }
        }

        // --- Errores de OpenGL / LWJGL ---
        if (className.contains("GLFW") || message.contains("GLFW") || message.contains("OpenGL")) {
            return "ERROR GRÁFICO: No se pudo inicializar el sistema gráfico (OpenGL/GLFW).\n"
                    + "CAUSA PROBABLE: Los drivers de la tarjeta gráfica están desactualizados o no soportan OpenGL 3.3+.\n"
                    + "SOLUCIÓN: Actualice los drivers de su tarjeta gráfica.";
        }

        // --- Errores de memoria ---
        if (t instanceof OutOfMemoryError) {
            return "ERROR DE MEMORIA: La aplicación se quedó sin memoria disponible.\n"
                    + "CAUSA PROBABLE: Mapa muy grande, demasiados gráficos cargados, o poca RAM del sistema.\n"
                    + "SOLUCIÓN: Cierre otras aplicaciones y reinicie. Si persiste, aumente la memoria con -Xmx en las opciones de Java.";
        }

        // --- Errores de archivos ---
        if (t instanceof java.io.IOException) {
            return "ERROR DE ARCHIVO: No se pudo leer o escribir un archivo necesario.\n"
                    + "CAUSA PROBABLE: Permisos insuficientes, disco lleno, o archivo bloqueado por otro programa.\n"
                    + "SOLUCIÓN: Verifique que tiene permisos de escritura en la carpeta de la aplicación y que el disco no esté lleno.";
        }

        // --- Mensaje genérico mejorado ---
        return "ERROR INESPERADO: " + className + "\n"
                + "DETALLE: " + (message.isEmpty() ? "(sin mensaje)" : message) + "\n"
                + "Si este error persiste, por favor repórtelo en: https://github.com/Lorwp/Argentum-Forge/issues";
    }

    /**
     * Genera un informe de crash completo con información del sistema.
     */
    private static String buildCrashReport(String userMessage, Throwable t, String stackTrace) {
        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════════════════════════\n");
        sb.append("  ARGENTUM FORGE - INFORME DE ERROR\n");
        sb.append("  Fecha: ").append(java.time.LocalDateTime.now()).append("\n");
        sb.append("═══════════════════════════════════════════════════════════════\n\n");

        sb.append("▶ RESUMEN PARA EL USUARIO:\n");
        sb.append(userMessage).append("\n\n");

        sb.append("▶ INFORMACIÓN DEL SISTEMA:\n");
        sb.append("  OS: ").append(System.getProperty("os.name"))
                .append(" v").append(System.getProperty("os.version"))
                .append(" [").append(System.getProperty("os.arch")).append("]\n");
        sb.append("  Java: ").append(System.getProperty("java.version"))
                .append(" (").append(System.getProperty("java.vendor")).append(")\n");
        Runtime rt = Runtime.getRuntime();
        sb.append("  Memoria: ").append(rt.totalMemory() / 1024 / 1024).append("MB total, ")
                .append(rt.freeMemory() / 1024 / 1024).append("MB libre, ")
                .append(rt.maxMemory() / 1024 / 1024).append("MB max\n\n");

        sb.append("▶ DETALLES TÉCNICOS:\n");
        sb.append("  Excepción: ").append(t.getClass().getName()).append("\n");
        sb.append("  Mensaje: ").append(t.getMessage()).append("\n\n");

        sb.append("▶ TRAZA COMPLETA:\n");
        sb.append(stackTrace);

        sb.append("\n═══════════════════════════════════════════════════════════════\n");
        sb.append("  Si necesita ayuda, comparta este archivo (crash.log) en:\n");
        sb.append("  https://github.com/Lorwp/Argentum-Forge/issues\n");
        sb.append("═══════════════════════════════════════════════════════════════\n");

        return sb.toString();
    }

}