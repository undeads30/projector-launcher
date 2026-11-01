package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/** Curated utilities installed for this projector; system and dock apps never enter the folder. */
final class InstalledAppsFolder {
    static List<String> orderedMembers(Collection<String> installed, Collection<String> mainApps,
                                       Collection<String> movedIn, Collection<String> movedOut,
                                       List<String> savedOrder) {
        return TileOrder.restore(members(installed, mainApps, movedIn, movedOut), savedOrder);
    }

    static final List<String> PACKAGES = java.util.Collections.unmodifiableList(Arrays.asList(
            "me.zhanghai.android.files", "com.v2ray.ang", "org.liskovsoft.androidtv.rukeyboard"));
    private static final List<String> OWN_APPS = Arrays.asList(
            "com.gathin.tvhome", "ru.projector.digitalclock", "ru.projector.hdmiautoclose");

    static List<String> members(Collection<String> installed, Collection<String> mainApps) {
        return members(installed, mainApps, java.util.Collections.emptyList(), java.util.Collections.emptyList());
    }

    static List<String> members(Collection<String> installed, Collection<String> mainApps,
                                Collection<String> movedIn, Collection<String> movedOut) {
        List<String> result = new ArrayList<>();
        for (String pkg : PACKAGES) {
            if (installed.contains(pkg) && !mainApps.contains(pkg) && !movedOut.contains(pkg)) result.add(pkg);
        }
        for (String pkg : new java.util.TreeSet<>(movedIn)) {
            if (installed.contains(pkg) && !OWN_APPS.contains(pkg)
                    && !movedOut.contains(pkg) && !result.contains(pkg)) result.add(pkg);
        }
        return result;
    }
}
