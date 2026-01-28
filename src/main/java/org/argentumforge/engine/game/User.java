package org.argentumforge.engine.game;

import org.argentumforge.engine.game.models.*;

import static org.argentumforge.engine.game.models.Character.*;
import static org.argentumforge.engine.game.models.Direction.*;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.*;
import org.argentumforge.engine.utils.GameData; // Import class to avoid resolution errors
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
    private short userBody = 1;
    private short userHead = 4; // Default cambiado a 4 segun preferencia del usuario

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

        org.tinylog.Logger.info("refreshUserCharacter START: Index=" + userCharIndex);

        // Asegurar que el slot esta activo y configurado
        GameData.charList[userCharIndex].setActive(true);
        GameData.charList[userCharIndex].getPos().setX(userPos.getX());
        GameData.charList[userCharIndex].getPos().setY(userPos.getY());
        GameData.charList[userCharIndex].setHeading(Direction.DOWN);

        // Aplicar apariencia persistente
        GameData.charList[userCharIndex].setiBody(userBody);
        GameData.charList[userCharIndex].setiHead(userHead);

        // Actualizar objetos de datos (Importante para renderizado!)
        if (AssetRegistry.bodyData != null && userBody < AssetRegistry.bodyData.length
                && AssetRegistry.bodyData[userBody] != null) {
            GameData.charList[userCharIndex].setBody(new BodyData(AssetRegistry.bodyData[userBody]));
        } else {
            GameData.charList[userCharIndex].setBody(new BodyData());
        }

        if (AssetRegistry.headData != null && userHead < AssetRegistry.headData.length
                && AssetRegistry.headData[userHead] != null) {
            GameData.charList[userCharIndex].setHead(new HeadData(AssetRegistry.headData[userHead]));
        } else {
            GameData.charList[userCharIndex].setHead(new HeadData());
        }

        GameData.charList[userCharIndex].setHelmet(new HeadData()); // Dummy
        GameData.charList[userCharIndex].setWeapon(new WeaponData());
        GameData.charList[userCharIndex].setShield(new ShieldData());

        // Update lastChar just in case
        if (userCharIndex > org.argentumforge.engine.game.models.Character.lastChar) {
            org.argentumforge.engine.game.models.Character.lastChar = userCharIndex;
        }

        // Actualizar mapa
        if (GameData.mapData != null) {
            int x = userPos.getX();
            int y = userPos.getY();
            // Check bounds directly using mapData length
            if (x >= 1 && x < GameData.mapData.length && y >= 1 && y < GameData.mapData[0].length) {
                GameData.mapData[x][y].setCharIndex(userCharIndex);
            }
        }
    }

    public void setUserBody(short body) {
        this.userBody = body;
    }

    public short getUserBody() {
        return userBody;
    }

    public void setUserHead(short head) {
        this.userHead = head;
    }

    public short getUserHead() {
        return userHead;
    }

    /**
     * @param nDirection direccion pasada por parametro Mueve la camara hacia una
     *                   direccion.
     */
    public void moveScreen(Direction nDirection) {
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
            addToUserPos.setX(x);
            userPos.setX(tX);
            addToUserPos.setY(y);
            userPos.setY(tY);
            userMoving = true;
            underCeiling = checkUnderCeiling();
        }

    }

    /**
     * Checkea si estamos bajo techo segun el trigger en donde esta parado el
     * usuario.
     */
    public boolean checkUnderCeiling() {
        return mapData[userPos.getX()][userPos.getY()].getTrigger() == 1 ||
                mapData[userPos.getX()][userPos.getY()].getTrigger() == 2 ||
                mapData[userPos.getX()][userPos.getY()].getTrigger() == 4;
    }

    /**
     * @param charIndex  Numero de identificador de personaje
     * @param nDirection Direccion del personaje Mueve el personaje segun la
     *                   direccion establecida en "nHeading".
     */
    public void moveCharbyHead(short charIndex, Direction nDirection) {
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

        mapData[nX][nY].setCharIndex(charIndex);
        charList[charIndex].getPos().setX(nX);
        charList[charIndex].getPos().setY(nY);
        mapData[x][y].setCharIndex(0);

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
        return ((mapData[x][y].getLayer(1).getGrhIndex() >= 1505 && mapData[x][y].getLayer(1).getGrhIndex() <= 1520) ||
                (mapData[x][y].getLayer(1).getGrhIndex() >= 5665 && mapData[x][y].getLayer(1).getGrhIndex() <= 5680) ||
                (mapData[x][y].getLayer(1).getGrhIndex() >= 13547 && mapData[x][y].getLayer(1).getGrhIndex() <= 13562))
                &&
                mapData[x][y].getLayer(2).getGrhIndex() == 0;
    }

    /**
     * @param charIndex Numero de identificador de personaje
     * @param nX        Posicion X a actualizar
     * @param nY        Posicion Y a actualizar Mueve el personaje segun la
     *                  direccion establecida en "nX" y "nY".
     */
    public void moveCharbyPos(short charIndex, int nX, int nY) {
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

        mapData[nX][nY].setCharIndex(charIndex);
        charList[charIndex].getPos().setX(nX);
        charList[charIndex].getPos().setY(nY);
        mapData[x][y].setCharIndex(0);

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
                moveCharbyHead(userCharIndex, direction);

                // Check for Map Transfer (Trigger 1)
                if (userCharIndex > 0 && userCharIndex < charList.length) {
                    int x = charList[userCharIndex].getPos().getX();
                    int y = charList[userCharIndex].getPos().getY();

                    // Trigger 1 = Map Transfer
                    if (inMapBounds(x, y) && mapData[x][y].getTrigger() == 1) {
                        int destMap = mapData[x][y].getExitMap();
                        if (destMap > 0) {
                            int response = javax.swing.JOptionPane.showConfirmDialog(null,
                                    "¿Desea viajar al Mapa " + destMap + "?",
                                    "Traslado de Mapa",
                                    javax.swing.JOptionPane.YES_NO_OPTION);

                            if (response == javax.swing.JOptionPane.YES_OPTION) {
                                if (org.argentumforge.engine.utils.MapManager.checkUnsavedChanges()) {
                                    org.argentumforge.engine.utils.MapManager.loadMap(destMap);
                                    // Update position to exit coordinates
                                    int destX = mapData[x][y].getExitX();
                                    int destY = mapData[x][y].getExitY();
                                    if (inMapBounds(destX, destY)) {
                                        userPos.setX(destX);
                                        userPos.setY(destY);
                                        refreshUserCharacter(); // Ensure correct position on new map
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (walkingmode && charList[userCharIndex].getHeading() != direction) {
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
            charList[userCharIndex].getPos().setX(x);
            charList[userCharIndex].getPos().setY(y);
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
                    // Prevent Clones: Brute-force clear user from ENTIRE map
                    // This ensures no ghost at 50,50 or anywhere else
                    if (userCharIndex > 0 && GameData.mapData != null) {
                        for (int x = XMinMapSize; x <= XMaxMapSize; x++) {
                            for (int y = YMinMapSize; y <= YMaxMapSize; y++) {
                                if (GameData.mapData[x][y].getCharIndex() == userCharIndex) {
                                    GameData.mapData[x][y].setCharIndex(0);
                                }
                            }
                        }
                    }

                    // Teleport to Camera Center
                    teleport(cam.getCenterX(), cam.getCenterY());
                }
            }

            if (userCharIndex <= 0) {
                org.tinylog.Logger.warn("WalkingMode enabled with invalid UserCharIdx. Forcing refresh.");
                refreshUserCharacter();
            } else if (GameData.charList != null && userCharIndex < GameData.charList.length) {
                // Check if char slot is active/valid
                if (!GameData.charList[userCharIndex].isActive()
                        || GameData.charList[userCharIndex].getBody() == null) {
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
        if (mapData[x][y].getBlocked())
            return false;

        final int charIndex = mapData[x][y].getCharIndex();

        // ¿Hay un personaje?
        if (charIndex > 0) {
            return false;
        }

        return true;
    }

    public void resetMovement() {
        this.userMoving = false;
        this.addToUserPos.setX(0);
        this.addToUserPos.setY(0);

        if (userCharIndex > 0 && userCharIndex < charList.length) {
            charList[userCharIndex].setMoveOffsetX(0);
            charList[userCharIndex].setMoveOffsetY(0);
            charList[userCharIndex].setMoving(false);
            charList[userCharIndex].setScrollDirectionX(0);
            charList[userCharIndex].setScrollDirectionY(0);
        }
    }

    public void removeInstanceFromMap() {
        if (userCharIndex > 0 && GameData.mapData != null) {
            for (int x = XMinMapSize; x <= XMaxMapSize; x++) {
                for (int y = YMinMapSize; y <= YMaxMapSize; y++) {
                    if (GameData.mapData[x][y].getCharIndex() == userCharIndex) {
                        GameData.mapData[x][y].setCharIndex(0);
                    }
                }
            }
        }
    }
}
