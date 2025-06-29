package com.example.logextractor;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class Main {

    private static final String DEFAULT_OUTPUT_DIR_NAME = "ivi_logs";

    public static void main(String[] args) {
        if (args.length > 0 && (args[0].equals("-h") || args[0].equals("--help"))) {
            printHelp();
            return;
        }

        System.out.println("INFO: Android IVI Log Extractor");
        System.out.println("INFO: =========================");

        AdbHelper adbHelper = new AdbHelper();
        String targetDevice = null;

        if (args.length > 0 && !args[0].startsWith("-")) {
            targetDevice = args[0];
            System.out.println("INFO: User specified target device: " + targetDevice);
        }

        // AdbHelper.isDeviceConnected now prints its own INFO/ERROR messages.
        if (!adbHelper.isDeviceConnected(targetDevice)) {
            System.err.println("ERROR: Pre-requisite check failed: No operational Android device/emulator found" + (targetDevice != null ? " matching ID '" + targetDevice + "'" : "") + ".");
            System.err.println("ERROR: Please ensure a device is connected, authorized for USB debugging, and in 'device' state (not 'unauthorized' or 'offline').");
            System.err.println("ERROR: You can check connected devices by running 'adb devices' in your terminal.");
            System.err.println("INFO: Log Extractor finished with errors.");
            return;
        }
        System.out.println("INFO: Device " + (targetDevice != null ? targetDevice : "default") + " connected and ready.");

        Scanner scanner = new Scanner(System.in);
        String outputDirStr;

        // Determine output directory: use argument if provided, else prompt
        int outputDirArgIndex = (targetDevice == null ? 0 : 1);
        if (args.length > outputDirArgIndex && args[outputDirArgIndex] != null && !args[outputDirArgIndex].trim().isEmpty()) {
            outputDirStr = args[outputDirArgIndex];
            System.out.println("INFO: Using specified output directory: " + outputDirStr);
        } else {
            String defaultPath = System.getProperty("user.home") + File.separator + DEFAULT_OUTPUT_DIR_NAME;
            System.out.print("PROMPT: Enter the directory to save logs (default: " + defaultPath + "): ");
            outputDirStr = scanner.nextLine().trim();
            if (outputDirStr.isEmpty()) {
                outputDirStr = defaultPath;
                System.out.println("INFO: Using default output directory: " + outputDirStr);
            } else {
                System.out.println("INFO: User provided output directory: " + outputDirStr);
            }
        }

        File outputDir = new File(outputDirStr);
        if (!outputDir.exists()) {
            System.out.println("INFO: Directory '" + outputDirStr + "' does not exist. Attempting to create it...");
            if (outputDir.mkdirs()) {
                System.out.println("INFO: Directory created: " + outputDir.getAbsolutePath());
            } else {
                System.err.println("ERROR: Failed to create directory: " + outputDir.getAbsolutePath() + ".");
                System.err.println("ERROR: Please check permissions or try a different path.");
                scanner.close();
                System.out.println("INFO: Log Extractor finished with errors.");
                return;
            }
        } else if (!outputDir.isDirectory()) {
            System.err.println("ERROR: The specified path is not a directory: " + outputDir.getAbsolutePath());
            scanner.close();
            System.out.println("INFO: Log Extractor finished with errors.");
            return;
        }
        System.out.println("INFO: Using output directory: " + outputDir.getAbsolutePath());


        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String safeDeviceName = targetDevice != null ? targetDevice.replaceAll("[^a-zA-Z0-9_-]", "_") : "default";
        String outputFileName = "ivi_logcat_" + safeDeviceName + "_" + timestamp + ".txt";

        System.out.println("INFO: Starting logcat extraction...");
        System.out.println("INFO: Logs will be saved to: " + new File(outputDir, outputFileName).getAbsolutePath());

        // AdbHelper.extractLogcat now prints its own INFO/ERROR messages.
        File logFile = adbHelper.extractLogcat(targetDevice, outputDir.getAbsolutePath(), outputFileName);

        if (logFile != null && logFile.exists()) {
            System.out.println("SUCCESS: Log extraction successful!");
            System.out.println("SUCCESS: Log file saved at: " + logFile.getAbsolutePath());
            System.out.println("INFO: Log Extractor finished successfully.");
        } else {
            System.err.println("ERROR: Log extraction failed. Please check the console for ADB errors or file system issues.");
            System.err.println("ERROR: Ensure 'adb' is in your system PATH and the connected device remains authorized and operational during extraction.");
            System.out.println("INFO: Log Extractor finished with errors.");
        }
        scanner.close();
    }

    private static void printHelp() {
        System.out.println("Android IVI Log Extractor - Help");
        System.out.println("---------------------------------");
        System.out.println("Usage: java -jar LogExtractor.jar [device_id] [output_directory]");
        System.out.println("\nArguments:");
        System.out.println("  [device_id]        (Optional) The serial ID of the ADB device/emulator to target (e.g., emulator-5554).");
        System.out.println("                     If not specified, the tool will use the first available operational device.");
        System.out.println("                     If multiple devices are connected and none is specified, behavior may depend on ADB_SERIAL.");
        System.out.println("  [output_directory] (Optional) The directory where log files will be saved.");
        System.out.println("                     If not specified, prompts the user, defaulting to '~/ivi_logs' (user's home directory).");
        System.out.println("\nOptions:");
        System.out.println("  -h, --help         Show this help message and exit.");
        System.out.println("\nExamples:");
        System.out.println("  java -jar LogExtractor.jar");
        System.out.println("    (Prompts for output directory, uses first available device)");
        System.out.println("\n  java -jar LogExtractor.jar emulator-5554");
        System.out.println("    (Targets 'emulator-5554', prompts for output directory)");
        System.out.println("\n  java -jar LogExtractor.jar /path/to/my/custom_logs");
        System.out.println("    (Uses first available device, saves to '/path/to/my/custom_logs')");
        System.out.println("\n  java -jar LogExtractor.jar R58M726X7XN D:\\AndroidLogs");
        System.out.println("    (Targets device 'R58M726X7XN', saves to 'D:\\AndroidLogs')");
        System.out.println("\nPrerequisites:");
        System.out.println("  - Java Runtime Environment (JRE) installed.");
        System.out.println("  - Android Debug Bridge (adb) installed and in your system's PATH.");
        System.out.println("  - Target Android device or emulator connected, with USB Debugging enabled and authorized.");
    }
}
