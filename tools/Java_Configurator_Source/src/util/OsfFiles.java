package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import javax.swing.JTextArea;

/**
 * Helper for standard file paths in the OSF git repo.
 */
public class OsfFiles {
    private static final String COMMITS_FILE = "commits.txt";
    private static final String CONFIG_H_FILE = "src/controller/config.h";
    private static final String EXPERIMENTAL_SETTINGS_DIR = "experimental settings";
    private static final String PROVEN_SETTINGS_DIR = "proven settings";

    private String rootPath;

    /**
     * Look for the root of the OSF git repo.
     * @throws FileNotFoundException If the root of the OSF git repo can't be found.
     */
    public OsfFiles() throws FileNotFoundException {
        // Try to find the git repo root
        File currentDir = new File(Paths.get(".").toAbsolutePath().normalize().toString());
        File rootDir = currentDir;

        while (rootDir != null && !Arrays.asList(rootDir.list()).contains(CompileWorker.COMPILE_SCRIPT_SH)) {
            rootDir = rootDir.getParentFile();
        }
        if (rootDir == null) {
            throw new FileNotFoundException("Could not find the root path of the OSF git repo.");
        }
        rootPath = rootDir.getAbsolutePath();
    }

    public String getRootPath() {
        return rootPath;
    }

    /**
     * Read the first line from commits.txt.
     * @return The line, or null if the file doesn't exist or can't be read.
     */
    public String readLatestCommit() {
        File commitsFile = new File(getPathStr(COMMITS_FILE));
        if (commitsFile.exists()) {
            String commitsPath = commitsFile.getAbsolutePath();
            try (BufferedReader br = new BufferedReader(new FileReader(commitsPath))) {
                return br.readLine();
            } catch (Exception ex) {
                // ignore
            }
        }
        return null;
    }

    public File[] readProvenSettingsFiles() {
        File provenSettingsDir = new File(getPathStr(PROVEN_SETTINGS_DIR));
        return provenSettingsDir.listFiles();
    }

    public File[] readExperimentalSettingsFiles() {
        File experimentalSettingsDir = new File(getPathStr(EXPERIMENTAL_SETTINGS_DIR));
        return experimentalSettingsDir.listFiles();
    }

    public File writeExperimentalSettingsFile(String content, JTextArea output) throws IOException {
        String newFileName = new SimpleDateFormat("yyyyMMdd-HHmmssz").format(new Date()) + ".ini";
        Path newFilePath = getPath(EXPERIMENTAL_SETTINGS_DIR, newFileName);

        output.append("Writing settings to " + newFilePath + "\n");
        Files.writeString(newFilePath, content);
        return newFilePath.toFile();
    }

    public File writeConfigHFile(String content, JTextArea output) throws IOException {
        Path configHPath = getPath(CONFIG_H_FILE);

        output.append("Writing " + configHPath + "\n");
        Files.writeString(configHPath, content);
        return configHPath.toFile();
    }

    private Path getPath(String... parts) {
        return Paths.get(rootPath, parts);
    }

    private String getPathStr(String... parts) {
        return getPath(parts).toString();
    }
}
