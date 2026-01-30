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
    // Base URL
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

        // Decide URL based on options
        String targetUrl;
        if (checkPre) {
            targetUrl = BASE_URL + "/releases?per_page=1";
        } else {
            targetUrl = BASE_URL + "/releases/latest";
        }

        System.out.println("[UpdateChecker] Starting check... (Pre-releases: " + checkPre + ")");
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

        Logger.info("Checking for updates from: " + targetUrl);

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    System.out.println("[UpdateChecker] Response status: " + response.statusCode());

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
                        System.out.println("[UpdateChecker] Remote tag: " + release.tagName);
                        boolean newer = isNewerVersion(release.tagName);
                        System.out
                                .println("[UpdateChecker] Local version: " + Engine.VERSION + " | Is newer? " + newer);

                        if (newer) {
                            latestRelease = release;
                            Logger.info("New version found: " + release.tagName);
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
                        System.out.println("[UpdateChecker] Release object is null after parsing.");
                    }
                })
                .exceptionally(e -> {
                    System.err.println("[UpdateChecker] Exception: " + e.getMessage());
                    Console.INSTANCE.addMsgToConsole(
                            "[Update] " + I18n.INSTANCE.get("update.error_generic", e.getMessage()), FontStyle.BOLD,
                            new RGBColor(1f, 0f, 0f));
                    e.printStackTrace();
                    Logger.error(e, "Failed to check for updates");
                    return null;
                });
    }

    private static ReleaseInfo parseRelease(String json) {
        if (json == null)
            return null;
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = null;

            // Check if it starts with [ (Array) or { (Object)
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
            System.err.println("[UpdateChecker] JSON Parse Error: " + e.getMessage());
            Logger.error(e, "Error parsing release JSON");
        }
        return null;
    }

    private static boolean isNewerVersion(String tagName) {
        String currentVersion = Engine.VERSION;
        // Strip 'v' prefix if present
        String vRemote = tagName.startsWith("v") ? tagName.substring(1) : tagName;
        String vLocal = currentVersion.startsWith("v") ? currentVersion.substring(1) : currentVersion;

        return compareVersions(vRemote, vLocal) > 0;
    }

    /**
     * Compare two version strings.
     * Returns > 0 if v1 > v2, < 0 if v1 < v2, and 0 if equal.
     */
    public static int compareVersions(String v1, String v2) {
        // Remove suffixes like -beta3 for numeric comparison
        String[] v1Parts = v1.split("-");
        String[] v2Parts = v2.split("-");

        String[] v1Nums = v1Parts[0].split("\\.");
        String[] v2Nums = v2Parts[0].split("\\.");

        int length = Math.max(v1Nums.length, v2Nums.length);

        for (int i = 0; i < length; i++) {
            int num1 = i < v1Nums.length ? Integer.parseInt(v1Nums[i]) : 0;
            int num2 = i < v2Nums.length ? Integer.parseInt(v2Nums[i]) : 0;

            if (num1 > num2)
                return 1;
            if (num1 < num2)
                return -1;
        }

        // If main parts are equal, check pre-release (simple check: version with
        // pre-release is OLDER than version without)
        // e.g. 1.0.0-beta < 1.0.0
        boolean v1HasPre = v1Parts.length > 1;
        boolean v2HasPre = v2Parts.length > 1;

        if (v1HasPre && !v2HasPre)
            return -1;
        if (!v1HasPre && v2HasPre)
            return 1;

        // If both have pre-release code, compare them lexicographically (e.g. alpha <
        // beta)
        if (v1HasPre && v2HasPre) {
            return v1Parts[1].compareTo(v2Parts[1]);
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
