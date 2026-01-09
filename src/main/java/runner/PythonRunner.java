package runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class PythonRunner implements SubmissionRunner {

    @Override
    public String compileAndRun(String className, String code, int timeoutSeconds) throws IOException, InterruptedException {

        Path tempDir = Files.createTempDirectory("student_code");
        tempDir.toFile().deleteOnExit();

        Path pyFile = tempDir.resolve(className + ".py");

        String preamble = "";
        String fullCode = preamble + code;

        Files.writeString(pyFile, fullCode);

        ProcessBuilder runPb = new ProcessBuilder("python3", pyFile.toAbsolutePath().toString());

        runPb.directory(tempDir.toFile());
        runPb.redirectErrorStream(true);

        Process runProcess = runPb.start();

        boolean finished = runProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            runProcess.destroyForcibly();
            return "Execution timed out!";
        }

        String output = new String(runProcess.getInputStream().readAllBytes());

        if (runProcess.exitValue() != 0) {
            return "Execution failed:\n" + output;
        }

        return output;
    }
}
