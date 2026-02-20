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

    // Sets para control de estado
    private Set<Integer> pendingIds;
    private Set<Integer> failedIds;

    // Mapas auxiliares para el proceso de carga
    private Map<Integer, Texture> placeholderTextures;
    private Map<Integer, Integer> retryCounts; // Contador de reintentos por ID

    private static final int MAX_RETRIES = 3;

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
        if (retryCounts == null)
            retryCounts = new ConcurrentHashMap<>();
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
                if (data.fileName != null && data.fileName.matches("\\d+")) {
                    int id = Integer.parseInt(data.fileName);
                    Texture tex = placeholderTextures.remove(id);
                    // Si tex es null, quizás se limpió la caché mientras cargaba, buscamos en el
                    // mapa principal
                    if (tex == null)
                        tex = textures.get(id);

                    if (tex != null) {
                        tex.upload(data);
                    }
                    pendingIds.remove(id);
                    retryCounts.remove(id); // Éxito, borramos contador de reintentos
                } else if (data.fileName != null) {
                    // Cargas manuales por nombre de archivo (GUI, etc.)
                    // No gestionadas por pendingIds/failedIds numéricos
                }
            } catch (Exception e) {
                Logger.error(e, "Error subiendo textura a GPU");
            } finally {
                data.cleanup();
            }
            count++;
        }

        // Feedback visual para fallos definitivos
        if (missingTexture != null && missingTexture.getId() != 0) {
            for (Integer failedId : failedIds) {
                Texture current = textures.get(failedId);
                // Si la textura existe pero tiene ID 0 (no cargada) y no está pendiente de
                // carga
                if (current != null && current.getId() == 0 && !pendingIds.contains(failedId)) {
                    // Reemplazar placeholder vacío con textura de error para que sea visible
                    // Ojo: No reemplazamos el objeto Texture en el mapa 'textures',
                    // simplemente hacemos que 'current' apunte a los datos de missingTexture si
                    // fuera posible,
                    // pero aquí estamos limitados. Lo que haremos es re-asignar en el mapa si fuera
                    // necesario
                    // o confiar en que getTexture devuelva missingTexture para futuros accesos.

                    // Mejor enfoque: Si getTexture se llama para un failedId, devolver
                    // missingTexture.
                    // Pero para objetos ya instanciados (referencias viejas), no podemos cambiar su
                    // ID GL fácilmente sin upload.
                    // Podríamos hacer current.upload(missingTextureData) pero es costoso hacerlo
                    // cada frame.
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
        if (failedIds != null)
            failedIds.clear();
        if (retryCounts != null)
            retryCounts.clear();
        if (readyToUpload != null)
            readyToUpload.clear();
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
     * Reintenta cargar las texturas que fallaron anteriormente.
     * Limpia la lista de fallos y vuelve a enviar las tareas de carga.
     */
    public void retryFailedTextures() {
        if (failedIds == null || failedIds.isEmpty()) {
            Logger.info("No hay texturas fallidas para reintentar.");
            return;
        }

        Logger.info("Surface: Reintentando {} texturas fallidas...", failedIds.size());

        // Copiamos la lista para iterar seguros
        Set<Integer> idsToRetry = new java.util.HashSet<>(failedIds);
        failedIds.clear(); // Limpiamos estado de error
        if (retryCounts != null)
            retryCounts.clear(); // Reseteamos contadores de reintento

        for (Integer id : idsToRetry) {
            Texture tex = textures.get(id);
            if (tex == null) {
                // Si no existe, createTexture lo manejará cuando se solicite
                continue;
            }

            // Si existe pero falló, resubimos la tarea
            submitLoadTask(id, tex);
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
            // Retornar la textura de error (Magenta/Negro) para feedback visual inmediato
            return missingTexture != null ? missingTexture : new Texture();
        }

        Texture texture = new Texture();
        textures.put(fileNum, texture);

        submitLoadTask(fileNum, texture);

        return texture;
    }

    /**
     * Envía la tarea de carga al ExecutorService.
     */
    private void submitLoadTask(int fileNum, Texture texture) {
        if (pendingIds.add(fileNum)) {
            placeholderTextures.put(fileNum, texture);

            loaderExecutor.submit(() -> {
                // Lógica de carga en hilo secundario
                try {
                    Texture.TextureData data = Texture.prepareData(null, String.valueOf(fileNum), false);

                    if (data != null) {
                        readyToUpload.add(data);
                    } else {
                        handleLoadFailure(fileNum, texture);
                    }
                } catch (Exception e) {
                    Logger.error(e, "Excepción no controlada cargando textura {}", fileNum);
                    handleLoadFailure(fileNum, texture);
                }
            });
        }
    }

    /**
     * Maneja el fallo de carga, implementando la lógica de reintento automático.
     */
    private void handleLoadFailure(int fileNum, Texture texture) {
        // Remover de pendientes para permitir reintento
        pendingIds.remove(fileNum);
        placeholderTextures.remove(fileNum);

        int retries = retryCounts.getOrDefault(fileNum, 0);

        if (retries < MAX_RETRIES) {
            retryCounts.put(fileNum, retries + 1);
            Logger.warn("Fallo cargando textura {}. Reintentando ({}/{})", fileNum, retries + 1, MAX_RETRIES);

            // Pequeña espera antes de reintentar para dar tiempo al disco/SO
            try {
                Thread.sleep(50 + (retries * 50));
            } catch (InterruptedException ignored) {
            }

            // Re-enviar tarea
            submitLoadTask(fileNum, texture);
        } else {
            // Fallo definitivo tras reintentos
            Logger.error("Fallo definitivo cargando textura {} tras {} intentos.", fileNum, MAX_RETRIES);
            if (failedIds != null) {
                failedIds.add(fileNum);
            }
        }
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

    /**
     * Carga de forma síncrona todas las texturas indicadas, bloqueando el hilo
     * actual hasta que todas estén en GPU.
     * <p>
     * Usar solo durante la exportación de imagen, donde el bloqueo del hilo de
     * render es aceptable y necesario para garantizar texturas completas.
     *
     * @param fileNums conjunto de IDs de archivo de textura a pre-cargar
     */
    /**
     * Carga de forma síncrona todas las texturas indicadas, bloqueando el hilo
     * actual hasta que todas estén en GPU.
     * <p>
     * Usar solo durante la exportación de imagen.
     *
     * @param fileNums conjunto de IDs de archivo de textura a pre-cargar
     */
    public void preloadSync(Set<Integer> fileNums) {
        Logger.info("Surface:Pre-cargando {} texturas síncronamente...", fileNums.size());
        for (Integer fileNum : fileNums) {
            syncLoad(fileNum);
        }
        Logger.info("Surface: Pre-carga síncrona completada.");
    }

    /**
     * Fuerza la carga síncrona de una textura específica en el hilo actual.
     * Si la textura no existe, se crea. Si ya está cargada, no hace nada.
     * Utilizado como fallback en exportación para evitar gráficos faltantes.
     *
     * @param fileNum ID del archivo de textura
     */
    public void syncLoad(int fileNum) {
        // Obtener o crear el objeto Texture en el mapa
        Texture texture = textures.get(fileNum);
        if (texture == null) {
            texture = new Texture();
            textures.put(fileNum, texture);
        }

        // Si ya está en GPU, saltar
        if (texture.getId() > 0)
            return;

        // Si ya falló anteriormente, no reintentar infinitamente en el loop de render
        if (failedIds != null && failedIds.contains(fileNum))
            return;

        // Cancelar cualquier tarea asíncrona pendiente para este ID
        pendingIds.remove(fileNum);
        placeholderTextures.remove(fileNum);

        try {
            // Carga y upload síncronos en el hilo GL actual
            Texture.TextureData data = Texture.prepareData(null, String.valueOf(fileNum), false);
            if (data != null) {
                texture.upload(data);
                data.cleanup();
                failedIds.remove(fileNum);
                retryCounts.remove(fileNum);
            } else {
                Logger.warn("Surface: Fallo síncrono texture {}", fileNum);
                failedIds.add(fileNum);
            }
        } catch (Exception e) {
            Logger.error(e, "Surface: Excepción síncrona texture {}", fileNum);
            failedIds.add(fileNum);
        }
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
