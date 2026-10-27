package ru.projector.hdmiautoclose;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public final class HdmiPromptService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable inspect = this::dismissPrompt;

    @Override protected void onServiceConnected() {
        handler.post(inspect);
        handler.postDelayed(inspect, 500);
        handler.postDelayed(inspect, 2000);
        Log.i("HdmiAutoClose", "Service ready");
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event != null && "com.android.settings".contentEquals(event.getPackageName() == null ? "" : event.getPackageName())) {
            handler.removeCallbacks(inspect);
            handler.postDelayed(inspect, 100);
        }
    }

    private void dismissPrompt() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            if (!"com.android.settings".contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) return;
            List<AccessibilityNodeInfo> summaries = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/summary");
            List<AccessibilityNodeInfo> open = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/show");
            List<AccessibilityNodeInfo> cancel = root.findAccessibilityNodeInfosByViewId("com.android.settings:id/ignore");
            try {
                for (AccessibilityNodeInfo summary : summaries) {
                    CharSequence text = summary.getText();
                    if (!HdmiPromptPolicy.shouldDismiss(root.getPackageName().toString(), text == null ? null : text.toString(),
                            !open.isEmpty(), !cancel.isEmpty())) continue;
                    for (AccessibilityNodeInfo button : cancel) {
                        if (button.isVisibleToUser() && button.isEnabled() && button.isClickable()
                                && button.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            Log.i("HdmiAutoClose", "HDMI prompt dismissed: " + text);
                            return;
                        }
                    }
                }
            } finally {
                recycle(summaries);
                recycle(open);
                recycle(cancel);
            }
        } finally {
            root.recycle();
        }
    }

    private static void recycle(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo node : nodes) node.recycle();
    }

    @Override public void onInterrupt() { handler.removeCallbacks(inspect); }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
