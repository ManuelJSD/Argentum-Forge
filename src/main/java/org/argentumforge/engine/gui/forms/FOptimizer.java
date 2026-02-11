package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.utils.GameData;
import org.argentumforge.engine.utils.MapContext;
import org.argentumforge.engine.utils.editor.MapOptimizer;
import org.argentumforge.engine.utils.editor.MapOptimizer.OptimizationOptions;
import org.argentumforge.engine.utils.editor.MapOptimizer.OptimizationResult;
import org.argentumforge.engine.utils.editor.commands.CommandManager;

public class FOptimizer extends Form {

    private final OptimizationOptions options = new OptimizationOptions();
    private String report = "";
    private boolean analysisDone = false;

    public FOptimizer() {
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(500, 550, imgui.flag.ImGuiCond.FirstUseEver);

        if (ImGui.begin(I18n.INSTANCE.get("optimizer.title"), ImGuiWindowFlags.NoCollapse)) {

            ImGui.textWrapped(I18n.INSTANCE.get("optimizer.description"));
            ImGui.separator();

            // Sección: Limpieza
            ImGui.textColored(0.2f, 0.8f, 1.0f, 1.0f, I18n.INSTANCE.get("optimizer.section.cleanup"));
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.cleanBorder"), options.cleanBorders)) {
                options.cleanBorders = !options.cleanBorders;
                analysisDone = false;
            }
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.removeBlockedExits"), options.removeBlockedExits)) {
                options.removeBlockedExits = !options.removeBlockedExits;
                analysisDone = false;
            }
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.removeBlockedTriggers"),
                    options.removeBlockedTriggers)) {
                options.removeBlockedTriggers = !options.removeBlockedTriggers;
                analysisDone = false;
            }
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.removeTriggersOnExits"),
                    options.removeTriggersOnExits)) {
                options.removeTriggersOnExits = !options.removeTriggersOnExits;
                analysisDone = false;
            }

            ImGui.separator();

            // Sección: Automatización
            ImGui.textColored(0.2f, 0.8f, 1.0f, 1.0f, I18n.INSTANCE.get("optimizer.section.objects"));
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.autoBlock"), options.autoBlockObjects)) {
                options.autoBlockObjects = !options.autoBlockObjects;
                analysisDone = false;
            }
            if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.option.autoLayer3"), options.autoMapObjects)) {
                options.autoMapObjects = !options.autoMapObjects;
                analysisDone = false;
            }

            if (options.autoBlockObjects || options.autoMapObjects) {
                ImGui.indent();
                ImGui.textDisabled(I18n.INSTANCE.get("optimizer.option.includeTypes"));
                if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.type.trees"), options.includeTrees)) {
                    options.includeTrees = !options.includeTrees;
                    analysisDone = false;
                }
                ImGui.sameLine();
                if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.type.signs"), options.includeSigns)) {
                    options.includeSigns = !options.includeSigns;
                    analysisDone = false;
                }

                if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.type.forums"), options.includeForums)) {
                    options.includeForums = !options.includeForums;
                    analysisDone = false;
                }
                ImGui.sameLine();
                if (ImGui.checkbox(I18n.INSTANCE.get("optimizer.type.deposits"), options.includeDeposits)) {
                    options.includeDeposits = !options.includeDeposits;
                    analysisDone = false;
                }
                ImGui.unindent();
            }

            ImGui.separator();

            // Botones de Acción
            if (ImGui.button(I18n.INSTANCE.get("optimizer.btn.analyze"), 120, 30)) {
                runAnalysis();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("optimizer.btn.optimize"), 120, 30)) {
                runOptimization();
            }
            ImGui.sameLine();
            if (ImGui.button(I18n.INSTANCE.get("common.cancel"), 120, 30)) {
                this.close();
            }

            ImGui.separator();

            // Área de Reporte
            if (analysisDone) {
                ImGui.textColored(1.0f, 1.0f, 0.0f, 1.0f, I18n.INSTANCE.get("optimizer.report.title"));
                ImGui.beginChild("ReportArea", 0, 0, true);
                ImGui.textUnformatted(report);
                ImGui.endChild();
            }

        }
        ImGui.end();
    }

    private void runAnalysis() {
        MapContext context = GameData.getActiveContext();
        if (context == null) {
            report = I18n.INSTANCE.get("optimizer.error.noMap");
            analysisDone = true;
            return;
        }

        OptimizationResult result = MapOptimizer.analyze(context, options);
        buildReport(result);
        analysisDone = true;
    }

    private void runOptimization() {
        MapContext context = GameData.getActiveContext();
        if (context == null)
            return;

        OptimizationResult result = MapOptimizer.optimize(context, options);
        if (result.command != null) {
            CommandManager.getInstance().executeCommand(result.command);
            report = I18n.INSTANCE.get("optimizer.success") + "\n\n";
            buildReport(result);
            analysisDone = true;
        } else {
            report = I18n.INSTANCE.get("optimizer.noChanges");
            analysisDone = true;
        }
    }

    private void buildReport(OptimizationResult result) {
        StringBuilder sb = new StringBuilder();
        if (!report.isEmpty() && !report.startsWith(I18n.INSTANCE.get("optimizer.success"))) {
            // Keep existing success header if present
            sb.append(report);
        } else if (report.startsWith(I18n.INSTANCE.get("optimizer.success"))) {
            sb.append(report);
        }

        sb.append(I18n.INSTANCE.get("optimizer.report.affectedTiles")).append(": ").append(result.totalTilesAffected)
                .append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.borderItems")).append(": ")
                .append(result.itemsRemovedFromBorders).append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.blockedExits")).append(": ")
                .append(result.blockedExitsRemoved).append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.blockedTriggers")).append(": ")
                .append(result.blockedTriggersRemoved).append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.exitTriggers")).append(": ")
                .append(result.triggersOnExitsRemoved).append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.mappedObjs")).append(": ")
                .append(result.objectsMappedToLayer3).append("\n");
        sb.append("- ").append(I18n.INSTANCE.get("optimizer.report.blockedObjs")).append(": ")
                .append(result.objectsBlocked).append("\n");

        report = sb.toString();
    }

    @Override
    public void close() {
        super.close();
    }
}
