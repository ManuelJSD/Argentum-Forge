package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.inits.MapData;
import org.argentumforge.engine.utils.inits.ObjData;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.editor.commands.MassOptimizeCommand;
import org.argentumforge.engine.scenes.Camera;

/**
 * Utilidad para realizar optimizaciones y limpiezas automáticas en el mapa.
 */
public class MapOptimizer {

    public static class OptimizationOptions {
        public boolean cleanBorders = false;
        public boolean removeBlockedExits = false;
        public boolean removeBlockedTriggers = false;
        public boolean removeTriggersOnExits = false;
        public boolean autoMapObjects = false; // Mapear árboles/carteles a capa 3
        public boolean autoBlockObjects = false; // Bloquear árboles/carteles

        // Tipos de objetos a considerar para auto-map/block (IDs de tipo de objeto)
        // 4=Árboles, 8=Carteles, 10=Foros, 22=Yacimientos
        public boolean includeTrees = true; // Type 4
        public boolean includeSigns = true; // Type 8
        public boolean includeForums = true; // Type 10
        public boolean includeDeposits = true; // Type 22
    }

    public static class OptimizationResult {
        public int itemsRemovedFromBorders = 0;
        public int blockedExitsRemoved = 0;
        public int blockedTriggersRemoved = 0;
        public int triggersOnExitsRemoved = 0;
        public int objectsMappedToLayer3 = 0;
        public int objectsBlocked = 0;
        public int totalTilesAffected = 0;
        public MassOptimizeCommand command = null;
    }

    /**
     * Analiza el mapa y genera un reporte de los cambios que se realizarían.
     * 
     * @param context Contexto del mapa a analizar.
     * @param options Opciones de optimización.
     * @return Resultado del análisis (sin comando generado).
     */
    public static OptimizationResult analyze(MapContext context, OptimizationOptions options) {
        return process(context, options, true);
    }

    /**
     * Ejecuta la optimización sobre el mapa.
     * 
     * @param context Contexto del mapa a optimizar.
     * @param options Opciones de optimización.
     * @return Resultado de la operación, incluyendo el comando para Undo.
     */
    public static OptimizationResult optimize(MapContext context, OptimizationOptions options) {
        return process(context, options, false);
    }

