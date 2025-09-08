import java.io.IOException;

import com.dksd.dvim.engine.VimEng;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VimEngTest {

    private VimEng vimEng;
    @BeforeEach
    void setUp() throws IOException {
        vimEng = new VimEng(null, null);
    }

    @Test
    void parseCommandKeys() {

    }

    @Test
    void execute() {

    }

    @Test
    void handleKey() throws IOException {
        //vimEng.handleKey(new KeyStroke('i', false, false));
        //vimEng.handleKey(new KeyStroke(' ', false, false));
//        vimEng.handleKey(new KeyStroke('f', false, false));
    }
}