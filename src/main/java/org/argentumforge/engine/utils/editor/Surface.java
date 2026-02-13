package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.listeners.KeyHandler;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.commands.BlockChangeCommand;
import org.argentumforge.engine.utils.editor.commands.BulkTileChangeCommand;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.commands.MacroCommand;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;

/**
 * Clase singleton para gestionar la edición de superficies (capas de suelo) en
 * el mapa.
 * Permite insertar o eliminar gráficos (GRH) en cualquiera de las 4 capas del
 * mapa.
 */
public class Surface {

    private static volatile Surface instance;
    private static final Object lock = new Object();

    public enum BrushShape {
        SQUARE, CIRCLE, SCATTER
    }

    public enum ToolMode {
        BRUSH, BUCKET, MAGIC_WAND
    }

    private int mode; // 0 = ninguno, 1 = colocar, 2 = borrar, 3 = capturar (pick)
    private int surfaceIndex;
    private int layer;

    private BrushShape brushShape = BrushShape.SQUARE;
    private ToolMode toolMode = ToolMode.BRUSH;
    private int brushSize = 1;
    private float scatterDensity = 0.3f;

    private boolean autoBlock = false;
    private int mosaicWidth = 1;
    private int mosaicHeight = 1;
    private boolean useMosaic = true;

    private Surface() {
        this.mode = 0;
        this.surfaceIndex = 1;
        this.layer = 1;
    }

