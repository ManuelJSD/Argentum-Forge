package org.argentumforge.engine.game;

import org.argentumforge.engine.game.models.*;

import org.argentumforge.engine.utils.AssetRegistry;

import static org.argentumforge.engine.game.models.Character.*;
import static org.argentumforge.engine.game.models.Direction.*;
import static org.argentumforge.engine.scenes.Camera.*;
import static org.argentumforge.engine.utils.GameData.*;

/**
 * <p>
 * Representa el usuario controlado por el jugador. Centraliza toda la
 * informacion, logica de movimiento y estados del personaje
 * principal.
 * <p>
 * Esta clase maneja aspectos fundamentales como la posicion del usuario en el
 * mapa, direccion de movimiento, atributos y
 * estadisticas, inventario, hechizos y todas las interacciones con el entorno
 * de juego. Actua como una entidad central que
 * coordina las acciones del jugador con el resto del mundo virtual.
 * <p>
 * Entre sus responsabilidades principales se encuentran:
 * <ul>
 * <li>Control del movimiento y posicionamiento del usuario</li>
 * <li>Gestión de la posición en el mapa</li>
 * <li>Control de estados especiales (bajo techo, etc.)</li>
 * <li>Interacción con el entorno y otros personajes</li>
 * </ul>
 * <p>
 * La clase representa el nucleo de la experiencia de juego, siendo el punto de
 * conexion entre las acciones del jugador y su
 * representacion en el mundo virtual.
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

    User() {
        this.userPos = new Position();
        this.addToUserPos = new Position();
        this.walkingmode = false;
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

        if (!(tX < minXBorder || tX > maxXBorder || tY < minYBorder || tY > maxYBorder)) {
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

    private void resetState() {
    }

    public void teleport(int x, int y) {
        if (x < minXBorder || x > maxXBorder || y < minYBorder || y > maxYBorder)
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
    }

    /**
     * @param x Posicion X del usuario.
     * @param y Posicion Y del usuario.
     * @return True si el usuario puede caminar hacia cierta posicion, false caso
     *         contrario.
     */
    private boolean moveToLegalPos(int x, int y) {
        // Limite del mapa
        if (x < minXBorder || x > maxXBorder || y < minYBorder || y > maxYBorder)
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
}
