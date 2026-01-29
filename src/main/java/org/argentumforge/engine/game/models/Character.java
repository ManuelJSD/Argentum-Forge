package org.argentumforge.engine.game.models;

import org.argentumforge.engine.renderer.RGBColor;
import org.argentumforge.engine.utils.inits.*;

import org.argentumforge.engine.utils.AssetRegistry;

import static org.argentumforge.engine.game.models.Direction.DOWN;
import static org.argentumforge.engine.renderer.Drawn.drawTexture;

// static import removed
import static org.argentumforge.engine.utils.AssetRegistry.*;
import static org.argentumforge.engine.utils.Time.timerTicksPerFrame;

/**
 * Clase que representa a un personaje dentro del mundo de Argentum Online.
 * <p>
 * Esta clase implementa toda la funcionalidad relacionada con los personajes,
 * incluyendo jugadores controlados por usuarios, NPCs
 * y criaturas.
 * <p>
 * Gestiona los siguientes aspectos:
 * <ul>
 * <li>Atributos físicos (cuerpo, cabeza, armas, escudos, cascos)</li>
 * <li>Posicionamiento y movimiento en el mundo</li>
 * <li>Renderizado gráfico y animaciones</li>
 * <li>Efectos visuales asociados al personaje</li>
 * <li>Nombre del personaje</li>
 * </ul>
 * <p>
 * La clase contiene numerosas constantes que definen los rangos de indices de
 * cabezas y cuerpos para diferentes razas y generos,
 * asi como metodos estaticos para operaciones como crear, eliminar o redibujar
 * personajes en el mapa.
 * <p>
 * Character es una pieza central del motor grafico, gestionando tanto la
 * representacion visual como la logica de estado de todas
 * las entidades animadas que pueblan el mundo.
 */

public final class Character {

    // ultimo personaje del array
    public static short lastChar = 0;
    private boolean active;
    private Direction direction;
    private Position pos;
    private short iHead;
    private short iBody;
    private BodyData body;
    private HeadData head;
    private HeadData helmet;
    private WeaponData weapon;
    private ShieldData shield;

    private int walkingSpeed;

    // FX
    private GrhInfo fX;
    private int fxIndex;

    private short scrollDirectionX;
    private short scrollDirectionY;

    private boolean moving;
    private float moveOffsetX;
    private float moveOffsetY;

    public Character() {
        body = new BodyData();
        head = new HeadData();
        helmet = new HeadData();
        weapon = new WeaponData();
        shield = new ShieldData();
        this.pos = new Position();
        this.fX = new GrhInfo();

        this.direction = DOWN;
        this.active = false;
        this.fxIndex = 0;
        this.moving = false;
        this.pos.setX(0);
        this.pos.setY(0);

        this.walkingSpeed = 8;
    }

    /**
     * Crea un nuevo personaje segun los parametros establecidos.
     */
    /**
     * Crea un nuevo personaje segun los parametros establecidos.
     */
    public static void makeChar(short charIndex, int body, int head, Direction direction, int x, int y, int weapon,
            int shield, int helmet) {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        // apuntamos al ultimo char
        if (charIndex > lastChar)
            lastChar = charIndex;

        if (weapon == 0)
            weapon = 2;
        if (shield == 0)
            shield = 2;
        if (helmet == 0)
            helmet = 2;

        charList[charIndex].setiHead(head);
        charList[charIndex].setiBody(body);

        charList[charIndex].setHead(new HeadData(AssetRegistry.headData[head]));
        charList[charIndex].setBody(new BodyData(AssetRegistry.bodyData[body]));

        if (AssetRegistry.weaponData != null && weapon < AssetRegistry.weaponData.length
                && AssetRegistry.weaponData[weapon] != null)
            charList[charIndex].setWeapon(new WeaponData(AssetRegistry.weaponData[weapon]));
        else
            charList[charIndex].setWeapon(new WeaponData(new WeaponData()));

        if (AssetRegistry.shieldData != null && shield < AssetRegistry.shieldData.length
                && AssetRegistry.shieldData[shield] != null)
            charList[charIndex].setShield(new ShieldData(AssetRegistry.shieldData[shield]));
        else
            charList[charIndex].setShield(new ShieldData(new ShieldData()));

        if (AssetRegistry.helmetsData != null && helmet < AssetRegistry.helmetsData.length
                && AssetRegistry.helmetsData[helmet] != null)
            charList[charIndex].setHelmet(new HeadData(AssetRegistry.helmetsData[helmet]));
        else
            charList[charIndex].setHelmet(new HeadData(new HeadData()));

        charList[charIndex].setHeading(direction);

        // reiniciar estadísticas de movimiento
        charList[charIndex].setMoving(false);
        charList[charIndex].setMoveOffsetX(0);
        charList[charIndex].setMoveOffsetY(0);

        // update position
        charList[charIndex].getPos().setX(x);
        charList[charIndex].getPos().setY(y);

        // Hacer activo
        charList[charIndex].setActive(true);

        // trazar en el mapa
        mapData[x][y].setCharIndex(charIndex);
    }

