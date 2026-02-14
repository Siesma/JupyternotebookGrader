package helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class NotebookCell {

    private final ArrayList<JSONObject> cells;

    private final int cellStartIndex;
    private final NotebookType type;

    public NotebookCell(ArrayList<JSONObject> cells, int cellStartIndex, NotebookType type) {
        this.cells = cells;
        this.cellStartIndex = cellStartIndex;
        this.type = type;
    }

    public static NotebookCell parseCell(JSONObject noteBook, int[] bounds, int providedID, NotebookType type) {
        int notebookCellID = bounds[0] + bounds[1] * providedID;
        ArrayList<JSONObject> cells = new ArrayList<>();
        JSONArray allCells = noteBook.getJSONArray("cells");
        for (int i = 0; i < bounds[1]; i++) {
            cells.add(allCells.getJSONObject(notebookCellID + i));
        }
        return new NotebookCell(cells, notebookCellID, type);
    }

    public ArrayList<JSONObject> getCells() {
        return cells;
    }

    public int getCellStartIndex() {
        return cellStartIndex;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (JSONObject cell : cells) {
            builder.append(cell.toString()).append("\n");
        }
        return builder.toString();
    }


    public int determineTaskCellIndex() {
        if (type != NotebookType.SOLUTION) {
            return -1;
        }
        int bestIndex = -1;
        int bestScore = 0;

        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);

            // Only code cells
            if (!"code".equals(cell.getString("cell_type"))) continue;

            JSONArray sourceArray = cell.optJSONArray("source");
            if (sourceArray == null || sourceArray.length() == 0) continue;

            String src = normalizedSource(sourceArray);

            // Score: number of meaningful lines (ignore empty lines and comments)
            String[] lines = src.split("\\r?\\n");
            int meaningfulLines = 0;
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) continue;
                meaningfulLines++;
            }

            // Skip cells with no meaningful lines
            if (meaningfulLines == 0) continue;

            // Choose the cell with the most meaningful lines
            if (meaningfulLines > bestScore) {
                bestScore = meaningfulLines;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    public int determineSolutionCellIndex() {
        if (type != NotebookType.SOLUTION) {
            return -1;
        }

        int bestIndex = -1;
        int bestLength = 0;

        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);

            if (!isCodeCell(cell)) continue;
            if (!hasExecutableCode(cell)) continue;
            if (i == determineGradingSchemeCellIndex()) {
                continue;
            }
            String src = normalizedSource(cell.getJSONArray("source"));
            int length = src.length();
            if (length > bestLength) {
                bestLength = length;
                bestIndex = i;
            }
        }

        return bestIndex;
    }


    public int determineStudentSolutionCellIndex() {
        if (type != NotebookType.HANDIN) {
            return -1;
        }

        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);

            if (!isCodeCell(cell)) continue;
            if (!hasNonEmptyMetadata(cell)) continue;
            if (!hasExecutableCode(cell)) continue;

            JSONObject meta = cell.getJSONObject("metadata");

            boolean studentOwned =
                    meta.has("student_id") ||
                            meta.has("exercise_id");

            if (!studentOwned) continue;

            String src = normalizedSource(cell.getJSONArray("source"));
            if (src.matches("^[A-Za-z0-9_\\.]+\\s*\\(.*\\)\\s*;?$")) {
                continue;
            }

            return i;
        }

        return -1;
    }


    public int determineGradingSchemeCellIndex() {
        if (type != NotebookType.SOLUTION) {
            System.out.println("Notebook is not solution");
            return -1;
        }
        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);
            if (!hasSource(cell)) {
                continue;
            }
            JSONArray sourceData = cell.getJSONArray("source");
            int bulletCount = 0;
            for (int j = 0; j < sourceData.length(); j++) {
                if (!isBulletPoint(sourceData.getString(j))) {
                    continue;
                }
                bulletCount++;
            }
            if (bulletCount >= 2) {
                return i;
            }
        }
        System.out.println("No grading scheme found");
        return -1;
    }

    public int determineScoreCellIndex() {
        if (type != NotebookType.HANDIN) {
            return -1;
        }
        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);
            if (!isMarkdownCell(cell)) {
                continue;
            }
            if (!hasNonEmptyMetadata(cell)) {
                continue;
            }
            JSONObject metadata = cell.getJSONObject("metadata");
            if (metadata.has("score_cell")) {
                return i;
            }
        }
        return -1;
    }

    public int determineCommentCellIndex() {
        if (type != NotebookType.HANDIN) {
            return -1;
        }
        for (int i = 0; i < cells.size(); i++) {
            JSONObject cell = cells.get(i);
            if (!isMarkdownCell(cell)) {
                continue;
            }
            if (!hasNonEmptyMetadata(cell)) {
                continue;
            }
            JSONObject metadata = cell.getJSONObject("metadata");
            if (metadata.has("comment_cell")) {
                return i;
            }
        }
        return -1;
    }

    private boolean isBulletPoint(String soruceData) {
        return soruceData.matches(".*\\dP.*\n");
    }

    private boolean hasNonEmptyMetadata(JSONObject cell) {
        JSONObject meta = cell.optJSONObject("metadata");
        return meta != null && !meta.isEmpty();
    }

    private boolean hasSource(JSONObject cell) {
        JSONArray src = cell.optJSONArray("source");
        return src != null && !src.isEmpty();
    }

    private boolean isMarkdownCell(JSONObject cell) {
        return "markdown".equals(cell.optString("cell_type"));
    }

    private boolean isCodeCell(JSONObject cell) {
        return "code".equals(cell.optString("cell_type"));
    }

    private String normalizedSource(JSONArray source) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < source.length(); i++) {
            code.append(source.getString(i));
        }

        return code.toString()
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)//.*$", "")
                .replaceAll("(?m)#.*$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean hasExecutableCode(JSONObject cell) {
        if (!hasSource(cell)) return false;
        String normalized = normalizedSource(cell.getJSONArray("source"));
        return !normalized.isEmpty();
    }

    public void printRoleMappingDetected() {
        NotebookType.Pair[] roles = type.getPairs();
        System.out.printf("Role mapping (of type %s) with a starting index of %d%n", type, cellStartIndex);

        for (NotebookType.Pair role : roles) {
            int detectedIndex = detectRoleCellIndex(role.name());

            if (detectedIndex >= 0) {
                System.out.printf(
                        " - %-30s\t offset index %d %s\t %s\n",
                        role.name(),
                        detectedIndex,
                        role.mandatory() ? "(mandatory)" : "(optional)",
                        getCells().get(detectedIndex).toString()
                );
            } else {
                System.out.printf(
                        " - %-30s\t MISSING %s%n",
                        role.name(),
                        role.mandatory() ? "(mandatory)" : "(optional)"
                );
            }
        }
        System.out.println();
    }

    private HashMap<String, Integer> getCellMapping() {
        HashMap<String, Integer> map = new HashMap<>();
        NotebookType.Pair[] roles = type.getPairs();
        for (NotebookType.Pair role : roles) {
            map.put(role.name(), detectRoleCellIndex(role.name()));
        }
        return map;
    }

    private int detectRoleCellIndex(String roleName) {
        switch (roleName) {
            /*
            This code is not yet correct, but honestly - they are irrelevant fields anyway
            case "TASK_HEADER", "SOLUTION_HEADER", "SUBMISSION_HEADER" -> {
                if (!cells.isEmpty() && isMarkdownCell(cells.get(0))) return 0;
            }
            case "EXECUTION_TASK", "EXECUTION_SOLUTION", "EXECUTION_STUDENT_SOLUTION" -> {
                if (!cells.isEmpty() && isCodeCell(cells.get(0))) return 0;
            }
            */

            case "TASK" -> {
                int idx = determineTaskCellIndex();
                if (idx != -1) return idx;
            }
            case "SOLUTION" -> {
                int idx = determineSolutionCellIndex();
                if (idx != -1) return idx;
            }
            case "STUDENT_SOLUTION" -> {
                int idx = determineStudentSolutionCellIndex();
                if (idx != -1) return idx;
            }
            case "SCORE" -> {
                int idx = determineScoreCellIndex();
                if (idx != -1) return idx;
            }
            case "COMMENT" -> {
                int idx = determineCommentCellIndex();
                if (idx != -1) return idx;
            }
            case "GRADING_SCHEME" -> {
                int idx = determineGradingSchemeCellIndex();
                if (idx != -1) return idx;
            }
        }
        return -1;
    }
}
