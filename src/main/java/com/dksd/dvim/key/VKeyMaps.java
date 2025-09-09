package com.dksd.dvim.key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.history.Harpoons;
import com.dksd.dvim.telescope.Telescope;
import com.dksd.dvim.view.VimMode;
import com.dksd.dvim.key.trie.Trie;
import com.dksd.dvim.key.trie.TrieNode;
import com.dksd.dvim.view.Line;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.buildobjects.process.ProcBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VKeyMaps {

    private final VimEng vimEng;
    private Logger logger = LoggerFactory.getLogger(VKeyMaps.class);
    private final Map<VimMode, Trie> mappings = new ConcurrentHashMap<>();
    private final Map<KeyStroke, String> keyStrokeToStringMapping = new HashMap<>();
    //Used for duplication and getting a list of keys
    private final Set<String> keysMappings = new HashSet<>();
    private final Harpoons harpoons = new Harpoons();

    public VKeyMaps(VimEng ve) {
        this.vimEng = ve;
        loadVimKeyConverter();
        harpoons.add(Harpoons.DIRS, Paths.get("").toAbsolutePath().toString());
    }

    //TODO  read this from configuration file or something
    private void loadVimKeyConverter() {//VIM conversion only
        for (int i = 33; i < 127; i++) {
            final String chr = "" + (char) i;
            keyStrokeToStringMapping.put(new KeyStroke((char) i, false, false, false), chr);
            keyStrokeToStringMapping.put(new KeyStroke((char) i, true, false, false), "<c-" + chr + ">");
            keyStrokeToStringMapping.put(new KeyStroke((char) i, false, true, false), "<a-" + chr + ">");
            keyStrokeToStringMapping.put(new KeyStroke((char) i, false, false, true), "<s-" + chr + ">");
        }
        for (int i = 97; i < 122; i++) {
            final String chr = "" + (char) i;
            keyStrokeToStringMapping.put(
                    new KeyStroke(Character.toUpperCase((char) i), false, false, true),
                    "<s-" + chr.toUpperCase() + ">");
            putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH, VimMode.FUZZY_FIND), "<s-" + chr.toUpperCase() + ">", "Shift key mapping",
                    s -> {
                        vimEng.getActiveBuf().insertIntoLine(chr.toUpperCase());
                        return null;//no mapping
                    }, true);
            keyStrokeToStringMapping.put(
                    new KeyStroke(Character.toUpperCase((char) i), true, false, true),
                    "<sc-" + chr.toUpperCase() + ">");
            keyStrokeToStringMapping.put(
                    new KeyStroke(Character.toUpperCase((char) i), false, true, true),
                    "<sa-" + chr.toUpperCase() + ">");
        }
        shiftMap(keyStrokeToStringMapping, '!');
        shiftMap(keyStrokeToStringMapping, '@');
        shiftMap(keyStrokeToStringMapping, '#');
        shiftMap(keyStrokeToStringMapping, '$');
        shiftMap(keyStrokeToStringMapping, '%');
        shiftMap(keyStrokeToStringMapping, '^');
        shiftMap(keyStrokeToStringMapping, '&');
        shiftMap(keyStrokeToStringMapping, '*');
        shiftMap(keyStrokeToStringMapping, '(');
        shiftMap(keyStrokeToStringMapping, ')');
        shiftMap(keyStrokeToStringMapping, '_');
        shiftMap(keyStrokeToStringMapping, '+');
        shiftMap(keyStrokeToStringMapping, '{');
        shiftMap(keyStrokeToStringMapping, '}');
        shiftMap(keyStrokeToStringMapping, '|');
        shiftMap(keyStrokeToStringMapping, '"');
        shiftMap(keyStrokeToStringMapping, ':');
        shiftMap(keyStrokeToStringMapping, '<');
        shiftMap(keyStrokeToStringMapping, '>');
        shiftMap(keyStrokeToStringMapping, '?');
        shiftMap(keyStrokeToStringMapping, '~');

        keyStrokeToStringMapping.put(new KeyStroke(' ', false, false, false), "<leader>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.Enter, false, false, false), "<enter>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.Escape, false, false, false), "<esc>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.Backspace, false, false, false), "<bs>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.Delete, false, false, false), "<del>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.Home, false, false, false), "<home>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.End, false, false, false), "<end>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.ArrowLeft, false, false, false),
                "<left>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.ArrowRight, false, false, false),
                "<right>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.ArrowUp, false, false, false), "<up>");
        keyStrokeToStringMapping.put(new KeyStroke(KeyType.ArrowDown, false, false, false),
                "<down>");

        createSimpleCharMappings();

        putKeyMap(VimMode.COMMAND, "h", "Move left", s -> {
            vimEng.moveCursor(0, -1); //left
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "j", "Move down", s -> {
            vimEng.moveCursor(+1, 0); //down
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "k", "move up", s -> {
            vimEng.moveCursor(-1, 0); //up
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "l", "desc", s -> {
            vimEng.moveCursor(0, +1); //right
            return null;//no mapping
        }, true);
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH), "<left>", "desc", s -> {
            vimEng.moveCursor(0, -1); //left
            return null;//no mapping
        }, true);
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<down>", "desc", s -> {
            vimEng.moveCursor(+1, 0); //down
            return null;//no mapping
        }, true);
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<up>", "desc", s -> {
            vimEng.moveCursor(-1, 0); //up
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "/", "desc", s -> {
            vimEng.setVimMode(VimMode.INSERT);
            telescope(Line.convertLines(vimEng.getView().getActiveBuf().getLinesDangerous()),
                    lineResult -> {
                        vimEng.moveCursor(vimEng.getView().getActiveBufNo(), lineResult.getLineNumber(), 0);
                        vimEng.setVimMode(VimMode.COMMAND);
                    });
            return null;//no mapping
        });
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH), "<right>", "desc", s -> {
            vimEng.moveCursor(0, +1); //right
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.INSERT, "<enter>", "desc", s -> {
            vimEng.splitToNextLine();
            return null;//no mapping
        }, true);
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<home>", "desc", s -> {
            Line line = vimEng.getCurrentLine();
            int stripped = line.getContent().stripLeading().length();
            vimEng.moveCursor(line.length() - stripped, 0);
            return null;//no mapping
        }, true);
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<end>", "desc", s -> {
            Line line = vimEng.getCurrentLine();
            vimEng.moveCursor(0, line.length());
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "x", "desc", s -> {
            vimEng.deleteInLine(1);
            return null;//no mapping
        }, true);
        putKeyMap(VimMode.COMMAND, "w", "move forward a word", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            int offset = line.getContent().indexOf(" ", col);
            vimEng.moveCursor(0, offset);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "b", "move backward a word", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            int offset = 0;
            for (int i = col; i >= 0; i--) {
                if (Character.isLetter(line.getContent().charAt(i))) {
                    offset++;
                }
            }
            vimEng.moveCursor(0, -offset);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "yy", "copy line to mem", s -> {
            harpoons.addAll(Harpoons.CLIPBOARD, vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "dd", "deletes current line and stores in clipboard", s -> {
            harpoons.addAll(Harpoons.CLIPBOARD, vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            vimEng.deleteLines(vimEng.getRow(), vimEng.getRow() + 1);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "p", "paste clipboard", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            String l = line.getContent().substring(0, col) + harpoons.get(Harpoons.CLIPBOARD) + line.getContent().substring(col);
            vimEng.setLine(vimEng.getRow(), l);
            return null;//no mapping
        });

        putKeyMap(VimMode.COMMAND, "i", "desc", s -> {
            vimEng.setVimMode(VimMode.INSERT);
            System.out.println("Pressed i to go into insert mode");
            return null;//no mapping
        });
        putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT, VimMode.SEARCH, VimMode.FUZZY_FIND),
                List.of("<esc>"), "escape from any mode to command mode", s -> {
                    vimEng.setVimMode(VimMode.COMMAND);
                    vimEng.clearKeys();
                    System.out.println("Pressed Esc to go into command mode");
                    return null;//no mapping
                });
        putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH, VimMode.FUZZY_FIND), "<C-[>", "desc", s -> {
            vimEng.setVimMode(VimMode.COMMAND);
            System.out.println("Pressed <C-[> to go into command mode");
            return null;//no mapping
        });
        putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH, VimMode.FUZZY_FIND), "<bs>", "desc", s -> {
            vimEng.moveCursor(0, -1);
            vimEng.deleteInLine(1);
            vimEng.removeLastKeyStroke();
            System.out.println("Backspace pressed");
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>ff", "find files", s -> {
            telescope(streamPath(getCurrentPath(), Files::isRegularFile).toList(),
                    lineResult -> {
                vimEng.loadFile(vimEng.getActiveBuf(), lineResult.getContent());
            });
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<leader>fd", "find and set directories", s -> {
            telescope(streamPath(getCurrentPath(), Files::isDirectory).toList(), line -> {
                vimEng.setCurrentDir(line.getContent());
            });
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<c-w><right>", "expand active buffer to the right", s -> {
            vimEng.getView().expandScrollView(0,0,0,1);
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<c-w><left>", "expand active buffer to the right", s -> {
            vimEng.getView().expandScrollView(0,0,-1,0);
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<c-w><up>", "expand active buffer to the right", s -> {
            vimEng.getView().expandScrollView(-1,0,0,0);
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<c-w><down>", "expand active buffer to the right", s -> {
            vimEng.getView().expandScrollView(0,1,0,0);
            return null;
        });
        putKeyMap(VimMode.COMMAND, ":", "open command window", s -> {
            List<String> options = List.of("write", "read", "quit", "find", "grep");
            telescope(options, lineResult -> vimEng.executeFunction(vimEng.getActiveBuf(), lineResult.getContent()));
            return null;
        });
        putKeyMap(VimMode.COMMAND, "<leader>fm", "find key mapping", s -> {
            telescope(keysMappings.stream().toList(), lineResult -> harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent()));
            return null;
        });
        //Be kind cool to show various time options in telescope, then select mapping then another
        //telescope to show the recent files.
        //TODO
        putKeyMap(VimMode.COMMAND, "<leader>flf", "find recently changed files", s -> {
            //based on time range: find ./ -maxdepth 3 -type f -mtime -5
            //sorts: find ./ -type f -exec stat -c "%Y %n" {} + | sort -k1n
            //best so far: find . -type f -name "*.txt" -print0 | xargs -0 ls -t
            List<String> options = exec("find", getCurrentPath().toAbsolutePath().toString(), "-type f", "-print0",
                    "| xargs -0 ls " +
                            "-t");
            telescope(options, lineResult -> {
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>fb", "find buffer", s -> {
            telescope(vimEng.getView().getBufferFilenames(), lineResult -> {
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            telescope(exec("git", "branch", "-a"), lineResult -> {
                //TODO use git clipboard
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>gsha", "get latest SHA of git branch", s -> {
            telescope(exec("git", "log", "-n", "1"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            telescope(exec("git", "branch", "-a"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });
            return null;//no mapping
        });

        putKeyMap(VimMode.COMMAND, "<c-i>", "go to next or go in", s -> {
            //telescope(VKeyMaps::getBufferFilenames, System.out::println);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<c-o>", "go to prev or go out", s -> {
            //telescope(VKeyMaps::getBufferFilenames, System.out::println);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>hl", "list harpoon files", s -> {
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
        putKeyMap(VimMode.COMMAND, "<leader>ha", "add file/buffer to harpoon", s -> {
            //Would love it to popup an editable version... I like the zindex idea again.
            harpoons.get(Harpoons.FILES).add(vimEng.getActiveBuf().getFilename());
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>hc", "clear harpoon list", s -> {
            harpoons.get(Harpoons.FILES).clear();
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>hn", "switch to next file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).next()
        );
        putKeyMap(VimMode.COMMAND, "<leader>hp", "switch to prev file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).prev()
        );
        putKeyMap(VimMode.COMMAND, "<leader>ec", "create file (think echelon shhh)",
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
        putKeyMap(VimMode.COMMAND, "<leader>em", "move file",
                s -> {
                    //TODO
                    //try predict which directory to move to
                    //List directories from open buffers as options in telescope
                    //add recently created files.
                    //vimEng.File.moveFile();
                    return null;//no mapping
                });
        putKeyMap(VimMode.COMMAND, "<leader>er", "rename file/echelon",
                s -> {
                    //TODO
                    //vimEng.File.renameFile();
                    return null;//no mapping
                });
        putKeyMap(VimMode.COMMAND, "<leader>ecd", "create directory",
                s -> {
                    //TODO
                    //vimEng.File.createDir();
                    return null;//no mapping
                });
        putKeyMap(VimMode.COMMAND, "<leader>erd", "rename directory",
                s -> {
                    //TODO
                    //vimEng.File.renameDir();
                    return null;//no mapping
                });
        putKeyMap(VimMode.COMMAND, "<leader>emd", "move directory",
                s -> {
                    //TODO
                    //vimEng.File.renameDir();
                    return null;//no mapping
                });
        putKeyMap(List.of(VimMode.INSERT, VimMode.COMMAND), "<c-d><c-u>", "go up a directory (global)",
                s -> {
                    Directories.goUp(harpoons);
                    return null;//no mapping
                });
        putKeyMap(VimMode.COMMAND, "<leader>vl", "see/find all current variables", s -> {
            //List<String> varNames = var_clipboards.getAllVars();
            //varNames.addAll(var_dirs.getAllVars());
            //telescope(varNames, line -> varNames);  //want to copy to clipboard?
            return null;//no mapping
        });
        putKeyMap(List.of(VimMode.INSERT, VimMode.FUZZY_FIND), "<leader>", "desc", s -> {
            vimEng.writeBuf(" ");
            System.out.println("Typed a space!");
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "n", "desc", s -> {
            //findForward(getIntVar("search_buf_no"));
            //TODO
            return null;//no mapping
        });
        /*putKeyMap(VimMode.COMMAND, ":grep", "desc", s -> {
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
        putKeyMap(VimMode.COMMAND, ":get_routing_expr", "desc", s -> {
            List<String> output = exec("/Users/ddawkins/Developer/scripts/getRoutingConfig.sh");
//            int newBuf = getView().vsplitRight(getActiveBufNo(), "expr_routing", BorderType.LEFT);
//            getView().getBuffer(newBuf).setLinesFromStream(output);
//            getView().setActiveBufNo(newBuf);
            return null;//no mapping
        });
        putKeyMap(VimMode.COMMAND, "<leader>nml", "desc", s -> {
            return ":e /Users/ddawkins/Developer/notes/ml.md<enter>";
        });
        putKeyMap(VimMode.COMMAND, "<leader>rt", "run unit test", s -> {
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

    private void telescope(List<String> options, Consumer<Line> consumer) {
        //Support delete, tab completions as well from AI yes?
        Telescope.builder(vimEng, this)
                .options(options)
                .consumer(consumer)
                .timeout(30, TimeUnit.SECONDS)                 // shorter timeout
                .buildAndRun();
    }

    public void reMap(List<VimMode> vimModes, String left, String desc, Function<String, String> remapFunc, boolean hideMapping) {
        for (VimMode vimMode : vimModes) {
            TrieNode node = mappings.get(vimMode).find(left);
            if (node != null) {
                if (!node.getLastFunc().equals(remapFunc)) {
                    node.addFunction(desc, remapFunc);
                }
            }
        }
    }

    private int execScriptToBuffer(String filename, String script) {
        try {
            Path path = Files.writeString(Path.of(filename), script);
            List<String> output = exec(path.getFileName().toString());
//            int newBuf = getView().vsplitRight(getActiveBufNo(), "expr_routing", BorderType.LEFT);
//            getView().getBuffer(newBuf).setLinesFromStream(output);
//            getView().setActiveBufNo(newBuf);
//TODO fix
            //return newBuf;
        } catch (Exception ep) {
            ep.printStackTrace();
        }
        return -1;
    }

    private String getTeleEntryStream(VimMode vimMode,
                                      Map.Entry<String, KeyMap> entry) {
        if (entry.getValue().getDescription() == null) {
            return vimMode.name().toLowerCase() + "  ->  " + entry.getKey();
        }
        return vimMode.name().toLowerCase() + "  ->  " + entry.getKey()
                + ", " + entry.getValue().getDescription();
    }

    public Path getCurrentPath() {
        return Directories.getCurrentPath(harpoons);
    }

    public static Stream<String> streamPath(Path dir, Predicate<Path> filter) {
        try {
            return Files.walk(dir)
                    .filter(filter)
                    .map((path) -> path.toAbsolutePath().toString())
                    .filter(file -> !file.contains("/build/"))
                    .filter(file -> !file.contains("/target/"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void shiftMap(Map<KeyStroke, String> keyStrokeToStringMapping, char chr) {
        keyStrokeToStringMapping.put(new KeyStroke(chr, false, false, true), "" + chr);
    }

    private void createSimpleCharMappings() {
        for (int i = 32; i < 127; i++) {
            final String chr = "" + (char) i;
            putKeyMap(List.of(VimMode.INSERT, VimMode.SEARCH, VimMode.FUZZY_FIND), chr, "simple key mapping",
                    s -> {
                        vimEng.getActiveBuf().insertIntoLine(chr);
                        return null;//no mapping
                    }, true);
        }
    }

    private List<String> exec(String cmd, String... args) {
        String output = ProcBuilder.run(cmd, args);
        String[] lines = output.split("\n");
        return Arrays.asList(lines);
    }

    public void mapRecursively(List<TrieNode> nodes, int depth, VimMode vimMode, String keyStrokes) {
        if (keyStrokes == null || depth > 10) {
            return;
        }
        TrieNode foundNode = mappings.get(vimMode).find(keyStrokes);
        if (foundNode == null) {
            return;
        }
        nodes.add(foundNode);
        if (foundNode.isWord()) {
            String funcResult = foundNode.getLastFunc().apply(keyStrokes);
            mapRecursively(nodes, depth + 1, vimMode, funcResult);
        }
    }

    public String toVim(List<KeyStroke> keyStrokes) {
        StringBuffer sb = new StringBuffer();
        for (KeyStroke keyStroke : keyStrokes) {
            sb.append(keyStrokeToStringMapping.get(keyStroke));
        }
        String ret = sb.toString();
        return ret;
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                               String left,
                               String description,
                               Function<String, String> rightFunc) {
        return putKeyMap(vimModes, left, description, rightFunc, false);
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                               String left,
                               String description,
                               Function<String, String> rightFunc,
                               boolean systemMap) {
        return putKeyMap(vimModes, List.of(left), description, rightFunc, systemMap);
    }

    public TrieNode putKeyMap(VimMode vimMode,
                               String left,
                               String description,
                               Function<String, String> rightFunc) {
        return putKeyMap(vimMode, left, description, rightFunc, false);
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                               List<String> lefts,
                               String description,
                               Function<String, String> rightFunc) {
        return putKeyMap(vimModes, lefts, description, rightFunc, false);
    }

    /**
     * @param vimMode
     * @param left
     * @param description
     * @param rightFunc
     * @param hideMap
     */
    public TrieNode putKeyMap(VimMode vimMode,
                               String left,
                               String description,
                               Function<String, String> rightFunc,
                               boolean hideMap) {
        mappings.computeIfAbsent(vimMode, k -> new Trie());
        TrieNode node = mappings.get(vimMode).insert(left, description, rightFunc);
        node.setHideMapping(hideMap);
        keysMappings.add(left);
        return node;
    }

    public List<TrieNode> putKeyMap(List<VimMode> vimModes,
                               List<String> lefts,
                               String description,
                               Function<String, String> rightFunc,
                               boolean systemMap) {
        List<TrieNode> retNodes = new ArrayList<>();
        for (VimMode vimMode : vimModes) {
            for (String left : lefts) {
                putKeyMap(vimMode, left, description, rightFunc, systemMap);
            }
        }
        return retNodes;
    }

}
