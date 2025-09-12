package com.dksd.dvim.utils;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.history.Harpoons;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PathHelper {

    private static Harpoons harpoons;

    public PathHelper(Harpoons harpoons) {
        PathHelper.harpoons = harpoons;
    }

    public static Stream<String> streamPathToStr(Path dir, Predicate<Path> filter) {
        return streamPath(dir, filter, 1000L).map(Path::toString);
    }

    public static Stream<Path> streamPath(Path dir, Predicate<Path> filter) {
        return streamPath(dir, filter, 1000L);
    }

    public static Stream<Path> streamPath(Path dir, Predicate<Path> filter, Long limit) {
        try {
            return Files.walk(dir)
                    .filter(filter)
                    .filter(file -> !file.toString().contains("/build/"))
                    .filter(file -> !file.toString().contains("/target/"))
                    .limit((limit != null) ? limit : 1000L);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getCurrentDir() {
        return Path.of(System.getProperty("user.dir"));
    }

    //Not sure when you would want to call this?
    public static List<Buf> loadFilesIntoBufs(Path dir, Predicate<Path> filter) {
        return streamPath(dir, filter, null)
                .map(path -> {
                    try {
                        Buf buf = new Buf(path.toString(), path.toString(), harpoons.getBuffers().getNextInt(), null, null, false);
                        buf.setLinesListStr(Files.readAllLines(path, StandardCharsets.UTF_8));
                        return buf;
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + path, e);
                    }
                })
                .collect(Collectors.toList());
    }
}
