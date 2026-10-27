package ru.projector.hdmiautoclose;

public final class HdmiPromptPolicyTest {
    public static void main(String[] args) {
        check(true, "com.android.settings", "HDMI1 Is Connected", true, true);
        check(true, "com.android.settings", "HDMI2 Is Connected", true, true);
        check(true, "com.android.settings", "HDMI3 Is Connected", true, true);
        check(false, "com.gathin.tvhome", "HDMI1 Is Connected", true, true);
        check(false, "com.android.settings", "USB Is Connected", true, true);
        check(false, "com.android.settings", "Uninstall application?", true, true);
        check(false, "com.android.settings", "HDMI1 Is Connected", false, true);
        check(false, "com.android.settings", "HDMI1 Is Connected", true, false);
        check(false, "com.android.settings", null, true, true);
        check(false, null, "HDMI1 Is Connected", true, true);
        check(false, "com.android.settings", "HDMI1 Is Disconnected", true, true);
        check(false, "com.android.settings", "HDMI1 Is Connected. Delete settings?", true, true);
        System.out.println("PASS: 12 HDMI prompt safety cases");
    }

    private static void check(boolean expected, String pkg, String summary, boolean open, boolean cancel) {
        boolean actual = HdmiPromptPolicy.shouldDismiss(pkg, summary, open, cancel);
        if (actual != expected) throw new AssertionError("Expected " + expected + " for " + pkg + ": " + summary);
    }
}
