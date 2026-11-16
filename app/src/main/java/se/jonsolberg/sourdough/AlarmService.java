package se.jonsolberg.sourdough;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Rings like an alarm clock when a step timer ends: loops the alarm sound on the alarm
 * volume (so it also sounds when the phone is on silent), vibrates and shows a full-screen
 * screen with a Stop button. Gives up after a few minutes.
 */
public class AlarmService extends Service {
    static final String ACTION_RING = "se.jonsolberg.levain.RING";
    static final String ACTION_STOP = "se.jonsolberg.levain.STOP";
    static final String EXTRA_LABEL = "label";
    private static final long MAX_RING_MS = 3 * 60 * 1000;

    private static volatile boolean ringing;
    /** Timers that have rung since the alarm started (more than one may end at once). */
    private static final List<String> labels = new ArrayList<>();

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = () -> stopRinging(true);

    static boolean isRinging() {
        return ringing;
    }

    static synchronized String text() {
        return String.join(" · ", labels);
    }

    static void ring(Context c, String label) {
        Intent i = new Intent(c, AlarmService.class).setAction(ACTION_RING).putExtra(EXTRA_LABEL, label);
        try {
            c.startForegroundService(i);
        } catch (RuntimeException e) {
            // e.g. not allowed to start a foreground service right now: plain loud notification instead
            Notifications.showReady(c, label);
        }
    }

    static void stop(Context c) {
        if (!ringing) return;
        c.startService(new Intent(c, AlarmService.class).setAction(ACTION_STOP));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            stopRinging(false);
            return START_NOT_STICKY;
        }
        String label = intent != null ? intent.getStringExtra(EXTRA_LABEL) : null;
        synchronized (AlarmService.class) {
            if (!ringing) labels.clear();
            if (label != null && !labels.contains(label)) labels.add(label);
        }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifications.ID_ALARM, Notifications.buildAlarm(this, text()),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(Notifications.ID_ALARM, Notifications.buildAlarm(this, text()));
        }
        if (!ringing) {
            ringing = true;
            acquireWakeLock();
            startSound();
            startVibration();
        }
        handler.removeCallbacks(timeout);
        handler.postDelayed(timeout, MAX_RING_MS);
        sendBroadcast(new Intent(AlarmActivity.ACTION_UPDATE).setPackage(getPackageName()));
        return START_NOT_STICKY;
    }

    private void startSound() {
        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception e) {
            if (player != null) player.release();
            player = null;
        }
    }

    private void startVibration() {
        if (Build.VERSION.SDK_INT >= 31) {
            vibrator = getSystemService(VibratorManager.class).getDefaultVibrator();
        } else {
            vibrator = getSystemService(Vibrator.class);
        }
        if (vibrator == null || !vibrator.hasVibrator()) return;
        long[] pattern = {0, 600, 400, 600, 400, 600, 1200};
        VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0);
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM));
        } else {
            vibrateLegacy(effect);
        }
    }

    @SuppressWarnings("deprecation")
    private void vibrateLegacy(VibrationEffect effect) {
        vibrator.vibrate(effect, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
    }

    private void acquireWakeLock() {
        PowerManager pm = getSystemService(PowerManager.class);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Levain:alarm");
        wakeLock.acquire(MAX_RING_MS + 10_000);
    }

    /** @param timedOut true when nobody stopped it: leave a quiet notice behind. */
    private void stopRinging(boolean timedOut) {
        handler.removeCallbacks(timeout);
        String done = text();
        ringing = false;
        if (player != null) {
            try {
                player.stop();
            } catch (IllegalStateException ignored) {
            }
            player.release();
            player = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (timedOut) Notifications.showReadyQuiet(this, done);
        sendBroadcast(new Intent(AlarmActivity.ACTION_CLOSE).setPackage(getPackageName()));
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (ringing) stopRinging(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
