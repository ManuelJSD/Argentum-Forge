package org.argentumforge.engine.renderer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import org.lwjgl.BufferUtils;
import org.tinylog.Logger;

/**
 * Gestiona un conjunto de texturas (instancias de la clase {@code Texture})
 * mediante un mapa de identificadores numericos.
 * Proporciona metodos para inicializar, obtener, crear y eliminar texturas.
 */

public enum Surface {

    INSTANCE;

    /**
     * Mapa que asocia identificadores numericos con sus correspondientes texturas
     * en memoria.
     */
    private Map<Integer, Texture> textures;
    private Texture whiteTexture;

    // Async Loading
    private ExecutorService loaderExecutor;
    private ConcurrentLinkedQueue<Texture.TextureData> readyToUpload;
    private Set<Integer> pendingIds;
    private Set<Integer> failedIds;
    private Map<Integer, Texture> placeholderTextures;

    /**
     * Inicializa el contenedor de texturas y el sistema de carga asíncrona.
     */
    public void init() {
        if (textures == null)
            textures = new ConcurrentHashMap<>();

        if (whiteTexture == null) {
            whiteTexture = new Texture();
            whiteTexture.createWhitePixel();
        }

        // Inicializar textura de error en el hilo principal de OpenGL
        if (missingTexture == null) {
            createMissingTexture();
        }

        if (loaderExecutor == null || loaderExecutor.isShutdown()) {
            loaderExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
                Thread t = new Thread(r, "TextureLoader");
                t.setDaemon(true);
                return t;
            });
        }

        if (readyToUpload == null)
            readyToUpload = new ConcurrentLinkedQueue<>();
        if (pendingIds == null)
            pendingIds = ConcurrentHashMap.newKeySet();
        if (failedIds == null)
            failedIds = ConcurrentHashMap.newKeySet();
        if (placeholderTextures == null)
            placeholderTextures = new ConcurrentHashMap<>();
    }

    /**
     * Procesa las texturas que han terminado de cargarse en segundo plano.
     * DEBE llamarse desde el hilo principal de OpenGL (Engine loop).
     */
    public void dispatchUploads() {
        if (readyToUpload == null)
            return;
        Texture.TextureData data;
        int count = 0;
        int limit = 50; // Aumentado para mayor velocidad de carga inicial
        while ((data = readyToUpload.poll()) != null && count < limit) {
            try {
                int id = Integer.parseInt(data.fileName);
                Texture tex = placeholderTextures.remove(id);
                if (tex != null) {
                    tex.upload(data);
                }
                pendingIds.remove(id);
            } catch (Exception e) {
                Logger.error(e, "Error subiendo textura a GPU");
            } finally {
                data.cleanup();
            }
            count++;
        }

        // Procesar texturas fallidas: Reemplazar placeholders vacíos con missingTexture
        if (missingTexture != null && missingTexture.getId() != 0) {
            for (Integer failedId : failedIds) {
                Texture current = textures.get(failedId);
                if (current != null && current.getId() == 0) {
                    // Reemplazar placeholder vacío con textura de error
                    textures.put(failedId, missingTexture);
                }
            }
        }
    }

    /**
     * Elimina todas las texturas gestionadas y apaga el hilo de carga.
     */
    public void deleteAllTextures() {
        // Optimization: Only clear if we have a lot of textures (> 3000)
        // This avoids the 1s lag during map transitions for small/medium maps
        if (textures.size() < 3000) {
            org.tinylog.Logger.info("Surface: Skipping texture cleanup (cache size: " + textures.size() + ")");
            return;
        }

        org.tinylog.Logger.info("Surface: Cleaning up " + textures.size() + " textures...");

        // No apagar el loaderExecutor aquí, ya que se necesita para seguir cargando
        // texturas después de una limpieza de recursos o cambio de mapa.
        for (Texture texture : textures.values()) {
            if (texture != whiteTexture && texture != missingTexture) {
                texture.cleanup();
            }
        }
        textures.clear();
        placeholderTextures.clear();
        pendingIds.clear();

        // CRITICAL FIX: Limpiar failedIds para resetear estado de cargas fallidas
        // Esto evita que gráficos que fallaron temporalmente (archivo bloqueado, etc.)
        // queden marcados permanentemente como fallidos tras un cambio de mapa
        if (failedIds != null) {
            failedIds.clear();
        }

        if (readyToUpload != null) {
            readyToUpload.clear();
        }
        // CRITICAL: We NO LONGER delete whiteTexture here because many renderers
        // (including GUI and selection ghosts) depend on it being valid
        // even during map transitions.
    }

    /**
     * Apaga definitivamente el sistema de carga asíncrona.
     * Debe llamarse al cerrar la aplicación.
     */
    public void shutdown() {
        if (loaderExecutor != null) {
            loaderExecutor.shutdownNow();
        }
    }

    /**
     * Limpia la lista de IDs fallidos, permitiendo que el sistema intente cargar
     * nuevamente las texturas que fallaron anteriormente.
     * Util para recuperar texturas tras fallos de red o desbloqueo de archivos.
     */
    public void retryFailedTextures() {
        if (failedIds != null) {
            int count = failedIds.size();
            failedIds.clear();
            Logger.info("Se han reiniciado {} texturas fallidas para reintento.", count);
        }
    }

    /**
     * Obtiene una textura asociada al identificador numerico especificado.
     * <p>
     * Si la textura no existe en el mapa, se crea y se agrega al mismo.
     *
     * @param fileNum identificador numerico asociado al archivo de textura
     * @return la textura obtenida o creada asociada al identificador proporcionado
     */
    public Texture getTexture(int fileNum) {
        if (textures.containsKey(fileNum))
            return textures.get(fileNum);
        return createTexture(fileNum);
    }

    /**
     * Crea una nueva textura asociada a un identificador numerico y la agrega al
     * mapa de texturas.
     * <p>
     * La textura es cargada desde un archivo comprimido especificado y configurada
     * adecuadamente.
     *
     * @param fileNum identificador numerico unico asociado a la textura que se
     *                desea crear
     * @return la nueva textura creada asociada al identificador proporcionado
     */
    private Texture createTexture(int fileNum) {
        if (failedIds != null && failedIds.contains(fileNum)) {
            // Retornar la textura de error (Magenta/Negro) para feedback visual
            return missingTexture != null ? missingTexture : new Texture();
        }

        Texture texture = new Texture();
        textures.put(fileNum, texture);

        if (pendingIds.add(fileNum)) {
            placeholderTextures.put(fileNum, texture);
            loaderExecutor.submit(() -> {
                // Pasamos null como primer argumento ya que no usamos archivos .ao
                Texture.TextureData data = Texture.prepareData(null, String.valueOf(fileNum), false);
                if (data != null) {
                    readyToUpload.add(data);
                } else {
                    pendingIds.remove(fileNum);
                    placeholderTextures.remove(fileNum);
                    if (failedIds != null) {
                        failedIds.add(fileNum);
                        // NO llamar getMissingTexture() aquí (hilo async sin contexto OpenGL)
                        // La sustitución se hará en dispatchUploads (hilo principal)
                        Logger.warn("Grafico {} falló y será reemplazado por MissingTexture.", fileNum);
                    }
                }
            });
        }

        return texture;
    }

    public Texture createTexture(String file, boolean isGUI) {
        return createTexture(null, file, isGUI);
    }

    /**
     * Crea una nueva textura a partir de un archivo especificado.
     * <p>
     * La textura es inicializada con la informacion proporcionada, incluyendo el
     * archivo comprimido, el archivo de textura y si
     * esta destinada a interfaces graficas de usuario (GUI).
     *
     * @param fileCompressed nombre del archivo comprimido que contiene la textura
     * @param file           nombre del archivo dentro del archivo comprimido que
     *                       contiene los datos de la textura
     * @param isGUI          indica si la textura esta destinada a ser utilizada en
     *                       interfaces graficas de usuario (GUI)
     * @return la textura creada, o {@code null} si el nombre del archivo
     *         especificado esta vacio
     */
    public Texture createTexture(String ignoredSource, String file, boolean isGUI) {
        if (file.isEmpty())
            return null;
        Texture texture = new Texture();
        // Cargamos directamente, el argumento de archivo comprimido se ignora
        texture.loadTexture(texture, null, file, isGUI);
        return texture;
    }

    public Texture getWhiteTexture() {
        if (whiteTexture == null) {
            whiteTexture = new Texture();
            whiteTexture.createWhitePixel();
        }
        return whiteTexture;
    }

    public Texture getMissingTexture() {
        if (missingTexture == null) {
            createMissingTexture();
        }
        return missingTexture;
    }

    private Texture missingTexture;

    private void createMissingTexture() {
        missingTexture = new Texture();
        // Generar patrón de tablero de ajedrez Magenta/Negro 32x32
        int w = 32;
        int h = 32;
        ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);

        for (int i = 0; i < w * h; i++) {
            int x = i % w;
            int y = i / w;
            boolean check = ((x / 16) + (y / 16)) % 2 == 0;
            if (check) { // Magenta
                pixels.put((byte) 255).put((byte) 0).put((byte) 255).put((byte) 255);
            } else { // Black
                pixels.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 255);
            }
        }
        pixels.flip();

        // Usar TextureData para subirlo limpiamente
        Texture.TextureData data = new Texture.TextureData();
        data.width = w;
        data.height = h;
        data.pixels = pixels;
        data.fileName = "MISSING_TEXTURE";

        missingTexture.upload(data);
    }
}
