package util;

import java.util.ArrayList;

import javax.swing.JToggleButton;

import components.NumberField;

/**
 * Helper for formatting values to be saved in config.h and the settings .ini file.
 */
public class ConfigSerializer {
    private final ArrayList<String> iniLines = new ArrayList<>();
    private final ArrayList<String> configHLines = new ArrayList<>();

    public String getIni() {
        return String.join("\n", iniLines);
    }

    public String getConfigH() {
        return String.join("\n", configHLines);
    }

    /** Add any text to config.h */
    public void addConfigHLine(Object line) {
        configHLines.add(String.valueOf(line));
    }

    /** Only #define a config value for the field (don't save in ini). */
    public void define(String name, Object value) {
        configHLines.add("#define " + name + " " + value);
    }

    /** Save a value to the .ini file */
    public void save(Object value) {
        iniLines.add(String.valueOf(value));
    }

    /** Save the value to the ini file, and #define a config value */
    public void saveAndDefine(String name, JToggleButton rb) {
        saveAndDefine(name, rb.isSelected());
    }

    /** Save the value to the ini file, and #define a config value */
    public void saveAndDefine(String name, NumberField<?> txt) {
        String strValue = txt.getValue().toString();
        define(name, strValue);
        iniLines.add(strValue);
    }

    /** Save the value to the ini file, and #define a config value */
    public void saveAndDefine(String name, int value) {
        define(name, value);
        save(value);
    }

    /** Save the value to the ini file, and #define a config value */
    public void saveAndDefine(String name, boolean value) {
        define(name, value ? 1 : 0);
        save(value);
    }

    /** Save the value in the ini file, and #define a config value if it's true. */
    public void saveMaybeDefine(String name, JToggleButton rb, int value) {
        if (rb.isSelected()) {
            define(name, value);
        }
        save(rb.isSelected());
    }

    /** Save the value in the ini file, and #define two config values if it's true. */
    public void saveMaybeDefineMulti(JToggleButton rb, String name1, int value1, String name2, int value2) {
        if (rb.isSelected()) {
            define(name1, value1);
            define(name2, value2);
        }
        save(rb.isSelected());
    }
}
