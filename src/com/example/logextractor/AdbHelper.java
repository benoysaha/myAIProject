package com.example.logextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AdbHelper {

    private static final String ADB_COMMAND = "adb";
    private static final int TIMEOUT_SECONDS = 30; // Default timeout for ADB commands

    /**
     * Executes an ADB command and returns its output.
     *
     * @param commandArgs The ADB command and its arguments (e.g., "logcat", "-d").
     * @return A list of strings, where each string is a line of the command output.
     * @throws IOException If an I/O error occurs during command execution.
     * @throws InterruptedException If the command execution is interrupted.
     */
    public List<String> executeAdbCommand(String targetDevice, List<String> commandArgs) throws IOException, InterruptedException {
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(ADB_COMMAND);
        if (targetDevice != null && !targetDevice.isEmpty()) {
            fullCommand.add("-s");
            fullCommand.add(targetDevice);
        }
        fullCommand.addAll(commandArgs);

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
        processBuilder.redirectErrorStream(true); // Combine stdout and stderr

        System.out.println("INFO: Executing ADB command: " + String.join(" ", fullCommand));

        Process process = processBuilder.start();
        List<String> output = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
        }

        boolean exited = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
            System.err.println("ERROR: ADB command timed out: " + String.join(" ", fullCommand));
            throw new IOException("ADB command timed out: " + String.join(" ", fullCommand));
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            System.err.println("ERROR: ADB command failed with exit code " + exitCode + ": " + String.join(" ", fullCommand));
            // Limit printing of potentially very long output from failed command
            String errorOutput = String.join("\n", output);
            if (errorOutput.length() > 1000) {
                errorOutput = errorOutput.substring(0, 1000) + "... (output truncated)";
            }
            System.err.println("ERROR: ADB command output:\n" + errorOutput);
            throw new IOException("ADB command failed with exit code " + exitCode + ". Output:\n" + errorOutput);
        }
        System.out.println("INFO: ADB command executed successfully.");
        return output;
    }

    /**
     * Checks if any ADB devices are connected.
     *
     * @return true if at least one device is connected (or the specified device is connected), false otherwise.
     */
    public boolean isDeviceConnected(String targetDevice) {
        System.out.println("INFO: Checking for ADB devices" + (targetDevice != null ? " (specifically " + targetDevice + ")" : "") + "...");
        try {
            List<String> args = new ArrayList<>();
            args.add("devices");
            List<String> output = executeAdbCommand(null, args); // Always list all devices first

            if (output.size() <= 1) { // Only header or empty
                System.out.println("INFO: No devices found or 'adb devices' returned empty list (after header).");
                return false;
            }

            boolean foundDevice = false;
            for (int i = 1; i < output.size(); i++) { // Skip header line
                String line = output.get(i).trim();
                if (line.isEmpty() || line.startsWith("*") || line.startsWith("List of devices")) {
                    continue;
                }
                // Example lines: "emulator-5554\tdevice", "R58M726X7XN\tunauthorized"
                String[] parts = line.split("\\s+"); // Split by whitespace
                if (parts.length >= 2) {
                    String deviceId = parts[0];
                    String deviceState = parts[1];

                    if ("device".equalsIgnoreCase(deviceState)) { // Check if device is in "device" state
                        if (targetDevice != null && !targetDevice.isEmpty()) {
                            if (deviceId.equals(targetDevice)) {
                                System.out.println("INFO: Specified device " + targetDevice + " found and connected.");
                                foundDevice = true;
                                break;
                            }
                        } else {
                            System.out.println("INFO: At least one device found and connected: " + deviceId);
                            foundDevice = true;
                            break; // Any connected device is fine if no target is specified
                        }
                    } else {
                         System.out.println("INFO: Device " + deviceId + " found but in state: " + deviceState);
                    }
                }
            }
            if (!foundDevice) {
                System.out.println("INFO: No " + (targetDevice != null ? "specified device ("+targetDevice+")" : "operational (state: device)") + " ADB device found.");
            }
            return foundDevice;
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR: Error checking for ADB devices: " + e.getMessage());
            return false;
        }
    }

    /**
     * Extracts logs using "adb logcat -d" and saves them to a specified file.
     *
     * @param targetDevice The specific device ID to target (can be null).
     * @param outputDirectory The directory where the log file should be saved.
     * @param outputFileName The name of the log file.
     * @return The File object representing the saved log file, or null if an error occurred.
     */
    public File extractLogcat(String targetDevice, String outputDirectory, String outputFileName) {
        System.out.println("INFO: Attempting to extract logcat for device: " + (targetDevice != null ? targetDevice : "default"));
        if (!isDeviceConnected(targetDevice)) {
            // isDeviceConnected already prints detailed error/info messages
            System.err.println("ERROR: Cannot extract logcat, device not connected or not in operational state.");
            return null;
        }

        List<String> logcatArgs = new ArrayList<>();
        logcatArgs.add("logcat");
        logcatArgs.add("-d"); // Dump the log and exit

        try {
            List<String> logLines = executeAdbCommand(targetDevice, logcatArgs);
            System.out.println("INFO: Logcat data retrieved, " + logLines.size() + " lines.");

            File dir = new File(outputDirectory);
            if (!dir.exists()) {
                System.out.println("INFO: Output directory does not exist, attempting to create: " + outputDirectory);
                if (!dir.mkdirs()) {
                    System.err.println("ERROR: Failed to create output directory: " + outputDirectory);
                    return null;
                }
                System.out.println("INFO: Output directory created: " + outputDirectory);
            }

            File outputFile = new File(dir, outputFileName);
            java.nio.file.Files.write(outputFile.toPath(), logLines);

            System.out.println("INFO: Logs extracted successfully to: " + outputFile.getAbsolutePath());
            return outputFile;
        } catch (IOException | InterruptedException e) {
            System.err.println("ERROR: Error during logcat extraction or file writing: " + e.getMessage());
            // e.printStackTrace(); // Keep this commented unless deeper debugging is needed by a developer
            return null;
        }
    }

    public static void main(String[] args) {
        // Simple test for AdbHelper
        AdbHelper adbHelper = new AdbHelper();
        // To test with a specific device, uncomment and set:
        // String testDevice = "emulator-5554";
        String testDevice = null;

        System.out.println("INFO: --- AdbHelper Test ---");
        System.out.println("INFO: Checking for connected devices (target: " + (testDevice != null ? testDevice : "any") + ")...");
        if (adbHelper.isDeviceConnected(testDevice)) {
            System.out.println("INFO: Device " + (testDevice != null ? testDevice : "any") + " is connected.");

            System.out.println("INFO: Attempting to extract logcat...");
            String tempDir = System.getProperty("java.io.tmpdir") + File.separator + "LogExtractorTest";
            new File(tempDir).mkdirs(); // Ensure test directory exists
            String testLogFilename = "test_logcat_" + (testDevice != null ? testDevice.replace(":", "_") + "_" : "") + "extract.txt";

            System.out.println("INFO: Test logs will be saved to: " + tempDir + File.separator + testLogFilename);

            File logFile = adbHelper.extractLogcat(testDevice, tempDir, testLogFilename);
            if (logFile != null && logFile.exists()) {
                System.out.println("INFO: Logcat extraction test successful. File: " + logFile.getAbsolutePath());
            } else {
                System.err.println("ERROR: Logcat extraction test failed.");
            }
        } else {
            System.err.println("ERROR: AdbHelper Test: No ADB device found (or specific device " + (testDevice != null ? testDevice : "any") + " not found/operational). Please connect an Android device/emulator, ensure USB debugging is enabled, and the device is authorized.");
        }
        System.out.println("INFO: --- AdbHelper Test Finished ---");
    }
}
