package com.dksd.dvim.organize;

import com.dksd.dvim.view.Line;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TodoHelper {

    private TodoCursor findTodo(int row, List<Line> buffer) {
        if (row < 0 || (row == 0 && row + 1 == buffer.size())) {
            return null;
        }
        boolean flag = false;
        int minIndent = getMinIndent(row, buffer);
        int start = row;
        int end = row;
        int bufSize = buffer.size();
        boolean lockStart = countLeadingSpaces(buffer.get(row).getContent()) == minIndent;
        boolean lockEnd = row + 1 == bufSize
                || countLeadingSpaces(buffer.get(row + 1).getContent()) == minIndent;

        while (flag == false) {
            flag = true;
            int startCache = start - 1;
            if (startCache >= 0) {
                int startIndent = countLeadingSpaces(buffer.get(startCache).getContent());
                if (!lockStart && startIndent >= minIndent && countLeadingSpaces(buffer.get(start).getContent()) > minIndent) {
                    start--;
                    flag = false;
                }
            }
            int endCache = end + 1;
            if (endCache < bufSize) {
                int endIndent = countLeadingSpaces(buffer.get(endCache).getContent());
                if (!lockEnd && endIndent > minIndent) {
                    end++;
                    flag = false;
                }
            }
        }
        return new TodoCursor(start, end);
    }

    private int countLeadingSpaces(String entry) {
        int spaceCount = 0;
        for (char c : entry.toCharArray()) {
            if (c == ' ') {
                spaceCount++;
            } else {
                break;
            }
        }
        return spaceCount;
    }

    private int getMinIndent(int row, List<Line> todo) {
        int minIndent = Integer.MAX_VALUE;
        for (int i = row; i >= 0; i--) {
            if (countLeadingSpaces(todo.get(i).getContent()) < minIndent) {
                minIndent = countLeadingSpaces(todo.get(i).getContent());
                if (minIndent == 0) {
                    return 0;
                }
            }
        }
        return minIndent;
    }

    public void moveTodoUpVim(int row, List<Line> buffer) {
        TodoCursor entry = findTodo(row, buffer);
        TodoCursor entryAbove = findTodo(entry.start - 1, buffer);
        swapTodos(buffer, entry, entryAbove);
    }

    public void moveTodoDownVim(int pos, List<Line> buffer) {
        TodoCursor entry = findTodo(pos, buffer);
        TodoCursor entryBelow = findTodo(entry.end + 1, buffer);
        swapTodos(buffer, entryBelow, entry);
    }

    private static void swapTodos(List<Line> buffer, TodoCursor entry, TodoCursor entryAbove) {
        if (entry == null || entryAbove == null) {
            return;
        }
        List<Line> lines = new ArrayList<>();
        int i = entry.end - entry.start;
        while (i >=0) {
            lines.add(buffer.remove(entry.start));
            i--;
        }
        buffer.addAll(entryAbove.start, lines);
        for (int j = 0; j < buffer.size(); j++) {
            buffer.get(j).setLineNumber(j);
        }
    }

    public class TodoCursor {

        int start;
        int end = -1;

        public TodoCursor(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TodoCursor cursor = (TodoCursor) o;
            return start == cursor.start && end == cursor.end;
        }

        @Override
        public int hashCode() {
            return Objects.hash(start, end);
        }
    }
}
