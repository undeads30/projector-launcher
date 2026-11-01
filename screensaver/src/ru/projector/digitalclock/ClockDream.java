package ru.projector.digitalclock;

import android.service.dreams.DreamService;
import android.os.Handler;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class ClockDream extends DreamService {
    private final Handler handler = new Handler();
    private final Random random = new Random();
    private TextView time, date;
    private LinearLayout group;
    private long lastMinute = -1;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            Date now = new Date();
            time.setText(new SimpleDateFormat("HH:mm", Locale.ROOT).format(now));
            date.setText(new SimpleDateFormat("EEEE, d MMMM", new Locale("ru")).format(now));
            long minute = now.getTime() / 60000;
            if (minute != lastMinute) {
                lastMinute = minute;
                float density = getResources().getDisplayMetrics().density;
                group.setTranslationX(-random.nextInt(9) * density);
                group.setTranslationY(-random.nextInt(9) * density);
            }
            handler.postDelayed(this, 1000 - System.currentTimeMillis() % 1000);
        }
    };

    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        ImageView background = new ImageView(this);
        background.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try (InputStream image = getAssets().open("background.jpg")) {
            background.setImageDrawable(Drawable.createFromStream(image, "background"));
        } catch (IOException e) {
            android.util.Log.e("ClockDream", "Cannot load screensaver background", e);
        }
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));
        group = new LinearLayout(this);
        group.setOrientation(LinearLayout.VERTICAL);
        group.setGravity(Gravity.RIGHT);
        time = new TextView(this);
        time.setTextSize(110);
        time.setTextColor(Color.rgb(235, 235, 235));
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setGravity(Gravity.RIGHT);
        time.setShadowLayer(5, 0, 2, 0xB0000000);
        date = new TextView(this);
        date.setTextSize(24);
        date.setTextColor(Color.rgb(210, 210, 210));
        date.setGravity(Gravity.RIGHT);
        date.setShadowLayer(4, 0, 2, 0xB0000000);
        group.addView(time);
        group.addView(date);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.RIGHT);
        float density = getResources().getDisplayMetrics().density;
        params.rightMargin = Math.round(48 * density);
        params.bottomMargin = Math.round(32 * density);
        root.addView(group, params);
        setContentView(root);
        handler.post(tick);
    }

    @Override public void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }
}
