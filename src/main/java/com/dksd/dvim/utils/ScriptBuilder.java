package com.dksd.dvim.utils;

import org.buildobjects.process.ProcBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ScriptBuilder {

    List<String> scriptLines = new ArrayList<>();

    public ScriptBuilder withLine(String line) {
        scriptLines.add(line);
        return this;
    }

    public Path build(String filename) {
        try {
            /*var script = """
                 #!/bin/bash
                 echo "hello, world - via Java Program"
                """;*/

            Path path = Files.write(Path.of(filename), scriptLines);
            System.out.println("File [hello.sh] has been written");

        /* Set the File permission with the meaningful methods.
        setPosixFilePermission method takes two arguments:
         1. The path to the target file
         2. A `java.util.Set` that takes the list of permissions,
            each denoted by a predefinex constant defined
            in the `PosixFilePermission` class.
            `OWNER_XXX` for Owners,
            `GROUP_XXX` for the Group,
            and `OTHERS_XXX` for Others,
            where the XXX can be of any `READ`, `WRITE`, `EXECUTE`.
        */
            Files.setPosixFilePermissions(path,
                Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE));
            return path;
        } catch (Exception ep) {

        }
        return null;
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

    public List<String> exec(String cmd, String... args) {
        String output = ProcBuilder.run(cmd, args);
        String[] lines = output.split("\n");
        return Arrays.asList(lines);
    }
}
