package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Saved component order, reconciled with the currently installed tiles. */
final class TileOrder {
    static List<String> restore(List<String> current, List<String> saved) {
        Set<String> remaining = new LinkedHashSet<>(current);
        List<String> result = new ArrayList<>();
        for (String key : saved) if (remaining.remove(key)) result.add(key);
        result.addAll(remaining);
        return result;
    }

    static <T> boolean move(List<T> items, int from, int to) {
        if (from < 0 || to < 0 || from >= items.size() || to >= items.size() || from == to) return false;
        items.add(to, items.remove(from));
        return true;
    }

    static int destination(int position, int count, int columns, int dx, int dy) {
        if (position < 0 || position >= count || columns <= 0) return position;
        if (dx < 0) return position % columns == 0 ? position : position - 1;
        if (dx > 0) return position % columns == columns - 1 || position == count - 1 ? position : position + 1;
        if (dy < 0) return position < columns ? position : position - columns;
        if (dy > 0) return position / columns == (count - 1) / columns ? position : Math.min(position + columns, count - 1);
        return position;
    }
}