    private static OptimizationResult process(MapContext context, OptimizationOptions options, boolean simulate) {
        OptimizationResult result = new OptimizationResult();
        MassOptimizeCommand command = new MassOptimizeCommand(context);
        MapData[][] map = context.getMapData();

        if (map == null)
            return result;

        int xMin = Camera.XMinMapSize;
        int xMax = Camera.XMaxMapSize;
        int yMin = Camera.YMinMapSize;
        int yMax = Camera.YMaxMapSize;

        int mapWidth = map.length;
        int mapHeight = map[0].length;

        int clientWidth = GameData.options.getClientWidth();
        int clientHeight = GameData.options.getClientHeight();

        int halfW = clientWidth / 2;
        int halfH = clientHeight / 2;

        int minXBorder = halfW;
        int maxXBorder = mapWidth - halfW;
        int minYBorder = halfH;
        int maxYBorder = mapHeight - halfH;

        for (int y = yMin; y <= yMax; y++) {
            for (int x = xMin; x <= xMax; x++) {
                MapData currentTile = map[x][y];
                boolean changed = false;

                // Creamos una copia del tile para simular los cambios
                // En una implementación real de 'optimize', crearíamos el snapshot ANTES de
                // modificar el 'currentTile' real
                // Para simplificar y reusar lógica, usaremos un objeto MapData temporal que
                // copiamos del actual
                // Si 'simulate' es false, aplicaremos los cambios de este temporal al real y
                // guardaremos en el comando

                // DATA CLONE MANUAL (Costoso pero seguro)
                MapData tempTile = cloneTileData(currentTile);

                // 1. Limpieza de Bordes
                if (options.cleanBorders) {
                    if (x <= minXBorder || x >= maxXBorder || y <= minYBorder || y >= maxYBorder) {
                        if (tempTile.getNpcIndex() > 0) {
                            tempTile.setNpcIndex(0);
                            tempTile.setCharIndex(0); // Assuming CharIndex needs clear too
                            result.itemsRemovedFromBorders++;
                            changed = true;
                        }
                        if (tempTile.getObjIndex() > 0) {
                            tempTile.setObjIndex(0);
                            tempTile.setObjAmount(0);
                            tempTile.getObjGrh().setGrhIndex(0);
                            result.itemsRemovedFromBorders++;
                            changed = true;
                        }
                        if (tempTile.getExitMap() > 0) {
                            tempTile.setExitMap(0);
                            tempTile.setExitX(0);
                            tempTile.setExitY(0);
                            result.itemsRemovedFromBorders++;
                            changed = true;
                        }
                        if (tempTile.getTrigger() > 0) {
                            tempTile.setTrigger(0);
                            result.itemsRemovedFromBorders++;
                            changed = true;
                        }
                    }
                }

                // 2. Validaciones de Bloqueo
                if (tempTile.getBlocked()) {
                    if (options.removeBlockedExits && tempTile.getExitMap() > 0) {
                        tempTile.setExitMap(0);
                        tempTile.setExitX(0);
                        tempTile.setExitY(0);
                        result.blockedExitsRemoved++;
                        changed = true;
                    }
                    if (options.removeBlockedTriggers && tempTile.getTrigger() > 0) {
                        tempTile.setTrigger(0);
                        result.blockedTriggersRemoved++;
                        changed = true;
                    }
                }

                // 3. Validaciones de Traslados vs Triggers
                if (options.removeTriggersOnExits && tempTile.getExitMap() > 0) {
                    if (tempTile.getTrigger() > 0) {
                        tempTile.setTrigger(0);
                        result.triggersOnExitsRemoved++;
                        changed = true;
                    }
                }

                // 4. Automatización de Objetos
                if ((options.autoMapObjects || options.autoBlockObjects) && tempTile.getObjIndex() > 0) {
                    ObjData objData = AssetRegistry.objs.get(tempTile.getObjIndex());
                    if (objData != null && isTargetObjType(objData.getType(), options)) {

                        // Mapear a Capa 3
                        if (options.autoMapObjects) {
                            if (tempTile.getLayer(3).getGrhIndex() != tempTile.getObjGrh().getGrhIndex()) {
                                tempTile.getLayer(3).setGrhIndex(tempTile.getObjGrh().getGrhIndex());
                                result.objectsMappedToLayer3++;
                                changed = true;
                            }
                        }

                        // Auto Bloquear
                        if (options.autoBlockObjects) {
                            if (!tempTile.getBlocked()) {
                                tempTile.setBlocked(true);
                                result.objectsBlocked++;
                                changed = true;
                            }
                        }
                    }
                }

                if (changed) {
                    result.totalTilesAffected++;
                    if (!simulate) {
                        // Registramos el cambio en el comando
                        command.addChange(x, y, currentTile, tempTile);
                    }
                }
            }
        }

        if (!simulate && result.totalTilesAffected > 0) {
            result.command = command;
        }

        return result;
    }

    private static boolean isTargetObjType(int type, OptimizationOptions options) {
        if (options.includeTrees && type == 4)
            return true; // Árboles
        if (options.includeSigns && type == 8)
            return true; // Carteles
        if (options.includeForums && type == 10)
            return true; // Foros
        if (options.includeDeposits && type == 22)
            return true; // Yacimientos
        return false;
    }

    private static MapData cloneTileData(MapData src) {
        MapData dest = new MapData();
        dest.setBlocked(src.getBlocked());
        dest.setTrigger(src.getTrigger());
        dest.setExitMap(src.getExitMap());
        dest.setExitX(src.getExitX());
        dest.setExitY(src.getExitY());
        dest.setNpcIndex(src.getNpcIndex());
        dest.setCharIndex(src.getCharIndex());
        dest.setObjIndex(src.getObjIndex());
        dest.setObjAmount(src.getObjAmount());
        dest.setParticleIndex(src.getParticleIndex());

        for (int i = 1; i <= 4; i++) {
            dest.getLayer(i).setGrhIndex(src.getLayer(i).getGrhIndex());
        }
        dest.getObjGrh().setGrhIndex(src.getObjGrh().getGrhIndex());

        return dest;
    }
}
