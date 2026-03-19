package liege.counter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Checks for app updates via a hosted version.json file and downloads the new APK
 * using the system DownloadManager.
 *
 * Setup (one-time):
 *   1. Set VERSION_JSON_URL in gradle.properties (or local.properties) pointing to your
 *      hosted version.json, e.g. hosted on GitHub Releases:
 *        VERSION_JSON_URL=https://github.com/USER/REPO/releases/latest/download/version.json
 *   2. In every release, upload:
 *        - version.json  →  {"versionCode": 2, "versionName": "1.1", "apkUrl": "https://...update.apk"}
 *        - your signed release APK
 *   3. Sign your APK with the same keystore for every release so Android allows the upgrade.
 *
 * The check runs on a background thread and shows a dialog on the UI thread if an update
 * is available. The user can choose to install immediately or dismiss and update later.
 */
public class UpdateChecker {

    private static final String TAG = "UpdateChecker";

    // Pending install info — stored when the user still needs to grant install permission.
    private static volatile String pendingApkUrl;
    private static volatile String pendingVersionName;

    /** Call this from MainActivity.onCreate() to perform a background update check. */
    public static void check(Activity activity) {
        String url = BuildConfig.VERSION_JSON_URL;
        if (url == null || url.trim().isEmpty()) {
            Log.d(TAG, "VERSION_JSON_URL nicht konfiguriert — Update-Prüfung übersprungen.");
            return;
        }

        new Thread(() -> {
            try {
                VersionInfo info = fetchVersionInfo(url);
                if (info == null) return;

                if (info.versionCode > BuildConfig.VERSION_CODE) {
                    Log.i(TAG, "Update verfügbar: " + info.versionName
                            + " (aktuell: " + BuildConfig.VERSION_NAME + ")");
                    showUpdateDialog(activity, info);
                } else {
                    Log.d(TAG, "App ist aktuell (v" + BuildConfig.VERSION_NAME + ").");
                }
            } catch (Exception e) {
                Log.w(TAG, "Update-Prüfung fehlgeschlagen", e);
            }
        }, "UpdateChecker").start();
    }

    /**
     * Call this from MainActivity.onResume() to resume a pending install after the user
     * has returned from the system settings where they granted install permission.
     */
    public static void checkPendingInstall(Activity activity) {
        String url = pendingApkUrl;
        String version = pendingVersionName;
        if (url == null || url.isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            return; // permission still not granted
        }
        // Clear before starting so a second onResume call won't re-trigger the download.
        pendingApkUrl = null;
        pendingVersionName = null;
        downloadAndInstall(activity, url, version);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static VersionInfo fetchVersionInfo(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setConnectTimeout(8_000);
        conn.setReadTimeout(8_000);
        conn.setRequestMethod("GET");
        int status = conn.getResponseCode();
        if (status != 200) {
            Log.w(TAG, "HTTP " + status + " beim Abrufen der Version-Info");
            return null;
        }
        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
            return new Gson().fromJson(reader, VersionInfo.class);
        }
    }

