package org.argentumforge.engine.game;

import org.argentumforge.engine.game.models.*;
import static org.argentumforge.engine.game.models.Character.*;
import static org.argentumforge.engine.game.models.Direction.*;
import static org.argentumforge.engine.scenes.Camera.*;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.AssetRegistry;
import org.argentumforge.engine.utils.inits.BodyData;
import org.argentumforge.engine.utils.inits.HeadData;
import org.argentumforge.engine.utils.inits.WeaponData;
import org.argentumforge.engine.utils.inits.ShieldData;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.scenes.GameScene;
import org.argentumforge.engine.scenes.Camera;

/**
 * Entidad del usuario principal.
 * Gestiona la posición, estado y movimiento del jugador en el mundo.
 */
public enum User {

    INSTANCE;

    private final Position userPos;
    private final Position addToUserPos;
    private boolean underCeiling;
    private boolean userMoving;
    private boolean walkingmode;
    // mapa
    private short userMap;
    private short userCharIndex;

    // Apariencia persistente
    private int userBody = GameData.options.getUserBody();
    private int userHead = GameData.options.getUserHead();
    private int userWaterBody = GameData.options.getUserWaterBody();

    User() {
        this.userPos = new Position();
        this.addToUserPos = new Position();
        this.walkingmode = false;
    }

    /**
     * Restaura o inicializa el personaje del usuario en la lista global.
     * Debe llamarse tras cargar un mapa nuevo.
     */
    public void refreshUserCharacter() {
        if (userCharIndex <= 0) {
            org.tinylog.Logger
                    .warn("refreshUserCharacter: Invalid userCharIndex=" + userCharIndex + " -> Auto-fixing to 1");
            userCharIndex = 1;
        }

        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        org.tinylog.Logger.info("refreshUserCharacter START: Index=" + userCharIndex);

        // Asegurar que el slot esta activo y configurado
        charList[userCharIndex].setActive(true);
        charList[userCharIndex].getPos().setX(userPos.getX());
        charList[userCharIndex].getPos().setY(userPos.getY());
        charList[userCharIndex].setHeading(Direction.DOWN);

        // Aplicar apariencia persistente
        charList[userCharIndex].setiBody((short) userBody);
        charList[userCharIndex].setiHead((short) userHead);

        // Actualizar objetos de datos (Importante para renderizado!)
        if (AssetRegistry.bodyData != null && userBody < AssetRegistry.bodyData.length
                && AssetRegistry.bodyData[userBody] != null) {
            charList[userCharIndex].setBody(new BodyData(AssetRegistry.bodyData[userBody]));
        } else {
            charList[userCharIndex].setBody(new BodyData());
        }

        if (AssetRegistry.headData != null && userHead < AssetRegistry.headData.length
                && AssetRegistry.headData[userHead] != null) {
            charList[userCharIndex].setHead(new HeadData(AssetRegistry.headData[userHead]));
        } else {
            charList[userCharIndex].setHead(new HeadData());
        }

        charList[userCharIndex].setHelmet(new HeadData()); // Dummy
        charList[userCharIndex].setWeapon(new WeaponData());
        charList[userCharIndex].setShield(new ShieldData());

        // Update lastChar just in case
        if (userCharIndex > org.argentumforge.engine.game.models.Character.lastChar) {
            org.argentumforge.engine.game.models.Character.lastChar = userCharIndex;
        }

        // Actualizar mapa
        if (mapData != null) {
            int x = userPos.getX();
            int y = userPos.getY();
            // Check bounds directly using mapData length
            if (x >= 1 && x < mapData.length && y >= 1 && y < mapData[0].length) {
                mapData[x][y].setCharIndex(userCharIndex);
            }
        }
    }

    public void setUserBody(int body) {
        this.userBody = body;
        GameData.options.setUserBody(body);
    }

    public int getUserBody() {
        return userBody;
    }

    public void setUserHead(int head) {
        this.userHead = head;
        GameData.options.setUserHead(head);
    }

    public int getUserHead() {
        return userHead;
    }

    public void setUserWaterBody(int body) {
        this.userWaterBody = body;
        GameData.options.setUserWaterBody(body);
    }

    public int getUserWaterBody() {
        return userWaterBody;
    }

    /**
     * @param nDirection direccion pasada por parametro Mueve la camara hacia una
     *                   direccion.
     */
    public void moveScreen(Direction nDirection) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;
        var mapData = context.getMapData();
        var charList = context.getCharList();

        int x = 0, y = 0;
        switch (nDirection) {
            case UP:
                y = -1;
                break;
            case RIGHT:
                x = 1;
                break;
            case DOWN:
                y = 1;
                break;
            case LEFT:
                x = -1;
                break;
        }

        // En modo cámara libre, mover 2 tiles a la vez (50% más rápido)
        int multiplier = walkingmode ? 1 : 2;
        x *= multiplier;
        y *= multiplier;

