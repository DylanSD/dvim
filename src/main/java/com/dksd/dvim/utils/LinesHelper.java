package com.dksd.dvim.utils;

import com.dksd.dvim.view.Line;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LinesHelper {


    public static <T> List<Line> convertToLines(List<T> lines) {
        return IntStream.range(0, lines.size())
                .mapToObj(i -> new Line(i, lines.get(i).toString(), null))
                .collect(Collectors.toList());
    }
}
