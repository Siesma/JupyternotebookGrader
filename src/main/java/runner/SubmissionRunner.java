package runner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class SubmissionRunner {

    public static String compileAndRun(String className, String code, int timeoutSeconds) throws IOException, InterruptedException {
        Path tempDir = Files.createTempDirectory("student_code");
        tempDir.toFile().deleteOnExit();

        Path javaFile = tempDir.resolve(className + ".java");


        String preamble = "import java.util.Arrays;\n";
        String fullCode = preamble + code;

        Files.writeString(javaFile, fullCode);

        ProcessBuilder compilePb = new ProcessBuilder("javac", javaFile.toAbsolutePath().toString());
        compilePb.directory(tempDir.toFile());
        compilePb.redirectErrorStream(true);
        Process compileProcess = compilePb.start();

        String compileOutput = new String(compileProcess.getInputStream().readAllBytes());
        boolean finished = compileProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            compileProcess.destroyForcibly();
            return "Compilation timed out!";
        }

        if (compileProcess.exitValue() != 0) {
            return "Compilation failed:\n" + compileOutput;
        }

        ProcessBuilder runPb = new ProcessBuilder("java", "-cp", tempDir.toAbsolutePath().toString(), className);
        runPb.directory(tempDir.toFile());
        runPb.redirectErrorStream(true);
        Process runProcess = runPb.start();

        finished = runProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        if (!finished) {
            runProcess.destroyForcibly();
            return "Execution timed out!";
        }

        String runOutput = new String(runProcess.getInputStream().readAllBytes());

        return runOutput;
    }
}
