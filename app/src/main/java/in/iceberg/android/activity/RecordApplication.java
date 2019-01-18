package in.iceberg.android.activity;

import android.app.Application;
import android.content.Context;

public class RecordApplication extends Application {

    private static RecordApplication recordApplication;

    public static RecordApplication getInstance() {
        return recordApplication;
    }

    public static Context getContext() {
        return recordApplication.getApplicationContext();
    }

    @Override
    public void onCreate() {
        recordApplication = this;
        super.onCreate();
    }
}
