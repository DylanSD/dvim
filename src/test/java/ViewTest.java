import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;

import static com.dksd.dvim.view.View.HEADER_BUFFER;
import static com.dksd.dvim.view.View.INPUT_BUFFER;
import static com.dksd.dvim.view.View.MAIN_BUFFER;
import static com.dksd.dvim.view.View.SIDE_BUFFER;
import static com.dksd.dvim.view.View.STATUS_BUFFER;
import static com.dksd.dvim.view.View.TERMINAL_BUFFER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.MockitoAnnotations.openMocks;
import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.buffer.BufferMode;
import com.dksd.dvim.view.LineIndicator;
import com.dksd.dvim.view.View;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.screen.TerminalScreen;
import de.gesundkrank.fzf4j.Fzf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class ViewTest {

    private View view;
    @Mock
    private TerminalScreen terminalScreenMock;
    @Mock
    private TerminalSize terminalSizeMock;
    @Mock
    private TextGraphics textGraphicsMock;

    @BeforeEach
    void setUp() throws IOException {
        openMocks(this);
        Mockito.when(terminalScreenMock.getTerminalSize()).thenReturn(terminalSizeMock);
        Mockito.when(terminalScreenMock.newTextGraphics()).thenReturn(textGraphicsMock);
        Mockito.when(terminalSizeMock.getRows()).thenReturn(80);
        Mockito.when(terminalSizeMock.getColumns()).thenReturn(100);
        view = new View("testView",terminalScreenMock, Executors.newVirtualThreadPerTaskExecutor());
    }

    @Test
    void createViews() {
        int screenWidth = 100;
        int screenHeight = 80;

        view.calcScrollView(100, 80);

        assertEquals(view.getBufferByName(STATUS_BUFFER).getScrollView().getRowStart(), 0);
        assertEquals(view.getBufferByName(HEADER_BUFFER).getScrollView().getRowStart(), 1);
        assertEquals(view.getBufferByName(INPUT_BUFFER).getScrollView().getRowStart(), 2);
        assertEquals(view.getBufferByName(MAIN_BUFFER).getScrollView().getRowStart(), 3);
        assertEquals(view.getBufferByName(SIDE_BUFFER).getScrollView().getRowStart(), 3);
        assertEquals(view.getBufferByName(TERMINAL_BUFFER).getScrollView().getRowStart(), 20);

        assertEquals(view.getBufferByName(MAIN_BUFFER).getScrollView().getRowStart(), 3);
        assertEquals(view.getBufferByName(MAIN_BUFFER).getScrollView().getColStart(), 0);
        assertEquals(view.getBufferByName(MAIN_BUFFER).getScrollView().getRowEnd(), 20);
        assertEquals(view.getBufferByName(MAIN_BUFFER).getScrollView().getColEnd(), 50);

    }

    @Test
    void testCursorMainView() {

    }
}