package com.dksd.dvim;

import java.util.List;
import java.util.Objects;

public class TodoHelper {

    private TodoCursor findTodo(int todoPos, List<String> buffer) {
        if (todoPos < 0 || (todoPos == 0 && todoPos + 1 == buffer.size())) {
            return new TodoCursor(0, 0);
        }
        boolean flag = false;
        int minIndent = getMinIndent(todoPos, buffer);
        int start = todoPos;
        int end = todoPos;
        int bufSize = buffer.size();
        boolean lockStart = countLeadingSpaces(buffer.get(todoPos)) == minIndent;
        boolean lockEnd = todoPos + 1 == bufSize
                || countLeadingSpaces(buffer.get(todoPos + 1)) == minIndent;

        while (flag == false) {
            flag = true;
            int startCache = start - 1;
            if (startCache >= 0) {
                int startIndent = countLeadingSpaces(buffer.get(startCache));
                if (!lockStart && startIndent >= minIndent && countLeadingSpaces(buffer.get(start)) > minIndent) {
                    start--;
                    flag = false;
                }
            }
            int endCache = end + 1;
            if (endCache < bufSize) {
                int endIndent = countLeadingSpaces(buffer.get(endCache));
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

    private int getMinIndent(int pos, List<String> todo) {
        int minIndent = Integer.MAX_VALUE;
        for (int i = pos; i >= 0; i--) {
            if (countLeadingSpaces(todo.get(i)) < minIndent) {
                minIndent = countLeadingSpaces(todo.get(i));
                if (minIndent == 0) {
                    return 0;
                }
            }
        }
        return minIndent;
    }

    public String moveTodoUpVim(int pos, List<String> buffer) {
        TodoCursor entry = findTodo(pos, buffer);
        TodoCursor entryAbove = findTodo(entry.start - 1, buffer);
        if (entry.equals(entryAbove)) {
            return ":noop";
        }
        //Everything is shifted by 1 because vim buffer starts at 1
        return ":" + (entry.start + 1) + "," + (entry.end + 1) + "m" + " " + (entryAbove.start);
    }

    public String moveTodoDownVim(int pos, List<String> buffer) {
        TodoCursor entry = findTodo(pos, buffer);
        TodoCursor entryBelow = findTodo(entry.end + 1, buffer);
        if (entry.equals(entryBelow)) {
            return ":noop";
        }
        //Everything is shifted by 1 because vim buffer starts at 1
        return ":" + (entry.start+1) + "," + (entry.end+1) + "m" + " " + (entryBelow.end+1);
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
