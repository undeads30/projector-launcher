package ru.projector.hdmiautoclose;

final class HdmiPromptPolicy {
    static boolean shouldDismiss(String packageName, String summary,
                                 boolean hasOpenButton, boolean hasCancelButton) {
        return "com.android.settings".equals(packageName)
                && summary != null && summary.matches("HDMI[1-3] Is Connected")
                && hasOpenButton && hasCancelButton;
    }
}
