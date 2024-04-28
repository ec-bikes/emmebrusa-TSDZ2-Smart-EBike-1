package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

/**
 * This worker handles compiling and flashing the TSDZ2 firmware, while appending all console output
 * to a text area without hanging the main thread.
 * On completion, it will show a dialog with a success or error message.
 * It also handles canceling the process if needed.
 */
// Generics:
// - ResultType is returned by the doInBackground and get methods.
// - String is for the log lines passed to publish() and consumed by process() to update the text area.
public class CompileWorker extends SwingWorker<CompileWorker.ResultType, String> {
    public enum ResultType { SUCCESS, ERROR, CANCEL };
    public static final String COMPILE_SCRIPT_BAT = "compile_and_flash_20.bat";
    public static final String COMPILE_SCRIPT_SH = "compile_and_flash_20.sh";

    private JTextArea textArea;
    private String fileName;
    private String rootDir;
    // Store the message and result type here for more convenient use in done()
    private String message;
    private ResultType resultType;

    public CompileWorker(JTextArea textArea, String fileName, String rootDir) {
        this.textArea = textArea;
        this.fileName = fileName.substring(0, fileName.lastIndexOf('.')); //remove ini extension
        this.rootDir = rootDir;
    }

    @Override
    public ResultType doInBackground() {
        ProcessBuilder processBuilder = getProcessBuilder();
        if (processBuilder == null) {
            resultType = ResultType.ERROR;
            message = "Unknown OS.\n Please run:\ncd src/controller && make && make clear_eeprom && make flash\n" +
                    "to compile and flash your TSDZ2.";
            return resultType;
        }

        textArea.append("\nRunning: " + String.join(" ", processBuilder.command()) + "\n");

        // Ensure the correct working directory
        processBuilder.directory(new File(rootDir));
        // Redirect stderr to stdout so that it can be read in the same stream
        processBuilder.redirectErrorStream(true);

        try (AutoCloseProcess process = new AutoCloseProcess(processBuilder);
                InputStreamReader isr = new InputStreamReader(process.getInputStream());
                BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                publish(line);
                if (isCancelled()) {
                    resultType = ResultType.CANCEL;
                    return resultType;
                }
            }
            int exitCode = process.waitFor();
            resultType = exitCode == 0 ? ResultType.SUCCESS : ResultType.ERROR;
        } catch (IOException | InterruptedException e1) {
            e1.printStackTrace(System.err);
            resultType = ResultType.ERROR;
            message = e1.getMessage();
        }

        return resultType;
    }

    @Override
    protected void process(java.util.List<String> chunks) {
        for (String line : chunks) {
            appendText(line);
        }
    }

    @Override
    protected void done() {
        String genericMessage = "Check output pane for details";
        switch (resultType) {
            case SUCCESS:
                JOptionPane.showMessageDialog(null, genericMessage, "Success", JOptionPane.PLAIN_MESSAGE);
                break;
            case ERROR:
                JOptionPane.showMessageDialog(null, message == null ? genericMessage : message, "Compiling or flashing failed", JOptionPane.ERROR_MESSAGE);
                break;
            case CANCEL:
                appendText("\nCANCELLED");
                break;
        }
    }

    private void appendText(String text) {
        textArea.append("\n" + text);
        textArea.setCaretPosition(textArea.getDocument().getLength());
    }

    /**
     * Get the ProcessBuilder for the current OS.
     * Returns null if the OS is not recognized.
     */
    private ProcessBuilder getProcessBuilder() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        boolean isMac = OS.contains("mac") || OS.contains("darwin");

        if (isMac || OS.contains("nux")) {
            String shell = "bash";
            String preCommand = "";

            if (isMac) {
                // Extra setup for Macs: due to the lack of a built-in package manager, when running
                // shell scripts relying on non-builtin commands, the PATH needs to be updated to
                // account for the package manager (such as homebrew) or other custom setup.
                // Usually this is configured in the profile or rc file for the OS-default shell.
                // (zsh is default on newer macOS versions.)
                String sourceFormat = ". ~/%s &>/dev/null; ";
                if (System.getenv("SHELL").endsWith("/zsh")) {
                    shell = "zsh";
                    preCommand = String.format(sourceFormat + sourceFormat, ".zprofile", ".zshrc");
                } else {
                    preCommand = String.format(sourceFormat + sourceFormat, ".bash_profile", ".bashrc");
                }
            }

            // Start a shell rather than running the command directly so that the PATH is set.
            return new ProcessBuilder(
                    "/bin/" + shell,
                    "-c", String.format("%s sh %s %s --gui", preCommand, COMPILE_SCRIPT_SH, fileName));
        }

        if (OS.contains("win")) {
            return new ProcessBuilder("cmd", "/c", "start", COMPILE_SCRIPT_BAT, fileName);
        }

        return null;
    }

    /**
     * Wraps a Process with the AutoCloseable interface for try-with-resource.
     * It will destroy the process on close.
     */
    private class AutoCloseProcess implements AutoCloseable {
        private final Process process;

        /** Start the process from the given ProcessBuilder. */
        public AutoCloseProcess(ProcessBuilder processBuilder) throws IOException {
            this.process = processBuilder.start();
        }

        public int waitFor() throws InterruptedException { return process.waitFor(); }
        public InputStream getInputStream() { return process.getInputStream(); }
        @Override
        public void close() { process.destroy(); }
    }
}
