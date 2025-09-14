package com.dksd.dvim;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TodoHelperTest {

    private final TodoHelper helper = new TodoHelper();

    @Test
    void moveTodoUpVim_simpleTopLevel() {
        List<String> buffer = Arrays.asList(
                "- Task A",
                "- Task B",
                "- Task C"
        );

        // Move "Task B" (index 1) up → should move above Task A
        String cmd = helper.moveTodoUpVim(1, buffer);
        assertEquals(":2,2m 0", cmd);
    }

    @Test
    void moveTodoDownVim_simpleTopLevel() {
        List<String> buffer = Arrays.asList(
                "- Task A",
                "- Task B",
                "- Task C"
        );

        // Move "Task B" (index 1) down → should move after Task C
        String cmd = helper.moveTodoDownVim(1, buffer);
        assertEquals(":2,2m 3", cmd);
    }

    @Test
    void moveTodoUpVim_nestedBlock() {
        List<String> buffer = Arrays.asList(
                "- Parent",
                "  - Child A",
                "  - Child B",
                "- Sibling"
        );

        // Move the "Parent + children" block (index 0) up → already top, noop
        String cmd = helper.moveTodoUpVim(0, buffer);
        assertEquals(":noop", cmd);

        // Move "Sibling" (index 3) up → should move above Parent block
        String cmd2 = helper.moveTodoUpVim(3, buffer);
        // Sibling is at line 4 (vim 1-based), Parent block spans 1–3
        assertEquals(":4,4m 0", cmd2);
    }

    @Test
    void moveTodoDownVim_nestedBlock() {
        List<String> buffer = Arrays.asList(
                "- Parent",
                "  - Child A",
                "  - Child B",
                "- Sibling"
        );

        // Move "Parent + children" block (index 0) down → should move below Sibling
        String cmd = helper.moveTodoDownVim(0, buffer);
        // Parent block is lines 1–3, sibling is line 4
        assertEquals(":1,3m 4", cmd);

        // Move "Sibling" (index 3) down → already last, noop
        String cmd2 = helper.moveTodoDownVim(3, buffer);
        assertEquals(":noop", cmd2);
    }

    @Test
    void moveTodoUpVim_firstTodoIsNoop() {
        List<String> buffer = Arrays.asList(
                "- Task A",
                "- Task B"
        );

        // First todo cannot move up
        String cmd = helper.moveTodoUpVim(0, buffer);
        assertEquals(":noop", cmd);
    }

    @Test
    void moveTodoDownVim_lastTodoIsNoop() {
        List<String> buffer = Arrays.asList(
                "- Task A",
                "- Task B"
        );

        // Last todo cannot move down
        String cmd = helper.moveTodoDownVim(1, buffer);
        assertEquals(":noop", cmd);
    }
}
