package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.game.models.Character;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.inits.MapData;

import java.util.ArrayList;
import java.util.List;

public class MapValidator {

    public static class ValidationError {
        public int x;
        public int y;
        public String description;
        public String type; // "ERROR", "WARNING"

        public ValidationError(int x, int y, String description, String type) {
            this.x = x;
            this.y = y;
            this.description = description;
            this.type = type;
        }
    }

    public static List<ValidationError> validateCurrentMap() {
        List<ValidationError> errors = new ArrayList<>();
        MapContext context = GameData.getActiveContext();

        if (context == null)
            return errors;

        MapData[][] mapData = context.getMapData();
        Character[] charList = context.getCharList();

        // Check bounds
        int minX = GameData.X_MIN_MAP_SIZE;
        int maxX = GameData.X_MAX_MAP_SIZE;
        int minY = GameData.Y_MIN_MAP_SIZE;
        int maxY = GameData.Y_MAX_MAP_SIZE;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {

                // 1. Check Invalid Transfers
                if (mapData[x][y].getTileExit().getMap() > 0) {
                    // Si hay mapa destino, verificar que sea válido (mayor a 0 y menor que max maps
                    // si tuvieramos el dato, por ahora > 0)
                    // Y verificar que las coordenadas destino sean válidas
                    int destX = mapData[x][y].getTileExit().getX();
                    int destY = mapData[x][y].getTileExit().getY();

                    if (destX < minX || destX > maxX || destY < minY || destY > maxY) {
                        errors.add(new ValidationError(x, y,
                                "Traslado a coordenadas inválidas (" + destX + "," + destY + ")", "ERROR"));
                    }
                } else {
                    // Si Map es 0, pero X o Y son distintos de 0, es un traslado sucio/roto
                    if (mapData[x][y].getTileExit().getX() != 0 || mapData[x][y].getTileExit().getY() != 0) {
                        errors.add(new ValidationError(x, y,
                                "Datos de traslado corruptos (Mapa 0 pero X/Y definidos)", "WARNING"));
                    }
                }

                // 2. Check Objects/NPCs on Blocked Tiles
                boolean isBlocked = mapData[x][y].getBlocked();

                // Check Autos/NPCs
                int charIndex = mapData[x][y].getCharIndex();
                if (charIndex > 0 && charIndex < charList.length && charList[charIndex].isActive()) {
                    if (isBlocked) {
                        errors.add(new ValidationError(x, y,
                                "NPC/Personaje ubicado en tile bloqueado", "WARNING"));
                    }
                }

                // Check Objects (ObjIndex)
                if (mapData[x][y].getObjIndex() > 0) {
                    if (isBlocked) {
                        errors.add(new ValidationError(x, y,
                                "Objeto ubicado en tile bloqueado", "WARNING"));
                    }
                }
            }
        }

        return errors;
    }
}
