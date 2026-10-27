package com.gathin.tvhome;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class TileOrderTest {
    @Test public void restoresSavedOrderAndAppendsNewApps() {
        assertEquals(Arrays.asList("c", "a", "b", "new"),
                TileOrder.restore(Arrays.asList("a", "b", "c", "new"), Arrays.asList("c", "a")));
    }
    @Test public void removesMissingAndDuplicateSavedKeys() {
        assertEquals(Arrays.asList("b", "a"),
                TileOrder.restore(Arrays.asList("a", "b"), Arrays.asList("gone", "b", "b")));
    }
    @Test public void emptySavedOrderKeepsConfiguredOrder() {
        assertEquals(Arrays.asList("b", "a"),
                TileOrder.restore(Arrays.asList("b", "a"), new ArrayList<>()));
        assertTrue(TileOrder.restore(new ArrayList<>(), Arrays.asList("gone")).isEmpty());
    }
    @Test public void movesByInsertionAndCanRestoreSnapshot() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        List<String> snapshot = new ArrayList<>(items);
        assertTrue(TileOrder.move(items, 0, 3));
        assertEquals(Arrays.asList("b", "c", "d", "a"), items);
        assertTrue(TileOrder.move(items, 3, 0));
        assertEquals(snapshot, items);
        assertEquals(Arrays.asList("a", "b", "c", "d"), snapshot);
    }
    @Test public void invalidOrUnchangedMovesDoNothing() {
        List<String> items = new ArrayList<>(Arrays.asList("a", "b"));
        assertFalse(TileOrder.move(items, -1, 1));
        assertFalse(TileOrder.move(items, 0, 2));
        assertFalse(TileOrder.move(items, 0, 0));
        assertEquals(Arrays.asList("a", "b"), items);
    }
    @Test public void gridEdgesDoNotWrap() {
        assertEquals(0, TileOrder.destination(0, 10, 4, -1, 0));
        assertEquals(3, TileOrder.destination(3, 10, 4, 1, 0));
        assertEquals(0, TileOrder.destination(0, 10, 4, 0, -1));
        assertEquals(9, TileOrder.destination(9, 10, 4, 1, 0));
        assertEquals(8, TileOrder.destination(8, 10, 4, 0, 1));
    }
    @Test public void verticalMoveClampsToShortFinalRow() {
        assertEquals(9, TileOrder.destination(7, 10, 4, 0, 1));
        assertEquals(1, TileOrder.destination(5, 10, 4, 0, -1));
        assertEquals(4, TileOrder.destination(0, 10, 4, 0, 1));
        assertEquals(2, TileOrder.destination(1, 10, 4, 1, 0));
    }
}
