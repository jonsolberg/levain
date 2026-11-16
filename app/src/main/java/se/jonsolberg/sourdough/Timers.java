package se.jonsolberg.sourdough;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Levain's own step timers. Several can run at once; each is kept in SharedPreferences
 * and scheduled with AlarmManager, so it fires even when the app is closed (and is
 * rescheduled after a reboot).
 */
final class Timers {
    static final String ACTION_FIRE = "se.jonsolberg.levain.TIMER_FIRE";
    static final String ACTION_CANCEL = "se.jonsolberg.levain.TIMER_CANCEL";
    static final String EXTRA_ID = "id";

    private static final String PREFS = "timers";
    private static final String KEY = "list";

    private Timers() {
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** All running timers as a JSON array of {id, label, endAt}. */
    static synchronized JSONArray all(Context c) {
        try {
            return new JSONArray(prefs(c).getString(KEY, "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static void save(Context c, JSONArray list) {
        prefs(c).edit().putString(KEY, list.toString()).apply();
    }

    /** Removes the timer and returns its label, or null if it wasn't running. */
    private static synchronized String remove(Context c, int id) {
        JSONArray list = all(c), keep = new JSONArray();
        String label = null;
        for (int i = 0; i < list.length(); i++) {
            JSONObject t = list.optJSONObject(i);
            if (t == null) continue;
            if (t.optInt("id") == id) label = t.optString("label");
            else keep.put(t);
        }
        save(c, keep);
        return label;
    }

    static long start(Context c, int id, String label, long seconds) {
        long endAt = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        synchronized (Timers.class) {
            remove(c, id);
            JSONArray list = all(c);
            try {
                list.put(new JSONObject().put("id", id).put("label", label).put("endAt", endAt));
            } catch (JSONException ignored) {
            }
            save(c, list);
        }
        schedule(c, id, endAt);
        Notifications.showRunning(c, id, label, endAt);
        return endAt;
    }

    static void cancel(Context c, int id) {
        remove(c, id);
        c.getSystemService(AlarmManager.class).cancel(firePI(c, id));
        Notifications.cancelRunning(c, id);
    }

    /** Called by AlarmReceiver when a timer's time is up. */
    static void fire(Context c, int id) {
        String label = remove(c, id);
        Notifications.cancelRunning(c, id);
        if (label != null) AlarmService.ring(c, label);
    }

    /** After a reboot or app update: put the alarms back, ring for any that ran out meanwhile. */
    static void restore(Context c) {
        JSONArray list = all(c);
        long now = System.currentTimeMillis();
        for (int i = 0; i < list.length(); i++) {
            JSONObject t = list.optJSONObject(i);
            if (t == null) continue;
            int id = t.optInt("id");
            long endAt = t.optLong("endAt");
            if (endAt <= now) {
                fire(c, id);
            } else {
                schedule(c, id, endAt);
                Notifications.showRunning(c, id, t.optString("label"), endAt);
            }
        }
    }

    private static void schedule(Context c, int id, long endAt) {
        AlarmManager am = c.getSystemService(AlarmManager.class);
        PendingIntent pi = firePI(c, id);
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pi);
        }
    }

    private static PendingIntent firePI(Context c, int id) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, id, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent cancelPI(Context c, int id) {
        Intent i = new Intent(c, AlarmReceiver.class).setAction(ACTION_CANCEL).putExtra(EXTRA_ID, id);
        return PendingIntent.getBroadcast(c, 1_000_000 + id, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    static PendingIntent openPI(Context c) {
        Intent i = new Intent(c, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
