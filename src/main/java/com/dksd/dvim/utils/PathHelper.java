package com.dksd.dvim.utils;

import com.dksd.dvim.buffer.Buf;
import com.dksd.dvim.event.EventType;
import com.dksd.dvim.event.VimEvent;
import com.dksd.dvim.history.Harpoons;
import com.dksd.dvim.view.Line;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
                    .filter((filter != null) ? filter : f -> true)
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

    public static List<Path> loadPathsIntoHarpoon(String directory, Predicate<Path> filter) {
        return streamPath(Path.of(directory), filter, null).toList();
    }

    public static List<Line> readFile(Path filename) {
        try {
            return Line.convert(Files.readAllLines(filename, StandardCharsets.UTF_8));
            //eventQueue.add(new VimEvent(bufNo, EventType.BUF_CHANGE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(String filename, List<Line> lines) {
        try {
            writeFile(filename.split(""), lines);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeFile(String[] filenameOuts, List<Line> lines) throws IOException {
        for (String filenameOut : filenameOuts) {
            try (
                    PrintWriter printWriter = new PrintWriter(
                            new BufferedWriter(
                                    new OutputStreamWriter(
                                            Files.newOutputStream(Paths.get(filenameOut)),
                                            StandardCharsets.UTF_8
                                    )
                            )
                    )
            ) {
                for (Line line : lines) {
                    printWriter.println(line.getContent());
                }
            }
            System.out.println("Wrote " + filenameOut);
        }
    }

}
