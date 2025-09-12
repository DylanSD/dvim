package com.dksd.dvim.mapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.history.Harpoons;
import com.dksd.dvim.model.ChatModel;
import com.dksd.dvim.model.ModelName;
import com.dksd.dvim.utils.ScriptBuilder;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.complete.TabCompletion;
import com.dksd.dvim.complete.Telescope;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;
import com.dksd.dvim.view.Line;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import de.gesundkrank.fzf4j.matchers.FuzzyMatcherV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.dksd.dvim.utils.PathHelper.getCurrentDir;
import static com.dksd.dvim.utils.PathHelper.streamPath;
import static com.dksd.dvim.utils.PathHelper.streamPathToStr;
import static com.dksd.dvim.view.View.MAIN_BUFFER;
import static com.dksd.dvim.view.View.SIDE_BUFFER;

public class VKeyMaps {

    private final VimEng vimEng;
    private Logger logger = LoggerFactory.getLogger(VKeyMaps.class);
    //Used for duplication and getting a list of keys
    //private final Set<String> keysMappings = new HashSet<>();
    private final TrieMapManager tm;
    private final ScriptBuilder sb = new ScriptBuilder();
    private final Harpoons harpoons = new Harpoons();
    private ChatModel chatMercuryModel = new ChatModel(ModelName.MERCURY);
    private ChatModel chatMercuryCoderModel = new ChatModel(ModelName.MERCURY_CODER);

    public VKeyMaps(VimEng ve, TrieMapManager tm) {
        this.vimEng = ve;
        this.tm = tm;
        loadVimKeyConverter();
        harpoons.add(Harpoons.DIRS, getCurrentDir().toString());
    }

