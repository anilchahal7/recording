package in.iceberg.android.db;

import android.content.SharedPreferences;

public class AppRecordData {
    private static final String STORAGE_PERMISSION_DENIED = "STORAGE_PERMISSION_DENIED";
    private static final String RECORD_PERMISSION_DENIED = "RECORD_PERMISSION_DENIED";

    private synchronized static SharedPreferences getSharedPreferences() {
        return SharedPreferencesCreator.getInstance().getSharedPreferences();
    }

    private synchronized static SharedPreferences.Editor getEditor() {
        return SharedPreferencesCreator.getInstance().getEditor();
    }

    public static boolean isStoragePermissionDenied() {
        return getSharedPreferences().getBoolean(STORAGE_PERMISSION_DENIED, false);
    }

    public static void setStoragePermissionDenied(boolean permissionDenied) {
        getEditor().putBoolean(STORAGE_PERMISSION_DENIED, permissionDenied);
        getEditor().apply();
    }

    public static boolean isRecordPermissionDenied() {
        return getSharedPreferences().getBoolean(RECORD_PERMISSION_DENIED, false);
    }

    public static void setRecordPermissionDenied(boolean permissionDenied) {
        getEditor().putBoolean(RECORD_PERMISSION_DENIED, permissionDenied);
        getEditor().apply();
    }

}
