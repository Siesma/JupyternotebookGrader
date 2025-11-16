package com.example.demo;

import helper.JSONImporter;
import helper.Submission;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.json.JSONArray;
import org.json.JSONObject;
import runner.SubmissionRunner;

import java.util.ArrayList;
import java.util.List;

public class HelloController {

    @FXML
    private TextArea submissionText;
    @FXML
    private TextField scoreField;
    @FXML
    private TextArea commentField;

    @FXML
    private CheckBox cbMethodSignature;
    @FXML
    private CheckBox cbNewArray;
    @FXML
    private CheckBox cbLoop;
    @FXML
    private CheckBox cbFillArray;
    @FXML
    private CheckBox cbReturn;
    @FXML
    private TextArea outputArea;
    @FXML
    private Label submissionLabel;


    private List<Submission> submissions = new ArrayList<>();
    private int currentIndex = 0;
    private List<String> args;

    private JSONObject submissionNotebook;

    private List<String> tempComments;
    private List<boolean[]> tempGrading;
    private final double[] points = {0.5, 0.5, 1.0, 0.5, 0.5};

    public void initializeWithArgs(List<String> args) {
        this.args = args;
        String pathToNotebook = args.get(0);
        String pathToSolution = args.get(1);

        JSONImporter importer = new JSONImporter();
        submissionNotebook = importer.importJSONObj(pathToNotebook);

        JSONArray allCells = submissionNotebook.getJSONArray("cells");

        for (int i = 0; i < allCells.length(); i++) {
            JSONObject curCell = allCells.getJSONObject(i);
            if (!Submission.isSubmissionCell(curCell)) continue;

            Submission submission = Submission.parseSubmission(submissionNotebook, i);
            submissions.add(submission);
            i += submission.getCells().size() - 1;
        }

        tempComments = new ArrayList<>();
        tempGrading = new ArrayList<>();
        for (int i = 0; i < submissions.size(); i++) {
            tempComments.add("");
            tempGrading.add(new boolean[5]);
        }

        addCheckboxListeners();

        if (!submissions.isEmpty()) showSubmission(0);
    }

    private void addCheckboxListeners() {
        CheckBox[] boxes = {cbMethodSignature, cbNewArray, cbLoop, cbFillArray, cbReturn};
        for (int i = 0; i < boxes.length; i++) {
            final int idx = i;
            boxes[i].setOnAction(e -> updateScorePreview(idx));
        }
    }

    private void updateScorePreview(int idxChanged) {
        boolean[] grading = tempGrading.get(currentIndex);
        CheckBox[] boxes = {cbMethodSignature, cbNewArray, cbLoop, cbFillArray, cbReturn};

        grading[idxChanged] = boxes[idxChanged].isSelected();

        double total = 0;
        for (int j = 0; j < points.length; j++) {
            if (grading[j]) total += points[j];
        }
        scoreField.setText(String.format("%.1f / %.1f", total, 3.0));
    }

    private void showSubmission(int index) {
        submissionLabel.setText(String.format("Submission Viewer (Submission #%d)", currentIndex + 1));


        Submission s = submissions.get(index);

        JSONObject submissionCell = s.getCells().get("submission");
        JSONArray sourceField = submissionCell.optJSONArray("source");
        if (sourceField != null) {
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < sourceField.length(); i++) {
                codeBuilder.append(sourceField.optString(i));
            }
            submissionText.setText(codeBuilder.toString());
        } else {
            submissionText.setText("");
        }

        boolean[] grading = tempGrading.get(index);
        CheckBox[] boxes = {cbMethodSignature, cbNewArray, cbLoop, cbFillArray, cbReturn};
        for (int i = 0; i < boxes.length; i++) boxes[i].setSelected(grading[i]);

        commentField.setText(tempComments.get(index));

        double total = 0;
        for (int i = 0; i < points.length; i++) if (grading[i]) total += points[i];
        scoreField.setText(String.format("%.1f / %.1f", total, 3.0));

