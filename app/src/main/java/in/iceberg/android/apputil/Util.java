package in.iceberg.android.apputil;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;

public class Util {
    public static void showAlertDialog(@Nullable String title, @Nullable String message,
                                       @Nullable DialogInterface.OnClickListener onPositiveButtonClickListener,
                                       @NonNull String positiveText,
                                       @Nullable DialogInterface.OnClickListener onNegativeButtonClickListener,
                                       @NonNull String negativeText, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, onPositiveButtonClickListener);
        builder.setNegativeButton(negativeText, onNegativeButtonClickListener);
        builder.show();
    }
}