    //TODO  read this from configuration file or something
    private void loadVimKeyConverter() {//VIM conversion only
        for (int i = 33; i < 127; i++) {
            final String chr = "" + (char) i;
            tm.addStrokeMapping(new KeyStroke((char) i, false, false, false), chr);
            tm.addStrokeMapping(new KeyStroke((char) i, true, false, false), "<c-" + chr + ">");
            tm.addStrokeMapping(new KeyStroke((char) i, false, true, false), "<a-" + chr + ">");
            tm.addStrokeMapping(new KeyStroke((char) i, false, false, true), "<s-" + chr + ">");
        }
        for (int i = 97; i < 122; i++) {
            final String chr = "" + (char) i;
            tm.addStrokeMapping(
                    new KeyStroke(Character.toUpperCase((char) i), false, false, true),
                    "<s-" + chr.toUpperCase() + ">");
            tm.addStrokeMapping(
                    new KeyStroke(Character.toUpperCase((char) i), false, true, false),
                    "<a-" + chr + ">");
            tm.addStrokeMapping(
                    new KeyStroke(Character.toUpperCase((char) i), false, true, true),
                    "<a-" + chr.toUpperCase() + ">");
            tm.putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH), "<s-" + chr.toUpperCase() + ">", "Shift key mapping",
                    s -> {
                        vimEng.getActiveBuf().insertIntoLine(chr.toUpperCase());
                        return null;//no mapping
                    }, true);
            tm.addStrokeMapping(
                    new KeyStroke(Character.toUpperCase((char) i), true, false, true),
                    "<sc-" + chr.toUpperCase() + ">");
            tm.addStrokeMapping(
                    new KeyStroke(Character.toUpperCase((char) i), false, true, true),
                    "<sa-" + chr.toUpperCase() + ">");
        }
        shiftMap( '!');
        shiftMap( '@');
        shiftMap( '#');
        shiftMap( '$');
        shiftMap( '%');
        shiftMap( '^');
        shiftMap( '&');
        shiftMap( '*');
        shiftMap( '(');
        shiftMap( ')');
        shiftMap( '_');
        shiftMap( '+');
        shiftMap( '{');
        shiftMap( '}');
        shiftMap( '|');
        shiftMap( '"');
        shiftMap( ':');
        shiftMap( '<');
        shiftMap( '>');
        shiftMap( '?');
        shiftMap( '~');

        tm.addStrokeMapping(new KeyStroke(' ', false, false, false), "<leader>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Enter, false, false, false), "<enter>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Tab, false, false, false), "<tab>");
        tm.addStrokeMapping(new KeyStroke('\t', false, false, false), "<tab>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ReverseTab, false, false, false), "<r-tab>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Escape, false, false, false), "<esc>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Backspace, false, false, false), "<bs>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Delete, false, false, false), "<del>");
        tm.addStrokeMapping(new KeyStroke(KeyType.Home, false, false, false), "<home>");
        tm.addStrokeMapping(new KeyStroke(KeyType.End, false, false, false), "<end>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowLeft, false, false, false),
                "<left>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowLeft, false, true, false),
                "<a-left>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowRight, false, false, false),
                "<right>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowRight, false, true, false),
                "<a-right>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowUp, false, false, false), "<up>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowUp, false, true, false), "<a-up>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowDown, false, false, false),
                "<down>");
        tm.addStrokeMapping(new KeyStroke(KeyType.ArrowDown, false, true, false),
                "<a-down>");
        tm.addStrokeMapping(new KeyStroke(KeyType.PageDown, false, false, false),
                "<page-down>");
        tm.addStrokeMapping(new KeyStroke(KeyType.PageUp, false, false, false),
                "<page-up>");

        createSimpleCharMappings();

        tm.putKeyMap(VimMode.COMMAND, "h", "Move left", s -> {
            vimEng.moveCursor(0, -1); //left
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "j", "Move down", s -> {
            vimEng.moveCursor(+1, 0); //down
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "k", "move up", s -> {
            vimEng.moveCursor(-1, 0); //up
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "l", "desc", s -> {
            vimEng.moveCursor(0, +1); //right
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH), "<left>", "desc", s -> {
            vimEng.moveCursor(0, -1); //left
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<down>", "desc", s -> {
            vimEng.moveCursor(+1, 0); //down
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<up>", "desc", s -> {
            vimEng.moveCursor(-1, 0); //up
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "/", "search for text and jump to the text", s -> {
            vimEng.setVimMode(VimMode.INSERT);
            telescope(Line.convertLines(vimEng.getView().getActiveBuf().getLinesDangerous()),
                    lineResult -> {
                        Buf activeBuf = vimEng.getView().getActiveBuf();
                        vimEng.moveCursor(activeBuf.getBufNo(), lineResult.getLineNumber() - vimEng.getRow(), vimEng.getLineAt(lineResult.getLineNumber()).getContent().indexOf(lineResult.getContent()) - vimEng.getCol());
                        vimEng.setVimMode(VimMode.COMMAND);
                    });
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH), "<right>", "desc", s -> {
            vimEng.moveCursor(0, +1); //right
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.INSERT, "<enter>", "desc", s -> {
            vimEng.splitToNextLine();
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "<enter>", "desc", s -> {
            //NOOP
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<tab>", "tab completion or insert spaces", s -> {
            TabCompletion tabCompletion = new TabCompletion(vimEng.getThreadPool());
            //create a context and ask the llm.
            //pri, code, files, classes
            //TODO Actually need a larger history since too many funcs
            //TODO what was the previous function description?
            //Can then load the correct context for options..
            //So I can have context
            Buf activeBuf = vimEng.getActiveBuf();
            tabCompletion.handleTabComplete(
                    List.of("optione one", "two", "three"),
                    vimEng,
                    lineResult -> {
                        //TODO can move back a word
                        activeBuf.insertIntoLine(lineResult.getContent());//brutal
                    });
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<leader>m", "call mercury llm", s -> {

            chatMercuryModel.chat("Can you review the text that follows and offer suggestions?: " +
                    vimEng.getActiveBuf().getLinesAsStr(),
                    vimEng.getView().getBufferByName(View.SIDE_BUFFER));
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<leader>c", "call mercury coder llm", s -> {
            chatMercuryCoderModel.chat("Can you review the text that follows and offer suggestions?: " +
                            vimEng.getActiveBuf().getLinesAsStr(),
                    vimEng.getView().getBufferByName(View.SIDE_BUFFER));
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<home>", "desc", s -> {
            //Line line = vimEng.getCurrentLine();
            vimEng.moveCursor(0, -vimEng.getCol());
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<end>", "desc", s -> {
            Line line = vimEng.getCurrentLine();
            vimEng.moveCursor(0, line.length());
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<page-up>", "desc", s -> {
            int delta = Math.min(vimEng.getRow(), vimEng.getActiveBuf().getScrollView().getHeight());
            vimEng.moveCursor(-delta, 0);
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<page-down>", "desc", s -> {
            int delta = Math.min(vimEng.getActiveBuf().size() - 1 - vimEng.getRow(), vimEng.getActiveBuf().getScrollView().getHeight());
            vimEng.moveCursor(delta, 0);
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "x", "desc", s -> {
            vimEng.deleteInLine(1);
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, List.of("w", "<a-right>"), "move forward a word", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            int offset = line.getContent().indexOf(" ", col);
            vimEng.moveCursor(0, offset - col + 1);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, List.of("b", "<a-left>"), "move backward a word", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            for (int i = col - 1; i >= 0; i--) {
                if (' ' == line.getContent().charAt(i)) {
                    vimEng.moveCursor(0, i - col);
                    return null;
                }
            }
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "yy", "copy line to mem", s -> {
            harpoons.addAll(Harpoons.CLIPBOARD, vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "dd", "deletes current line and stores in clipboard", s -> {
            harpoons.addAll(Harpoons.CLIPBOARD, vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            vimEng.deleteLines(vimEng.getRow());
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "p", "paste clipboard", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            String l = line.getContent().substring(0, col) + harpoons.get(Harpoons.CLIPBOARD) + line.getContent().substring(col);
            vimEng.setLine(vimEng.getRow(), l);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "u", "undo the last action", s -> {
            //vimEng.setVimMode(VimMode.INSERT);
            vimEng.popPrevChange();
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, ".", "redo the last action", s -> {
            //vimEng.setVimMode(VimMode.INSERT);
            tm.getPrevFunctionRuns().getFirst().getLastFunc().apply(vimEng.getCurrentLine().getContent());
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "i", "desc", s -> {
            vimEng.setVimMode(VimMode.INSERT);
            System.out.println("Pressed i to go into insert mode");
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH),
                List.of("<esc>"), "escape from any mode to command mode", s -> {
                    vimEng.setVimMode(VimMode.COMMAND);
                    vimEng.clearKeys();
                    vimEng.cancelTelescope();
                    System.out.println("Pressed Esc to go into command mode");
                    return null;//no mapping
                });
        tm.putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH), "<C-[>", "desc", s -> {
            vimEng.setVimMode(VimMode.COMMAND);
            System.out.println("Pressed <C-[> to go into command mode");
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH), "<bs>", "desc", s -> {
            vimEng.moveCursor(0, -1);
            vimEng.deleteInLine(1);
            vimEng.removeLastKeyStroke();
            System.out.println("Backspace pressed");
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>ff", "find files", s -> {
            telescope(streamPathToStr(getCurrentPath(), Files::isRegularFile).toList(),
                    lineResult -> {
                vimEng.loadFile(vimEng.getActiveBuf(), lineResult.getContent());
            });
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fd", "find and set directories", s -> {
            Predicate<Path> filter = path -> Files.isDirectory(path) &&
                    (path.toString().contains("projects") ||
                            path.toString().contains("wtcode") ||
                            path.toString().contains("dev"));
            telescope(streamPathToStr(getCurrentPath().getParent(), filter).toList(), line -> {
                setCurrentDir(line.getContent());
            });
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<c-w><right>", "expand active buffer to the right", s -> {
            if (vimEng.getView().getActiveBuf().getName().equals(MAIN_BUFFER)) {
                vimEng.getView().setActiveBufByName(View.SIDE_BUFFER);
            }
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<c-w><left>", "expand active buffer to the right", s -> {
            if (vimEng.getView().getActiveBuf().getName().equals(SIDE_BUFFER)) {
                vimEng.getView().setActiveBufByName(MAIN_BUFFER);
            }
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<c-w><up>", "expand active buffer to the right", s -> {
            //vimEng.getView().expandScrollView(-1,0,0,0);
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<c-w><down>", "expand active buffer to the right", s -> {
            //vimEng.getView().expandScrollView(0,1,0,0);
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, ":", "open command window", s -> {
            List<String> options = List.of("write", "read", "quit", "find", "grep");
            telescope(options, lineResult -> vimEng.executeFunction(vimEng.getActiveBuf(), lineResult.getContent()));
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fm", "find key mapping", s -> {
            //telescope(keysMappings.stream().toList(), lineResult -> harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent()));
            //TODO
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fh", "find harpoons", s -> {
            telescope(harpoons.getList(), null);
            return null;
        });
        //Be kind cool to show various time options in telescope, then select mapping then another
        //telescope to show the recent files.
        //TODO
        tm.putKeyMap(VimMode.COMMAND, "<leader>flf", "find recently changed files", s -> {
            //based on time range: find ./ -maxdepth 3 -type f -mtime -5
            //sorts: find ./ -type f -exec stat -c "%Y %n" {} + | sort -k1n
            //best so far: find . -type f -name "*.txt" -print0 | xargs -0 ls -t
            List<String> options = sb.exec("find", getCurrentPath().toAbsolutePath().toString(), "-type f", "-print0",
                    "| xargs -0 ls " +
                            "-t");
            telescope(options, lineResult -> {
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fb", "find buffer", s -> {
            telescope(vimEng.getView().getBufferFilenames(), lineResult -> {
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            telescope(sb.exec("git", "branch", "-a"), lineResult -> {
                //TODO use git clipboard
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gsha", "get latest SHA of git branch", s -> {
            telescope(sb.exec("git", "log", "-n", "1"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            telescope(sb.exec("git", "branch", "-a"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });

        tm.putKeyMap(VimMode.COMMAND, "<c-i>", "go to next or go in", s -> {
            //telescope(VKeyMaps::getBufferFilenames, System.out::println);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<c-o>", "go to prev or go out", s -> {
            //telescope(VKeyMaps::getBufferFilenames, System.out::println);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>hl", "list harpoon files", s -> {
            telescope(harpoons.get(Harpoons.FILES).list(), line -> {
                //TODO navigate to file
            });
            return null;//no mapping
        });
        /*
        TODO can standardize.
        think of class based on string key, basically everything is a harpoon
        - current value
        - list of possible values
        - maintain list
         */
        tm.putKeyMap(VimMode.COMMAND, "<leader>ha", "add file/buffer to harpoon", s -> {
            //Would love it to popup an editable version... I like the zindex idea again.
            harpoons.get(Harpoons.FILES).add(vimEng.getActiveBuf().getFilename());
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>hc", "clear harpoon list", s -> {
            harpoons.get(Harpoons.FILES).clear();
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>hn", "switch to next file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).next()
        );
        tm.putKeyMap(VimMode.COMMAND, "<leader>hp", "switch to prev file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).prev()
        );
        tm.putKeyMap(VimMode.COMMAND, "<leader>ec", "create file (think echelon shhh)",
                s -> {
                    //TODO
                    //Just create in the directory the source file is from.
                    //Can fx  via the command line lazy or whatever.
                    //try predict directory.
                    //Could be a 2 step telescope, select directoy and then the file.
                    //List directories from open buffers as options in telescope
                    //add recently created files.
                    //vimEng.File.createFile();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>em", "move file",
                s -> {
                    //TODO
                    //try predict which directory to move to
                    //List directories from open buffers as options in telescope
                    //add recently created files.
                    //vimEng.File.moveFile();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>er", "rename file/echelon",
                s -> {
                    //TODO
                    //vimEng.File.renameFile();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>ecd", "create directory",
                s -> {
                    //TODO
                    //vimEng.File.createDir();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>erd", "rename directory",
                s -> {
                    //TODO
                    //vimEng.File.renameDir();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>emd", "move directory",
                s -> {
                    //TODO
                    //vimEng.File.renameDir();
                    return null;//no mapping
                });
        tm.putKeyMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<c-d><c-u>", "go up a directory (global)",
                s -> {
                    harpoons.get(Harpoons.DIRS).current();
                    return null;//no mapping
                });
        tm.putKeyMap(VimMode.COMMAND, "<leader>vl", "see/find all current variables", s -> {
            //List<String> varNames = var_clipboards.getAllVars();
            //varNames.addAll(var_dirs.getAllVars());
            //telescope(varNames, line -> varNames);  //want to copy to clipboard?
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.INSERT), "<leader>", "desc", s -> {
            vimEng.writeBuf(" ");
            System.out.println("Typed a space!");
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "n", "desc", s -> {
            //findForward(getIntVar("search_buf_no"));
            //TODO
            return null;//no mapping
        });
        /*tm.putKeyMap(VimMode.COMMAND, ":grep", "desc", s -> {
            //Consumer<List<TeleResult<String>>> resultConsumer =
            //        (str) -> {
                        //TODO Does buf exist?
//                    int newBuf = getView().vsplitRight(getActiveBufNo(), "new buf", BorderType.LEFT);
//                    getView().getBuffer(newBuf).readFile(str.get(0).teleEntry.getDisplayLine());
//                    getView().setActiveBufNo(newBuf);
                    };
//            teleScopeFactory.createRepeatable((lineSoFar) -> exec("grep", lineSoFar, Vars.getStrVar(Vars.CURRENT_DIR),
//                "-R"), resultConsumer);
            return null;//no mapping
        });*/
        tm.putKeyMap(VimMode.COMMAND, ":get_routing_expr", "desc", s -> {
            List<String> output = sb.exec("/Users/ddawkins/Developer/scripts/getRoutingConfig.sh");
//            int newBuf = getView().vsplitRight(getActiveBufNo(), "expr_routing", BorderType.LEFT);
//            getView().getBuffer(newBuf).setLinesFromStream(output);
//            getView().setActiveBufNo(newBuf);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>nml", "desc", s -> {
            return ":e /Users/ddawkins/Developer/notes/ml.md<enter>";
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>rt", "run unit test", s -> {
            //TODO
            //int linePos = findLineAbove("@Test");
            //or findClosestMethodName();
            //findClosestBlock(EMPTY_LINE, "{", "}", getCursorPos()); // for java, capture empty line followed by curly
//            findClosestIndentedBlock("{", "}"); //for python
//            findGradleModule()
//            getJavaPackage()
//            vimEng.executeCommand("gradle", :feature-extraction:common:test --tests "com.sift.flow2.dataproducers");
            //:test --tests "ViewTest.hsplitViewsMultipleTimes"
//            putResultsInQuickfixWindow()
            //execute gradle command.
            //be cool to also set jdk version
            // execute gradle using jproc: :feature-extraction:common:test --tests "com.sift.flow2.dataproducers
            // .UsersPerDataProducerTest"
            return null;//no mapping
        });
        /*addKeyMap(VimMode.COMMAND, ":e", s -> {

            return null;//no mapping
        });*/
    }

    private void setCurrentDir(String dir) {
        harpoons.get(Harpoons.DIRS).setCurrent(dir);
    }

    private Path getCurrentPath() {
        return Path.of(harpoons.get(Harpoons.DIRS).current());
    }

    private void telescope(List<String> options, Consumer<Line> consumer) {
        Telescope<String, Line> telescope = new Telescope<>(vimEng);
        telescope.setOptions(options);
        telescope.setConsumer(consumer);
        telescope.setOptionToStrFunc(String::toString);
        telescope.setOnEnterFunc(tele -> {
            Line selected = tele.getResultsBuf().getCurrentLine();
            if (!selected.isEmpty()) {
                telescope.getResultFuture().complete(selected);
            } else {
                telescope.getResultFuture().complete(tele.getInputBuf().getCurrentLine());
            }
            return null;
        });
        telescope.setTreiManager(tm);
        telescope.start();
    }

    private void shiftMap(char chr) {
        tm.addStrokeMapping(new KeyStroke(chr, false, false, true), "" + chr);
    }

    private void createSimpleCharMappings() {
        for (int i = 32; i < 127; i++) {
            final String chr = "" + (char) i;
            tm.putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH), chr, "simple key mapping",
                    s -> {
                        vimEng.getActiveBuf().insertIntoLine(chr);
                        return null;//no mapping
                    }, true);
        }
    }

    public TrieMapManager getTrieManager() {
        return tm;
    }
}
