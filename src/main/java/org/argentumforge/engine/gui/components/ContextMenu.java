package org.argentumforge.engine.gui.components;

import imgui.ImGui;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.editor.Transfer;
import org.argentumforge.engine.utils.editor.commands.CommandManager;
import org.argentumforge.engine.utils.editor.commands.NpcChangeCommand;
import org.argentumforge.engine.utils.editor.commands.ObjChangeCommand;
import org.argentumforge.engine.utils.editor.commands.TransferChangeCommand;

public class ContextMenu {

    private static int tileX;
    private static int tileY;
    private static boolean shouldOpen = false;

    public static void open(int x, int y) {
        tileX = x;
        tileY = y;
        shouldOpen = true;
    }

    public static void render() {
        if (shouldOpen) {
            ImGui.openPopup("TileContextMenu");
            shouldOpen = false;
        }

        if (ImGui.beginPopup("TileContextMenu")) {
            var context = GameData.getActiveContext();
            if (context == null || context.getMapData() == null) {
                ImGui.textDisabled("Sin mapa cargado");
                ImGui.endPopup();
                return;
            }
            var mapData = context.getMapData();

            ImGui.textDisabled("Tile (" + tileX + ", " + tileY + ")");
            ImGui.separator();

            // --- NPC ---
            int npcIndex = mapData[tileX][tileY].getNpcIndex();
            if (npcIndex > 0) {
                if (ImGui.menuItem("Eliminar NPC (" + npcIndex + ")")) {
                    CommandManager.getInstance().executeCommand(
                            new NpcChangeCommand(context, tileX, tileY, npcIndex, 0));
                }
                ImGui.separator();
            }

            // --- Object ---
            if (mapData[tileX][tileY].getObjIndex() > 0) {
                if (ImGui.menuItem("Eliminar Objeto")) {
                    int oldGrh = mapData[tileX][tileY].getObjGrh().getGrhIndex();
                    CommandManager.getInstance().executeCommand(
                            new ObjChangeCommand(context, tileX, tileY, oldGrh, 0));
                }
                ImGui.separator();
            }

            // --- Transfer ---
            int exitMap = mapData[tileX][tileY].getExitMap();
            if (exitMap > 0) {
                // "Capturar Coordenadas" (Recuperar el destino de este traslado)
                if (ImGui.menuItem("Capturar Destino (" + exitMap + ")")) {
                    int destX = mapData[tileX][tileY].getExitX();
                    int destY = mapData[tileX][tileY].getExitY();
                    Transfer.getInstance().captureCoordinates(exitMap, destX, destY);
                }

                if (ImGui.menuItem("Eliminar Traslado")) {
                    int oldX = mapData[tileX][tileY].getExitX();
                    int oldY = mapData[tileX][tileY].getExitY();
                    CommandManager.getInstance().executeCommand(
                            new TransferChangeCommand(context, tileX, tileY, exitMap, oldX, oldY, 0,
                                    0, 0));
                }
            }

            // --- Block ---
            boolean isBlocked = mapData[tileX][tileY].getBlocked();
            if (ImGui.menuItem(isBlocked ? "Desbloquear" : "Bloquear")) {
                mapData[tileX][tileY].setBlocked(!isBlocked);
            }

            ImGui.endPopup();
        }
    }
}
