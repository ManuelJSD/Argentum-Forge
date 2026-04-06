package org.argentumforge.engine.utils.inits;

/**
 * Almacena la definición técnica y los metadatos de un recurso gráfico (GRH).
 * <p>
 * {@code GrhData} contiene la información base cargada desde los archivos de
 * inicialización, incluyendo las coordenadas en el atlas de texturas,
 * dimensiones en píxeles, y la secuencia de frames para animaciones.
 *
 * @see GrhInfo
 * @see org.argentumforge.engine.utils.GameData
 * @see org.argentumforge.engine.renderer.Drawn
 */
public final class GrhData {

    private int sX;
    private int sY;

    private int fileNum;

    private int pixelWidth;
    private int pixelHeight;

    private float tileWidth;
    private float tileHeight;

    private int numFrames;
    private int[] frames;

    private float speed;

    public GrhData() {

    }

    public int getsX() {
        return sX;
    }

    public void setsX(int sX) {
        this.sX = sX;
    }

    public int getsY() {
        return sY;
    }

    public void setsY(int sY) {
        this.sY = sY;
    }

    public int getFileNum() {
        return fileNum;
    }

    public void setFileNum(int fileNum) {
        this.fileNum = fileNum;
    }

    public int getPixelWidth() {
        return pixelWidth;
    }

    public void setPixelWidth(int pixelWidth) {
        this.pixelWidth = pixelWidth;
    }

    public int getPixelHeight() {
        return pixelHeight;
    }

    public void setPixelHeight(int pixelHeight) {
        this.pixelHeight = pixelHeight;
    }

    public float getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(float tileWidth) {
        this.tileWidth = tileWidth;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(float tileHeight) {
        this.tileHeight = tileHeight;
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(int numFrames) {
        this.numFrames = numFrames;
    }

    public int getFrame(int index) {
        return frames[index];
    }

    public void setFrames(int[] frames) {
        this.frames = frames;
    }

    public void setFrame(int index, int frame) {
        this.frames[index] = frame;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

}