        JSONArray outputs = submissionCell.optJSONArray("outputs");
        if (outputs != null && outputs.length() > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < outputs.length(); i++) {
                JSONObject output = outputs.getJSONObject(i);
                String outputType = output.optString("output_type");
                if ("error".equals(outputType)) {
                    JSONArray traceback = output.optJSONArray("traceback");
                    if (traceback != null) {
                        for (int j = 0; j < traceback.length(); j++) {
                            sb.append(traceback.optString(j)).append("\n");
                        }
                    }
                } else if ("stream".equals(outputType) || "execute_result".equals(outputType)) {
                    String text = output.optString("text", "");
                    sb.append(text).append("\n");
                }
            }
            outputArea.setText(sb.toString());
        } else {
            outputArea.setText("");
        }
    }

    @FXML
    private void onNext() {
        saveTempCurrent();
        if (currentIndex < submissions.size() - 1) {
            currentIndex++;
            showSubmission(currentIndex);
        }
        runStudentCode();
    }

    @FXML
    private void onPrev() {
        saveTempCurrent();
        if (currentIndex > 0) {
            currentIndex--;
            showSubmission(currentIndex);
        }
    }

    @FXML
    private void runStudentCode() {
        String code = submissionText.getText();
        String className = extractClassName(code); // helper function to parse class name

        try {
            String output = SubmissionRunner.compileAndRun(className, code, 5); // 5 second timeout
            outputArea.setText(output);
        } catch (Exception e) {
            outputArea.setText("Error running code:\n" + e.getMessage());
        }
    }

    private String extractClassName(String code) {
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("class ")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) return parts[1];
            }
        }
        return "StudentClass"; // fallback
    }

    private void saveTempCurrent() {
        tempComments.set(currentIndex, commentField.getText());
        boolean[] grading = tempGrading.get(currentIndex);
        grading[0] = cbMethodSignature.isSelected();
        grading[1] = cbNewArray.isSelected();
        grading[2] = cbLoop.isSelected();
        grading[3] = cbFillArray.isSelected();
        grading[4] = cbReturn.isSelected();
    }

    @FXML
    private void saveGrade() {
        saveTempCurrent();

        for (int i = 0; i < submissions.size(); i++) {
            Submission current = submissions.get(i);
            int scoreIndex = current.getCellStartIndex() + 3;
            int commentIndex = current.getCellStartIndex() + 4;

            boolean[] grading = tempGrading.get(i);
            String[] labels = {"Methodensignatur", "Neues Array", "Schleife", "Werte eintragen", "Rückgabe"};

            double total = 0;
            StringBuilder breakdown = new StringBuilder();
            for (int j = 0; j < points.length; j++) {
                if (grading[j]) total += points[j];
                breakdown.append(String.format("%.1f/%.1f | %s\n", grading[j] ? points[j] : 0, points[j], labels[j]));
            }

            adjustSource(submissionNotebook, scoreIndex, String.valueOf(total));

            if(!commentField.getText().isBlank())  {
                breakdown.append("\n").append(commentField.getText());
            }

            adjustSource(submissionNotebook, commentIndex, breakdown.toString());
        }

        saveNotebookAsNew("/home/tom/Downloads/new.ipynb");
    }


    private void adjustSource(JSONObject notebook, int cellIndex, String newContent) {
        JSONArray cells = notebook.getJSONArray("cells");
        if (cellIndex >= 0 && cellIndex < cells.length()) {
            JSONObject cell = cells.getJSONObject(cellIndex);
            JSONArray source = cell.optJSONArray("source");
            if (source != null && source.length() > 0) {
                source.put(0, newContent);
            }
        }
    }

    private void saveNotebookAsNew(String outputPath) {
        if (submissionNotebook == null) return;
        try (java.io.FileWriter file = new java.io.FileWriter(outputPath)) {
            file.write(submissionNotebook.toString(2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