    private static void showUpdateDialog(Activity activity, VersionInfo info) {
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            try {
                new AlertDialog.Builder(activity,
                        android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                        .setTitle("Update verfügbar")
                        .setMessage("Version " + info.versionName + " ist verfügbar.\n"
                                + "Aktuell: " + BuildConfig.VERSION_NAME + "\n\n"
                                + "Jetzt aktualisieren?")
                        .setPositiveButton("Installieren", (d, w) ->
                                downloadAndInstall(activity, info.apkUrl, info.versionName))
                        .setNegativeButton("Später", null)
                        .show();
            } catch (Exception e) {
                Log.w(TAG, "Konnte Update-Dialog nicht anzeigen", e);
            }
        });
    }

    private static void downloadAndInstall(Activity activity, String apkUrl, String versionName) {
        if (apkUrl == null || apkUrl.isEmpty()) {
            Log.e(TAG, "APK-URL fehlt in version.json");
            return;
        }

        // On Android 8+, the app needs explicit permission to install packages.
        // If not yet granted, send the user to settings and store the pending install.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            pendingApkUrl = apkUrl;
            pendingVersionName = versionName;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                try {
                    new AlertDialog.Builder(activity,
                            android.R.style.Theme_DeviceDefault_Dialog_NoActionBar)
                            .setTitle("Berechtigung erforderlich")
                            .setMessage("Um Updates zu installieren, benötigt die App die Erlaubnis, "
                                    + "Pakete aus unbekannten Quellen zu installieren. "
                                    + "Bitte erlaube dies in den Einstellungen und kehre dann zurück.")
                            .setPositiveButton("Einstellungen öffnen", (d, w) -> {
                                Intent intent = new Intent(
                                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                        Uri.parse("package:" + activity.getPackageName()));
                                activity.startActivity(intent);
                            })
                            .setNegativeButton("Abbrechen", (d, w) -> {
                                pendingApkUrl = null;
                                pendingVersionName = null;
                            })
                            .show();
                } catch (Exception e) {
                    Log.w(TAG, "Konnte Berechtigungs-Dialog nicht anzeigen", e);
                }
            });
            return;
        }

        DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm == null) return;

        String fileName = "PushUpCounter-" + versionName + ".apk";

        // Use the app's private external directory so other apps cannot tamper with the APK.
        // Android still verifies the signature before installing.
        // setDestinationInExternalFilesDir is the correct API; it creates the directory if
        // needed and the DownloadManager service has permission to write there.
        java.io.File externalFilesDir =
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (externalFilesDir == null) {
            Log.e(TAG, "Externer Speicher nicht verfügbar — Update abgebrochen.");
            return;
        }
        final java.io.File destFile = new java.io.File(externalFilesDir, fileName);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl))
                .setTitle("Push-Up Counter Update")
                .setDescription("Version " + versionName + " wird heruntergeladen…")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setMimeType("application/vnd.android.package-archive");
        try {
            request.setDestinationInExternalFilesDir(
                    activity, Environment.DIRECTORY_DOWNLOADS, fileName);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Zielverzeichnis konnte nicht erstellt werden", e);
            return;
        }

        final long downloadId = dm.enqueue(request);

        // Listen for download completion.
        // Store the receiver reference so it can be unregistered on failure/cancellation.
        final BroadcastReceiver[] receiverHolder = new BroadcastReceiver[1];
        receiverHolder[0] = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id != downloadId) return;
                try {
                    activity.unregisterReceiver(this);
                } catch (Exception ignored) { }

                // Query the actual download outcome before attempting install.
                DownloadManager.Query query =
                        new DownloadManager.Query().setFilterById(downloadId);
                try (android.database.Cursor cursor = dm.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                        int status = statusIdx >= 0 ? cursor.getInt(statusIdx) : -1;
                        int reason = reasonIdx >= 0 ? cursor.getInt(reasonIdx) : -1;
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            triggerInstall(activity, destFile);
                        } else {
                            Log.e(TAG, "Download fehlgeschlagen. Status: " + status
                                    + ", Grund: " + reason);
                            activity.runOnUiThread(() -> {
                                if (!activity.isFinishing() && !activity.isDestroyed()) {
                                    android.widget.Toast.makeText(activity,
                                            "Update-Download fehlgeschlagen. "
                                                    + "Bitte versuche es später erneut.",
                                            android.widget.Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Fehler beim Prüfen des Download-Status", e);
                }
            }
        };

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                activity.registerReceiver(receiverHolder[0],
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                activity.registerReceiver(receiverHolder[0],
                        new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
            }
        } catch (Exception e) {
            Log.e(TAG, "Konnte BroadcastReceiver nicht registrieren", e);
        }
    }

    private static void triggerInstall(Activity activity, java.io.File apkFile) {
        if (!apkFile.exists()) {
            Log.e(TAG, "Heruntergeladene Datei nicht gefunden: " + apkFile.getPath());
            return;
        }

        try {
            Uri apkUri = androidx.core.content.FileProvider.getUriForFile(
                    activity,
                    activity.getPackageName() + ".provider",
                    apkFile);

            Intent install = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(apkUri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(install);
        } catch (Exception e) {
            Log.e(TAG, "APK-Installation fehlgeschlagen", e);
        }
    }

    // -------------------------------------------------------------------------
    // version.json model
    // -------------------------------------------------------------------------

    private static class VersionInfo {
        @SerializedName("versionCode")
        int versionCode;

        @SerializedName("versionName")
        String versionName;

        /** Direct download URL for the release APK. */
        @SerializedName("apkUrl")
        String apkUrl;
    }
}
