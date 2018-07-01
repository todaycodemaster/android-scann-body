/**
 *  Child Growth Monitor - quick and accurate data on malnutrition
 *  Copyright (c) $today.year Welthungerhilfe Innovation
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.welthungerhilfe.cgm.scanner.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

import java.lang.reflect.Field;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

import de.welthungerhilfe.cgm.scanner.models.Loc;

public class Utils {
    public static void overrideFont(Context context, String defaultFontNameToOverride, String customFontFileNameInAssets) {
        try {
            final Typeface customFontTypeface = Typeface.createFromAsset(context.getAssets(), customFontFileNameInAssets);

            final Field defaultFontTypefaceField = Typeface.class.getDeclaredField(defaultFontNameToOverride);
            defaultFontTypefaceField.setAccessible(true);
            defaultFontTypefaceField.set(null, customFontTypeface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isNetworkConnectionAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) return false;
        NetworkInfo.State network = info.getState();
        return (network == NetworkInfo.State.CONNECTED || network == NetworkInfo.State.CONNECTING);
    }

    public static String getAndroidID(ContentResolver resolver) {
        return Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID);
    }

    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890abcdefghijklmnopqrstuvwxyz";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();

        return saltStr;
    }

    public static long getUniversalTimestamp() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

        return calendar.getTimeInMillis();
    }

    public static boolean checkPermission (Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static String beautifyDate(long timestamp) {
        Date date = new Date(timestamp);

        return beautifyDate(date);
    }

    public static String beautifyDate(Date date) {
        SimpleDateFormat formatter = null;
        formatter = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        String res = formatter.format(date);

        return res;
    }

    public static Date stringToDate(String dt) {
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
        try {
            Date date = format.parse(dt);
            return date;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static double distanceBetweenLocs(Loc l1, Loc l2) {
        Location loc1 = new Location("");
        loc1.setLatitude(l1.getLatitude());
        loc1.setLongitude(l1.getLongitude());

        Location loc2 = new Location("");
        loc2.setLatitude(l2.getLatitude());
        loc2.setLongitude(l2.getLongitude());

        return loc1.distanceTo(loc2);
    }

    public static boolean checkDouble(String number) {
        try {
            Integer.parseInt(number);

            return false;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    public static int checkDoubleDecimals(String number) {
        int integerPlaces = number.indexOf('.');

        if (integerPlaces < 0)
            return 0;

        int decimalPlaces = number.length() - integerPlaces - 1;

        return decimalPlaces;
    }
}
