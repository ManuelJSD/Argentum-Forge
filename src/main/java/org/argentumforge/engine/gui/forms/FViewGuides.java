package org.argentumforge.engine.gui.forms;

import imgui.ImGui;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImInt;
import org.argentumforge.engine.game.Options;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.renderer.RenderSettings;

/**
 * Panel ligero para configurar guías visuales (Rejilla, Viewport, etc.)
 */
public class FViewGuides extends Form {

    public FViewGuides() {
        // No modal by default
    }

    @Override
    public void render() {
        ImGui.setNextWindowSize(320, 0, imgui.flag.ImGuiCond.Always);

        if (ImGui.begin(I18n.INSTANCE.get("options.grid.guides"),
                ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize | ImGuiWindowFlags.AlwaysAutoResize)) {

            RenderSettings settings = Options.INSTANCE.getRenderSettings();

            // --- REJILLA ---
            ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f), I18n.INSTANCE.get("options.grid.title") + ":");
            ImGui.separator();

            if (ImGui.checkbox(I18n.INSTANCE.get("menu.view.grid"), settings.isShowGrid())) {
                settings.setShowGrid(!settings.isShowGrid());
                Options.INSTANCE.save();
            }

            ImGui.sameLine();
            if (ImGui.checkbox(I18n.INSTANCE.get("options.grid.adaptive"), settings.isAdaptiveGrid())) {
                settings.setAdaptiveGrid(!settings.isAdaptiveGrid());
                Options.INSTANCE.save();
            }

            float[] gColor = settings.getGridColor();
            if (ImGui.colorEdit4(I18n.INSTANCE.get("options.grid.color"), gColor)) {
                settings.setGridColor(gColor);
                Options.INSTANCE.save();
            }

            ImGui.spacing();

            // --- REJILLA MAYOR ---
            if (ImGui.checkbox(I18n.INSTANCE.get("options.grid.showMajor"), settings.isShowMajorGrid())) {
                settings.setShowMajorGrid(!settings.isShowMajorGrid());
                Options.INSTANCE.save();
            }

            ImGui.setNextItemWidth(100);
            ImInt mInterval = new ImInt(settings.getGridMajorInterval());
            if (ImGui.inputInt(I18n.INSTANCE.get("options.grid.majorInterval"), mInterval)) {
                if (mInterval.get() < 2)
                    mInterval.set(2);
                settings.setGridMajorInterval(mInterval.get());
                Options.INSTANCE.save();
            }

            float[] gmColor = settings.getGridMajorColor();
            if (ImGui.colorEdit4(I18n.INSTANCE.get("options.grid.majorColor"), gmColor)) {
                settings.setGridMajorColor(gmColor);
                Options.INSTANCE.save();
            }

            ImGui.dummy(0, 10);

            // --- VIEWPORT ---
            ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f), I18n.INSTANCE.get("options.viewport") + ":");
            ImGui.separator();

            if (ImGui.checkbox(I18n.INSTANCE.get("options.viewport.enable"), settings.isShowViewportOverlay())) {
                settings.setShowViewportOverlay(!settings.isShowViewportOverlay());
                Options.INSTANCE.save();
            }

            ImInt vW = new ImInt((int) settings.getViewportWidth());
            ImInt vH = new ImInt((int) settings.getViewportHeight());
            ImGui.setNextItemWidth(80);
            if (ImGui.inputInt("W##vW", vW)) {
                settings.setViewportWidth(vW.get());
                Options.INSTANCE.save();
            }
            ImGui.sameLine();
            ImGui.setNextItemWidth(80);
            if (ImGui.inputInt("H##vH", vH)) {
                settings.setViewportHeight(vH.get());
                Options.INSTANCE.save();
            }

            float[] vColor = settings.getViewportColor();
            if (ImGui.colorEdit4(I18n.INSTANCE.get("options.viewport.color"), vColor)) {
                settings.setViewportColor(vColor);
                Options.INSTANCE.save();
            }

            ImGui.dummy(0, 10);

            // --- GHOST ---
            ImGui.textColored(ImGui.getColorU32(0.2f, 0.7f, 1.0f, 1.0f),
                    I18n.INSTANCE.get("options.paste.ghostAlpha") + ":");
            ImGui.separator();
            float[] ghostAlpha = { settings.getGhostOpacity() };
            if (ImGui.sliderFloat("##ghostAlphaGuides", ghostAlpha, 0.0f, 1.0f)) {
                settings.setGhostOpacity(ghostAlpha[0]);
                Options.INSTANCE.save();
            }
        }
        ImGui.end();
    }
}
