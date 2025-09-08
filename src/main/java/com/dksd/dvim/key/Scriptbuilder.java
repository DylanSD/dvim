package com.dksd.dvim.key;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Scriptbuilder {

    List<String> scriptLines = new ArrayList<>();

    public Scriptbuilder withLine(String line) {
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
}
