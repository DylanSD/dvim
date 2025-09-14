package com.dksd.dvim;

import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.mapping.VKeyMaps;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;


/**4
 * The second tutorial, using more complicated Terminal functionality
 * @author Martin
 */
public class Main {

    //What I am thinking is to maybe store the
    //string as one long string, then map to row/col
    //separately
    //Also using a StringBuffer. with more customer methods.
    public static void main(String[] args) throws IOException {

        //Font myFont = new Font("Monospaced", Font.PLAIN, 16); // Change the number 20 to your desired font size
        //AWTTerminalFontConfiguration myFontConfiguration = AWTTerminalFontConfiguration.newInstance(myFont);
        // Use myFontConfiguration when creating your terminal
        // Create a default terminal (will use Swing on desktop)
        // Use myFontConfiguration when creating your terminal


        //Terminal terminal = dtf.createTerminal();
        /*
        In this second tutorial, we'll expand on how to use the Terminal interface to provide more advanced
        functionality.
        */
        DefaultTerminalFactory defaultTerminalFactory = new DefaultTerminalFactory();
        defaultTerminalFactory.setTerminalEmulatorTitle("DKSD Terminal v0.0.1");

        TerminalSize terminalSize = new TerminalSize(100, 40);
        defaultTerminalFactory.setInitialTerminalSize(terminalSize);
        final TerminalScreen screen = defaultTerminalFactory.createScreen();
        Logger.getLogger(Main.class.getName()).info("Created terminal: " + screen.getTerminal().getClass().getName());

        System.err.println("Created terminal: " + screen.getTerminal().getClass().getName());
        try {
            screen.startScreen();
            /*
            Most terminals and terminal emulators supports what's known as "private mode" which is a separate buffer for
            the text content that does not support any scrolling. This is frequently used by text editors such as nano
            and vi in order to give a "fullscreen" view. When exiting from private mode, the previous content is usually
            restored, including the scrollback history. Emulators that don't support this properly might at least clear
            the screen after exiting.

            You can use the enterPrivateMode() to activate private mode, but you'll need to remember to also exit
            private mode afterwards so that you don't leave the terminal in a weird state when the application exists.
            The usual close() at the end will do this automatically, but you can also manually call exitPrivateMode()
            and finally Lanterna will register a shutdown hook that tries to restore the terminal (including exiting
            private mode, if necessary) as well.
             */
            //terminal.enterPrivateMode();


            screen.clear();

            ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
            TrieMapManager trieMapManager = new TrieMapManager();
            VKeyMaps vKeyMaps = new VKeyMaps();
            VimEng ve = new VimEng(screen, threadPool, trieMapManager);
            vKeyMaps.loadKeys(ve, trieMapManager);

            ve.updateStatusBuffer("");
            screen.getTerminal().addResizeListener((terminal1, newSize) -> {
                ve.getView().fitScrollView(newSize.getColumns(), newSize.getRows());
            });

            do {
                ve.handleKey(screen.readInput());
            } while (true);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            //ve.stop();
            if(screen != null) {
                try {
                    /*
                    The close() call here will exit private mode
                     */
                    screen.stopScreen();
                    screen.close();
                }
                catch(IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}