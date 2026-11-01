package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AppMenuPlacementTest {
    @Test public void shrinksEveryDimensionByTwentyPercent() {
        assertEquals(192, AppMenuPlacement.scaled(240));
        assertEquals(173, AppMenuPlacement.scaled(216));
    }

    @Test public void folderMenuStartsAtTheTilesVerticalCenter() {
        assertEquals(550, AppMenuPlacement.top(430, 240, 346, 0, 1080, 20, true));
    }

    @Test public void menuStaysOnScreenWhenTheTileIsNearAnEdge() {
        assertEquals(714, AppMenuPlacement.top(860, 240, 346, 0, 1080, 20, true));
        assertEquals(334, AppMenuPlacement.top(700, 220, 346, 0, 1080, 20, false));
        assertEquals(320, AppMenuPlacement.top(100, 200, 346, 0, 1080, 20, false));
    }
}
