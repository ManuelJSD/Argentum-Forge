package org.argentumforge.engine.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.argentumforge.engine.Engine;
import org.argentumforge.engine.game.console.Console;
import org.argentumforge.engine.game.console.FontStyle;
import org.argentumforge.engine.i18n.I18n;
import org.argentumforge.engine.renderer.RGBColor;
import org.tinylog.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GithubReleaseChecker {

    private static final String REPO_OWNER = "ManuelJSD";
    private static final String REPO_NAME = "Argentum-Forge";
    // URL Base
    private static final String BASE_URL = "https://api.github.com/repos/" + REPO_OWNER + "/" + REPO_NAME;

    private static ReleaseInfo latestRelease = null;
    private static boolean checkPerformed = false;

    public static class ReleaseInfo {
        public final String tagName;
        public final String htmlUrl;
        public final String name;
        public final String body;
        public final boolean isPrerelease;

        public ReleaseInfo(String tagName, String htmlUrl, String name, String body, boolean isPrerelease) {
            this.tagName = tagName;
            this.htmlUrl = htmlUrl;
            this.name = name;
            this.body = body;
            this.isPrerelease = isPrerelease;
        }
    }

    public static void checkForUpdates() {
        if (checkPerformed)
            return;
        checkPerformed = true;

        boolean checkPre = org.argentumforge.engine.game.Options.INSTANCE.isCheckPreReleases();

        // Decidir URL basado en opciones
        String targetUrl;
        if (checkPre) {
            targetUrl = BASE_URL + "/releases?per_page=1";
        } else {
            targetUrl = BASE_URL + "/releases/latest";
        }

        Logger.info("Starting update check... (Pre-releases: {})", checkPre);
        Console.INSTANCE.addMsgToConsole("[Update] " + I18n.INSTANCE.get("update.checking"), FontStyle.ITALIC,
                new RGBColor(1f, 1f, 0f));

        HttpClient client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(targetUrl))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

        Logger.info("Checking for updates from: {}", targetUrl);

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    Logger.debug("Response status: {}", response.statusCode());

                    if (response.statusCode() == 404) {
                        Console.INSTANCE.addMsgToConsole("[Update] " + I18n.INSTANCE.get("update.repo_404"),
                                FontStyle.BOLD, new RGBColor(1f, 0.5f, 0f));
                        return null;
                    }

                    if (response.statusCode() != 200) {
                        Console.INSTANCE.addMsgToConsole(
                                "[Update] " + I18n.INSTANCE.get("update.error_api", response.statusCode()),
                                FontStyle.REGULAR, new RGBColor(1f, 0f, 0f));
                        return null;
                    }

                    // Removed "Status:" log as requested
                    return response.body();
                })
                .thenApply(GithubReleaseChecker::parseRelease)
                .thenAccept(release -> {
                    if (release != null) {
                        Logger.debug("Remote tag: {}", release.tagName);
                        boolean newer = isNewerVersion(release.tagName);
                        Logger.debug("Local version: {} | Is newer? {}", Engine.VERSION, newer);

                        if (newer) {
                            latestRelease = release;
                            Logger.info("New version found: {}", release.tagName);
                            Console.INSTANCE.addMsgToConsole(
                                    "[Update] " + I18n.INSTANCE.get("update.new_version", release.tagName),
                                    FontStyle.BOLD,
                                    new RGBColor(0f, 1f, 0f));
                        } else {
                            Logger.info("No updates found or current version is up to date.");
                            Console.INSTANCE.addMsgToConsole("[Update] " + I18n.INSTANCE.get("update.up_to_date"),
                                    FontStyle.ITALIC,
                                    new RGBColor(0.5f, 1f, 0.5f));
                        }
                    } else {
                        Logger.warn("Release object is null after parsing.");
                    }
                })
                .exceptionally(e -> {
                    Logger.error(e, "Exception during update check");
                    Console.INSTANCE.addMsgToConsole(
                            "[Update] " + I18n.INSTANCE.get("update.error_generic", e.getMessage()), FontStyle.BOLD,
                            new RGBColor(1f, 0f, 0f));
                    return null;
                });
    }

    private static ReleaseInfo parseRelease(String json) {
        if (json == null)
            return null;
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = null;

            // Comprobar si comienza con [ (Array) o { (Objeto)
            String trimmed = json.trim();
            if (trimmed.startsWith("[")) {
                com.google.gson.JsonArray jsonArray = gson.fromJson(json, com.google.gson.JsonArray.class);
                if (jsonArray.size() > 0) {
                    jsonObject = jsonArray.get(0).getAsJsonObject();
                }
            } else {
                jsonObject = gson.fromJson(json, JsonObject.class);
            }

            if (jsonObject != null && jsonObject.has("tag_name")) {
                boolean isPre = false;
                if (jsonObject.has("prerelease")) {
                    isPre = jsonObject.get("prerelease").getAsBoolean();
                }

                return new ReleaseInfo(
                        jsonObject.get("tag_name").getAsString(),
                        jsonObject.get("html_url").getAsString(),
                        jsonObject.get("name").getAsString(),
                        jsonObject.get("body").getAsString(),
                        isPre);
            } else {
                Console.INSTANCE.addMsgToConsole("[Update] " + I18n.INSTANCE.get("update.no_releases"),
                        FontStyle.ITALIC,
                        new RGBColor(1f, 1f, 0f));
            }
        } catch (Exception e) {
            Logger.error(e, "JSON Parse Error during release check");
        }
        return null;
    }

    private static boolean isNewerVersion(String tagName) {
        String currentVersion = Engine.VERSION;
        // Eliminar el prefijo 'v' si está presente
        String vRemote = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        String vLocal = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

        return compareVersions(vRemote, vLocal) > 0;
    }

    /**
     * Comparar dos strings de versión.
     * Devuelve > 0 si v1 > v2, < 0 si v1 < v2, y 0 si son iguales.
     */
    public static int compareVersions(String v1, String v2) {
        if (v1 == null && v2 == null)
            return 0;
        if (v1 == null)
            return -1;
        if (v2 == null)
            return 1;

        // Eliminar sufijos como -beta5 para comparación numérica y normalizar a
        // minúsculas
        String[] v1Parts = v1.toLowerCase().split("-");
        String[] v2Parts = v2.toLowerCase().split("-");

        String[] v1Nums = v1Parts[0].split("\\.");
        String[] v2Nums = v2Parts[0].split("\\.");

        int length = Math.max(v1Nums.length, v2Nums.length);

        for (int i = 0; i < length; i++) {
            int num1 = 0;
            int num2 = 0;

            try {
                if (i < v1Nums.length && !v1Nums[i].isEmpty())
                    num1 = Integer.parseInt(v1Nums[i]);
            } catch (NumberFormatException e) {
                // Si no es un número válido (ej: "beta"), tratar como 0 o manejar error
                // Para robustez simple, asumimos 0
            }

            try {
                if (i < v2Nums.length && !v2Nums[i].isEmpty())
                    num2 = Integer.parseInt(v2Nums[i]);
            } catch (NumberFormatException e) {
                // Igual que arriba
            }

            if (num1 > num2)
                return 1;
            if (num1 < num2)
                return -1;
        }

        // Si las partes principales son iguales, verificar pre-release (simple check:
        // version con pre-release es MÁS ANTIGUA que la versión sin)
        // e.g. 1.0.0-beta < 1.0.0
        boolean v1HasPre = v1Parts.length > 1;
        boolean v2HasPre = v2Parts.length > 1;

        if (v1HasPre && !v2HasPre)
            return -1;
        if (!v1HasPre && v2HasPre)
            return 1;

        // Si ambas tienen código pre-release, compararlas lexicográficamente (e.g.
        // alpha < beta)
        if (v1HasPre) { // v2HasPre is implied true here
            String pre1 = v1Parts[1];
            String pre2 = v2Parts[1];

            // Manejo especial para RC (Release Candidate)
            // Necesitamos orden: alpha < beta < rc
            // Alfabéticamente: alpha < beta < rc funciona correctamente
            // Pero qué pasa con "snapshot"? "snapshot" > "rc" > "beta"
            // Por ahora confiamos en el orden alfabético para alpha/beta/rc
            return pre1.compareTo(pre2);
        }

        return 0;
    }

    public static ReleaseInfo getLatestRelease() {
        return latestRelease;
    }

    public static boolean isUpdateAvailable() {
        return latestRelease != null;
    }
}
