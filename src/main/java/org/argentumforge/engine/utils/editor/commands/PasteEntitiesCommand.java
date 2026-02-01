package org.argentumforge.engine.utils.editor.commands;

import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.editor.Clipboard;
import org.argentumforge.engine.utils.editor.Selection;
import java.util.ArrayList;
import java.util.List;

/**
 * Comando para pegar múltiples entidades desde el portapapeles.
 */
public class PasteEntitiesCommand extends AbstractCommand {
    @Override
    public String getName() {
        return I18n.INSTANCE.get("history.command.paste");
    }

    private final List<Command> commands = new ArrayList<>();

    public PasteEntitiesCommand(org.argentumforge.engine.utils.MapContext context, List<Clipboard.ClipboardItem> items,
            int destX, int destY) {
        this(context, items, destX, destY, Clipboard.getInstance().getSettings());
    }

    public PasteEntitiesCommand(org.argentumforge.engine.utils.MapContext context, List<Clipboard.ClipboardItem> items,
            int destX, int destY, Clipboard.PasteSettings settings) {
        super(context);
        var mapData = context.getMapData();
        if (mapData == null)
            return;

        for (Clipboard.ClipboardItem item : items) {
            int tx = destX + item.offsetX;
            int ty = destY + item.offsetY;

            if (tx < 0 || tx >= mapData.length || ty < 0 || ty >= mapData[0].length)
                continue;

            var tile = mapData[tx][ty];

            // 1. NPC
            if (item.type == Selection.EntityType.NPC && settings.npc) {
                int oldNpc = tile.getNpcIndex();
                commands.add(new NpcChangeCommand(context, tx, ty, oldNpc, item.id));
            }
            // 2. OBJECT
            else if (item.type == Selection.EntityType.OBJECT && settings.objects) {
                int oldObj = tile.getObjGrh().getGrhIndex();
                commands.add(new ObjChangeCommand(context, tx, ty, oldObj, item.id));
            }
            // 3. TILE (All attributes)
            else if (item.type == Selection.EntityType.TILE) {
                // Layers
                if (item.layers != null) {
                    for (int i = 1; i <= 4; i++) {
                        if (settings.layers[i - 1]) {
                            short oldGrh = (short) tile.getLayer(i).getGrhIndex();
                            commands.add(new TileChangeCommand(context, tx, ty, i, oldGrh, (short) item.layers[i]));
                        }
                    }
                }

                // Blocking
                if (settings.blocked) {
                    boolean oldBlocked = tile.getBlocked();
                    if (oldBlocked != item.blocked) {
                        java.util.Map<BlockChangeCommand.TilePos, Boolean> oldMap = java.util.Map
                                .of(new BlockChangeCommand.TilePos(tx, ty), oldBlocked);
                        java.util.Map<BlockChangeCommand.TilePos, Boolean> newMap = java.util.Map
                                .of(new BlockChangeCommand.TilePos(tx, ty), item.blocked);
                        commands.add(new BlockChangeCommand(context, oldMap, newMap));
                    }
                }

                // Trigger
                if (settings.triggers) {
                    int oldTrigger = tile.getTrigger();
                    if (oldTrigger != item.trigger) {
                        java.util.Map<TriggerChangeCommand.TilePos, Short> oldMap = java.util.Map
                                .of(new TriggerChangeCommand.TilePos(tx, ty), (short) oldTrigger);
                        java.util.Map<TriggerChangeCommand.TilePos, Short> newMap = java.util.Map
                                .of(new TriggerChangeCommand.TilePos(tx, ty), (short) item.trigger);
                        commands.add(new TriggerChangeCommand(context, oldMap, newMap));
                    }
                }

                // Transition (Exit)
                if (settings.transitions) {
                    int oldExitMap = tile.getExitMap();
                    int oldExitX = tile.getExitX();
                    int oldExitY = tile.getExitY();
                    if (oldExitMap != item.exitMap || oldExitX != item.exitX || oldExitY != item.exitY) {
                        commands.add(new TransferChangeCommand(context, tx, ty, oldExitMap, oldExitX, oldExitY,
                                item.exitMap, item.exitX, item.exitY));
                    }
                }

                // Particles
                if (settings.particles) {
                    int oldParticle = tile.getParticleIndex();
                    if (oldParticle != item.particleIndex) {
                        java.util.Map<ParticleChangeCommand.TilePos, Integer> oldMap = java.util.Map
                                .of(new ParticleChangeCommand.TilePos(tx, ty), oldParticle);
                        java.util.Map<ParticleChangeCommand.TilePos, Integer> newMap = java.util.Map
                                .of(new ParticleChangeCommand.TilePos(tx, ty), item.particleIndex);
                        commands.add(new ParticleChangeCommand(context, oldMap, newMap));
                    }
                }
            }
        }
    }

    @Override
    public void execute() {
        for (Command cmd : commands) {
            cmd.execute();
        }
    }

    @Override
    public void undo() {
        for (int i = commands.size() - 1; i >= 0; i--) {
            commands.get(i).undo();
        }
    }
}