    public static Surface getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Surface();
                }
            }
        }
        return instance;
    }

    // Para testing
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getSurfaceIndex() {
        return surfaceIndex;
    }

    public void setSurfaceIndex(int surfaceIndex) {
        this.surfaceIndex = surfaceIndex;

        // [FIX] Pre-cargar texturas de animación para evitar parpadeo en el ghost
        // Al seleccionar un GRH animado, solicitamos la carga de todos sus frames
        // inmediatamente
        // para que estén listos cuando el usuario mueva el mouse al mapa.
        try {
            if (surfaceIndex > 0 && org.argentumforge.engine.utils.AssetRegistry.grhData != null
                    && surfaceIndex < org.argentumforge.engine.utils.AssetRegistry.grhData.length) {
                var data = org.argentumforge.engine.utils.AssetRegistry.grhData[surfaceIndex];
                if (data != null && data.getNumFrames() > 1) {
                    // Cargar todos los frames de la animación
                    for (int i = 0; i < data.getNumFrames(); i++) {
                        int frameIdx = data.getFrame(i);
                        if (frameIdx > 0 && frameIdx < org.argentumforge.engine.utils.AssetRegistry.grhData.length) {
                            var frameData = org.argentumforge.engine.utils.AssetRegistry.grhData[frameIdx];
                            if (frameData != null && frameData.getFileNum() > 0) {
                                // Llamar a getTexture dispara la carga asíncrona si no existe
                                org.argentumforge.engine.renderer.Surface.INSTANCE.getTexture(frameData.getFileNum());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignorar errores de precarga para no bloquear la UI
        }
    }

    public int getLayer() {
        return layer;
    }

    public boolean isUseMosaic() {
        return useMosaic;
    }

    public void setUseMosaic(boolean useMosaic) {
        this.useMosaic = useMosaic;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public BrushShape getBrushShape() {
        return brushShape;
    }

    public void setBrushShape(BrushShape brushShape) {
        this.brushShape = brushShape;
    }

    public ToolMode getToolMode() {
        return toolMode;
    }

    public void setToolMode(ToolMode toolMode) {
        this.toolMode = toolMode;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = Math.max(1, brushSize);
    }

    public float getScatterDensity() {
        return scatterDensity;
    }

    public void setScatterDensity(float scatterDensity) {
        this.scatterDensity = scatterDensity;
    }

    public boolean isAutoBlock() {
        return autoBlock;
    }

    public void setAutoBlock(boolean autoBlock) {
        this.autoBlock = autoBlock;
    }

    public int getMosaicWidth() {
        return mosaicWidth;
    }

    public void setMosaicWidth(int mosaicWidth) {
        this.mosaicWidth = Math.max(1, mosaicWidth);
    }

    public int getMosaicHeight() {
        return mosaicHeight;
    }

    public void setMosaicHeight(int mosaicHeight) {
        this.mosaicHeight = Math.max(1, mosaicHeight);
    }

    private boolean isBatching = false;
    private MacroCommand batchCommand;

    public void startBatch() {
        if (!isBatching) {
            isBatching = true;
            batchCommand = new MacroCommand();
        }
    }

    public void endBatch() {
        if (isBatching) {
            isBatching = false;
            if (batchCommand != null && !batchCommand.getCommands().isEmpty()) {
                CommandManager.getInstance()
                        .executeCommand(batchCommand);
            }
            batchCommand = null;
        }
    }

    public void surface_edit(MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();

        if (mapData == null || x < 0 || x >= mapData.length || y < 0 || y >= mapData[0].length) {
            return;
        }

        if (mode == 3) { // Pick siempre es unitario
            this.pick(context, x, y);
            return;
        }

        if (toolMode == ToolMode.MAGIC_WAND) {
            this.magic_wand_select(context, x, y);
            return;
        }

        if (toolMode == ToolMode.BUCKET) {
            this.bucket_fill(context, x, y);
        } else {
            this.brush_edit(context, x, y);
        }
    }

    private void magic_wand_select(MapContext context, int x, int y) {
        var mapData = context.getMapData();
        if (mapData[x][y] == null)
            return;

        Selection selection = Selection.getInstance();
        selection.setActive(true);
        if (!KeyHandler.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
            selection.getSelectedEntities().clear();
        }

        int targetGrh = mapData[x][y].getLayer(layer).getGrhIndex();

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] { x, y });

        boolean[][] visited = new boolean[mapData.length][mapData[0].length];
        visited[x][y] = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int currX = pos[0];
            int currY = pos[1];

            // Añadir a selección (Tile)
            boolean alreadySelected = false;
            for (Selection.SelectedEntity se : selection.getSelectedEntities()) {
                if (se.x == currX && se.y == currY && se.type == Selection.EntityType.TILE) {
                    alreadySelected = true;
                    break;
                }
            }
            if (!alreadySelected) {
                selection.getSelectedEntities()
                        .add(new Selection.SelectedEntity(Selection.EntityType.TILE, 0, currX, currY));
            }

            int[][] neighbors = { { currX + 1, currY }, { currX - 1, currY }, { currX, currY + 1 },
                    { currX, currY - 1 } };
            for (int[] next : neighbors) {
                int nextX = next[0];
                int nextY = next[1];

                if (nextX >= 0 && nextX < mapData.length && nextY >= 0 && nextY < mapData[0].length) {
                    if (!visited[nextX][nextY] && mapData[nextX][nextY] != null
                            && mapData[nextX][nextY].getLayer(layer).getGrhIndex() == targetGrh) {
                        visited[nextX][nextY] = true;
                        queue.add(next);
                    }
                }
            }
        }

        selection.startAreaSelect(x, y); // Solo para activar estado visual si necesario, aunque aquí ya tenemos
                                         // entidades
        selection.cancelDrag(); // Reset drag state pero mantener seleccion
        // Forzamos "active" pero no "dragging" ni "areaSelecting"
        // El selection manager pinta cuadros sobre selectedEntities automáticamente
    }

    private void pick(MapContext context, int x, int y) {
        var mapData = context.getMapData();
        if (mapData != null && x >= 0 && x < mapData.length && y >= 0 && y < mapData[0].length) {
            int grhIdx = mapData[x][y].getLayer(layer).getGrhIndex();
            if (grhIdx > 0) {
                this.surfaceIndex = grhIdx;
                this.mode = 1; // Volvemos a modo insertar con el nuevo índice
                this.toolMode = ToolMode.BRUSH; // Reset a brush por si estaba en bucket
            }
        }
    }

    private void brush_edit(MapContext context, int x, int y) {
        if (mode == 0)
            return;

        var mapData = context.getMapData();

        Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
        Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> oldBlocks = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newBlocks = new HashMap<>();

        int half = brushSize / 2;

        if (useMosaic && (mosaicWidth > 1 || mosaicHeight > 1)) {
            // Modo Estampado: Colocamos el bloque completo
            for (int dx = 0; dx < mosaicWidth; dx++) {
                for (int dy = 0; dy < mosaicHeight; dy++) {
                    int mapX = x + dx;
                    int mapY = y + dy;
                    if (mapX >= 0 && mapX < mapData.length && mapY >= 0 && mapY < mapData[0].length) {
                        int targetGrhWithMosaic;
                        if (mode == 2) { // Modo Borrar
                            targetGrhWithMosaic = (layer == 1 ? 1 : 0);
                        } else { // Modo Insertar (mode == 1)
                            targetGrhWithMosaic = (surfaceIndex + (dy * mosaicWidth) + dx);
                        }
                        int currentGrh = mapData[mapX][mapY].getLayer(layer).getGrhIndex();
                        if (currentGrh != targetGrhWithMosaic) {
                            oldTiles.put(
                                    new BulkTileChangeCommand.TilePos(
                                            mapX, mapY),
                                    currentGrh);
                            newTiles.put(
                                    new BulkTileChangeCommand.TilePos(
                                            mapX, mapY),
                                    targetGrhWithMosaic);
                        }

                        if (autoBlock && mode == 1) {
                            boolean currentBlock = mapData[mapX][mapY].getBlocked();
                            if (!currentBlock) {
                                oldBlocks.put(
                                        new BlockChangeCommand.TilePos(
                                                mapX, mapY),
                                        false);
                                newBlocks.put(
                                        new BlockChangeCommand.TilePos(
                                                mapX, mapY),
                                        true);
                            }
                        }
                    }
                }
            }
        } else {
            // Modo Pincel Normal (con tiling sin costuras)
            double radiusSq = Math.pow(brushSize / 2.0, 2);
            for (int i = x - half; i <= x + half; i++) {
                for (int j = y - half; j <= y + half; j++) {
                    if (i >= 0 && i < mapData.length && j >= 0 && j < mapData[0].length) {
                        // Lógica de forma
                        if (brushShape == BrushShape.CIRCLE || brushShape == BrushShape.SCATTER) {
                            double dx = i - x;
                            double dy = j - y;
                            if (dx * dx + dy * dy > radiusSq)
                                continue;
                        }

                        // Lógica de Scatter
                        if (brushShape == BrushShape.SCATTER) {
                            if (Math.random() > scatterDensity)
                                continue;
                        }

                        int targetGrhWithMosaic = (mode == 1 ? surfaceIndex : (layer == 1 ? 1 : 0));
                        if (mode == 1 && (mosaicWidth > 1 || mosaicHeight > 1)) {
                            int relX = (i % mosaicWidth);
                            int relY = (j % mosaicHeight);
                            targetGrhWithMosaic = (surfaceIndex + (relY * mosaicWidth) + relX);
                        }

                        int currentGrh = mapData[i][j].getLayer(layer).getGrhIndex();
                        if (currentGrh != targetGrhWithMosaic) {
                            BulkTileChangeCommand.TilePos pos = new BulkTileChangeCommand.TilePos(
                                    i, j);
                            oldTiles.put(pos, currentGrh);
                            newTiles.put(pos, targetGrhWithMosaic);
                        }

                        if (autoBlock && mode == 1) {
                            boolean currentBlock = mapData[i][j].getBlocked();
                            if (!currentBlock) {
                                oldBlocks.put(
                                        new BlockChangeCommand.TilePos(i,
                                                j),
                                        false);
                                newBlocks.put(
                                        new BlockChangeCommand.TilePos(i,
                                                j),
                                        true);
                            }
                        }
                    }
                }
            }
        }

        if (!oldTiles.isEmpty() || !oldBlocks.isEmpty()) {
            MacroCommand macro = new MacroCommand();
            if (!oldTiles.isEmpty()) {
                macro.addCommand(new BulkTileChangeCommand(
                        context, layer,
                        oldTiles, newTiles));
            }
            if (!oldBlocks.isEmpty()) {
                macro.addCommand(
                        new BlockChangeCommand(
                                context, oldBlocks, newBlocks));
            }

            if (isBatching && batchCommand != null) {
                // Durante batch, añadimos al macro acumulador
                // Optimización opcional: Si el macro acumulador ya tiene un comando del mismo
                // tipo, podríamos fusionarlos
                // Pero por ahora, simplemente añadimos sub-comandos al macro principal.
                batchCommand.addCommand(macro);

                // Ejecutamos visualmente (sin historial todavía) para que el usuario vea
                // cambios
                macro.execute();
            } else {
                org.argentumforge.engine.utils.editor.commands.CommandManager.getInstance().executeCommand(macro);
            }
        }
    }

    private void bucket_fill(MapContext context, int x, int y) {
        if (mode == 0)
            return;

        var mapData = context.getMapData();

        int targetGrh = (mode == 1 ? surfaceIndex : (layer == 1 ? 1 : 0));
        int startGrh = mapData[x][y].getLayer(layer).getGrhIndex();

        if (startGrh == targetGrh && !autoBlock && (mosaicWidth <= 1 && mosaicHeight <= 1))
            return;

        Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
        Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> oldBlocks = new HashMap<>();
        Map<BlockChangeCommand.TilePos, Boolean> newBlocks = new HashMap<>();

        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[] { x, y });

        boolean[][] visited = new boolean[mapData.length][mapData[0].length];
        visited[x][y] = true;

        while (!queue.isEmpty()) {
            int[] pos = queue.poll();
            int currX = pos[0];
            int currY = pos[1];

            int targetGrhWithMosaic = targetGrh;
            if (mode == 1 && (mosaicWidth > 1 || mosaicHeight > 1)) {
                int offsetX = currX % mosaicWidth;
                int offsetY = currY % mosaicHeight;
                targetGrhWithMosaic = (surfaceIndex + (offsetY * mosaicWidth) + offsetX);
            }

            oldTiles.put(new BulkTileChangeCommand.TilePos(currX, currY),
                    startGrh);
            newTiles.put(new BulkTileChangeCommand.TilePos(currX, currY),
                    targetGrhWithMosaic);

            if (autoBlock && mode == 1) {
                boolean currentBlock = mapData[currX][currY].getBlocked();
                if (!currentBlock) {
                    oldBlocks.put(
                            new BlockChangeCommand.TilePos(currX, currY),
                            false);
                    newBlocks.put(
                            new BlockChangeCommand.TilePos(currX, currY),
                            true);
                }
            }

            int[][] neighbors = { { currX + 1, currY }, { currX - 1, currY }, { currX, currY + 1 },
                    { currX, currY - 1 } };
            for (int[] next : neighbors) {
                int nextX = next[0];
                int nextY = next[1];

                if (nextX >= 0 && nextX < mapData.length && nextY >= 0 && nextY < mapData[0].length) {
                    if (!visited[nextX][nextY] && mapData[nextX][nextY].getLayer(layer).getGrhIndex() == startGrh) {
                        visited[nextX][nextY] = true;
                        queue.add(next);
                    }
                }
            }
        }

        if (!oldTiles.isEmpty() || !oldBlocks.isEmpty()) {
            MacroCommand macro = new MacroCommand();
            if (!oldTiles.isEmpty()) {
                macro.addCommand(new BulkTileChangeCommand(
                        context, layer,
                        oldTiles, newTiles));
            }
            if (!oldBlocks.isEmpty()) {
                macro.addCommand(
                        new BlockChangeCommand(
                                context, oldBlocks, newBlocks));
            }

            // Bucket usually is a single action, but technically can be part of a batch if
            // user clicks rapidly
            if (isBatching && batchCommand != null) {
                batchCommand.addCommand(macro);
                macro.execute();
            } else {
                CommandManager.getInstance().executeCommand(macro);
            }
        }
    }

    public void fillLayer(MapContext context) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
        Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

        // If we are filling with the "erase" graphic (0), acts like clear
        // But if mode is 1 (insert) use surfaceIndex.
        int targetGrh = surfaceIndex;

        // However, usually "Fill Layer" implies using the selected GRH.
        // "Clear Layer" implies using 0.

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                int currentGrh = mapData[x][y].getLayer(layer).getGrhIndex();
                if (currentGrh != targetGrh) {
                    BulkTileChangeCommand.TilePos pos = new BulkTileChangeCommand.TilePos(
                            x, y);
                    oldTiles.put(pos, currentGrh);
                    newTiles.put(pos, targetGrh);
                }
            }
        }

        if (!oldTiles.isEmpty()) {
            BulkTileChangeCommand command = new BulkTileChangeCommand(
                    context, layer, oldTiles, newTiles);
            CommandManager.getInstance().executeCommand(command);
        }
    }

    public void clearLayer(MapContext context) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
        Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

        int targetGrh = 0; // Clear implies 0

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                int currentGrh = mapData[x][y].getLayer(layer).getGrhIndex();
                if (currentGrh != targetGrh) {
                    BulkTileChangeCommand.TilePos pos = new BulkTileChangeCommand.TilePos(
                            x, y);
                    oldTiles.put(pos, currentGrh);
                    newTiles.put(pos, targetGrh);
                }
            }
        }

        if (!oldTiles.isEmpty()) {
            BulkTileChangeCommand command = new BulkTileChangeCommand(
                    context, layer, oldTiles, newTiles);
            CommandManager.getInstance().executeCommand(command);
        }
    }

    public void fillBorders(MapContext context) {
        applyGlobalAction(context, true, true);
    }

    public void clearBorders(MapContext context) {
        applyGlobalAction(context, false, true);
    }

    private void applyGlobalAction(MapContext context, boolean fill,
            boolean bordersOnly) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        Map<BulkTileChangeCommand.TilePos, Integer> oldTiles = new HashMap<>();
        Map<BulkTileChangeCommand.TilePos, Integer> newTiles = new HashMap<>();

        // If filtering (fill=true), use selected surface, else use 0 (empty)
        int targetGrh = fill ? surfaceIndex : 0;

        int mapWidth = mapData.length;
        int mapHeight = mapData[0].length;

        int clientWidth = GameData.options.getClientWidth();
        int clientHeight = GameData.options.getClientHeight();

        int halfW = clientWidth / 2;
        int halfH = clientHeight / 2;

        int minXBorder = halfW;
        int maxXBorder = mapWidth - 1 - halfW;

        int minYBorder = halfH;
        int maxYBorder = mapHeight - 1 - halfH;

        for (int x = 0; x < mapData.length; x++) {
            for (int y = 0; y < mapData[0].length; y++) {
                if (mapData[x][y] != null) {
                    // Logic from Block.java
                    boolean isBorder = x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder;

                    if (!bordersOnly || isBorder) {
                        int currentGrh = mapData[x][y].getLayer(layer).getGrhIndex();
                        if (currentGrh != targetGrh) {
                            BulkTileChangeCommand.TilePos pos = new BulkTileChangeCommand.TilePos(
                                    x, y);
                            oldTiles.put(pos, currentGrh);
                            newTiles.put(pos, targetGrh);
                        }
                    }
                }
            }
        }

        if (!oldTiles.isEmpty()) {
            BulkTileChangeCommand command = new BulkTileChangeCommand(
                    context, layer, oldTiles, newTiles);
            CommandManager.getInstance().executeCommand(command);
        }
    }

}
