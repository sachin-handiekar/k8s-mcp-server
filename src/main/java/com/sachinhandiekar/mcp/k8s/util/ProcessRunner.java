package com.sachinhandiekar.mcp.k8s.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utility to execute external CLI commands (helm, kubectl explain).
 *
 * <p>Captures stdout and stderr, enforces a timeout, and returns
 * the combined output as a string.</p>
 */
@Component
public class ProcessRunner {

    private static final Logger log = LoggerFactory.getLogger(ProcessRunner.class);
    private static final long DEFAULT_TIMEOUT_SECONDS = 30;

    /**
     * Result of a process execution.
     */
    public record ProcessResult(int exitCode, String stdout, String stderr) {
        public boolean isSuccess() {
            return exitCode == 0;
        }

        public String output() {
            if (isSuccess()) {
                return stdout;
            }
            return stderr.isBlank() ? stdout : stderr;
        }
    }

    /**
     * Runs a command with the given arguments.
     *
     * @param command the command and arguments
     * @return the process result
     */
    public ProcessResult run(List<String> command) {
        return run(command, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * Runs a command with a custom timeout.
     *
     * @param command        the command and arguments
     * @param timeoutSeconds maximum execution time
     * @return the process result
     */
    public ProcessResult run(List<String> command, long timeoutSeconds) {
        log.info("Executing command: {}", String.join(" ", command));
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(false);
            Process process = pb.start();

            String stdout;
            String stderr;
            try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                stdout = stdoutReader.lines().collect(Collectors.joining("\n"));
                stderr = stderrReader.lines().collect(Collectors.joining("\n"));
            }

            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return new ProcessResult(-1, "", "Command timed out after " + timeoutSeconds + " seconds");
            }

            return new ProcessResult(process.exitValue(), stdout, stderr);
        } catch (IOException e) {
            log.error("Failed to execute command: {}", e.getMessage());
            return new ProcessResult(-1, "", "Failed to execute command: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(-1, "", "Command interrupted");
        }
    }
}
