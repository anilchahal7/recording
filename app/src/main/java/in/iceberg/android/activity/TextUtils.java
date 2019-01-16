package in.iceberg.android.activity;

public final class TextUtils {
    public static boolean isNull(String string) {
        return string == null;
    }

    public static boolean isNotNull(String string) {
        return string != null;
    }

    public static boolean isNullOrEmpty(String string) {
        return isNull(string) || string.equalsIgnoreCase("") || string.trim().length() == 0;
    }

    public static boolean isNotNullOrEmpty(String string) {
        return isNotNull(string) && !string.equalsIgnoreCase("") && string.trim().length() != 0;
    }

    public static String getNonNullString(String string) {
        return isNotNullOrEmpty(string) ? string : "";
    }
}