# Android IVI Log Extractor

This project is a Java application designed to extract logs from an Android IVI (In-Vehicle Infotainment) system and store them locally.

## Features

- Connects to Android IVI device via ADB (Android Debug Bridge).
- Extracts logs using ADB commands.
- Stores logs locally in a user-specified directory.
- Simple command-line interface.

## Prerequisites

- **Java Development Kit (JDK):** Version 8 or higher recommended. Ensure `java` and `javac` are in your system's PATH.
- **Android Debug Bridge (ADB):** Must be installed and the `adb` command must be accessible from your system's PATH.
- **Android Device/Emulator:** An Android IVI system, other Android device, or emulator with USB Debugging enabled and authorized on the machine running this tool.

## Setup

1.  **Clone the repository (or download the source code):**
    ```bash
    git clone <repository-url>
    cd android-ivi-log-extractor
    ```
    (Replace `<repository-url>` with the actual URL if cloned from a Git repo. If you have the files directly, navigate to the project's root directory.)

2.  **(Optional) Download Pre-compiled JAR:**
    If a pre-compiled `LogExtractor.jar` is provided, you can skip the Build steps and go directly to Usage.

## Build

The project can be compiled and packaged into a runnable JAR file using standard JDK tools. No external build tools like Maven or Gradle are required.

1.  **Open a terminal or command prompt** in the root directory of the project.

2.  **Compile the source code:**
    ```bash
    # Create output directory for compiled classes
    mkdir -p out/com/example/logextractor
    # (For Windows, use: mkdir out\com\example\logextractor if out directory doesn't exist)

    # Compile .java files
    javac -d out -Xlint:deprecation src/com/example/logextractor/AdbHelper.java src/com/example/logextractor/Main.java
    ```
    This command compiles the Java files and places the `.class` files into the `out` directory, preserving the package structure.

3.  **Create the Manifest File:**
    Create a file named `manifest.txt` in the project root with the following content:
    ```
    Main-Class: com.example.logextractor.Main

    ```
    *Important: Ensure there is a newline character at the end of the `manifest.txt` file.*

4.  **Package into a Runnable JAR:**
    ```bash
    jar cfm LogExtractor.jar manifest.txt -C out .
    ```
    This command creates `LogExtractor.jar` in the project root. The `-C out .` part tells `jar` to change to the `out` directory and include all its contents.

## Usage

Once you have `LogExtractor.jar` (either by building it or downloading a pre-compiled version), you can run it from the terminal:

```bash
java -jar LogExtractor.jar [device_id] [output_directory]
```

**Arguments:**

-   `[device_id]` (Optional):
    -   The serial ID of the ADB device/emulator to target (e.g., `emulator-5554`, `R58M726X7XN`).
    -   If not specified, the tool will attempt to use the first available operational device found by `adb devices`.
    -   If multiple devices are connected and no ID is specified, ADB's behavior might be unpredictable or it might use the device specified by the `ADB_SERIAL` environment variable if set. It's recommended to specify the device ID if multiple are present.

-   `[output_directory]` (Optional):
    -   The directory where the extracted log file will be saved.
    -   If not specified, the application will prompt you to enter a directory.
    -   If you skip the prompt by pressing Enter, it will default to a directory named `ivi_logs` inside your user home directory (e.g., `/home/youruser/ivi_logs` or `C:\Users\YourUser\ivi_logs`).
    -   The log filename will be `ivi_logcat_<deviceNameOrSerial>_<timestamp>.txt`.

**Options:**

-   `-h`, `--help`:
    -   Show a detailed help message including usage, arguments, options, and examples, then exit.
    ```bash
    java -jar LogExtractor.jar -h
    ```

**Examples:**

1.  **Interactive mode (prompts for output directory, uses first available device):**
    ```bash
    java -jar LogExtractor.jar
    ```

2.  **Target a specific device (prompts for output directory):**
    ```bash
    java -jar LogExtractor.jar emulator-5554
    ```

3.  **Specify output directory (uses first available device):**
    ```bash
    java -jar LogExtractor.jar /path/to/my/log_storage
    # On Windows:
    # java -jar LogExtractor.jar D:\MyAndroidLogs
    ```

4.  **Target a specific device AND specify output directory:**
    ```bash
    java -jar LogExtractor.jar RF8M12ABCDE /var/logs/ivi_dumps
    ```

## How it Works

1.  The application uses the `adb` command-line tool.
2.  It first checks for connected devices using `adb devices`.
3.  It then executes `adb logcat -d` (optionally with `-s <device_id>`) to dump the current log buffer from the target device.
4.  The output is captured and saved to a local text file in the specified output directory with a timestamped filename.

## Troubleshooting

-   **`'adb' is not recognized...` or `adb: command not found`**: Ensure ADB is installed and its directory is added to your system's PATH environment variable.
-   **No device found / Device unauthorized**:
    -   Make sure your Android device has "USB Debugging" enabled in Developer Options.
    -   When you connect the device via USB, your device should show a prompt "Allow USB debugging?". Accept it.
    -   You can run `adb devices` manually in your terminal to check the status. It should show `device` next to your device ID, not `unauthorized` or `offline`.
-   **Permission denied (for output directory/file)**: Ensure you have write permissions for the chosen output directory. Try running the command from a directory where you have write access, or choose a different output directory.
-   **JAR execution issues (`Error: Could not find or load main class com.example.logextractor.Main`)**:
    -   Verify the `manifest.txt` was created correctly with the `Main-Class` attribute and a trailing newline.
    -   Ensure the `jar cfm` command was run correctly, including the `-C out .` part to correctly package classes from the `out` directory.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.