        final int tX = userPos.getX() + x;
        final int tY = userPos.getY() + y;

        if (!(tX < XMinMapSize || tX > XMaxMapSize || tY < YMinMapSize || tY > YMaxMapSize)) {

            // 1. Clean Old Position
            if (mapData != null) {
                mapData[userPos.getX()][userPos.getY()].setCharIndex(0);
            }

            // 2. Update User Position
            addToUserPos.setX(x); // Keep this for camera/legacy User logic
            userPos.setX(tX);
            addToUserPos.setY(y);
            userPos.setY(tY);
            userMoving = true;
            underCeiling = checkUnderCeiling();

            // 3. Update CharList Entry (Critical for Rendering & Map Logic)
            if (userCharIndex > 0 && userCharIndex < charList.length) {
                org.argentumforge.engine.game.models.Character chr = charList[userCharIndex];

                // Sync Position
                chr.getPos().setX(tX);
                chr.getPos().setY(tY);

                // Update Map
                if (mapData != null) {
                    mapData[tX][tY].setCharIndex(userCharIndex);
                }

                // Smooth Movement Setup (Offsets in Pixels)
                // Assuming TILE_PIXEL_SIZE is 32. Using constants if available.
                chr.setMoveOffsetX(-1 * (32 * x));
                chr.setMoveOffsetY(-1 * (32 * y));
                chr.setMoving(true);
                chr.setHeading(nDirection);
                chr.setScrollDirectionX(x);
                chr.setScrollDirectionY(y);

                // 4. Handle Walking Mode Appearance (Swimming)
                if (walkingmode) {
                    checkAppearance();
                }
            }
        }
    }

    /**
     * Checkea si estamos bajo techo segun el trigger en donde esta parado el
     * usuario.
     */
    public boolean checkUnderCeiling() {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return false;
        var mapData = context.getMapData();

        return mapData[userPos.getX()][userPos.getY()].getTrigger() == 1
                || mapData[userPos.getX()][userPos.getY()].getTrigger() == 2
                || mapData[userPos.getX()][userPos.getY()].getTrigger() == 4;
    }

    /**
     * @param charIndex  Numero de identificador de personaje
     * @param nDirection Direccion del personaje Mueve el personaje segun la
     *                   direccion establecida en "nHeading".
     */
    public void moveCharbyHead(short charIndex, Direction nDirection) {
        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        int addX = 0, addY = 0;
        switch (nDirection) {
            case UP:
                addY = -1;
                break;
            case RIGHT:
                addX = 1;
                break;
            case DOWN:
                addY = 1;
                break;
            case LEFT:
                addX = -1;
                break;
        }

        final int x = charList[charIndex].getPos().getX();
        final int y = charList[charIndex].getPos().getY();
        final int nX = x + addX;
        final int nY = y + addY;

        // Validar límites antes de acceder a mapData
        if (nX < 1 || nX > 100 || nY < 1 || nY > 100) {
            return;
        }

        if (mapData != null) {
            mapData[nX][nY].setCharIndex(charIndex);
            mapData[x][y].setCharIndex(0);
        }

        charList[charIndex].getPos().setX(nX);
        charList[charIndex].getPos().setY(nY);

        charList[charIndex].setMoveOffsetX(-1 * (TILE_PIXEL_SIZE * addX));
        charList[charIndex].setMoveOffsetY(-1 * (TILE_PIXEL_SIZE * addY));

        charList[charIndex].setMoving(true);
        charList[charIndex].setHeading(nDirection);

        charList[charIndex].setScrollDirectionX(addX);
        charList[charIndex].setScrollDirectionY(addY);
    }

    /**
     * @param x Posicion X del usuario.
     * @param y Posicion Y del usuario.
     * @return True si se encuentra dentro del limite del mapa, false en caso
     *         contrario.
     */
    public boolean inMapBounds(int x, int y) {
        return x < TILE_BUFFER_SIZE || x > XMaxMapSize - TILE_BUFFER_SIZE || y < TILE_BUFFER_SIZE
                || y > YMaxMapSize - TILE_BUFFER_SIZE;
    }

    public boolean hayAgua(int x, int y) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return false;
        var mapData = context.getMapData();
        return ((mapData[x][y].getLayer(1).getGrhIndex() >= 1505 && mapData[x][y].getLayer(1).getGrhIndex() <= 1520)
                || (mapData[x][y].getLayer(1).getGrhIndex() >= 5665 && mapData[x][y].getLayer(1).getGrhIndex() <= 5680)
                || (mapData[x][y].getLayer(1).getGrhIndex() >= 13547
                        && mapData[x][y].getLayer(1).getGrhIndex() <= 13562))
                && mapData[x][y].getLayer(2).getGrhIndex() == 0;
    }

    /**
     * @param charIndex Numero de identificador de personaje
     * @param nX        Posicion X a actualizar
     * @param nY        Posicion Y a actualizar Mueve el personaje segun la
     *                  direccion establecida en "nX" y "nY".
     */
    public void moveCharbyPos(short charIndex, int nX, int nY) {
        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        final int x = charList[charIndex].getPos().getX();
        final int y = charList[charIndex].getPos().getY();

        final int addX = nX - x;
        final int addY = nY - y;

        if (sgn((short) addX) == 1)
            charList[charIndex].setHeading(RIGHT);
        else if (sgn((short) addX) == -1)
            charList[charIndex].setHeading(LEFT);
        else if (sgn((short) addY) == -1)
            charList[charIndex].setHeading(UP);
        else if (sgn((short) addY) == 1)
            charList[charIndex].setHeading(DOWN);

        if (mapData != null) {
            mapData[nX][nY].setCharIndex(charIndex);
            mapData[x][y].setCharIndex(0);
        }

        charList[charIndex].getPos().setX(nX);
        charList[charIndex].getPos().setY(nY);

        charList[charIndex].setMoveOffsetX(-1 * (TILE_PIXEL_SIZE * addX));
        charList[charIndex].setMoveOffsetY(-1 * (TILE_PIXEL_SIZE * addY));

        charList[charIndex].setMoving(true);

        charList[charIndex].setScrollDirectionX(sgn((short) addX));
        charList[charIndex].setScrollDirectionY(sgn((short) addY));

    }

    /**
     * @param direction Mueve nuestro personaje a una cierta direccion si es
     *                  posible.
     */
    public void moveTo(Direction direction) {
        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();

        boolean legalOk = switch (direction) {
            case UP -> moveToLegalPos(userPos.getX(), userPos.getY() - 1);
            case RIGHT -> moveToLegalPos(userPos.getX() + 1, userPos.getY());
            case DOWN -> moveToLegalPos(userPos.getX(), userPos.getY() + 1);
            case LEFT -> moveToLegalPos(userPos.getX() - 1, userPos.getY());
        };

        if (legalOk) {
            moveScreen(direction);

            // Solo mover al personaje si el modo caminata está activo
            if (walkingmode) {

                // Duplicate Trigger 1 check removed. Handled by EditorInputManager.
            }
        } else if (walkingmode && userCharIndex > 0 && charList[userCharIndex].getHeading() != direction) {
            // Solo cambiar el rumbo en modo caminata
            charList[userCharIndex].setHeading(direction);
        }

    }

    public boolean isUserMoving() {
        return userMoving;
    }

    public void setUserMoving(boolean userMoving) {
        this.userMoving = userMoving;
    }

    public Position getUserPos() {
        return userPos;
    }

    public Position getAddToUserPos() {
        return addToUserPos;
    }

    public boolean isUnderCeiling() {
        return underCeiling;
    }

    public void setUnderCeiling(boolean underCeiling) {
        this.underCeiling = underCeiling;
    }

    public short getUserCharIndex() {
        return userCharIndex;
    }

    public void setUserCharIndex(short userCharIndex) {
        this.userCharIndex = userCharIndex;
    }

    public short getUserMap() {
        return userMap;
    }

    public void setUserMap(short userMap) {
        this.userMap = userMap;
    }

    public void teleport(int x, int y) {
        if (x < XMinMapSize || x > XMaxMapSize || y < YMinMapSize || y > YMaxMapSize)
            return;

        userPos.setX(x);
        userPos.setY(y);
        addToUserPos.setX(0); // Reset smooth movement offset if any
        addToUserPos.setY(0);
        underCeiling = checkUnderCeiling();

        // Si hay char index asociado, actualizar su posición
        if (userCharIndex > 0) {
            var context = GameData.getActiveContext();
            if (context != null) {
                var charList = context.getCharList();
                charList[userCharIndex].getPos().setX(x);
                charList[userCharIndex].getPos().setY(y);
            }
        }
    }

    public boolean isWalkingmode() {
        return walkingmode;
    }

    public void setWalkingmode(boolean walkingmode) {
        this.walkingmode = walkingmode;
        if (walkingmode) {
            // AUTO-SYNC: Update position to match Camera center when enabling Walk Mode
            if (Engine.getCurrentScene() instanceof GameScene) {
                Camera cam = ((GameScene) Engine.getCurrentScene()).getCamera();
                if (cam != null) {
                    var context = GameData.getActiveContext();
                    if (context != null) {
                        var mapData = context.getMapData();

                        // Prevent Clones: Brute-force clear user from ENTIRE map
                        // This ensures no ghost at 50,50 or anywhere else
                        if (userCharIndex > 0 && mapData != null) {
                            for (int x = XMinMapSize; x <= XMaxMapSize; x++) {
                                for (int y = YMinMapSize; y <= YMaxMapSize; y++) {
                                    if (mapData[x][y].getCharIndex() == userCharIndex) {
                                        mapData[x][y].setCharIndex(0);
                                    }
                                }
                            }
                        }
                    }

                    // Teleport to Camera Center
                    teleport(cam.getCenterX(), cam.getCenterY());
                }
            }

            var context = GameData.getActiveContext();
            if (context == null)
                return;
            var charList = context.getCharList();

            if (userCharIndex <= 0) {
                org.tinylog.Logger.warn("WalkingMode enabled with invalid UserCharIdx. Forcing refresh.");
                refreshUserCharacter();
            } else if (charList != null && userCharIndex < charList.length) {
                // Check if char slot is active/valid
                if (!charList[userCharIndex].isActive()
                        || charList[userCharIndex].getBody() == null) {
                    org.tinylog.Logger.warn("WalkingMode enabled but CharInx " + userCharIndex
                            + " is inactive/empty. Forcing refresh.");
                    refreshUserCharacter();
                } else {
                    // Always refresh to sync visual position with new UserPos
                    refreshUserCharacter();
                }
            } else {
                refreshUserCharacter();
            }
        }
    }

    /**
     * @param x Posicion X del usuario.
     * @param y Posicion Y del usuario.
     * @return True si el usuario puede caminar hacia cierta posicion, false caso
     *         contrario.
     */
    private boolean moveToLegalPos(int x, int y) {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return false;
        var mapData = context.getMapData();

        // Limite del mapa
        if (x < XMinMapSize || x > XMaxMapSize || y < YMinMapSize || y > YMaxMapSize)
            return false;

        // ¿Modo caminata activo?
        if (!walkingmode) {
            // Modo cámara libre - sin restricciones
            return true;
        }

        // Modo caminata - aplicar restricciones
        // ¿Tile bloqueado?
        if (mapData[x][y].getBlocked()) {
            return false;
        }

        final int charIndex = mapData[x][y].getCharIndex();

        // ¿Hay un personaje?
        if (charIndex > 0) {
            return false;
        }

        return true;
    }

    /**
     * Checks if the user needs to update appearance based on terrain (e.g.
     * swimming)
     * and refreshes the character.
     */
    public void checkAppearance() {
        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();

        if (userCharIndex > 0 && charList != null && userCharIndex < charList.length) {
            org.argentumforge.engine.game.models.Character chr = charList[userCharIndex];
            boolean onWater = hayAgua(userPos.getX(), userPos.getY());

            // Logic for appearance change
            if (onWater) {
                // Swimming Appearance: User Configured Water Body
                // Optimization: Check if already set
                if (AssetRegistry.bodyData != null && userWaterBody < AssetRegistry.bodyData.length
                        && AssetRegistry.bodyData[userWaterBody] != null) {
                    if (chr.getBody().getWalk(3).getGrhIndex() != AssetRegistry.bodyData[userWaterBody].getWalk(3)
                            .getGrhIndex()) {
                        chr.setBody(new BodyData(AssetRegistry.bodyData[userWaterBody]));
                        chr.setHead(new HeadData()); // Empty
                                                     // head
                                                     // (invisible)
                    }
                }
            } else {
                // Restore Normal Appearance
                // Optimization: Check if reset needed
                if (AssetRegistry.bodyData != null && userBody < AssetRegistry.bodyData.length
                        && AssetRegistry.bodyData[userBody] != null) {
                    if (chr.getBody().getWalk(3).getGrhIndex() != AssetRegistry.bodyData[userBody].getWalk(3)
                            .getGrhIndex()) {
                        refreshUserCharacter();
                    }
                }
            }
        }
    }

    public void resetMovement() {
        this.userMoving = false;
        this.addToUserPos.setX(0);
        this.addToUserPos.setY(0);

        var context = GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();

        if (userCharIndex > 0 && userCharIndex < charList.length) {
            charList[userCharIndex].setMoveOffsetX(0);
            charList[userCharIndex].setMoveOffsetY(0);
            charList[userCharIndex].setMoving(false);
            charList[userCharIndex].setScrollDirectionX(0);
            charList[userCharIndex].setScrollDirectionY(0);
        }
    }

    public void removeInstanceFromMap() {
        var context = GameData.getActiveContext();
        if (context == null || context.getMapData() == null)
            return;
        var mapData = context.getMapData();

        if (userCharIndex > 0 && mapData != null) {
            for (int x = XMinMapSize; x <= XMaxMapSize; x++) {
                for (int y = YMinMapSize; y <= YMaxMapSize; y++) {
                    if (mapData[x][y].getCharIndex() == userCharIndex) {
                        mapData[x][y].setCharIndex(0);
                    }
                }
            }
        }
    }
}
