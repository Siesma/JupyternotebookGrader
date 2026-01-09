package runner;

import java.io.*;

public interface SubmissionRunner {
    String compileAndRun(String className, String code, int timeoutSeconds) throws IOException, InterruptedException;
}
