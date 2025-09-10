import java.util.ArrayList;
import java.util.List;

import com.googlecode.lanterna.input.KeyStroke;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VKeyMapsTest {

    //private VKeyMaps vKeyMaps;

    @BeforeEach
    public void setUp() throws Exception {
//        VKeyMaps vKeyMaps = new VKeyMaps();
//        vKeyMaps.putKeyMap(VimMode.INSERT, "i", "i");
//        vKeyMaps.addKeyMap(VimMode.INSERT, "ii", "iii");
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "i", ":vim.api.insertmode()");
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "h", ":vim.api.write_buf(%k)"); //%k means the key typed
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "e", ":vim.api.write_buf(%k)");
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "l", ":vim.api.write_buf(%k)");
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "l", ":vim.api.write_buf(%k)");
//        vKeyMaps.addKeyMap(VimMode.COMMAND, "o", ":vim.api.write_buf(%k)");
    }

    @Test
    public void testMappings() {
        //Can be KeyStrokes which is how we will get them.
        //And then convert to strings and then process.

        /*System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "i"));
        System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "h"));
        System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "e"));
        System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "l"));
        System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "l"));
        System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "o"));*/

        //System.out.println(VKeyMaps.mapRecursively(VimMode.COMMAND, "i"));


        //execute
        //verify logs from commands.
        //should maybe move to different test class aka vim engine.

        // I want to type some charactiers and then see how they
        //get executed on the backend.
        //via logs from the command processor.

        //want to verify the engine performed certain operations
        //maybe in tring log form
        //must at each step look for special characters. and then decide next mappings


     /*   nvim_set_keymap("i", "jj", "<Esc>", {noremap=false})

nvim_set_keymap("n", "tw", ":Twilight<enter>", {noremap=false})
nvim_set_keymap("n", "tk", ":blast<enter>", {noremap=false})
nvim_set_keymap("n", "tj", ":bfirst<enter>", {noremap=false})
nvim_set_keymap("n", "th", ":bprev<enter>", {noremap=false})
nvim_set_keymap("n", "tl", ":bnext<enter>", {noremap=false})
nvim_set_keymap("n", "td", ":bdelete<enter>", {noremap=false})
nvim_set_keymap("n", "QQ", ":q!<enter>", {noremap=false})
nvim_set_keymap("n", "WW", ":w!<enter>", {noremap=false})
nvim_set_keymap("n", "E", "$", {noremap=false})
nvim_set_keymap("n", "B", "^", {noremap=false})
nvim_set_keymap("n", "TT", ":TransparentToggle<CR>", {noremap=true})
nvim_set_keymap("n", "st", ":TodoTelescope<CR>", {noremap=true})
nvim_set_keymap("n", "ss", ":noh<CR>", {noremap=true})
nvim_set_keymap("n", "<C-W>,", ":vertical resize -10<CR>", {noremap=true})
nvim_set_keymap("n", "<C-W>.", ":vertical resize +10<CR>", {noremap=true})
nvim_set_keymap('n', '<space><space>', "<cmd>set nohlsearch<CR>")


nvim_set_keymap({ 'n', 'v' }, '<Space>', '<Nop>', { silent = true })

nvim_set_keymap('n', 'k', "v:count == 0 ? 'gk' : 'k'", { expr = true, silent = true })
nvim_set_keymap('n', 'j', "v:count == 0 ? 'gj' : 'j'", { expr = true, silent = true })

        vim.g.mapleader = " "
        nvim_set_keymap("n", "<leader>pv", vim.cmd.Ex)

        nvim_set_keymap("v", "J", ":m '>+1<CR>gv=gv")
        nvim_set_keymap("v", "K", ":m '<-2<CR>gv=gv")

        nvim_set_keymap("n", "J", "mzJ`z")
        nvim_set_keymap("n", "<C-d>", "<C-d>zz")
        nvim_set_keymap("n", "<C-u>", "<C-u>zz")
        nvim_set_keymap("n", "n", "nzzzv")
        nvim_set_keymap("n", "N", "Nzzzv")

        nvim_set_keymap("n", "<leader>vwm", function()
            require("vim-with-me").StartVimWithMe()
            end)
        nvim_set_keymap("n", "<leader>svwm", function()
            require("vim-with-me").StopVimWithMe()
            end)

            -- greatest remap ever
        nvim_set_keymap("x", "<leader>p", [["_dP]])

            -- next greatest remap ever : asbjornHaland
        nvim_set_keymap({"n", "v"}, "<leader>y", [["+y]])
        nvim_set_keymap("n", "<leader>Y", [["+Y]])

        nvim_set_keymap({"n", "v"}, "<leader>d", [["_d]])

            -- This is going to get me cancelled
        nvim_set_keymap("i", "<C-c>", "<Esc>")

        nvim_set_keymap("n", "Q", "<nop>")
        nvim_set_keymap("n", "<C-f>", "<cmd>silent !tmux neww tmux-sessionizer<CR>")
        nvim_set_keymap("n", "<leader>f", vim.lsp.buf.format)

        nvim_set_keymap("n", "<C-k>", "<cmd>cnext<CR>zz")
        nvim_set_keymap("n", "<C-j>", "<cmd>cprev<CR>zz")
        nvim_set_keymap("n", "<leader>k", "<cmd>lnext<CR>zz")
        nvim_set_keymap("n", "<leader>j", "<cmd>lprev<CR>zz")

        nvim_set_keymap("n", "<leader>s", [[:%s/\<<C-r><C-w>\>/<C-r><C-w>/gI<Left><Left><Left>]])
        nvim_set_keymap("n", "<leader>x", "<cmd>!chmod +x %<CR>", { silent = true })

        nvim_set_keymap("n", "<leader>vpp", "<cmd>e ~/.dotfiles/nvim/.config/nvim/lua/theprimeagen/packer.lua<CR>");
        nvim_set_keymap("n", "<leader>mr", "<cmd>CellularAutomaton make_it_rain<CR>");

        nvim_set_keymap("n", "<leader><leader>", function()
            vim.cmd("so")
            end)

         */

    }

    @Test
    public void getKeyMappingCommand() {
        List<KeyStroke> lkeys = new ArrayList<>();
        lkeys.add(new KeyStroke('i', false, false, false));
        //VKeyMap keyMap = VKeyMaps.getKeyMappingCommand(VimMode.INSERT, lkeys);
        //System.out.println(keyMap);
    }
}