package br.com.globostore.autoupdate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;

import org.xmlpull.v1.XmlPullParserException;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.util.Log;

import android.widget.TextView;


public class GlobostoreAutoUpdate {
    public static final String TAG = "GlobostoreAutoUpdate";

    private List<String> supportedKeys = new ArrayList<>(Arrays.asList("appId", "appKey", "versionUrl", "method", "service", "downloadUrl"));
    private Map mapa = null;

    /**
     * Constructor.
     */
    public GlobostoreAutoUpdate() {
    }


    /**
     * Executes GlobostoreAutoUpdate*
     *
     * @param ctx Application Context.
     * @return True if the action was valid, false if not.
     */
    public boolean check(Context ctx) {
        int identifier = ctx.getResources().getIdentifier("globostore", "xml", ctx.getPackageName());
        mapa = loadConfigsFromXml(ctx.getResources(), identifier);
        Version remote = checkRemoteVersion();
        if (!remote.getVersion().equals(checkCurrentVersion(ctx))) {
            this.alert("Atualização", "Nova versão disponível", "OK", ctx, remote);
        }
        return true;
    }

    /**
     * Builds and shows a native Android alert with given Strings
     *
     * @param message         The message the alert should display
     * @param title           The title of the alert
     * @param buttonLabel     The label of the button
     * @param callbackContext The callback context
     */
    private synchronized void alert(final String message, final String title, final String buttonLabel, final Context callbackContext, final Version version) {
        Activity activity = (Activity) callbackContext;
        Runnable runnable = new Runnable() {
            public void run() {

                AlertDialog.Builder dlg = createDialog(callbackContext);
                dlg.setMessage(message);
                dlg.setTitle(title);
                dlg.setCancelable(true);
                dlg.setPositiveButton(buttonLabel,
                        new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                UpdateApp update = new UpdateApp();
                                update.setContext(callbackContext);
                                update.execute(mapa.get("downloadUrl").toString() + "/" + version.getDownloadUrl());
                            }
                        });
                dlg.setOnCancelListener(new AlertDialog.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        dialog.dismiss();
                    }
                });

                changeTextDirection(dlg);
            }

            ;
        };
        activity.runOnUiThread(runnable);
    }

    @SuppressLint("NewApi")
    private AlertDialog.Builder createDialog(Context context) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            return new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        } else {
            return new AlertDialog.Builder(context);
        }
    }

    @SuppressLint("NewApi")
    private void changeTextDirection(Builder dlg) {
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        dlg.create();
        AlertDialog dialog = dlg.show();
        if (currentapiVersion >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            messageView.setTextDirection(android.view.View.TEXT_DIRECTION_LOCALE);
        }
    }

    private Version checkRemoteVersion() {
        Version version = new Version();
        try {
            RequestVersion rv = new RequestVersion();
            rv.setMap(mapa);
            version = rv.execute(mapa.get("versionUrl").toString()).get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return version;

    }

    private String checkCurrentVersion(Context context) {

        String currentVersion = null;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            currentVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return currentVersion;
    }


    private Map loadConfigsFromXml(Resources res, int configXmlResourceId) {
        //
        // Resources is the same thing from above that can be obtained
        // by context.getResources()
        // configXmlResourceId is the resource id integer obtained in step 1
        XmlResourceParser xrp = res.getXml(configXmlResourceId);

        Map configs = new HashMap();

        //
        // walk the config.xml tree and save all <preference> tags we want
        //
        try {
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_DOCUMENT) {
                if ("preference".equals(xrp.getName())) {
                    String key = matchSupportedKeyName(xrp.getAttributeValue(null, "name"));
                    if (key != null) {
                        configs.put(key, xrp.getAttributeValue(null, "value"));
                    }
                }
                xrp.next();
            }
        } catch (XmlPullParserException ex) {
            Log.e(TAG, ex.toString());
        } catch (IOException ex) {
            Log.e(TAG, ex.toString());
        }

        return configs;
    }

    private String matchSupportedKeyName(String testKey) {
        //
        // If key matches, return the version with correct casing.
        // If not, return null.
        // O(n) here is okay because this is a short list of just a few items
        for (String realKey : supportedKeys) {
            if (realKey.equalsIgnoreCase(testKey)) {
                return realKey;
            }
        }
        return null;
    }


}
