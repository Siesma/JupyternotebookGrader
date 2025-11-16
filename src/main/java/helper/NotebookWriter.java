package helper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;

public class NotebookWriter {

    public static void updateSubmission(Submission submission, String newScore, String newComment, JSONObject notebook, String outputPath) throws IOException {
        JSONObject scoreCell = submission.getCells().get("score");
        JSONObject commentCell = submission.getCells().get("comment");

        JSONArray scoreSource = scoreCell.optJSONArray("source");
        if (scoreSource != null && !scoreSource.isEmpty()) {
            scoreSource.put(0, "### Score: " + newScore);
        }

        JSONArray commentSource = commentCell.optJSONArray("source");
        if (commentSource != null && !commentSource.isEmpty()) {
            commentSource.put(0, "### Comments: " + newComment);
        }

        try (FileWriter file = new FileWriter(outputPath)) {
            file.write(notebook.toString(2));
        }
    }
}

