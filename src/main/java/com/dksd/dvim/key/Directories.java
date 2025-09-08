package com.dksd.dvim.key;

import com.dksd.dvim.history.Harpoons;

import java.nio.file.Path;


public class Directories {
    public static void goUp(Harpoons harpoons) {
        /*
        String dir = var_dirs.getVar(CURRENT_DIR);
                    String odir = dir;
                    dir = dir.substring(0, dir.lastIndexOf("/"));
                    var_dirs.addVar(CURRENT_DIR, dir);
                    System.out.println("Went up a directory: " + dir + " from " + odir);
         */
    }

    public static Path getCurrentPath(Harpoons harpoons) {
        //TODO
        return Path.of("todo current");
        //return Paths.get(.getVar(CURRENT_DIR));
    }
}
