package com.example.demo;

import helper.JSONImporter;
import helper.Solution;
import helper.Submission;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;
import runner.SubmissionRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HelloController {

    @FXML
    private TextArea submissionText;
    @FXML
    private TextArea solutionText;
    @FXML
    private TextField scoreField;
    @FXML
    private TextArea commentField;

    @FXML
    private TextArea outputArea;
    @FXML
    private Label submissionLabel;

    @FXML
    private VBox gradingBox;

    private List<CheckBox> gradingCheckBoxes = new ArrayList<>();

    private List<Submission> submissions = new ArrayList<>();
    private List<Solution> solutions = new ArrayList<>();
    private int currentIndex = 0;
    private List<String> args;

    private JSONObject submissionNotebook;
    private JSONObject solutionNotebook;

    private List<String> tempComments;
    private List<boolean[]> tempGrading;
    private double[] points;
    private String[] pointLabels;

    public void initializeWithArgs(List<String> args) {
        this.args = args;
        if(args.size() != 3) {
            System.out.println("Usage: /../exercise_8.ipynb /../exam_solution.ipynb task_number");
            System.exit(1);
        }
        String pathToNotebook = args.get(0);
        String pathToSolution = args.get(1);
        int exerciseNumber = Integer.parseInt(args.get(2));

        loadSolutionNotebook(pathToSolution);
        loadHandinNotebook(pathToNotebook);
        loadRelevantSolution(exerciseNumber);


        tempComments = new ArrayList<>();
        tempGrading = new ArrayList<>();

        for (int i = 0; i < submissions.size(); i++) {
            tempComments.add("");
            tempGrading.add(new boolean[points.length]);
        }

        addCheckboxListeners();

        if (!submissions.isEmpty()) showSubmission(0);
        runStudentCode();
    }

    private void loadHandinNotebook(String pathToNotebook) {
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
    }

    private void loadSolutionNotebook(String pathToNotebook) {
        JSONImporter importer = new JSONImporter();
        solutionNotebook = importer.importJSONObj(pathToNotebook);

        JSONArray allCells = solutionNotebook.getJSONArray("cells");

        for (int i = 0; i < allCells.length(); i++) {
            JSONObject curCell = allCells.getJSONObject(i);
            if (!Solution.isSolutionCell(curCell)) continue;

            Solution solution = Solution.parseSolution(solutionNotebook, i);
            solutions.add(solution);
            i += solution.getCells().size() - 1;
        }
    }

    private void loadRelevantSolution(int exerciseNumber) {
        int curTask = 0;
        for (Solution solution : solutions) {

            curTask++;
            if (curTask != exerciseNumber) {
                continue;
            }
            JSONObject solutionCodeField = solution.getCells().get("solutionCodeField");
            JSONObject solutionSchema = solution.getCells().get("solutionSchema");
            loadSampleSolution(solutionCodeField);
            loadGradingSchema(solutionSchema);
        }
    }

    private void loadSampleSolution(JSONObject solutionCodeField) {
        JSONArray sourceField = solutionCodeField.optJSONArray("source");
        if (sourceField != null) {
            StringBuilder codeBuilder = new StringBuilder();
            for (int i = 0; i < sourceField.length(); i++) {
                codeBuilder.append(sourceField.optString(i));
            }
            solutionText.setText(codeBuilder.toString());
        } else {
            solutionText.setText("");
        }
    }

    private void loadGradingSchema(JSONObject solutionSchema) {
        JSONArray data = solutionSchema.optJSONArray("source");

        int offset = 0;
        while (offset < data.length() && !data.getString(offset).trim().isEmpty()) {
            offset++;
        }
        Pattern gradingPattern = Pattern.compile("(\\d+(?:\\.\\d+)?)P");

        int count = data.length() - offset - 1;
        points = new double[count];
        pointLabels = new String[count];

        for (int i = 0; i < count; i++) {
            String line = data.getString(i + offset + 1);
            Matcher matcher = gradingPattern.matcher(line);
            if (matcher.find()) {
                points[i] = Double.parseDouble(matcher.group(1));
                pointLabels[i] = line.substring(matcher.end()).trim();
            } else {
                points[i] = i;
                pointLabels[i] = String.format("Something about %d", i);
            }
        }

        buildGradingChecklist();
    }

    private void buildGradingChecklist() {
        gradingBox.getChildren().clear();
        gradingCheckBoxes.clear();

        for (int i = 0; i < points.length; i++) {
            int idx = i;

            CheckBox cb = new CheckBox(points[i] + "P | " + pointLabels[i]);
            cb.setStyle("-fx-text-fill: #ffffff;");

            cb.selectedProperty().addListener((obs, oldV, newV) -> {
                tempGrading.get(currentIndex)[idx] = newV;
                updateScorePreview();
            });

            gradingCheckBoxes.add(cb);
            gradingBox.getChildren().add(cb);
        }
    }


    private void addCheckboxListeners() {
        gradingCheckBoxes.forEach(e -> {
            e.setOnAction(b -> updateScorePreview());
        });
    }

    private void updateScorePreview() {
        boolean[] grading = tempGrading.get(currentIndex);
        double total = 0.0;

        for (int i = 0; i < points.length; i++) {
            if (grading[i]) total += points[i];
        }

        scoreField.setText(String.format("%.1f", total));
    }


    private void showSubmission(int index) {
        submissionLabel.setText(String.format("Submission Viewer (Submission #%d)", currentIndex));


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
        for (int i = 0; i < gradingCheckBoxes.size(); i++) {
            gradingCheckBoxes.get(i).setSelected(grading[i]);
        }
        for (int i = 0; i < gradingCheckBoxes.size(); i++) gradingCheckBoxes.get(i).setSelected(grading[i]);

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
        for (int i = 0; i < gradingCheckBoxes.size(); i++) {
            grading[i] = gradingCheckBoxes.get(i).isSelected();
        }
    }


    @FXML
    private void saveGrade() {
        saveTempCurrent();

        for (int i = 0; i < submissions.size(); i++) {
            Submission sub = submissions.get(i);

            int scoreIndex = sub.getCellStartIndex() + 3;
            int commentIndex = sub.getCellStartIndex() + 4;

            boolean[] grading = tempGrading.get(i);

            double total = 0;
            StringBuilder breakdown = new StringBuilder();

            for (int j = 0; j < points.length; j++) {
                double earned = grading[j] ? points[j] : 0.0;
                if (grading[j]) total += points[j];

                breakdown.append(String.format(
                        "%.1f/%.1f | %s%n",
                        earned, points[j], pointLabels[j]
                ));
            }

            adjustSource(submissionNotebook, scoreIndex, String.valueOf(total));

            if (!tempComments.get(i).isBlank()) {
                breakdown.append("\n").append(tempComments.get(i));
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
