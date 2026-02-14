package helper;

import org.json.JSONObject;

import java.util.ArrayList;

public class Notebook {
    private final ArrayList<NotebookCell> cells;
    private final NotebookType type;

    private JSONObject jsonRepresentation;

    public Notebook(String pathToNotebook, NotebookType type) {
        cells = new ArrayList<>();
        this.type = type;
        this.loadNotebook(pathToNotebook);
    }

    private void loadNotebook(String pathToNotebook) {
        JSONImporter importer = new JSONImporter();
        JSONObject notebook = importer.importJSONObj(pathToNotebook);
        NotebookSegmenter segmenter = new NotebookSegmenter(notebook);
        int[] bounds = segmenter.determineBoundedAreas();
        for (int i = 0; i < bounds[3]; i++) {
            this.cells.add(NotebookCell.parseCell(notebook, bounds, i, type));
        }
        this.jsonRepresentation = notebook;
    }

    public ArrayList<NotebookCell> getCells() {
        return cells;
    }

    public JSONObject getJsonRepresentation() {
        return jsonRepresentation;
    }

    @Override
    public String toString() {
        return this.jsonRepresentation.toString(2); // Pretty-print with 4-space indentation

    }


}