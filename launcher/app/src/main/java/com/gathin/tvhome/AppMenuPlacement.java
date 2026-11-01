package com.gathin.tvhome;

/** Menu dimensions and screen bounds, independent of Android views. */
final class AppMenuPlacement {
    static final float SCALE = 0.8f;

    static int scaled(int dimension) { return Math.round(dimension * SCALE); }

    static int top(int tileTop, int tileHeight, int menuHeight,
                   int screenTop, int screenBottom, int gap, boolean startAtCenter) {
        int below = tileTop + tileHeight + gap;
        int wanted = startAtCenter ? tileTop + tileHeight / 2
                : below + menuHeight <= screenBottom - gap ? below : tileTop - menuHeight - gap;
        return Math.max(screenTop + gap, Math.min(wanted, screenBottom - menuHeight - gap));
    }
}
