package com.gathin.tvhome;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class InstalledAppsFolderTest {
    @Test public void includesOnlyOurInstalledUtilitiesInStableOrder() {
        assertEquals(Arrays.asList("me.zhanghai.android.files", "com.v2ray.ang",
                "org.liskovsoft.androidtv.rukeyboard"), InstalledAppsFolder.members(Arrays.asList(
                "ru.projector.hdmiautoclose", "com.miui.analytics", "com.v2ray.ang",
                "ru.projector.digitalclock", "org.liskovsoft.androidtv.rukeyboard",
                "me.zhanghai.android.files", "com.v2ray.ang"), Collections.emptyList()));
    }

    @Test public void neverDuplicatesAnyAppAlreadyInTheMainRow() {
        assertEquals(Collections.singletonList("com.v2ray.ang"), InstalledAppsFolder.members(
                Arrays.asList("com.google.android.youtube.tv", "com.vk.tv", "mov.film",
                        "com.yandex.browser.tv", "me.zhanghai.android.files", "com.v2ray.ang"),
                Arrays.asList("com.google.android.youtube.tv", "com.vk.tv", "mov.film",
                        "com.yandex.browser.tv", "me.zhanghai.android.files")));
    }

    @Test public void removedUtilitiesDisappearAndMissingPackagesAreNotInvented() {
        assertEquals(Collections.emptyList(), InstalledAppsFolder.members(
                Arrays.asList("android", "com.gathin.tvhome", "com.miui.analytics"),
                Collections.emptyList()));
        assertEquals(Collections.singletonList("com.v2ray.ang"),
                InstalledAppsFolder.members(Collections.singletonList("com.v2ray.ang"),
                        Collections.emptyList()));
    }

    @Test public void explicitMoveToFolderCanIncludeAMainApp() {
        assertEquals(Arrays.asList("com.v2ray.ang", "com.vk.tv"), InstalledAppsFolder.members(
                Arrays.asList("com.vk.tv", "com.v2ray.ang"), Collections.singletonList("com.vk.tv"),
                Collections.singletonList("com.vk.tv"), Collections.emptyList()));
    }

    @Test public void explicitMoveToMainOverridesDefaultGrouping() {
        assertEquals(Collections.singletonList("com.v2ray.ang"), InstalledAppsFolder.members(
                Arrays.asList("me.zhanghai.android.files", "com.v2ray.ang"), Collections.emptyList(),
                Collections.emptyList(), Collections.singletonList("me.zhanghai.android.files")));
    }

    @Test public void explicitChoicesNeverInventMissingAppsOrDuplicates() {
        assertEquals(Collections.singletonList("com.v2ray.ang"), InstalledAppsFolder.members(
                Collections.singletonList("com.v2ray.ang"), Collections.emptyList(),
                Arrays.asList("com.v2ray.ang", "com.v2ray.ang", "missing.app"), Collections.emptyList()));
    }

    @Test public void ourOwnBackgroundAppsStayHiddenEvenWithOldExplicitChoices() {
        java.util.List<String> ownApps = Arrays.asList("ru.projector.digitalclock",
                "ru.projector.hdmiautoclose", "com.gathin.tvhome");
        assertEquals(Collections.emptyList(), InstalledAppsFolder.members(ownApps,
                Collections.emptyList(), ownApps, Collections.emptyList()));
    }

    @Test public void savedFolderOrderKeepsNewAppsAndDropsMissingOnes() {
        assertEquals(Arrays.asList("com.v2ray.ang", "me.zhanghai.android.files",
                "org.liskovsoft.androidtv.rukeyboard"), InstalledAppsFolder.orderedMembers(
                Arrays.asList("me.zhanghai.android.files", "com.v2ray.ang", "org.liskovsoft.androidtv.rukeyboard"),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Arrays.asList("gone.app", "com.v2ray.ang", "com.v2ray.ang", "me.zhanghai.android.files")));
    }

    @Test public void savedOrderCannotRestoreHiddenOrMovedOutApps() {
        assertEquals(Collections.singletonList("com.v2ray.ang"), InstalledAppsFolder.orderedMembers(
                Arrays.asList("ru.projector.digitalclock", "com.v2ray.ang", "me.zhanghai.android.files"),
                Collections.emptyList(), Collections.emptyList(), Collections.singletonList("me.zhanghai.android.files"),
                Arrays.asList("ru.projector.digitalclock", "me.zhanghai.android.files", "com.v2ray.ang")));
    }
}
