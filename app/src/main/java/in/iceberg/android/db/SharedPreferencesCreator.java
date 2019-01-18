package in.iceberg.android.db;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import in.iceberg.android.activity.RecordApplication;

public class SharedPreferencesCreator {
    private static SharedPreferencesCreator sharedPreferencesCreator;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    @SuppressLint("CommitPrefEdits")
    private SharedPreferencesCreator() {
        sharedPreferences = RecordApplication.getContext().
                getSharedPreferences("in.iceberg.android.preference", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static synchronized SharedPreferencesCreator getInstance() {
        if (sharedPreferencesCreator == null) {
            sharedPreferencesCreator = new SharedPreferencesCreator();
        }
        return sharedPreferencesCreator;
    }

    public synchronized SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public synchronized SharedPreferences.Editor getEditor() {
        return editor;
    }
}
