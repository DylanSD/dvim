package com.dksd.dvim.mapping;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.complete.TabCompletion;
import com.dksd.dvim.complete.Telescope;
import com.dksd.dvim.engine.VimEng;
import com.dksd.dvim.history.Harpoon;
import com.dksd.dvim.history.HarpoonFile;
import com.dksd.dvim.history.Harpoons;
import com.dksd.dvim.mapping.trie.TrieMapManager;
import com.dksd.dvim.model.ChatModel;
import com.dksd.dvim.model.ModelName;
import com.dksd.dvim.organize.TodoHelper;
import com.dksd.dvim.utils.PathHelper;
import com.dksd.dvim.utils.ScriptBuilder;
import com.dksd.dvim.view.Line;
import com.dksd.dvim.view.View;
import com.dksd.dvim.view.VimMode;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static com.dksd.dvim.utils.PathHelper.getCurrentDir;
import static com.dksd.dvim.utils.PathHelper.loadFilesIntoBufs;
import static com.dksd.dvim.utils.PathHelper.streamPathToStr;
import static com.dksd.dvim.view.View.SIDE_BUFFER;

public class VKeyMaps {

    private Logger logger = LoggerFactory.getLogger(VKeyMaps.class);
    private final ScriptBuilder sb = new ScriptBuilder();
    private final Harpoons harpoons = new Harpoons();
    private ChatModel chatMercuryModel = new ChatModel(ModelName.MERCURY);
    private ChatModel chatMercuryCoderModel = new ChatModel(ModelName.MERCURY_CODER);

    public void loadKeys(VimEng vimEng, TrieMapManager tm) {
        loadVimKeyConverter(vimEng, tm);
        harpoons.getDirs().add(getCurrentDir());
    }