    /**
     * @param charIndex Numero de identificador de personaje
     *                  Elimina un personaje del array de personajes.
     */
    public static void eraseChar(short charIndex) {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        charList[charIndex].setActive(false);

        if (charIndex == lastChar) {
            while (!charList[lastChar].isActive()) {
                lastChar--;
                if (lastChar == 0)
                    break;
            }
        }

        mapData[charList[charIndex].getPos().getX()][charList[charIndex].getPos().getY()].setCharIndex(0);

        /*
         * 'Remove char's dialog
         * Call Dialogos.RemoveDialog(CharIndex)
         */

        resetCharInfo(charIndex);
    }

    /**
     * elimina todos los personajes de nuestro array charList.
     */
    public static void eraseAllChars() {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        for (short i = 1; i < charList.length; i++) {
            if (charList[i] != null && charList[i].isActive()) {
                mapData[charList[i].getPos().getX()][charList[i].getPos().getY()].setCharIndex(0);
            }
            resetCharInfo(i);
        }
        lastChar = 0;
    }

    /**
     * @param charIndex Numero de identificador del personaje
     *                  Resetea los atributos del personaje.
     */
    private static void resetCharInfo(short charIndex) {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context != null) {
            context.getCharList()[charIndex] = new Character();
        }
    }

    /**
     * Actualiza todos los personajes visibles.
     */
    public static void refreshAllChars() {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();
        var mapData = context.getMapData();

        for (int loopC = 1; loopC <= lastChar; loopC++)
            if (charList[loopC] != null && charList[loopC].isActive())
                mapData[charList[loopC].getPos().getX()][charList[loopC].getPos().getY()].setCharIndex(loopC);
    }

    public static void drawCharacter(int charIndex, int PixelOffsetX, int PixelOffsetY, float alpha,
            RGBColor ambientcolor) {
        drawCharacter(charIndex, PixelOffsetX, PixelOffsetY, alpha, ambientcolor, 1.0f, 1.0f, 0.0f);
    }

    public static void drawCharacter(int charIndex, int PixelOffsetX, int PixelOffsetY, float alpha,
            RGBColor ambientcolor, float scaleX, float scaleY, float skewX) {
        var context = org.argentumforge.engine.utils.GameData.getActiveContext();
        if (context == null)
            return;
        var charList = context.getCharList();

        boolean moved = false;

        // Calcular factor de respiración
        float breathingScale = 1.0f;
        if (org.argentumforge.engine.game.Options.INSTANCE.getRenderSettings().isShowNpcBreathing()) {
            // Oscilación suave
            breathingScale = 1.0f + (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.025f);
        }

        if (charList[charIndex].getMoving()) {
            if (charList[charIndex].getScrollDirectionX() != 0) {

                charList[charIndex].setMoveOffsetX(charList[charIndex].getMoveOffsetX() +
                        charList[charIndex].getWalkingSpeed() * sgn(charList[charIndex].getScrollDirectionX())
                                * timerTicksPerFrame);

                if (charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).getSpeed() > 0.0f) {
                    charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).setStarted(true);
                }

                charList[charIndex].getWeapon().getWeaponWalk(charList[charIndex].getHeading().getId())
                        .setStarted(true);
                charList[charIndex].getShield().getShieldWalk(charList[charIndex].getHeading().getId())
                        .setStarted(true);

                moved = true;

                if ((sgn(charList[charIndex].getScrollDirectionX()) == 1 && charList[charIndex].getMoveOffsetX() >= 0)
                        ||
                        (sgn(charList[charIndex].getScrollDirectionX()) == -1
                                && charList[charIndex].getMoveOffsetX() <= 0)) {

                    charList[charIndex].setMoveOffsetX(0);
                    charList[charIndex].setScrollDirectionX(0);
                }
            }

            if (charList[charIndex].getScrollDirectionY() != 0) {
                charList[charIndex].setMoveOffsetY(charList[charIndex].getMoveOffsetY()
                        + charList[charIndex].getWalkingSpeed() * sgn(charList[charIndex].getScrollDirectionY())
                                * timerTicksPerFrame);

                if (charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).getSpeed() > 0.0f) {
                    charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).setStarted(true);
                }

                charList[charIndex].getWeapon().getWeaponWalk(charList[charIndex].getHeading().getId())
                        .setStarted(true);
                charList[charIndex].getShield().getShieldWalk(charList[charIndex].getHeading().getId())
                        .setStarted(true);

                moved = true;

                if ((sgn(charList[charIndex].getScrollDirectionY()) == 1 && charList[charIndex].getMoveOffsetY() >= 0)
                        || (sgn(charList[charIndex].getScrollDirectionY()) == -1
                                && charList[charIndex].getMoveOffsetY() <= 0)) {
                    charList[charIndex].setMoveOffsetY(0);
                    charList[charIndex].setScrollDirectionY(0);
                }
            }
        }

        if (!moved) {
            charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).setStarted(false);
            charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).setFrameCounter(1);

            charList[charIndex].getWeapon().getWeaponWalk(charList[charIndex].getHeading().getId()).setStarted(false);
            charList[charIndex].getWeapon().getWeaponWalk(charList[charIndex].getHeading().getId()).setFrameCounter(1);

            charList[charIndex].getShield().getShieldWalk(charList[charIndex].getHeading().getId()).setStarted(false);
            charList[charIndex].getShield().getShieldWalk(charList[charIndex].getHeading().getId()).setFrameCounter(1);

            charList[charIndex].setMoving(false);
        }

        PixelOffsetX += (int) charList[charIndex].getMoveOffsetX();
        PixelOffsetY += (int) charList[charIndex].getMoveOffsetY();

        if (charList[charIndex].getHead().getHead(charList[charIndex].getHeading().getId()).getGrhIndex() != 0) {

            float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();

            if (charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId())
                    .getGrhIndex() != 0) {
                drawTexture(charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()),
                        PixelOffsetX, PixelOffsetY, true, true, false, alpha, ambientcolor, scaleX,
                        breathingScale * scaleY, skewX);
            }

            if (charList[charIndex].getHead().getHead(charList[charIndex].getHeading().getId())
                    .getGrhIndex() != 0) {
                // La cabeza también respira (aunque menos notorio si solo se mueve, pero si el
                // cuerpo se estira, la cabeza debe acompañar)
                // Usualmente la cabeza NO se estira, solo se desplaza verticalmente.
                // Aquí solo aplico scale al offset Y. La cabeza mantiene su aspecto (scale
                // 1.0f, 1.0f).
                // Opcional: breathingScale también para la cabeza? Queda raro (cara larga).
                // Mejor solo desplazar.
                drawTexture(charList[charIndex].getHead().getHead(charList[charIndex].getHeading().getId()),
                        PixelOffsetX + (int) (charList[charIndex].getBody().getHeadOffset().getX() * scale * scaleX),
                        PixelOffsetY
                                + (int) (charList[charIndex].getBody().getHeadOffset().getY() * scale * breathingScale
                                        * scaleY),
                        true, false, false, alpha, ambientcolor, scaleX, scaleY, skewX);

                drawTexture(charList[charIndex].getHelmet().getHead(charList[charIndex].getHeading().getId()),
                        PixelOffsetX + (int) (charList[charIndex].getBody().getHeadOffset().getX() * scale * scaleX),
                        PixelOffsetY
                                + (int) (charList[charIndex].getBody().getHeadOffset().getY() * scale
                                        * breathingScale * scaleY)
                                - (int) (34 * scale * scaleY),
                        true, false, false, alpha, ambientcolor, scaleX, scaleY, skewX);

                drawTexture(
                        charList[charIndex].getWeapon().getWeaponWalk(charList[charIndex].getHeading().getId()),
                        PixelOffsetX, PixelOffsetY, true, true, false, alpha, ambientcolor, scaleX,
                        breathingScale * scaleY, skewX);

                drawTexture(
                        charList[charIndex].getShield().getShieldWalk(charList[charIndex].getHeading().getId()),
                        PixelOffsetX, PixelOffsetY, true, true, false, alpha, ambientcolor, scaleX,
                        breathingScale * scaleY, skewX);

            }

        } else {
            if (charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()).getGrhIndex() > 0) {
                drawTexture(charList[charIndex].getBody().getWalk(charList[charIndex].getHeading().getId()),
                        PixelOffsetX, PixelOffsetY, true, true, false, alpha, ambientcolor, scaleX,
                        breathingScale * scaleY, skewX);
            }
        }

        // Draw FX (sin respiración)
        if (charList[charIndex].fxIndex != 0) {
            float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();
            drawTexture(charList[charIndex].fX,
                    PixelOffsetX + (int) (fxData[charList[charIndex].fxIndex].getOffsetX() * scale),
                    PixelOffsetY + (int) (fxData[charList[charIndex].fxIndex].getOffsetY() * scale),
                    true, true, true, 1.0f, ambientcolor);

            // Comprobar si la animación ha terminado
            if (!charList[charIndex].fX.isStarted())
                charList[charIndex].setFxIndex(0);
        }
    }

    /**
     * Dibuja un "fantasma" de personaje sin necesidad de estar activado en la
     * lista global.
     */
    public static void drawCharacterGhost(int bodyIndex, int headIndex, int PixelOffsetX, int PixelOffsetY, float alpha,
            RGBColor ambientcolor) {
        if (bodyIndex <= 0 || bodyIndex >= AssetRegistry.bodyData.length || AssetRegistry.bodyData[bodyIndex] == null)
            return;

        BodyData body = AssetRegistry.bodyData[bodyIndex];
        Direction heading = DOWN;

        // Cuerpo (Frame 1)
        if (body.getWalk(heading.getId()).getGrhIndex() != 0) {
            drawTexture(body.getWalk(heading.getId()), PixelOffsetX, PixelOffsetY, true, false, false, alpha,
                    ambientcolor);
        }

        // Cabeza
        if (headIndex > 0 && headIndex < AssetRegistry.headData.length && AssetRegistry.headData[headIndex] != null) {
            HeadData head = AssetRegistry.headData[headIndex];
            float scale = org.argentumforge.engine.scenes.Camera.getZoomScale();
            if (head.getHead(heading.getId()).getGrhIndex() != 0) {
                drawTexture(head.getHead(heading.getId()),
                        PixelOffsetX + (int) (body.getHeadOffset().getX() * scale),
                        PixelOffsetY + (int) (body.getHeadOffset().getY() * scale),
                        true, false, false, alpha, ambientcolor);
            }
        }
    }

    public static int sgn(short number) {
        if (number == 0)
            return 0;
        return (number / Math.abs(number));
    }

    public int getWalkingSpeed() {
        return walkingSpeed;
    }

    public void setWalkingSpeed(int walkingSpeed) {
        this.walkingSpeed = walkingSpeed;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Direction getHeading() {
        return direction;
    }

    public void setHeading(Direction direction) {
        this.direction = direction;
    }

    public Position getPos() {
        return pos;
    }

    public short getiHead() {
        return iHead;
    }

    public void setiHead(int iHead) {
        this.iHead = (short) iHead;
    }

    public short getiBody() {
        return iBody;
    }

    public void setiBody(int iBody) {
        this.iBody = (short) iBody;
    }

    public BodyData getBody() {
        return body;
    }

    public void setBody(BodyData body) {
        this.body = body;
    }

    public HeadData getHead() {
        return head;
    }

    public void setHead(HeadData head) {
        this.head = head;
    }

    public HeadData getHelmet() {
        return helmet;
    }

    public void setHelmet(HeadData helmet) {
        this.helmet = helmet;
    }

    public WeaponData getWeapon() {
        return weapon;
    }

    public void setWeapon(WeaponData weapon) {
        this.weapon = weapon;
    }

    public ShieldData getShield() {
        return shield;
    }

    public void setShield(ShieldData shield) {
        this.shield = shield;
    }

    public GrhInfo getfX() {
        return fX;
    }

    public void setfX(GrhInfo fX) {
        this.fX = fX;
    }

    public int getFxIndex() {
        return fxIndex;
    }

    public void setFxIndex(int fxIndex) {
        this.fxIndex = fxIndex;
    }

    public short getScrollDirectionX() {
        return scrollDirectionX;
    }

    public void setScrollDirectionX(int scrollDirectionX) {
        this.scrollDirectionX = (short) scrollDirectionX;
    }

    public short getScrollDirectionY() {
        return scrollDirectionY;
    }

    public void setScrollDirectionY(int scrollDirectionY) {
        this.scrollDirectionY = (short) scrollDirectionY;
    }

    public boolean getMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public float getMoveOffsetX() {
        return moveOffsetX;
    }

    public void setMoveOffsetX(float moveOffsetX) {
        this.moveOffsetX = moveOffsetX;
    }

    public float getMoveOffsetY() {
        return moveOffsetY;
    }

    public void setMoveOffsetY(float moveOffsetY) {
        this.moveOffsetY = moveOffsetY;
    }

}
