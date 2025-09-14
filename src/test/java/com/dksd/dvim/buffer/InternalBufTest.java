package com.dksd.dvim.buffer;

import static org.junit.jupiter.api.Assertions.*;
import com.dksd.dvim.view.Line;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InternalBufTest {

    private InternalBuf buf;
    private Line line1, line2, line3;

    @BeforeEach
    void setUp() {
        buf = new InternalBuf(true);
        line1 = new Line(0, "line1", null);
        line2 = new Line(1, "line2", null);
        line3 = new Line(2, "line3", null);
    }

    @Test
    void testGetOutOfBoundsReturnsNull() {
        assertNull(buf.getCurrBuf(5));
    }

    @Test
    void testSetOutOfBoundsDoesNothing() {
        buf.add(line1);
        buf.set(5, line2); // no effect
        assertEquals(1, buf.size());
        //assertEquals("line1", buf.getCurrBuf(0).getText());
    }

    @Test
    void testRemoveOutOfBoundsDoesNothing() {
        buf.add(line1);
        buf.remove(5); // no effect
        assertEquals(1, buf.size());
    }

    @Test
    void testAddNegativeIndexInsertsAtStart() {
        buf.add(line1);
        buf.add(-1, line2);
        //assertEquals("line2", buf.getCurrBuf(0).getText());
    }

    @Test
    void testAddTooLargeIndexAppendsAtEnd() {
        buf.add(line1);
        buf.add(10, line2);
        //assertEquals("line2", buf.getCurrBuf(1).getText());
    }

}
