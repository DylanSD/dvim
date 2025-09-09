import java.io.IOException;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.junit.jupiter.api.Assertions.assertEquals;
import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.view.DispObj;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.ScrollView;
import com.dksd.dvim.view.VimMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@Nested
class BufTest {

    private Buf buf;

    @BeforeEach
    void setUp() {
        Queue<VimEvent> queue = new ConcurrentLinkedDeque<>();
        buf = new Buf("test","filename", 0,  new ScrollView(0, 0), queue, true);
        buf.getScrollView().setRowStart(0);
        buf.getScrollView().setColStart(0);
        buf.getScrollView().setRowEnd(1);
        buf.getScrollView().setColEnd(1);
    }

    @Test
    void insertIntoBlankLine() {
        buf.insertIntoLine("");
        //verify position
        assertEquals(0, buf.getCol());
        assertEquals(0, buf.getRow());
        assertEquals("", buf.getLine());
    }
    
    @Test
    void insertIntoLine() {
        buf.insertIntoLine("abcd");
        //verify position.
        assertEquals(4, buf.getCol());
        assertEquals(0, buf.getRow());
        assertEquals("abcd", buf.getLine());
    }

    @Test
    void insertIntoExistingLine() {
        buf.insertIntoLine("abjk");
        //buf.setCol(2);
        //buf.setRow(0);
        buf.insertIntoLine("cdefghi");
        //verify position.
        assertEquals(9, buf.getCol());
        assertEquals(0, buf.getRow());
        assertEquals("abcdefghijk", buf.getLine());
    }

    @Test
    void writeAndReadFiles() throws IOException {
        String fn = "/tmp/tmp_" + System.currentTimeMillis();
        buf.addRow("test1");
        buf.addRow("test2");
        buf.writeFile(fn);
        buf.readFile(fn);
        System.out.println(buf.getLinesDangerous());

    }

    @Test
    void croppedLinesOneHeightTest() {
        buf.setLines(List.of(new Line(0, "")));
        List<DispObj> dispObjs = buf.getLinesToDisplay(VimMode.COMMAND);
        assertEquals(dispObjs.size(), 1);
    }

    @Test
    void croppedLinesTenHeightTest() {
//        buf.
//        buf.setLines(List.of(new Line(0, "Line 1"),
//                new Line(1, "Line 2"),
//                new Line(2, "Line 2"),
//                new Line(3, "Line 2"),
//                new Line(4, "Line 2"),
//                new Line(5, "Line 2")));
        //List<Line> lines = buf.getCroppedLines();
        //assertEquals(lines.size(), 3);
    }

}