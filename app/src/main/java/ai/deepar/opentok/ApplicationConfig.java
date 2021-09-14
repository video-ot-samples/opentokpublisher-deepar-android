package ai.deepar.opentok;

import android.text.TextUtils;
import androidx.annotation.NonNull;

public class ApplicationConfig {


    /**
     *  DeepAR credentials
     */
    public static final String DEEPAR_LICENSEKEY = "";

    /**
     *  Vonage Video API credentials
     *  Fill the following variables using your own Project info from the OpenTok dashboard
     *  https://dashboard.tokbox.com/projects
     */
    // Replace with a API key
    public static final String API_KEY = "";

    // Replace with a generated Session ID
    public static final String SESSION_ID = "";

    // Replace with a generated token (from the dashboard or using an OpenTok server SDK)
    public static final String TOKEN = "";

    public static final String MASK = "file:///android_asset/tv80";

    public static boolean isValid() {
        if (TextUtils.isEmpty(ApplicationConfig.DEEPAR_LICENSEKEY)
                || TextUtils.isEmpty(ApplicationConfig.API_KEY)
                || TextUtils.isEmpty(ApplicationConfig.SESSION_ID)
                || TextUtils.isEmpty(ApplicationConfig.TOKEN)
                || TextUtils.isEmpty(ApplicationConfig.MASK)) {
            return false;
        }

        return true;
    }

    @NonNull
    public static String getDescription() {
        return "ApplicationConfig:" + "\n"
                + "DEEPAR_LICENSEKEY: " + ApplicationConfig.DEEPAR_LICENSEKEY + "\n"
                + "API_KEY: " + ApplicationConfig.API_KEY + "\n"
                + "SESSION_ID: " + ApplicationConfig.SESSION_ID + "\n"
                + "TOKEN: " + ApplicationConfig.TOKEN + "\n"
                + "MASK: " + ApplicationConfig.MASK + "\n";
    }
}