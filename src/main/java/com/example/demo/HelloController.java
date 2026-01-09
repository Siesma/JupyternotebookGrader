package com.example.demo;

import helper.JSONImporter;
import helper.Solution;
import helper.Submission;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.JSONArray;
import org.json.JSONObject;
import runner.JavaRunner;
import runner.PythonRunner;
import runner.SubmissionRunner;

import java.io.File;
import java.io.IOException;
import java.util.*;
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
    @FXML
    private ChoiceBox<String> runnerChoice;


    private List<Slider> gradingSliders = new ArrayList<>();

    private List<Submission> submissions = new ArrayList<>();
    private List<Solution> solutions = new ArrayList<>();
    private int currentIndex = 0;
    private List<String> args;

    private JSONObject submissionNotebook;
    private JSONObject solutionNotebook;

    private List<String> tempComments;
    private List<double[]> tempGrading;
    private double[] points;
    private String[] pointLabels;

    private SubmissionRunner submissionRunner;
    private final HashMap<String, SubmissionRunner> submissionRunners = new HashMap<>();

    public void initializeWithArgs(List<String> args) {
        this.args = args;
        initializeRunnerChoice();
    }

    @FXML
    private void initializeRunnerChoice() {

        this.submissionRunners.put("Java".toLowerCase(Locale.ROOT), new JavaRunner());
        this.submissionRunners.put("Python".toLowerCase(Locale.ROOT), new PythonRunner());

        runnerChoice.getItems().setAll(
                submissionRunners.keySet().stream()
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .toList()
        );

        runnerChoice.setValue("Java");
        this.changeSubmissionRunner(null);
        runnerChoice.setOnAction(this::changeSubmissionRunner);
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
        tempComments = new ArrayList<>();
        tempGrading = new ArrayList<>();
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

    private void initTempFields() {
        tempComments.clear();
        tempGrading.clear();

        for (int i = 0; i < submissions.size(); i++) {
            tempComments.add("");
            tempGrading.add(new double[points.length]);
        }
    }

    private void buildGradingChecklist() {
        gradingBox.getChildren().clear();
        gradingSliders.clear();

        for (int i = 0; i < points.length; i++) {
            int idx = i;

            Label label = new Label();
            label.setStyle("-fx-text-fill: #ffffff;");
            label.setMinWidth(450);
            label.setMaxWidth(450);

            Slider slider = new Slider(0, points[i], 0);
            slider.setBlockIncrement(0.5);
            slider.setMajorTickUnit(0.5);
            slider.setMinorTickCount(0);
            slider.setSnapToTicks(true);
            slider.setShowTickMarks(true);
            slider.setShowTickLabels(true);

            slider.valueProperty().addListener((obs, oldV, newV) -> {
                double rounded = Math.round(newV.doubleValue() * 2) / 2.0;

                if (slider.getValue() != rounded) {
                    slider.setValue(rounded);
                }

                tempGrading.get(currentIndex)[idx] = rounded;
                updateScorePreview();

                label.setText(
                        String.format("%.1f/%.1f | %s",
                                rounded, points[idx], pointLabels[idx])
                );
            });

            label.setText(
                    String.format("0.0/%.1f | %s",
                            points[idx], pointLabels[idx])
            );

            gradingSliders.add(slider);

            HBox row = new HBox(10, label, slider);
            row.setAlignment(Pos.CENTER_LEFT);

            gradingBox.getChildren().add(row);
        }
    }


    private void updateScorePreview() {
        double total = 0.0;
        for (double v : tempGrading.get(currentIndex)) {
            total += v;
        }

        double max = Arrays.stream(points).sum();
        scoreField.setText(String.format("%.1f / %.1f", total, max));
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

        double[] grading = tempGrading.get(index);
        for (int i = 0; i < gradingSliders.size(); i++) {
            gradingSliders.get(i).setValue(grading[i]);
        }

        commentField.setText(tempComments.get(index));

        double maxPoints = 0;
        for (int i = 0; i < points.length; i++) {
            maxPoints += points[i];
        }

        double total = 0;
        for (int i = 0; i < points.length; i++) {
            total += gradingSliders.get(i).getValue();
        }
        scoreField.setText(String.format("%.1f / %.1f", total, maxPoints));

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
        runStudentCode();
    }

    @FXML
    private void runStudentCode() {
        if (submissionRunner == null) {
            return;
        }
        String code = submissionText.getText();
        String className = extractMainClassName(code);
        if (className == null) {
            return;
        }
        try {
            String output = submissionRunner.compileAndRun(className, code, 5);
            outputArea.setText(output);
        } catch (Exception e) {
            outputArea.setText("Error running code:\n" + e.getMessage());
        }
    }

    private String extractMainClassName(String code) {
        if (submissionRunner instanceof PythonRunner) {
            return "student_code";
        }
        String[] lines = code.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.matches(".*public\\s+static\\s+void\\s+main\\s*\\(.*String\\s*\\[]\\s+args.*\\).*")) {

                for (int j = i; j >= 0; j--) {
                    String prev = lines[j].trim();

                    if (prev.matches("(public\\s+)?class\\s+\\w+.*")) {
                        return prev.replaceAll(".*class\\s+(\\w+).*", "$1");
                    }
                }
            }
        }
        return null;
    }

    private void saveTempCurrent() {
        tempComments.set(currentIndex, commentField.getText());

        double[] grading = tempGrading.get(currentIndex);
        for (int i = 0; i < gradingSliders.size(); i++) {
            grading[i] = gradingSliders.get(i).getValue();
        }
    }


    @FXML
    private void saveGrade() {
        saveTempCurrent();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Graded Notebook");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Jupyter Notebook (*.ipynb)", "*.ipynb")
        );
        chooser.setInitialFileName("graded_notebook.ipynb");

        File file = chooser.showSaveDialog(submissionText.getScene().getWindow());
        if (file == null) {
            return;
        }

        String path = file.getAbsolutePath();
        if (!path.endsWith(".ipynb")) {
            path += ".ipynb";
        }

        for (int i = 0; i < submissions.size(); i++) {
            Submission sub = submissions.get(i);

            int scoreIndex = sub.getCellStartIndex() + 3;
            int commentIndex = sub.getCellStartIndex() + 4;

            double[] grading = tempGrading.get(i);

            double total = 0;
            StringBuilder breakdown = new StringBuilder();

            for (int j = 0; j < points.length; j++) {
                double earned = grading[j];
                total += earned;

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

        saveNotebookAsNew(path);
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

    @FXML
    public void onLoadSolutionNotebook(ActionEvent actionEvent) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Solution Notebook");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Jupyter Notebook (*.ipynb)", "*.ipynb")
        );

        File file = chooser.showOpenDialog(submissionText.getScene().getWindow());
        if (file == null) return;

        loadSolutionNotebook(file.getAbsolutePath());

        if (solutions.isEmpty()) return;

        List<Integer> taskNumbers = new ArrayList<>();
        for (int i = 1; i <= solutions.size(); i++) {
            taskNumbers.add(i);
        }

        ChoiceDialog<Integer> dialog =
                new ChoiceDialog<>(taskNumbers.get(0), taskNumbers);

        dialog.setTitle("Select Exercise");
        dialog.setHeaderText("Choose which exercise to load");
        dialog.setContentText("Exercise:");

        dialog.setOnShown(e -> {
            Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
            stage.setAlwaysOnTop(true);
            stage.requestFocus();
        });

        dialog.showAndWait().ifPresent(this::loadRelevantSolution);
    }

    @FXML
    private void onLoadHandinNotebook() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Submission Notebook");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Jupyter Notebook (*.ipynb)", "*.ipynb")
        );

        File file = chooser.showOpenDialog(submissionText.getScene().getWindow());
        if (file != null) {
            loadHandinNotebook(file.getAbsolutePath());
            if (!submissions.isEmpty()) {
                currentIndex = 0;
            }
        } else {
            System.err.println("BUG");
        }
        initTempFields();
        if (!submissions.isEmpty()) showSubmission(0);
        runStudentCode();
    }


    @FXML
    private void emergencySave(ActionEvent actionEvent) {
        for (int i = 0; i < submissions.size(); i++) {
            double[] grading = tempGrading.get(i);
            double total = 0;
            StringBuilder breakdown = new StringBuilder();

            for (int j = 0; j < points.length; j++) {
                double earned = grading[j];
                total += earned;

                breakdown.append(String.format(
                        "%.1f/%.1f | %s%n",
                        earned, points[j], pointLabels[j]
                ));
            }
            System.out.printf("""
                    ----------\n
                    Submission %s\n
                    Grading: \n%s\n
                    Grade: %s\n
                    """, i, breakdown.toString(), total);
        }
    }

    @FXML
    private void changeSubmissionRunner(ActionEvent actionEvent) {
        String selected = runnerChoice.getValue();
        if (selected == null) return;

        submissionRunner = submissionRunners.get(
                selected.toLowerCase(Locale.ROOT)
        );

        runStudentCode();
    }


}
