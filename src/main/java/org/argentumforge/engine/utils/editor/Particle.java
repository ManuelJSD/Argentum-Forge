package org.argentumforge.engine.utils.editor;

import org.argentumforge.engine.utils.editor.commands.ParticleChangeCommand;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import java.util.HashMap;
import java.util.Map;

public class Particle {

    public enum BrushShape {
        SQUARE, CIRCLE
    }

    private static Particle instance;

    // Herramienta
    private boolean isActive;
    // 0: seleccionar, 1: pintar
    private int mode;
    private int selectedParticleId = 0;

    private int brushSize = 1;
    private BrushShape brushShape = BrushShape.SQUARE;

    private Particle() {
    }

    public static Particle getInstance() {
        if (instance == null) {
            instance = new Particle();
        }
        return instance;
    }

    public void particle_edit(org.argentumforge.engine.utils.MapContext context, int x, int y) {
        if (context == null)
            return;
        var mapData = context.getMapData();
        if (!isActive || mapData == null)
            return;

        Map<ParticleChangeCommand.TilePos, Integer> oldStates = new HashMap<>();
        Map<ParticleChangeCommand.TilePos, Integer> newStates = new HashMap<>();

        int offset = brushSize / 2;
        double radiusSq = Math.pow(brushSize / 2.0, 2);

        for (int i = x - offset; i <= x + offset; i++) {
            for (int j = y - offset; j <= y + offset; j++) {
                // Validar limites
                if (i >= 0 && i < mapData.length && j >= 0 && j < mapData[0].length) {

                    if (brushShape == BrushShape.CIRCLE) {
                        double dx = i - x;
                        double dy = j - y;
                        if (dx * dx + dy * dy > radiusSq)
                            continue;
                    }

                    int oldParticle = mapData[i][j].getParticleIndex();
                    if (oldParticle != selectedParticleId) {
                        ParticleChangeCommand.TilePos pos = new ParticleChangeCommand.TilePos(i, j);
                        oldStates.put(pos, oldParticle);
                        newStates.put(pos, selectedParticleId);
                    }
                }
            }
        }

        if (!oldStates.isEmpty()) {
            CommandManager.getInstance().executeCommand(
                    new ParticleChangeCommand(context, oldStates,
                            newStates));
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (!active) {
            this.mode = 0;
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
        if (mode > 0) {
            this.isActive = true;
        }
    }

    public boolean isActive() {
        return isActive;
    }

    public void setSelectedParticleId(int id) {
        this.selectedParticleId = id;
    }

    public int getSelectedParticleId() {
        return selectedParticleId;
    }

    public int getBrushSize() {
        return brushSize;
    }

    public void setBrushSize(int brushSize) {
        this.brushSize = brushSize;
    }

    public BrushShape getBrushShape() {
        return brushShape;
    }

    public void setBrushShape(BrushShape brushShape) {
        this.brushShape = brushShape;
    }
}
