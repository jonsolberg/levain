package se.jonsolberg.sourdough;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

/** Full-screen "Time's up" screen, shown over the lock screen while a timer alarm rings. */
public class AlarmActivity extends Activity {
    static final String ACTION_CLOSE = "se.jonsolberg.levain.CLOSE_ALARM";
    static final String ACTION_UPDATE = "se.jonsolberg.levain.UPDATE_ALARM";

    private TextView label;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (ACTION_UPDATE.equals(i.getAction())) label.setText(AlarmService.text());
            else finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm);

        label = findViewById(R.id.alarmLabel);
        label.setText(AlarmService.text());

        ScaleAnimation pulse = new ScaleAnimation(1f, 1.12f, 1f, 1.12f,
                Animation.RELATIVE_TO_SELF, .5f, Animation.RELATIVE_TO_SELF, .5f);
        pulse.setDuration(600);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        findViewById(R.id.alarmIcon).startAnimation(pulse);

        findViewById(R.id.alarmStop).setOnClickListener(v -> stopAndClose(false));
        findViewById(R.id.alarmOpen).setOnClickListener(v -> stopAndClose(true));

        IntentFilter f = new IntentFilter(ACTION_CLOSE);
        f.addAction(ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, RECEIVER_NOT_EXPORTED);
        else registerLegacy(f);
    }

    @SuppressWarnings("UnspecifiedRegisterReceiverFlag")
    private void registerLegacy(IntentFilter f) {
        registerReceiver(receiver, f);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!AlarmService.isRinging()) finish();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Volume buttons silence the alarm, like on an alarm clock.
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            stopAndClose(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void stopAndClose(boolean openApp) {
        AlarmService.stop(this);
        if (openApp) {
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (km != null && km.isKeyguardLocked()) km.requestDismissKeyguard(this, null);
            startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
