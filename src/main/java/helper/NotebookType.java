package helper;

public enum NotebookType {
    SOLUTION(
            new Pair("TASK_HEADER", false),
            new Pair("TASK", true),
            new Pair("EXECUTION_TASK", false),
            new Pair("SOLUTION_HEADER", false),
            new Pair("SOLUTION", true),
            new Pair("EXECUTION_SOLUTION", false),
            new Pair("GRADING_SCHEME", true)
    ),

    HANDIN(
            new Pair("SUBMISSION_HEADER", false),
            new Pair("STUDENT_SOLUTION", true),
            new Pair("EXECUTION_STUDENT_SOLUTION", false),
            new Pair("SCORE", true),
            new Pair("COMMENT", true)
    );

    final Pair[] pairs;

    NotebookType(Pair... types) {
        this.pairs = types;
    }

    public Pair[] getPairs() {
        return pairs;
    }

    public record Pair(String name, boolean mandatory) {
    }

}
