package helper;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;

public class JSONImporter {

    public JSONObject importJSONObj (String pathToNoteBook) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(pathToNoteBook)));
            return new JSONObject(content);
        } catch (Exception e) {
            return null;
        }
    }
}
