package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;

public class CompileThread extends SwingWorker<CompileThread.ResultType, String> {
    public enum ResultType { SUCCESS, ERROR, CANCEL };
    
    JTextArea textArea;
    String fileName;
    private String message;
    private ResultType resultType;

    public CompileThread(JTextArea ta, String f) {
        textArea = ta;
        fileName = f.substring(0, f.lastIndexOf('.')); //remove ini extension
    }

    @Override
    public CompileThread.ResultType doInBackground() {
        // Detect OS
        OSType os = getOperatingSystem();
        ProcessBuilder processBuilder;
        switch (os) {
            case Windows:
                processBuilder = new ProcessBuilder("cmd", "/c", "start", "compile_and_flash_20", fileName);
                break;
            case MacOS:
            case Linux:
                String home = System.getProperty("user.home");
                String shell = "bash";
                String preCommand = "";
                if (os == OSType.MacOS && Files.exists(Paths.get(home, ".zprofile"))) {
                    shell = "zsh";
                    preCommand = ". ~/.zprofile && ";
                }
                processBuilder = new ProcessBuilder(
                        "/bin/" + shell,
                        "-c", preCommand + "sh compile_and_flash_20.sh " + fileName);
                break;
            case Other:
            default:
                resultType = ResultType.ERROR;
                message = "Unknown OS.\n Please run:\ncd src/controller && make && make clear_eeprom && make flash\n" +
                        "to compile and flash your TSDZ2.";
                return resultType;
        }

        textArea.setText("Running: " + String.join(" ", processBuilder.command()));
        // didn't work?
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();

            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
                publish(line);
                if (isCancelled()) {
                    br.close();
                    isr.close();
                    process.destroy();
                    resultType = ResultType.CANCEL;
                    return resultType;
                }
            }
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                resultType = ResultType.SUCCESS;
            } else {
                resultType = ResultType.ERROR;
            }
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

    private enum OSType {
        Windows, MacOS, Linux, Other
    };

    private OSType getOperatingSystem() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if (OS.contains("mac") || OS.contains("darwin")) {
            return OSType.MacOS;
        } else if (OS.contains("win")) {
            return OSType.Windows;
        } else if (OS.contains("nux")) {
            return OSType.Linux;
        }
        return OSType.Other;
    }
}
