package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.flag.ImGuiCol;
import imgui.type.ImInt;
import org.argentumforge.engine.utils.editor.Transfer;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.i18n.I18n;

/**
 * Ventana para la unión automática de mapas adyacentes.
 * <p>
 * Permite definir los IDs de los mapas que rodean al actual y aplicar
 * automáticamente los traslados en los bordes correspondientes.
 */
public final class FAutoUnion extends Form {

    private final ImInt northMap = new ImInt(0);
    private final ImInt southMap = new ImInt(0);
    private final ImInt eastMap = new ImInt(0);
    private final ImInt westMap = new ImInt(0);

    private boolean northApply = true;
    private boolean southApply = true;
    private boolean eastApply = true;
    private boolean westApply = true;

    @Override
    public void render() {
        ImGui.setNextWindowSize(550, 420, ImGuiCond.Once);
        if (ImGui.begin(I18n.INSTANCE.get("editor.autoUnion.title"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {

            // === DIBUJO DEL ÁREA CENTRAL (Representación del Mapa) ===
            float winWidth = ImGui.getWindowWidth();
            float boxWidth = 350;
            float boxHeight = 220;
            float offsetX = (winWidth - boxWidth) / 2;
            float startY = 40;

            // Etiquetas de Coordenadas (Esquinas) - Estilo original
            // Top Left
            ImGui.setCursorPos(offsetX - 60, startY);
            ImGui.textColored(0.5f, 0.5f, 1.0f, 1.0f, I18n.INSTANCE.get("common.coord.y") + " 90");
            ImGui.setCursorPos(offsetX - 60, startY + 15);
            ImGui.textColored(1.0f, 0.6f, 0.6f, 1.0f, I18n.INSTANCE.get("common.coord.x") + " 89");

            // Top Right
            ImGui.setCursorPos(offsetX + boxWidth + 10, startY);
            ImGui.textColored(1.0f, 0.0f, 0.0f, 1.0f, I18n.INSTANCE.get("common.coord.x") + " 90");
            ImGui.setCursorPos(offsetX + boxWidth + 10, startY + 15);
            ImGui.textColored(0.3f, 0.3f, 0.8f, 1.0f, I18n.INSTANCE.get("common.coord.y") + " 10");

            // Bottom Left
            ImGui.setCursorPos(offsetX - 60, startY + boxHeight - 30);
            ImGui.textColored(0.0f, 1.0f, 1.0f, 1.0f, I18n.INSTANCE.get("common.coord.y") + " 91");
            ImGui.setCursorPos(offsetX - 60, startY + boxHeight - 15);
            ImGui.textColored(1.0f, 0.5f, 0.0f, 1.0f, I18n.INSTANCE.get("common.coord.x") + " 11");

            // Bottom Right
            ImGui.setCursorPos(offsetX + boxWidth + 10, startY + boxHeight - 30);
            ImGui.textColored(0.5f, 0.5f, 1.0f, 1.0f, I18n.INSTANCE.get("common.coord.y") + " 11");
            ImGui.setCursorPos(offsetX + boxWidth + 10, startY + boxHeight - 15);
            ImGui.textColored(1.0f, 0.6f, 0.8f, 1.0f, I18n.INSTANCE.get("common.coord.x") + " 12");

            // Rectángulo del Mapa (Uso de ChildWindow para simular el recuadro)
            ImGui.setCursorPos(offsetX, startY);
            ImGui.pushStyleColor(ImGuiCol.ChildBg, 0xFF333333);
            ImGui.beginChild("MapBox", boxWidth, boxHeight, true);

            // --- Norte ---
            ImGui.setCursorPos(boxWidth / 2 - 60, 20);
            ImGui.text(I18n.INSTANCE.get("editor.autoUnion.map"));
            ImGui.sameLine();
            ImGui.pushItemWidth(50);
            ImGui.inputInt("##NMap", northMap, 0, 0);
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.checkbox(I18n.INSTANCE.get("editor.autoUnion.apply") + "##N", northApply))
                northApply = !northApply;

            // --- Oeste ---
            ImGui.setCursorPos(15, boxHeight / 2 - 20);
            ImGui.text(I18n.INSTANCE.get("editor.autoUnion.map"));
            ImGui.sameLine();
            ImGui.pushItemWidth(50);
            ImGui.inputInt("##WMap", westMap, 0, 0);
            ImGui.popItemWidth();
            ImGui.setCursorPos(15, boxHeight / 2);
            if (ImGui.checkbox(I18n.INSTANCE.get("editor.autoUnion.apply") + "##W", westApply))
                westApply = !westApply;

            // --- Este ---
            ImGui.setCursorPos(boxWidth - 140, boxHeight / 2 - 20);
            ImGui.text(I18n.INSTANCE.get("editor.autoUnion.map"));
            ImGui.sameLine();
            ImGui.pushItemWidth(50);
            ImGui.inputInt("##EMap", eastMap, 0, 0);
            ImGui.popItemWidth();
            ImGui.setCursorPos(boxWidth - 140, boxHeight / 2);
            ImGui.sameLine();
            if (ImGui.checkbox(I18n.INSTANCE.get("editor.autoUnion.apply") + "##E", eastApply))
                eastApply = !eastApply;

            // --- Sur ---
            ImGui.setCursorPos(boxWidth / 2 - 60, boxHeight - 50);
            ImGui.text(I18n.INSTANCE.get("editor.autoUnion.map"));
            ImGui.sameLine();
            ImGui.pushItemWidth(50);
            ImGui.inputInt("##SMap", southMap, 0, 0); // Note: IDs might vary but content is the same
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.checkbox(I18n.INSTANCE.get("editor.autoUnion.apply") + "##S", southApply))
                southApply = !southApply;

            ImGui.endChild();
            ImGui.popStyleColor();

            // === SECCIÓN INFERIOR: ACCIONES ===
            float footerY = startY + boxHeight + 40;
            ImGui.setCursorPos(10, footerY);

            // Botón Default (Azul)
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0xFF882222);
            if (ImGui.button(I18n.INSTANCE.get("editor.autoUnion.default"), 90, 30)) {
                northMap.set(0);
                southMap.set(0);
                eastMap.set(0);
                westMap.set(0);
                northApply = southApply = eastApply = westApply = true;
            }
            ImGui.popStyleColor();

            ImGui.sameLine();
            ImGui.setCursorPosY(footerY + 5);
            ImGui.textColored(1f, 1f, 1f, 0.8f, I18n.INSTANCE.get("editor.autoUnion.note"));

            // Botón Aplicar (Verde) y Cancelar (Rojo)
            ImGui.setCursorPos(winWidth - 210, footerY);
            ImGui.pushStyleColor(imgui.flag.ImGuiCol.Button, 0xFF008800);
            if (ImGui.button(I18n.INSTANCE.get("common.apply"), 90, 30)) {
                int n = northApply ? northMap.get() : -1;
                int s = southApply ? southMap.get() : -1;
                int e = eastApply ? eastMap.get() : -1;
                int w = westApply ? westMap.get() : -1;
                Transfer.getInstance().autoUnionBorders(GameData.getActiveContext(), n,
                        s, e, w);
            }
            ImGui.popStyleColor();

            ImGui.sameLine();
            ImGui.pushStyleColor(ImGuiCol.Button, 0xFF000088);
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 90, 30)) {
                this.close();
            }
            ImGui.popStyleColor();

            // Leyenda de Colores
            ImGui.setCursorPos(20, footerY + 50);
            ImGui.textColored(1.0f, 0.4f, 0.4f, 1.0f, I18n.INSTANCE.get("editor.autoUnion.legendX"));
            ImGui.sameLine();
            ImGui.textColored(0.4f, 0.4f, 1.0f, 1.0f, I18n.INSTANCE.get("editor.autoUnion.legendY"));

            ImGui.end();
        }
    }
}