    //TODO  read this from configuration file or something
    private void loadVimKeyConverter(VimEng vimEng, TrieMapManager tm) {//VIM conversion only
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
            tm.putKeyMap(List.of(VimMode.INSERT), "<s-" + chr.toUpperCase() + ">", "Shift key mapping",
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
        shiftMap(tm, '!');
        shiftMap(tm, '@');
        shiftMap(tm, '#');
        shiftMap(tm, '$');
        shiftMap(tm, '%');
        shiftMap(tm, '^');
        shiftMap(tm, '&');
        shiftMap(tm, '*');
        shiftMap(tm, '(');
        shiftMap(tm, ')');
        shiftMap(tm, '_');
        shiftMap(tm, '+');
        shiftMap(tm, '{');
        shiftMap(tm, '}');
        shiftMap(tm, '|');
        shiftMap(tm, '"');
        shiftMap(tm, ':');
        shiftMap(tm, '<');
        shiftMap(tm, '>');
        shiftMap(tm, '?');
        shiftMap(tm, '~');

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

        //Key mappings
        createSimpleCharMappings(vimEng, List.of(VimMode.INSERT), tm);

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
        tm.putKeyMap(VimMode.COMMAND, "l", "move right", s -> {
            vimEng.moveCursor(0, +1); //right
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<left>", "move cursor left", s -> {
            vimEng.moveCursor(0, -1); //left
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<down>", "move cursor down", s -> {
            vimEng.moveCursor(+1, 0); //down
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<up>", "move cursor up", s -> {
            vimEng.moveCursor(-1, 0); //up
            return null;//no mapping
        }, true);
        tm.putKeyMap(VimMode.COMMAND, "/", "search for text and jump to the text", s -> {
            vimEng.setVimMode(VimMode.INSERT);
            telescope(vimEng,
                    tm,
                    vimEng.getView().getActiveBuf().getLinesDangerous(),
                    obj -> obj.getContent() + ((obj.getGhostContent() != null) ? " : " + obj.getGhostContent() : ""),
                    tele -> {
                        Line selected = tele.getEitherResult();
                        Buf activeBuf = vimEng.getView().getActiveBuf();
                        int foundRow = selected.getLineNumber();
                        String foundStr = vimEng.getLineAt(foundRow).getContent();
                        vimEng.moveCursor(activeBuf.getBufNo(),
                                foundRow - vimEng.getRow(),
                                foundStr.indexOf(selected.getContent()) - vimEng.getCol());
                        vimEng.setVimMode(VimMode.COMMAND);
                        return null;
                    });
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<right>", "desc", s -> {
            vimEng.moveCursor(0, 1); //right
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
                    List.of("option one", "two", "three"),
                    vimEng,
                    lineResult -> {
                        //TODO can move back a word
                        activeBuf.insertIntoLine(lineResult.getContent());//brutal
                    });
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<leader>m", "call mercury llm", s -> {
            chatMercuryModel.chat("Can you review the text that follows and offer suggestions?: " +
                            vimEng.getActiveBuf().getCurrentLine(),
                    vimEng.getView().getBufferByName(View.SIDE_BUFFER));
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<leader>c", "call mercury coder llm", s -> {
            chatMercuryCoderModel.chat("Can you review the text that follows and offer suggestions?: " +
                            vimEng.getActiveBuf().getCurrentLine(),
                    vimEng.getView().getBufferByName(View.SIDE_BUFFER));
            return null;//no mapping
        }, true);
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT), "<home>", "desc", s -> {
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
            harpoons.getClipboard().add(vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "dd", "deletes current line and stores in clipboard", s -> {
            harpoons.getClipboard().add(vimEng.copyLines(vimEng.getRow(), vimEng.getRow() + 1));
            vimEng.deleteLines(vimEng.getRow());
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "p", "paste clipboard", s -> {
            Line line = vimEng.getCurrentLine();
            int col = vimEng.getCol();
            String l = line.getContent().substring(0, col) + harpoons.getClipboard().current() + line.getContent().substring(col);
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
        tm.putKeyMap(List.of(VimMode.COMMAND, VimMode.INSERT),
                List.of("<esc>"), "escape from any mode to command mode", s -> {
                    vimEng.setVimMode(VimMode.COMMAND);
                    vimEng.cancelTelescope();
                    System.out.println("Pressed Esc to go into command mode");
                    return null;//no mapping
                });
        tm.putKeyMap(List.of(VimMode.INSERT), "<C-[>", "desc", s -> {
            vimEng.setVimMode(VimMode.COMMAND);
            System.out.println("Pressed <C-[> to go into command mode");
            return null;//no mapping
        });
        tm.putKeyMap(List.of(VimMode.INSERT), "<bs>", "backspace", s -> {
            vimEng.moveCursor(0, -1);
            vimEng.deleteInLine(1);
            System.out.println("Backspace pressed");
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>ff", "find files", s -> {
            Buf activeBuf = vimEng.getView().getActiveBuf();
            telescope(vimEng, tm,
                    streamPathToStr(harpoons.getDirs().current(), Files::isRegularFile).toList(),
                    null,
                    tele -> {
                        List<Line> ls = PathHelper.readFile(Path.of(tele.getEitherResult().getContent()));
                        activeBuf.setLines(ls, 0);
                        return tele.getEitherResult().getContent();
                    });
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fd", "find and set directories", s -> {
            telescope(vimEng, tm,
                    streamPathToStr(harpoons.getDirs().current(), Files::isDirectory).toList(),
                    null,
                    tele -> {
                        Path result = Path.of(tele.getEitherResult().getContent());
                        harpoons.getDirs().setCurrent(result);
                        return tele.getEitherResult().getContent();
                    });
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
            Buf activeBuf = vimEng.getActiveBuf();
            telescope(vimEng, tm,
                    options,
                    null,
                    tele -> {
                        String result = tele.getInputBuf().getCurrentLine().getContent();
                        vimEng.executeFunction(activeBuf, result);
                        return result;
                    });
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fm", "find key mapping", s -> {
            telescope(vimEng, tm,
                    tm.getAllMappings(),
                    l -> l.getContent() + " : " + l.getGhostContent(),
                    tele -> {
                        String result = tele.getInputBuf().getCurrentLine().getContent();
                        harpoons.getClipboard().setCurrent(List.of(result));
                        return tele.getInputBuf().getCurrentLine();
                    });
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>hp", "show harpoon prompts with telescope", s -> {
            Harpoon<Path> promptHarpoon = new HarpoonFile("/Users/dylan/Developer/prompts/");
            telescope(vimEng, tm,
                    promptHarpoon.toList(),
                    path -> path.toString(),
                    tele -> {
                        Line result = tele.getEitherResult();
                        Path resultPath = Path.of(result.getContent());
                        return promptHarpoon.setCurrent(resultPath);
                    }
            );
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fh", "find harpoons", s -> {
            //telescope(vimEng, tm, harpoons.getList(), null);
            return null;
        });
        //Be kind cool to show various time options in telescope, then select mapping then another
        //telescope to show the recent files.
        //TODO
        tm.putKeyMap(VimMode.COMMAND, "<leader>flf", "find recently changed files", s -> {
            /*List<String> options = sb.exec("find", getCurrentPath().toAbsolutePath().toString(), "-type f", "-print0",
                    "| xargs -0 ls " +
                            "-t");
            telescope(options, lineResult -> {
                harpoons.getClipboard().add(lineResult.getContent());
            });*/
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>fb", "find buffer", s -> {
            /*telescope(vimEng.getView().getBufferFilenames(), lineResult -> {
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });*/
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>to", "todo manager", s -> {
            loadFilesIntoBufs(harpoons.getTodoProjects(),
                    Path.of("/Users/dylan/Developer/todo"),
                    Files::isRegularFile,
                    vimEng.getEvents());
            setMainBufFromBuf(vimEng, harpoons.getTodoProjects().current());
            //Ok so now what? folding, move up, move down
            return null;//no mapping // great idea is to execute a whole buch of functions.
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>m<up>", "move todo up", s -> {
            TodoHelper.moveTodoUpVim(vimEng.getRow(), vimEng.getActiveBuf().getLinesDangerous());
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>m<down>", "move todo down", s -> {
            //Probably will complain about inmodifiable list
            TodoHelper.moveTodoDownVim(vimEng.getRow(), vimEng.getActiveBuf().getLinesDangerous());
            return null;
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            /*telescope(sb.exec("git", "branch", "-a"), lineResult -> {
                //TODO use git clipboard
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });*/
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gsha", "get latest SHA of git branch", s -> {
            /*telescope(sb.exec("git", "log", "-n", "1"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });*/
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>gl", "list git branches", s -> {
            /*telescope(sb.exec("git", "branch", "-a"), lineResult -> {
                //TODO use latest git sha var or clipboard?
                harpoons.add(Harpoons.CLIPBOARD, lineResult.getContent());
            });*/
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
            /*telescope(harpoons.getFiles().list(), line -> {
                //TODO navigate to file
            });*/
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
            harpoons.getFiles().add(Path.of(vimEng.getActiveBuf().getFilename()));
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>hc", "clear harpoon list", s -> {
            //harpoons.get(Harpoons.FILES).clear();
            return null;//no mapping
        });
        /*tm.putKeyMap(VimMode.COMMAND, "<leader>hn", "switch to next file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).next()
        );
        tm.putKeyMap(VimMode.COMMAND, "<leader>hp", "switch to prev file in list", s ->
                ":e " + harpoons.get(Harpoons.FILES).prev()
        );*/
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
                    harpoons.getDirs().current();
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
            List<String> output = sb.exec("/Users/dylan/Developer/scripts/getRoutingConfig.sh");
//            int newBuf = getView().vsplitRight(getActiveBufNo(), "expr_routing", BorderType.LEFT);
//            getView().getBuffer(newBuf).setLinesFromStream(output);
//            getView().setActiveBufNo(newBuf);
            return null;//no mapping
        });
        tm.putKeyMap(VimMode.COMMAND, "<leader>nml", "desc", s -> {
            return ":e /Users/dylan/Developer/notes/ml.md<enter>";
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

    private void setMainBufFromBuf(VimEng vimEng, Buf current) {
        if (current == null) {
            return;
        }
        Buf mainBuf = vimEng.getView().getMainBuffer();
        mainBuf.setLines(current.getLinesDangerous(), 0);
        mainBuf.setFilename(current.getFilename());
    }

    private <T> Telescope<T> telescope(VimEng vimEng,
                               TrieMapManager tm,
                               List<T> options,
                               Function<T, String> optionToStrFunc,
                               Function<Telescope<T>, T> onEnterFunc) {
        Telescope<T> telescope = new Telescope<>(vimEng);
        telescope.setOptions(options);
        telescope.setOptionToStrFunc(optionToStrFunc);
        telescope.setOnEnterFunc(onEnterFunc);
        telescope.setTrieManager(tm);
        telescope.start();
        return telescope;
    }

    private void shiftMap(TrieMapManager tm, char chr) {
        tm.addStrokeMapping(new KeyStroke(chr, false, false, true), "" + chr);
    }

    private void createSimpleCharMappings(VimEng vimEng, List<VimMode> vimModes, TrieMapManager tm) {
        for (int i = 32; i < 127; i++) {
            final String chr = "" + (char) i;
            tm.putKeyMap(vimModes, chr, "simple insert char key mapping",
                    s -> {
                        vimEng.getActiveBuf().insertIntoLine(chr);
                        return null;//no mapping
                    }, true);
        }
    }
}
