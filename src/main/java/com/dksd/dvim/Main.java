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
import java.util.concurrent.atomic.AtomicLong;
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
        //defaultTerminalFactory.setTerminalEmulatorFrameAutoCloseTrigger();
        //defaultTerminalFactory.setForceTextTerminal(true);
        //Terminal terminal = null;
        final TerminalScreen screen = defaultTerminalFactory.createScreen();
        Logger.getLogger(Main.class.getName()).info("Created terminal: " + screen.getTerminal().getClass().getName());

        System.err.println("Created terminal: " + screen.getTerminal().getClass().getName());
        try {
            //defaultTerminalFactory.setTerminalEmulatorFontConfiguration(myFontConfiguration);
            //defaultTerminalFactory.setForceAWTOverSwing(true);
            //terminal = defaultTerminalFactory.createTerminal();
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


            /*
            The terminal content should already be cleared after switching to private mode, but in case it's not, the
            clear method should make all content set to default background color with no characters and the input cursor
            in the top-left corner.
             */
            screen.clear();

            /*
            It's possible to tell the terminal to hide the text input cursor
             */
            //screen.setsetCursorVisible(true);

            /*
            You still need to flush for changes to become visible
             */
            //screen.refresh();

            ExecutorService threadPool = Executors.newVirtualThreadPerTaskExecutor();
            VimEng ve = new VimEng(screen, threadPool);
            TrieMapManager trieMapManager = new TrieMapManager();
            VKeyMaps vKeyMaps = new VKeyMaps(ve, trieMapManager);
            ve.setKeyMaps(vKeyMaps);
            ve.updateStatus();
            /*
            You can attach a resize listener to your Terminal object, which will invoke a callback method (usually on a
            separate thread) when it is informed of the terminal emulator window changing size. Notice that maybe not
            all implementations supports this. The UnixTerminal, for example, relies on the WINCH signal being sent to
            the java process, which might not make it though if your remote shell isn't forwarding the signal properly.
             */
            screen.getTerminal().addResizeListener((terminal1, newSize) -> {
                // Be careful here though, this is likely running on a separate thread. Lanterna is threadsafe in
                // a best-effort way so while it shouldn't blow up if you call terminal methods on multiple threads,
                // it might have unexpected behavior if you don't do any external synchronization
                //textGraphics.drawLine(5, 3, newSize.getColumns() - 1, 3, ' ');
                //textGraphics.putString(5, 3, "Terminal Size: ", SGR.BOLD);
                //textGraphics.putString(5 + "Terminal Size: ".length(), 3, newSize.toString());
                //textGraphics.putString(5 + "Terminal Size: ".length(), 3, newSize.toString());
                ve.getView().fitScrollView(newSize.getColumns(), newSize.getRows());
                //ve.getView().draw(screen);
            });

            ///textGraphics.putString(5, 4, "Last Keystroke: ", SGR.BOLD);
            //textGraphics.putString(5 + "Last Keystroke: ".length(), 4, "<Pending>");
            //screen.flush();

            /*
            Now let's try reading some input. There are two methods for this, pollInput() and readInput(). One is
            blocking (readInput) and one isn't (pollInput), returning null if there was nothing to read.
             */

            //ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
            /*
            The KeyStroke class has a couple of different methods for getting details on the particular input that was
            read. Notice that some keys, like CTRL and ALT, cannot be individually distinguished as the standard input
            stream doesn't report these as individual keys. Generally special keys are categorized with a special
            KeyType, while regular alphanumeric and symbol keys are all under KeyType.Character. Notice that tab and
            enter are not considered KeyType.Character but special types (KeyType.Tab and KeyType.Enter respectively)
             */
            long lastkeyTime = 0;
            do {
                KeyStroke key = screen.readInput();
                if (key != null) {
                    ve.handleKey(trieMapManager, key);
                    if (key.getEventTime() - lastkeyTime > 2) {
                        ve.getView().draw(screen);
                    }
                    lastkeyTime = key.getEventTime();
                }
            } while (true); //the engine will exit via a System.exit

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