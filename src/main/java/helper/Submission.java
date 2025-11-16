package helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Submission {

    private HashMap<String, JSONObject> cells;

    private int cellStartIndex;

    public Submission(HashMap<String, JSONObject> cells, int cellStartIndex) {
        this.cells = cells;
        this.cellStartIndex = cellStartIndex;
    }

    public HashMap<String, JSONObject> getCells() {
        return cells;
    }

    public int getCellStartIndex() {
        return cellStartIndex;
    }

    public void setCellStartIndex(int cellStartIndex) {
        this.cellStartIndex = cellStartIndex;
    }

    public static boolean isSubmissionCell(JSONObject cell) {
        JSONArray source = cell.optJSONArray("source");
        if (source == null) {
            return false;
        }

        for (int i = 0; i < source.length(); i++) {
            String line = source.optString(i);
            if (line.matches(".*## Submission.*")) {
                return true;
            }
        }
        return false;
    }

    // returns the number of cells that have to be skipped (or are imported for this submission)

    public static Submission parseSubmission(JSONObject noteBook, int cellIndex) {

        JSONArray allCells = noteBook.getJSONArray("cells");
// figure out a better way
        JSONObject indicatorOfSubmission = allCells.getJSONObject(cellIndex + 0);
        JSONObject submissionOfStudent = allCells.getJSONObject(cellIndex + 1);
        JSONObject submissionLauncher = allCells.getJSONObject(cellIndex + 2);
        JSONObject scoreField = allCells.getJSONObject(cellIndex + 3);
        JSONObject commentField = allCells.getJSONObject(cellIndex + 4);

        HashMap<String, JSONObject> submissionCells = new HashMap<>();

        submissionCells.put("indicator", indicatorOfSubmission);
        submissionCells.put("submission", submissionOfStudent);
        submissionCells.put("launcher", submissionLauncher);
        submissionCells.put("score", scoreField);
        submissionCells.put("comment", commentField);


        return new Submission(submissionCells, cellIndex);
    }

}
