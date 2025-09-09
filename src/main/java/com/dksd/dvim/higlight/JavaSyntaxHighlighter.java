package com.dksd.dvim.higlight;

import com.catppuccin.Palette;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSyntaxHighlighter {

    // Regex patterns for tokens
    private final String KEYWORDS = "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|null|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b";
    private final String STRING = "\"([^\"\\\\]|\\\\.)*\"";
    private final String COMMENT = "//.*|/\\*(.|\\R)*?\\*/";
    private final String NUMBER = "\\b\\d+(\\.\\d+)?\\b";

    // Palette-based colors
    private final TextColor keywordColor = rgbFromPalette(Palette.MOCHA.getBlue().getRGBComponents());
    private final TextColor stringColor  = rgbFromPalette(Palette.MOCHA.getGreen().getRGBComponents());
    private final TextColor commentColor = rgbFromPalette(Palette.MOCHA.getYellow().getRGBComponents());
    private final TextColor numberColor  = rgbFromPalette(Palette.MOCHA.getCrust().getRGBComponents());
    private final TextColor baseColor    = rgbFromPalette(Palette.MOCHA.getMantle().getRGBComponents());

    // Composite regex for all patterns
    private Pattern pattern = Pattern.compile(COMMENT + "|" + STRING + "|" + KEYWORDS + "|" + NUMBER);

    private TextColor rgbFromPalette(int[] rgb) {
        return new TextColor.RGB(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Draws syntax-highlighted code to a Lanterna TextGraphics
     */
    public void drawHighlightedCode(TextGraphics tg, String code, int startRow, int startCol, LinkedBlockingQueue<Future<?>> futures) {

        //Later add this back somehow
        //futures.add(executor.submit(() -> {
        //}));

        // Tokenize by regex application order: comments → strings → keywords → numbers
        String[] lines = code.split("\n");
        int row = startRow;

        for (String line : lines) {
            int col = startCol;
            int lastIndex = 0;

            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                // Print text before the match in base color
                String before = line.substring(lastIndex, matcher.start());
                tg.setForegroundColor(baseColor);
                tg.putString(col, row, before);
                col += before.length();

                // Print the match with its color
                String match = matcher.group();
                if (match.matches(COMMENT)) {
                    tg.setForegroundColor(commentColor);
                } else if (match.matches(STRING)) {
                    tg.setForegroundColor(stringColor);
                } else if (match.matches(KEYWORDS)) {
                    tg.setForegroundColor(keywordColor);
                } else if (match.matches(NUMBER)) {
                    tg.setForegroundColor(numberColor);
                } else {
                    tg.setForegroundColor(baseColor);
                }
                tg.putString(col, row, match);
                col += match.length();

                lastIndex = matcher.end();
            }

            // Remainder of the line
            if (lastIndex < line.length()) {
                String remainder = line.substring(lastIndex);
                tg.setForegroundColor(baseColor);
                tg.putString(col, row, remainder);
            }

            row++;
        }
        tg.setForegroundColor(baseColor);
    }
}
