package com.dksd.dvim.organize;

import com.dksd.dvim.utils.LinesHelper;
import com.dksd.dvim.view.Line;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TodoHelperTest {

    private final TodoHelper helper = new TodoHelper();

    @Test
    void moveTodoUpVim_simpleTopLevel() {
        List<String> bufferStr = Arrays.asList(
                "Task A",
                "  Task B",
                "    Task C",
                "Task 1",
                "  Task 2",
                "    Task 3"
        );
        List<Line> buffer = LinesHelper.convertToLines(bufferStr);
        helper.moveTodoDownVim(1, buffer);
        for (Line line : buffer) {
            System.out.println(line);
        }
        helper.moveTodoUpVim(3, buffer);
        for (Line line : buffer) {
            System.out.println(line);
        }
    }

}
