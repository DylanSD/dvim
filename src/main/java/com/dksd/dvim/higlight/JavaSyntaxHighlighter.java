package com.dksd.dvim.higlight;

import com.catppuccin.Palette;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JavaSyntaxHighlighter {

    // --- Regex patterns as named groups ---
    private static final String KEYWORDS =
            "\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|" +
                    "default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|" +
                    "import|instanceof|int|interface|long|native|new|null|package|private|protected|" +
                    "public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|" +
                    "transient|try|void|volatile|while)\\b";

    // Strings: normal + text blocks (Java 15+)
    private static final String STRING =
            "\"([^\"\\\\]|\\\\.)*\""          // double-quoted strings
                    + "|'''([\\s\\S]*?)'''"             // triple single-quoted (if you want symmetry with text blocks)
                    + "|'([^'\\\\]|\\\\.)*'";           // single-quoted strings/chars


    // Comments: single + multi-line
    private static final String COMMENT = "//.*" + "|/\\*[\\s\\S]*?\\*/";

    // Numbers: int, float, scientific, hex, binary, underscores
    private static final String NUMBER =
            "\\b\\d[\\d_]*(\\.[\\d_]+)?([eE][+-]?\\d+)?\\b" +  // decimal + float + scientific
                    "|0[xX][0-9a-fA-F_]+" +                           // hex
                    "|0[bB][01_]+";                                   // binary

    // Composite regex with named groups
    private static final Pattern PATTERN = Pattern.compile(
            "(?<COMMENT>" + COMMENT + ")"
                    + "|(?<STRING>" + STRING + ")"
                    + "|(?<KEYWORD>" + KEYWORDS + ")"
                    + "|(?<NUMBER>" + NUMBER + ")"
    );


    // Palette-based colors
    private final TextColor keywordColor = rgbFromPalette(Palette.MOCHA.getBlue().getRGBComponents());
    private final TextColor stringColor  = rgbFromPalette(Palette.MOCHA.getGreen().getRGBComponents());
    private final TextColor commentColor = rgbFromPalette(Palette.MOCHA.getYellow().getRGBComponents());
    private final TextColor numberColor  = rgbFromPalette(Palette.MOCHA.getYellow().getRGBComponents());
    private final TextColor baseColor    = rgbFromPalette(Palette.MOCHA.getText().getRGBComponents());

    private TextColor rgbFromPalette(int[] rgb) {
        return new TextColor.RGB(rgb[0], rgb[1], rgb[2]);
    }

    /**
     * Draws syntax-highlighted code to a Lanterna TextGraphics
     */
    public void drawHighlightedCode(TextGraphics tg, String code, int startRow, int startCol, LinkedBlockingQueue<Future<?>> futures) {

        // Could re-enable async processing later with executor + futures

        int row = startRow;
        int colBase = startCol;

        // Use one matcher for the whole code (so multiline comments work)
        Matcher matcher = PATTERN.matcher(code);
        int lastIndex = 0;
        int col = startCol;

        for (int i = 0; i < code.length(); i++) {
            // Break at newlines and reset col
            if (code.charAt(i) == '\n') {
                // Print remainder before newline if not already consumed
                if (lastIndex < i) {
                    String remainder = code.substring(lastIndex, i);
                    tg.setForegroundColor(baseColor);
                    tg.putString(col, row, remainder);
                }
                row++;
                col = colBase;
                lastIndex = i + 1;
            }
        }

        // Now handle tokens
        matcher.reset();
        row = startRow;
        col = startCol;
        lastIndex = 0;

        while (matcher.find()) {
            String before = code.substring(lastIndex, matcher.start());
            col = drawString(tg, before, row, col, baseColor);

            TextColor color;
            if (matcher.group("COMMENT") != null) {
                color = commentColor;
            } else if (matcher.group("STRING") != null) {
                color = stringColor;
            } else if (matcher.group("KEYWORD") != null) {
                color = keywordColor;
            } else if (matcher.group("NUMBER") != null) {
                color = numberColor;
            } else {
                color = baseColor;
            }

            col = drawString(tg, matcher.group(), row, col, color);
            lastIndex = matcher.end();

            // Track newlines inside the match (important for block comments, text blocks)
            int nlIndex = matcher.group().indexOf('\n');
            if (nlIndex != -1) {
                String[] parts = matcher.group().split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        row++;
                        col = colBase;
                    }
                    col = drawString(tg, parts[i], row, col, color);
                }
            }
        }

        // Print remainder after last match
        if (lastIndex < code.length()) {
            drawString(tg, code.substring(lastIndex), row, col, baseColor);
        }

        tg.setForegroundColor(baseColor);
    }

    private int drawString(TextGraphics tg, String text, int row, int col, TextColor color) {
        tg.setForegroundColor(color);
        tg.putString(col, row, text);
        return col + text.length();
    }
}
