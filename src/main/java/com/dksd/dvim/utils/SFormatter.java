package com.dksd.dvim.utils;

import com.googlecode.lanterna.input.KeyStroke;

import java.util.List;

public class SFormatter {
    public static String format(String str, String vimMode) {
        String ans = str.replace("{{status}}", vimMode);
        return ans;
    }

    public static String format(String str, String vimMode, String keyStrokes) {
        String ans = str.replace("{{status}}", vimMode);
        ans = ans.replace("{{keys}}", keyStrokes);
        return ans;
    }
}
