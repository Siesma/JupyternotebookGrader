package helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Solution {

    private HashMap<String, JSONObject> cells;

    private int cellStartIndex;

    public Solution(HashMap<String, JSONObject> cells, int cellStartIndex) {
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

    public static boolean isSolutionCell(JSONObject cell) {
        JSONArray source = cell.optJSONArray("source");
        if (source == null) {
            return false;
        }

        for (int i = 0; i < source.length(); i++) {
            String line = source.optString(i);
            if (line.matches(".*## Lösung.*")) {
                return true;
            }
        }
        return false;
    }

    // returns the number of cells that have to be skipped (or are imported for this submission)


    public static Solution parseSolution(JSONObject noteBook, int cellIndex) {

        JSONArray allCells = noteBook.getJSONArray("cells");
// figure out a better way
        JSONObject solutionIndicator = allCells.getJSONObject(cellIndex + 0);
        JSONObject solutionCodeField = allCells.getJSONObject(cellIndex + 1);
        JSONObject solutionRunField = allCells.getJSONObject(cellIndex + 2);
        JSONObject solutionSchema = allCells.getJSONObject(cellIndex + 3);

        HashMap<String, JSONObject> solutionCells = new HashMap<>();

        solutionCells.put("solutionIndicator", solutionIndicator);
        solutionCells.put("solutionCodeField", solutionCodeField);
        solutionCells.put("solutionRunField", solutionRunField);
        solutionCells.put("solutionSchema", solutionSchema);

        return new Solution(solutionCells, cellIndex);
    }
}
