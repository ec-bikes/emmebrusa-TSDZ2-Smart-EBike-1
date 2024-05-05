/*
This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.
This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software Foundation,
Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

/**
 *
 * @author stancecoke
 */
import util.CompileWorker;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.ListSelectionModel;

import components.IntField;
import components.NumberField;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JList;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Paths;
import java.util.Arrays;

public class TSDZ2_Configurator extends javax.swing.JFrame {
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        // If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
        // For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TSDZ2_Configurator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new TSDZ2_Configurator().setVisible(true);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    private static final String COMMITS_FILE = "commits.txt";
    private static final String CONFIG_H_FILE = "src/controller/config.h";
    private static final String EXPERIMENTAL_SETTINGS_DIR = "experimental settings";
    private static final String PROVEN_SETTINGS_DIR = "proven settings";

    private String rootPath;
    private File experimentalSettingsDir;
    private File lastSettingsFile = null;

    private DefaultListModel<FileContainer> provenSettingsFilesModel = new DefaultListModel<>();
    private DefaultListModel<FileContainer> experimentalSettingsFilesModel = new DefaultListModel<>();

    private CompileWorker compileWorker;
    private PrintWriter configWriter;
    private PrintWriter iniWriter;

    public class FileContainer {

        public FileContainer(File file) {
            this.file = file;
        }
        public final File file;

        @Override
        public String toString() {
            return file.getName();
        }

        public File getFile() {
            return file;
        }
    }

    private enum DisplayType {
        VLCD5, VLCD6, XH18, _850C("850C");
        private final String displayName;
        DisplayType() { this.displayName = name(); };
        DisplayType(String displayName) { this.displayName = displayName; }
        @Override
        public String toString() { return displayName; }
    }

    private static final String[] displayDataArray = {"motor temperature", "battery SOC rem. %", "battery voltage", "battery current", "motor power", "adc throttle 8b", "adc torque sensor 10b", "pedal cadence rpm", "human power", "adc pedal torque delta", "consumed Wh"};
    private static final String[] lightModeArray = {"<br>lights ON", "<br>lights FLASHING", "lights ON and BRAKE-FLASHING brak.", "lights FLASHING and ON when braking", "lights FLASHING BRAKE-FLASHING brak.", "lights ON and ON always braking", "lights ON and BRAKE-FLASHING alw.br.", "lights FLASHING and ON always braking", "lights FLASHING BRAKE-FLASHING alw.br.", "assist without pedal rotation", "assist with sensors error", "field weakening"};

    private static final int[] intAdcPedalTorqueAngleAdjArray = {160, 138, 120, 107, 96, 88, 80, 74, 70, 66, 63, 59, 56, 52, 50, 47, 44, 42, 39, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16};

    /** 0 is the street speed, 1 is the off-road max */
    public int[] intMaxSpeed = new int[2];
    /** 0 is the limit, 1-4 are values */
    public int[] intWalkSpeed = new int[5];
    /** 0 is the threshold, 1-4 are values */
    public int[] intCruiseSpeed = new int[5];
    public int intTorqueOffsetAdj;
    public int intTorqueRangeAdj;
    public int intTorqueAngleAdj;
    String strTorqueOffsetAdj;
    String strTorqueRangeAdj;
    String strTorqueAngleAdj;

    private static final int WEIGHT_ON_PEDAL = 25; // kg
    private static final int MIDDLE_OFFSET_ADJ = 20;
    private static final int OFFSET_MAX_VALUE = 34; // MIDDLE_OFFSET_ADJ * 2 - 6 (ADC_TORQUE_SENSOR_CALIBRATION_OFFSET)
    private static final int MIDDLE_RANGE_ADJ = 20;
    private static final int MIDDLE_ANGLE_ADJ = 20;

    public void loadSettings(File f) throws IOException {

        DisplayType displayType = DisplayType.VLCD5;

        try (BufferedReader in = new BufferedReader(new FileReader(f))) {
        RB_MOTOR_36V.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_MOTOR_48V.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_TORQUE_CALIBRATION.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_MOTOR_ACC.setIntStringValue(in.readLine());
        CB_ASS_WITHOUT_PED.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ASS_WITHOUT_PED_THRES.setIntStringValue(in.readLine());
        TF_TORQ_PER_ADC_STEP.setIntStringValue(in.readLine());
        TF_TORQUE_ADC_MAX.setIntStringValue(in.readLine());
        TF_BOOST_TORQUE_FACTOR.setIntStringValue(in.readLine());
        TF_MOTOR_BLOCK_TIME.setIntStringValue(in.readLine());
        TF_MOTOR_BLOCK_CURR.setIntStringValue(in.readLine());
        TF_MOTOR_BLOCK_ERPS.setIntStringValue(in.readLine());
        TF_BOOST_CADENCE_STEP.setIntStringValue(in.readLine());
        TF_BAT_CUR_MAX.setFloatStringValue(in.readLine());
        TF_BATT_POW_MAX.setIntStringValue(in.readLine());
        TF_BATT_CAPACITY.setIntStringValue(in.readLine());
        TF_BATT_NUM_CELLS.setIntStringValue(in.readLine());
        TF_MOTOR_DEC.setIntStringValue(in.readLine());
        TF_BATT_VOLT_CUT_OFF.setIntStringValue(in.readLine());
        TF_BATT_VOLT_CAL.setIntStringValue(in.readLine());
        TF_BATT_CAPACITY_CAL.setIntStringValue(in.readLine());
        TF_BAT_CELL_OVER.setFloatStringValue(in.readLine());
        TF_BAT_CELL_SOC.setFloatStringValue(in.readLine());
        TF_BAT_CELL_FULL.setFloatStringValue(in.readLine());
        TF_BAT_CELL_3_4.setFloatStringValue(in.readLine());
        TF_BAT_CELL_2_4.setFloatStringValue(in.readLine());
        TF_BAT_CELL_1_4.setFloatStringValue(in.readLine());
        TF_BAT_CELL_5_6.setFloatStringValue(in.readLine());
        TF_BAT_CELL_4_6.setFloatStringValue(in.readLine());
        TF_BAT_CELL_3_6.setFloatStringValue(in.readLine());
        TF_BAT_CELL_2_6.setFloatStringValue(in.readLine());
        TF_BAT_CELL_1_6.setFloatStringValue(in.readLine());
        TF_BAT_CELL_EMPTY.setFloatStringValue(in.readLine());
        TF_WHEEL_CIRCUMF.setIntStringValue(in.readLine());
        intMaxSpeed[0] = Integer.parseInt(in.readLine());
        CB_LIGHTS.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_WALK_ASSIST.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_BRAKE_SENSOR.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_ADC_OPTION_DIS.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_THROTTLE.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_TEMP_LIMIT.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STREET_MODE_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_SET_PARAM_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_ODO_COMPENSATION.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STARTUP_BOOST_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_TOR_SENSOR_ADV.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_LIGHT_MODE_ON_START.setIntStringValue(in.readLine());
        RB_POWER_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_TORQUE_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_CADENCE_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_EMTB_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_LIGHT_MODE_1.setIntStringValue(in.readLine());
        TF_LIGHT_MODE_2.setIntStringValue(in.readLine());
        TF_LIGHT_MODE_3.setIntStringValue(in.readLine());
        CB_STREET_POWER_LIM.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_STREET_POWER_LIM.setIntStringValue(in.readLine());
        intMaxSpeed[1] = Integer.parseInt(in.readLine());
        CB_STREET_THROTTLE.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STREET_CRUISE.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ADC_THROTTLE_MIN.setIntStringValue(in.readLine());
        TF_ADC_THROTTLE_MAX.setIntStringValue(in.readLine());
        TF_TEMP_MIN_LIM.setIntStringValue(in.readLine());
        TF_TEMP_MAX_LIM.setIntStringValue(in.readLine());
        CB_TEMP_ERR_MIN_LIM.setSelected(Boolean.parseBoolean(in.readLine()));
        displayType = Boolean.parseBoolean(in.readLine()) ? DisplayType.VLCD6 : displayType;
        displayType = Boolean.parseBoolean(in.readLine()) ? DisplayType.VLCD5 : displayType;
        displayType = Boolean.parseBoolean(in.readLine()) ? DisplayType.XH18 : displayType;
        RB_DISPLAY_WORK_ON.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_DISPLAY_ALWAY_ON.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_MAX_SPEED_DISPLAY.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_DELAY_MENU.setIntStringValue(in.readLine());
        CB_COASTER_BRAKE.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_COASTER_BRAKE_THRESHOLD.setIntStringValue(in.readLine());
        CB_AUTO_DISPLAY_DATA.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STARTUP_ASSIST_ENABLED.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_DELAY_DATA_1.setIntStringValue(in.readLine());
        TF_DELAY_DATA_2.setIntStringValue(in.readLine());
        TF_DELAY_DATA_3.setIntStringValue(in.readLine());
        TF_DELAY_DATA_4.setIntStringValue(in.readLine());
        TF_DELAY_DATA_5.setIntStringValue(in.readLine());
        TF_DELAY_DATA_6.setIntStringValue(in.readLine());
        TF_DATA_1.setIntStringValue(in.readLine());
        TF_DATA_2.setIntStringValue(in.readLine());
        TF_DATA_3.setIntStringValue(in.readLine());
        TF_DATA_4.setIntStringValue(in.readLine());
        TF_DATA_5.setIntStringValue(in.readLine());
        TF_DATA_6.setIntStringValue(in.readLine());
        TF_POWER_ASS_1.setIntStringValue(in.readLine());
        TF_POWER_ASS_2.setIntStringValue(in.readLine());
        TF_POWER_ASS_3.setIntStringValue(in.readLine());
        TF_POWER_ASS_4.setIntStringValue(in.readLine());
        TF_TORQUE_ASS_1.setIntStringValue(in.readLine());
        TF_TORQUE_ASS_2.setIntStringValue(in.readLine());
        TF_TORQUE_ASS_3.setIntStringValue(in.readLine());
        TF_TORQUE_ASS_4.setIntStringValue(in.readLine());
        TF_CADENCE_ASS_1.setIntStringValue(in.readLine());
        TF_CADENCE_ASS_2.setIntStringValue(in.readLine());
        TF_CADENCE_ASS_3.setIntStringValue(in.readLine());
        TF_CADENCE_ASS_4.setIntStringValue(in.readLine());
        TF_EMTB_ASS_1.setIntStringValue(in.readLine());
        TF_EMTB_ASS_2.setIntStringValue(in.readLine());
        TF_EMTB_ASS_3.setIntStringValue(in.readLine());
        TF_EMTB_ASS_4.setIntStringValue(in.readLine());
        intWalkSpeed[1] = Integer.parseInt(in.readLine());
        intWalkSpeed[2] = Integer.parseInt(in.readLine());
        intWalkSpeed[3] = Integer.parseInt(in.readLine());
        intWalkSpeed[4] = Integer.parseInt(in.readLine());
        intWalkSpeed[0] = Integer.parseInt(in.readLine());
        CB_WALK_TIME_ENA.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_WALK_ASS_TIME.setIntStringValue(in.readLine());
        intCruiseSpeed[1] = Integer.parseInt(in.readLine());
        intCruiseSpeed[2] = Integer.parseInt(in.readLine());
        intCruiseSpeed[3] = Integer.parseInt(in.readLine());
        intCruiseSpeed[4] = Integer.parseInt(in.readLine());
        CB_CRUISE_WHITOUT_PED.setSelected(Boolean.parseBoolean(in.readLine()));
        intCruiseSpeed[0] = Integer.parseInt(in.readLine());
        TF_TORQ_ADC_OFFSET.setIntStringValue(in.readLine());
        TF_NUM_DATA_AUTO_DISPLAY.setIntStringValue(in.readLine());
        RB_UNIT_KILOMETERS.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_UNIT_MILES.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ASSIST_THROTTLE_MIN.setIntStringValue(in.readLine());
        TF_ASSIST_THROTTLE_MAX.setIntStringValue(in.readLine());
        CB_STREET_WALK.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_HYBRID_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_NONE.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_SOC.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_VOLTS.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_FIELD_WEAKENING_ENABLED.setSelected(Boolean.parseBoolean(in.readLine()));
        strTorqueOffsetAdj = in.readLine();
        strTorqueRangeAdj = in.readLine();
        strTorqueAngleAdj = in.readLine();
        TF_TORQ_PER_ADC_STEP_ADV.setIntStringValue(in.readLine());
        RB_SOC_AUTO.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_SOC_WH.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_SOC_VOLTS.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_ADC_STEP_ESTIM.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_BOOST_AT_ZERO_CADENCE.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_BOOST_AT_ZERO_SPEED.setSelected(Boolean.parseBoolean(in.readLine()));
        displayType = Boolean.parseBoolean(in.readLine()) ? DisplayType._850C : displayType;
        CB_THROTTLE_LEGAL.setSelected(Boolean.parseBoolean(in.readLine()));
        }

        CMB_DISPLAY_TYPE.setSelectedItem(displayType);
        setBatteryFieldsEnabled(displayType);

        setWalkAssistFieldsEnabled();
        setLightsFieldsEnabled();
        setMaxSpeedOffroadEnabled();
        setStreetPowerLimitEnabled();
        setAssistWithoutPedThresholdEnabled();
        setAdcOptionFieldsEnabled();
        setBrakeSensorFieldsEnabled();

        updateTorqueCalibrationValues();

        intTorqueOffsetAdj = strTorqueOffsetAdj == null ? MIDDLE_OFFSET_ADJ : Integer.parseInt(strTorqueOffsetAdj);
        TF_TORQ_ADC_OFFSET_ADJ.setValue(intTorqueOffsetAdj - MIDDLE_OFFSET_ADJ);

        intTorqueRangeAdj = strTorqueRangeAdj == null ? MIDDLE_RANGE_ADJ : Integer.parseInt(strTorqueRangeAdj);
        TF_TORQ_ADC_RANGE_ADJ.setValue(intTorqueRangeAdj - MIDDLE_RANGE_ADJ);

        intTorqueAngleAdj = strTorqueAngleAdj == null ? MIDDLE_ANGLE_ADJ : Integer.parseInt(strTorqueAngleAdj);
        TF_TORQ_ADC_ANGLE_ADJ.setValue(intTorqueAngleAdj - MIDDLE_ANGLE_ADJ);

        jLabel_MOTOR_BLOCK_CURR.setVisible(false);
        jLabel_MOTOR_BLOCK_ERPS.setVisible(false);
        TF_MOTOR_BLOCK_CURR.setVisible(false);
        TF_MOTOR_BLOCK_ERPS.setVisible(false);

        updateDataLabel(TF_DATA_1, jLabelData1, 1);
        updateDataLabel(TF_DATA_2, jLabelData2, 2);
        updateDataLabel(TF_DATA_3, jLabelData3, 3);
        updateDataLabel(TF_DATA_4, jLabelData4, 4);
        updateDataLabel(TF_DATA_5, jLabelData5, 5);
        updateDataLabel(TF_DATA_6, jLabelData6, 6);

        updateLightsFieldLabel(TF_LIGHT_MODE_ON_START, jLabel_LIGHT_MODE_ON_START, 0);
        updateLightsFieldLabel(TF_LIGHT_MODE_1, jLabel_LIGHT_MODE_1, 1);
        updateLightsFieldLabel(TF_LIGHT_MODE_2, jLabel_LIGHT_MODE_2, 2);
        updateLightsFieldLabel(TF_LIGHT_MODE_3, jLabel_LIGHT_MODE_3, 3);

        updateUnits();
    }

    public void AddListItem(File newFile) {

        experimentalSettingsFilesModel.add(0, new FileContainer(newFile));

        expSet.setSelectedIndex(0);
        expSet.repaint();
    }

    public TSDZ2_Configurator() {

        initComponents();

        this.setLocationRelativeTo(null);

        // Try to find the git repo root
        File currentDir = new File(Paths.get(".").toAbsolutePath().normalize().toString());
        File rootDir = currentDir;

        while (rootDir != null && !Arrays.asList(rootDir.list()).contains(CompileWorker.COMPILE_SCRIPT_SH)) {
            rootDir = rootDir.getParentFile();
        }
        if (rootDir == null) {
            JOptionPane.showMessageDialog(this, "Could not find the root path of the OSF git repo.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        rootPath = rootDir.getAbsolutePath();

        // Update the latest commit
        File commitsFile = new File(Paths.get(rootPath, COMMITS_FILE).toString());
        if (commitsFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(commitsFile.getAbsolutePath()));
                LB_LAST_COMMIT.setText("<html>" + br.readLine() + "</html>");
                br.close();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.toString());
            }
        }


        // Read the proven settings
        populateSettingsList(provSet, provenSettingsFilesModel, PROVEN_SETTINGS_DIR);

        // Read the experimental settings
        populateSettingsList(expSet, experimentalSettingsFilesModel, EXPERIMENTAL_SETTINGS_DIR);
        expSet.setSelectedIndex(0);

        BTN_COMPILE.addActionListener((ActionEvent arg0) -> {
            BTN_COMPILE.setEnabled(false);

            String newFileName = new SimpleDateFormat("yyyyMMdd-HHmmssz").format(new Date()) + ".ini";
            String newFilePath = Paths.get(experimentalSettingsDir.getAbsolutePath(), newFileName).toString();
            String configHPath = Paths.get(rootPath, CONFIG_H_FILE).toString();
            TA_COMPILE_OUTPUT.setText("Writing settings to " + newFilePath + " and " + configHPath);

            File newFile = new File(newFilePath);
            AddListItem(newFile);

            try (PrintWriter iniWriter = new PrintWriter(new BufferedWriter(new FileWriter(newFile)));
                    PrintWriter configWriter = new PrintWriter(new BufferedWriter(new FileWriter(configHPath)))) {
                this.configWriter = configWriter;
                this.iniWriter = iniWriter;

                configWriter.println("/*\n"
                        + " * config.h\n"
                        + " *\n"
                        + " *  Automatically created by TSDS2 Parameter Configurator\n"
                        + " *  Author: stancecoke\n"
                        + " */\n"
                        + "\n"
                        + "#ifndef CONFIG_H_\n"
                        + "#define CONFIG_H_\n");

                saveAndDefineField("MOTOR_TYPE", RB_MOTOR_36V);
                iniWriter.println(RB_MOTOR_48V.isSelected());
                saveAndDefineField("TORQUE_SENSOR_CALIBRATED", CB_TORQUE_CALIBRATION);
                saveAndDefineField("MOTOR_ACCELERATION", TF_MOTOR_ACC);
                saveAndDefineField("MOTOR_ASSISTANCE_WITHOUT_PEDAL_ROTATION", CB_ASS_WITHOUT_PED);
                saveAndDefineField("ASSISTANCE_WITHOUT_PEDAL_ROTATION_THRESHOLD", TF_ASS_WITHOUT_PED_THRES);
                saveAndDefineField("PEDAL_TORQUE_PER_10_BIT_ADC_STEP_X100", TF_TORQ_PER_ADC_STEP);
                saveAndDefineField("PEDAL_TORQUE_ADC_MAX", TF_TORQUE_ADC_MAX);
                saveAndDefineField("STARTUP_BOOST_TORQUE_FACTOR", TF_BOOST_TORQUE_FACTOR);
                saveAndDefineField("MOTOR_BLOCKED_COUNTER_THRESHOLD", TF_MOTOR_BLOCK_TIME);
                saveAndDefineField("MOTOR_BLOCKED_BATTERY_CURRENT_THRESHOLD_X10", TF_MOTOR_BLOCK_CURR);
                saveAndDefineField("MOTOR_BLOCKED_ERPS_THRESHOLD", TF_MOTOR_BLOCK_ERPS);
                saveAndDefineField("STARTUP_BOOST_CADENCE_STEP", TF_BOOST_CADENCE_STEP);
                saveAndDefineField("BATTERY_CURRENT_MAX", TF_BAT_CUR_MAX);
                saveAndDefineField("TARGET_MAX_BATTERY_POWER", TF_BATT_POW_MAX);
                saveAndDefineField("TARGET_MAX_BATTERY_CAPACITY", TF_BATT_CAPACITY);
                saveAndDefineField("BATTERY_CELLS_NUMBER", TF_BATT_NUM_CELLS);
                saveAndDefineField("MOTOR_DECELERATION", TF_MOTOR_DEC);
                saveAndDefineField("BATTERY_LOW_VOLTAGE_CUT_OFF", TF_BATT_VOLT_CUT_OFF);
                saveAndDefineField("ACTUAL_BATTERY_VOLTAGE_PERCENT", TF_BATT_VOLT_CAL);
                saveAndDefineField("ACTUAL_BATTERY_CAPACITY_PERCENT", TF_BATT_CAPACITY_CAL);
                saveAndDefineField("LI_ION_CELL_OVERVOLT", TF_BAT_CELL_OVER);
                saveAndDefineField("LI_ION_CELL_RESET_SOC_PERCENT", TF_BAT_CELL_SOC);
                saveAndDefineField("LI_ION_CELL_VOLTS_FULL", TF_BAT_CELL_FULL);
                saveAndDefineField("LI_ION_CELL_VOLTS_3_OF_4", TF_BAT_CELL_3_4);
                saveAndDefineField("LI_ION_CELL_VOLTS_2_OF_4", TF_BAT_CELL_2_4);
                saveAndDefineField("LI_ION_CELL_VOLTS_1_OF_4", TF_BAT_CELL_1_4);
                saveAndDefineField("LI_ION_CELL_VOLTS_5_OF_6", TF_BAT_CELL_5_6);
                saveAndDefineField("LI_ION_CELL_VOLTS_4_OF_6", TF_BAT_CELL_4_6);
                saveAndDefineField("LI_ION_CELL_VOLTS_3_OF_6", TF_BAT_CELL_3_6);
                saveAndDefineField("LI_ION_CELL_VOLTS_2_OF_6", TF_BAT_CELL_2_6);
                saveAndDefineField("LI_ION_CELL_VOLTS_1_OF_6", TF_BAT_CELL_1_6);
                saveAndDefineField("LI_ION_CELL_VOLTS_EMPTY", TF_BAT_CELL_EMPTY);
                saveAndDefineField("WHEEL_PERIMETER", TF_WHEEL_CIRCUMF);
                saveAndDefineField("WHEEL_MAX_SPEED", intMaxSpeed[0]);
                saveAndDefineField("ENABLE_LIGHTS", CB_LIGHTS);
                saveAndDefineField("ENABLE_WALK_ASSIST", CB_WALK_ASSIST);
                saveAndDefineField("ENABLE_BRAKE_SENSOR", CB_BRAKE_SENSOR);

                // these are three mutually exclusive fields
                saveMaybeDefineFields(RB_ADC_OPTION_DIS, "ENABLE_THROTTLE", 0, "ENABLE_TEMPERATURE_LIMIT", 0);
                saveMaybeDefineFields(RB_THROTTLE, "ENABLE_THROTTLE", 1, "ENABLE_TEMPERATURE_LIMIT", 0);
                saveMaybeDefineFields(RB_TEMP_LIMIT, "ENABLE_THROTTLE", 0, "ENABLE_TEMPERATURE_LIMIT", 1);

                saveAndDefineField("ENABLE_STREET_MODE_ON_STARTUP", CB_STREET_MODE_ON_START);
                saveAndDefineField("ENABLE_SET_PARAMETER_ON_STARTUP", CB_SET_PARAM_ON_START);
                saveAndDefineField("ENABLE_ODOMETER_COMPENSATION", CB_ODO_COMPENSATION);
                saveAndDefineField("STARTUP_BOOST_ON_STARTUP", CB_STARTUP_BOOST_ON_START);
                saveAndDefineField("TORQUE_SENSOR_ADV_ON_STARTUP", CB_TOR_SENSOR_ADV);
                saveAndDefineField("LIGHTS_CONFIGURATION_ON_STARTUP", TF_LIGHT_MODE_ON_START);
                saveMaybeDefineField("RIDING_MODE_ON_STARTUP", RB_POWER_ON_START, 1);
                saveMaybeDefineField("RIDING_MODE_ON_STARTUP", RB_TORQUE_ON_START, 2);
                saveMaybeDefineField("RIDING_MODE_ON_STARTUP", RB_CADENCE_ON_START, 3);
                saveMaybeDefineField("RIDING_MODE_ON_STARTUP", RB_EMTB_ON_START, 4);
                saveAndDefineField("LIGHTS_CONFIGURATION_1", TF_LIGHT_MODE_1);
                saveAndDefineField("LIGHTS_CONFIGURATION_2", TF_LIGHT_MODE_2);
                saveAndDefineField("LIGHTS_CONFIGURATION_3", TF_LIGHT_MODE_3);
                saveAndDefineField("STREET_MODE_POWER_LIMIT_ENABLED", CB_STREET_POWER_LIM);
                saveAndDefineField("STREET_MODE_POWER_LIMIT", TF_STREET_POWER_LIM);
                saveAndDefineField("STREET_MODE_SPEED_LIMIT", intMaxSpeed[1]);
                saveAndDefineField("STREET_MODE_THROTTLE_ENABLED", CB_STREET_THROTTLE);
                saveAndDefineField("STREET_MODE_CRUISE_ENABLED", CB_STREET_CRUISE);
                saveAndDefineField("ADC_THROTTLE_MIN_VALUE", TF_ADC_THROTTLE_MIN);
                saveAndDefineField("ADC_THROTTLE_MAX_VALUE", TF_ADC_THROTTLE_MAX);
                saveAndDefineField("MOTOR_TEMPERATURE_MIN_VALUE_LIMIT", TF_TEMP_MIN_LIM);
                saveAndDefineField("MOTOR_TEMPERATURE_MAX_VALUE_LIMIT", TF_TEMP_MAX_LIM);
                saveAndDefineField("ENABLE_TEMPERATURE_ERROR_MIN_LIMIT", CB_TEMP_ERR_MIN_LIM);

                DisplayType displayType = (DisplayType) CMB_DISPLAY_TYPE.getSelectedItem();
                saveAndDefineField("ENABLE_VLCD6", displayType == DisplayType.VLCD6);
                saveAndDefineField("ENABLE_VLCD5", displayType == DisplayType.VLCD5);
                saveAndDefineField("ENABLE_XH18", displayType == DisplayType.XH18);

                saveAndDefineField("ENABLE_DISPLAY_WORKING_FLAG", RB_DISPLAY_WORK_ON);
                saveAndDefineField("ENABLE_DISPLAY_ALWAYS_ON", RB_DISPLAY_ALWAY_ON);
                saveAndDefineField("ENABLE_WHEEL_MAX_SPEED_FROM_DISPLAY", CB_MAX_SPEED_DISPLAY);
                saveAndDefineField("DELAY_MENU_ON", TF_DELAY_MENU);
                saveAndDefineField("COASTER_BRAKE_ENABLED", CB_COASTER_BRAKE);
                saveAndDefineField("COASTER_BRAKE_TORQUE_THRESHOLD", TF_COASTER_BRAKE_THRESHOLD);
                saveAndDefineField("ENABLE_AUTO_DATA_DISPLAY", CB_AUTO_DISPLAY_DATA);
                saveAndDefineField("STARTUP_ASSIST_ENABLED", CB_STARTUP_ASSIST_ENABLED);
                saveAndDefineField("DELAY_DISPLAY_DATA_1", TF_DELAY_DATA_1);
                saveAndDefineField("DELAY_DISPLAY_DATA_2", TF_DELAY_DATA_2);
                saveAndDefineField("DELAY_DISPLAY_DATA_3", TF_DELAY_DATA_3);
                saveAndDefineField("DELAY_DISPLAY_DATA_4", TF_DELAY_DATA_4);
                saveAndDefineField("DELAY_DISPLAY_DATA_5", TF_DELAY_DATA_5);
                saveAndDefineField("DELAY_DISPLAY_DATA_6", TF_DELAY_DATA_6);
                saveAndDefineField("DISPLAY_DATA_1", TF_DATA_1);
                saveAndDefineField("DISPLAY_DATA_2", TF_DATA_2);
                saveAndDefineField("DISPLAY_DATA_3", TF_DATA_3);
                saveAndDefineField("DISPLAY_DATA_4", TF_DATA_4);
                saveAndDefineField("DISPLAY_DATA_5", TF_DATA_5);
                saveAndDefineField("DISPLAY_DATA_6", TF_DATA_6);
                saveAndDefineField("POWER_ASSIST_LEVEL_1", TF_POWER_ASS_1);
                saveAndDefineField("POWER_ASSIST_LEVEL_2", TF_POWER_ASS_2);
                saveAndDefineField("POWER_ASSIST_LEVEL_3", TF_POWER_ASS_3);
                saveAndDefineField("POWER_ASSIST_LEVEL_4", TF_POWER_ASS_4);
                saveAndDefineField("TORQUE_ASSIST_LEVEL_1", TF_TORQUE_ASS_1);
                saveAndDefineField("TORQUE_ASSIST_LEVEL_2", TF_TORQUE_ASS_2);
                saveAndDefineField("TORQUE_ASSIST_LEVEL_3", TF_TORQUE_ASS_3);
                saveAndDefineField("TORQUE_ASSIST_LEVEL_4", TF_TORQUE_ASS_4);
                saveAndDefineField("CADENCE_ASSIST_LEVEL_1", TF_CADENCE_ASS_1);
                saveAndDefineField("CADENCE_ASSIST_LEVEL_2", TF_CADENCE_ASS_2);
                saveAndDefineField("CADENCE_ASSIST_LEVEL_3", TF_CADENCE_ASS_3);
                saveAndDefineField("CADENCE_ASSIST_LEVEL_4", TF_CADENCE_ASS_4);
                saveAndDefineField("EMTB_ASSIST_LEVEL_1", TF_EMTB_ASS_1);
                saveAndDefineField("EMTB_ASSIST_LEVEL_2", TF_EMTB_ASS_2);
                saveAndDefineField("EMTB_ASSIST_LEVEL_3", TF_EMTB_ASS_3);
                saveAndDefineField("EMTB_ASSIST_LEVEL_4", TF_EMTB_ASS_4);
                saveAndDefineField("WALK_ASSIST_LEVEL_1", intWalkSpeed[1]);
                saveAndDefineField("WALK_ASSIST_LEVEL_2", intWalkSpeed[2]);
                saveAndDefineField("WALK_ASSIST_LEVEL_3", intWalkSpeed[3]);
                saveAndDefineField("WALK_ASSIST_LEVEL_4", intWalkSpeed[4]);
                saveAndDefineField("WALK_ASSIST_THRESHOLD_SPEED", intWalkSpeed[0]);
                saveAndDefineField("WALK_ASSIST_DEBOUNCE_ENABLED", CB_WALK_TIME_ENA);
                saveAndDefineField("WALK_ASSIST_DEBOUNCE_TIME", TF_WALK_ASS_TIME);
                saveAndDefineField("CRUISE_TARGET_SPEED_LEVEL_1", intCruiseSpeed[1]);
                saveAndDefineField("CRUISE_TARGET_SPEED_LEVEL_2", intCruiseSpeed[2]);
                saveAndDefineField("CRUISE_TARGET_SPEED_LEVEL_3", intCruiseSpeed[3]);
                saveAndDefineField("CRUISE_TARGET_SPEED_LEVEL_4", intCruiseSpeed[4]);
                saveAndDefineField("CRUISE_MODE_WALK_ENABLED", CB_CRUISE_WHITOUT_PED);
                saveAndDefineField("CRUISE_THRESHOLD_SPEED", intCruiseSpeed[0]);
                saveAndDefineField("PEDAL_TORQUE_ADC_OFFSET", TF_TORQ_ADC_OFFSET);
                saveAndDefineField("AUTO_DATA_NUMBER_DISPLAY", TF_NUM_DATA_AUTO_DISPLAY);
                saveMaybeDefineField("UNITS_TYPE", RB_UNIT_KILOMETERS, 0);
                saveMaybeDefineField("UNITS_TYPE", RB_UNIT_MILES, 1);
                saveAndDefineField("ASSIST_THROTTLE_MIN_VALUE", TF_ASSIST_THROTTLE_MIN);
                saveAndDefineField("ASSIST_THROTTLE_MAX_VALUE", TF_ASSIST_THROTTLE_MAX);
                saveAndDefineField("STREET_MODE_WALK_ENABLED", CB_STREET_WALK);
                saveMaybeDefineField("RIDING_MODE_ON_STARTUP", RB_HYBRID_ON_START, 5);
                saveMaybeDefineField("DATA_DISPLAY_ON_STARTUP", RB_STARTUP_NONE, 0);
                saveMaybeDefineField("DATA_DISPLAY_ON_STARTUP", RB_STARTUP_SOC, 1);
                saveMaybeDefineField("DATA_DISPLAY_ON_STARTUP", RB_STARTUP_VOLTS, 2);
                saveAndDefineField("FIELD_WEAKENING_ENABLED", CB_FIELD_WEAKENING_ENABLED);
                saveAndDefineField("PEDAL_TORQUE_ADC_OFFSET_ADJ", intTorqueOffsetAdj);
                saveAndDefineField("PEDAL_TORQUE_ADC_RANGE_ADJ", intTorqueRangeAdj);
                defineField("PEDAL_TORQUE_ADC_ANGLE_ADJ", intAdcPedalTorqueAngleAdjArray[intTorqueAngleAdj]);
                iniWriter.println(intTorqueAngleAdj);
                saveAndDefineField("PEDAL_TORQUE_PER_10_BIT_ADC_STEP_ADV_X100", TF_TORQ_PER_ADC_STEP_ADV);
                saveMaybeDefineField("SOC_PERCENT_CALC", RB_SOC_AUTO, 0);
                saveMaybeDefineField("SOC_PERCENT_CALC", RB_SOC_WH, 1);
                saveMaybeDefineField("SOC_PERCENT_CALC", RB_SOC_VOLTS, 2);
                iniWriter.println(CB_ADC_STEP_ESTIM.isSelected());
                saveMaybeDefineField("STARTUP_BOOST_AT_ZERO", RB_BOOST_AT_ZERO_CADENCE, 0);
                saveMaybeDefineField("STARTUP_BOOST_AT_ZERO", RB_BOOST_AT_ZERO_SPEED, 1);
                saveAndDefineField("ENABLE_850C", displayType == DisplayType._850C);
                saveAndDefineField("STREET_MODE_THROTTLE_LEGAL", CB_THROTTLE_LEGAL);

                configWriter.println("\n#endif /* CONFIG_H_ */");
            } catch (IOException ioe) {
                ioe.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error writing settings to file: " + ioe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } finally {
                this.configWriter = null;
                this.iniWriter = null;
            }

            compileAndFlash(newFileName);
        });

        if (lastSettingsFile != null) {
            try {
                loadSettings(lastSettingsFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, ex.getMessage());
            }
            provSet.clearSelection();
            expSet.clearSelection();
        }
    }

    private void compileAndFlash(String fileName) {
        BTN_CANCEL.setEnabled(true);

        compileWorker = new CompileWorker(TA_COMPILE_OUTPUT, fileName, rootPath);
        compileWorker.execute();

        compileWorker.addPropertyChangeListener(
                new PropertyChangeListener() {
            boolean handled = false;

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (!handled && compileWorker != null && compileWorker.isDone()) {
                    handled = true;
                    compileDone();
                }
            }
        });
    }

    private void compileDone() {
        compileWorker = null;
        BTN_COMPILE.setEnabled(true);
        BTN_CANCEL.setEnabled(false);
    }

    /** Populate a list with settings files and configure the component. */
    private void populateSettingsList(JList<FileContainer> list, DefaultListModel<FileContainer> model, String dir) {
        // Read the files
        File settingsDir = new File(Paths.get(rootPath, dir).toString());
        File[] files = settingsDir.listFiles();
        Arrays.sort(files);
        for (File file : files) {
            model.addElement(new FileContainer(file));

            if (lastSettingsFile == null) {
                lastSettingsFile = file;
            } else if (file.lastModified() > lastSettingsFile.lastModified()) {
                lastSettingsFile = file;
            }
        }

        // Configure the list
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setLayoutOrientation(JList.VERTICAL);
        list.setVisibleRowCount(-1);
        list.setModel(model);

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedIndex = list.getSelectedIndex();
                list.setSelectedIndex(selectedIndex);
                // clear the other list's selection
                (list == provSet ? expSet : provSet).clearSelection();
                try {
                    loadSettings(list.getSelectedValue().file);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    list.clearSelection();
                }
            }
        });
    }

    private void saveAndDefineField(String name, JToggleButton rb) {
        saveAndDefineField(name, rb.isSelected());
    }

    private void saveAndDefineField(String name, NumberField<?> txt) {
        String strValue = txt.getValue().toString();
        defineField(name, strValue);
        iniWriter.println(strValue);
    }

    private void saveAndDefineField(String name, int value) {
        defineField(name, value);
        iniWriter.println(value);
    }

    private void saveAndDefineField(String name, boolean value) {
        defineField(name, value ? 1 : 0);
        iniWriter.println(value);
    }

    /** Save the value in the ini file, and #define a config value if it's true. */
    private void saveMaybeDefineField(String name, JToggleButton rb, int value) {
        if (rb.isSelected()) {
            defineField(name, value);
        }
        iniWriter.println(rb.isSelected());
    }

    /** Save the value in the ini file, and #define two config values if it's true. */
    private void saveMaybeDefineFields(JToggleButton rb, String name1, int value1, String name2, int value2) {
        if (rb.isSelected()) {
            defineField(name1, value1);
            defineField(name2, value2);
        }
        iniWriter.println(rb.isSelected());
    }

    /** Only #define a config value for the field (don't save in ini). */
    private void defineField(String name, Object value) {
        this.configWriter.println("#define " + name + " " + value);
    }

    /**
     * val is stored in km. If isKm is false, convert to miles.
     */
    private int valueInUnits(int val, boolean isKm) {
        return isKm ? val : (val * 10 + 5) / 16;
    }

    private boolean isKm() {
        return RB_UNIT_KILOMETERS.isSelected();
    }

    private void saveFieldInKm(IntField field, int[] values, int index) {
        values[index] = valueInUnits(field.getIntValue(), isKm());
    }

    private int handleFieldValueWithAdjustment(IntField field, int adjust) {
        String strValue = field.getText();
        Integer intObj = safeParseInt(strValue);
        int value = intObj != null ? intObj.intValue() : 0;
        if (intObj != null && value >= 0 && value <= adjust) {
            return value + adjust;
        }
        field.setValue(0);
        return adjust;
    }

    // TODO use min/max
    /** Update a field if its value is outside the min or max. */
    private void ensureFieldInRange(IntField field, int min, int max) {
        Integer intObj = safeParseInt(field.getText());
        if (intObj == null) {
        } else {
            int value = intObj.intValue();
            if (value < min) {
                field.setValue(min);
            } else if (value > max) {
                field.setValue(max);
            }
        }
    }

    private Integer safeParseInt(String str) {
        try {
            return Integer.valueOf(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateTorqueCalibrationValues() {
        boolean selected = CB_TORQUE_CALIBRATION.isSelected();
        TF_TORQ_ADC_OFFSET.setEnabled(selected);
        TF_TORQ_ADC_RANGE_ADJ.setEnabled(selected);
        TF_TORQ_ADC_ANGLE_ADJ.setEnabled(selected);
        TF_TORQUE_ADC_MAX.setEnabled(selected);
        CB_ADC_STEP_ESTIM.setEnabled(selected);
        TF_TORQ_PER_ADC_STEP_ADV.setEnabled(selected);
        TF_TORQ_PER_ADC_STEP.setEnabled(!selected);

        if (selected) {
            int intTorqueAdcOffset = Integer.parseInt(TF_TORQ_ADC_OFFSET.getText());
            int intTorqueAdcMax = Integer.parseInt(TF_TORQUE_ADC_MAX.getText());
            int intTorqueAdcOnWeight = intTorqueAdcOffset + ((intTorqueAdcMax - intTorqueAdcOffset) * 75) / 100;
            int intTorqueAdcStepCalc = (WEIGHT_ON_PEDAL * 167) / (intTorqueAdcOnWeight - intTorqueAdcOffset);
            TF_TORQ_PER_ADC_STEP.setValue(intTorqueAdcStepCalc);
        }
    }

    private void updateDataLabel(JTextField field, JLabel label, int dataNum) {
        try {
            int value = Integer.parseInt(field.getText());
            if (dataNum >= 0 && dataNum <= 10) {
                label.setText(String.format("Data %d - %s", dataNum, displayDataArray[value]));
            } else {
                label.setText("Data " + dataNum);
            }
        } catch (NumberFormatException ex) {
            label.setText("Data " + dataNum);
        }
    }

    private void updateLightsFieldLabel(JTextField field, JLabel label, int modeNum) {
        int max = modeNum == 2 ? 9 : 8; // not sure why this was different
        int extra = modeNum == 3 ? 10 : Integer.MAX_VALUE;
        String name = modeNum == 0 ? "Light mode on start" : "Light mode " + modeNum;
        try {
            int index = Integer.parseInt(field.getText());
            if ((index >= 0 && index <= max) || index == extra) {
                label.setText(String.format("<html>%s - %s</html>", name, lightModeArray[index]));
            } else {
                label.setText(name);
            }
        } catch (NumberFormatException ex) {
            label.setText(name);
        }
    }

    private void setAdcOptionFieldsEnabled() {
        boolean isThrottle = RB_THROTTLE.isSelected();
        boolean isTempLimit = RB_TEMP_LIMIT.isSelected();

        TF_ADC_THROTTLE_MIN.setEnabled(isThrottle);
        TF_ADC_THROTTLE_MAX.setEnabled(isThrottle);
        TF_ASSIST_THROTTLE_MIN.setEnabled(isThrottle);
        TF_ASSIST_THROTTLE_MAX.setEnabled(isThrottle);

        TF_TEMP_MIN_LIM.setEnabled(isTempLimit);
        TF_TEMP_MAX_LIM.setEnabled(isTempLimit);
        CB_TEMP_ERR_MIN_LIM.setEnabled(isTempLimit);
        CB_TEMP_ERR_MIN_LIM.setEnabled(isTempLimit);
    }

    /** Enables/disables fields walk assist time, cruise without pedaling, CB walk debounce time, throttle option */
    private void setBrakeSensorFieldsEnabled() {
        boolean brakeSensorSelected = CB_BRAKE_SENSOR.isSelected();
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && brakeSensorSelected && CB_WALK_ASSIST.isSelected());
        CB_CRUISE_WHITOUT_PED.setEnabled(brakeSensorSelected);
        CB_WALK_TIME_ENA.setEnabled(brakeSensorSelected && CB_WALK_ASSIST.isSelected());

        boolean coasterBrakeSelected = CB_COASTER_BRAKE.isSelected();
        boolean isThrottleEnabled = brakeSensorSelected && !coasterBrakeSelected;
        if (!isThrottleEnabled && RB_THROTTLE.isSelected()) {
            RB_ADC_OPTION_DIS.setSelected(true);
        }
        RB_THROTTLE.setEnabled(isThrottleEnabled);
        CB_THROTTLE_LEGAL.setEnabled(isThrottleEnabled);
        TF_COASTER_BRAKE_THRESHOLD.setEnabled(coasterBrakeSelected);
    }

    /** Enable/disable battery fraction text fields based on whether this is the VLCD5 or not */
    private void setBatteryFieldsEnabled(DisplayType selectedItem) {
        boolean isSixBars = selectedItem == DisplayType.VLCD5 || selectedItem == DisplayType._850C;
        TF_BAT_CELL_5_6.setEnabled(isSixBars);
        TF_BAT_CELL_4_6.setEnabled(isSixBars);
        TF_BAT_CELL_3_6.setEnabled(isSixBars);
        TF_BAT_CELL_2_6.setEnabled(isSixBars);
        TF_BAT_CELL_1_6.setEnabled(isSixBars);
        TF_BAT_CELL_3_4.setEnabled(!isSixBars);
        TF_BAT_CELL_2_4.setEnabled(!isSixBars);
        TF_BAT_CELL_1_4.setEnabled(!isSixBars);
    }

    private void updateUnits() {
        boolean isKm = this.isKm();
        String unitsStr = isKm ? "km/h" : "mph";
        jLabel_MAX_SPEED.setText("Max speed offroad mode (" + unitsStr + ")");
        jLabel_STREET_SPEED_LIM.setText("Street speed limit (" + unitsStr + ")");
        jLabelCruiseSpeedUnits.setText(unitsStr);
        jLabelWalkSpeedUnits.setText(unitsStr + "x10");
        TF_MAX_SPEED.setValue(valueInUnits(intMaxSpeed[0], isKm));
        TF_STREET_SPEED_LIM.setValue(valueInUnits(intMaxSpeed[1], isKm));
        TF_WALK_ASS_SPEED_1.setValue(valueInUnits(intWalkSpeed[1], isKm));
        TF_WALK_ASS_SPEED_2.setValue(valueInUnits(intWalkSpeed[2], isKm));
        TF_WALK_ASS_SPEED_3.setValue(valueInUnits(intWalkSpeed[3], isKm));
        TF_WALK_ASS_SPEED_4.setValue(valueInUnits(intWalkSpeed[4], isKm));
        TF_WALK_ASS_SPEED_LIMIT.setValue(valueInUnits(intWalkSpeed[0], isKm));
        TF_CRUISE_SPEED_ENA.setValue(valueInUnits(intCruiseSpeed[0], isKm));
        TF_CRUISE_ASS_1.setValue(valueInUnits(intCruiseSpeed[1], isKm));
        TF_CRUISE_ASS_2.setValue(valueInUnits(intCruiseSpeed[2], isKm));
        TF_CRUISE_ASS_3.setValue(valueInUnits(intCruiseSpeed[3], isKm));
        TF_CRUISE_ASS_4.setValue(valueInUnits(intCruiseSpeed[4], isKm));
    }

    private void setWalkAssistFieldsEnabled() {
        boolean enabled = CB_WALK_ASSIST.isSelected();
        TF_WALK_ASS_SPEED_1.setEnabled(enabled);
        TF_WALK_ASS_SPEED_2.setEnabled(enabled);
        TF_WALK_ASS_SPEED_3.setEnabled(enabled);
        TF_WALK_ASS_SPEED_4.setEnabled(enabled);
        TF_WALK_ASS_SPEED_LIMIT.setEnabled(enabled);
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && CB_BRAKE_SENSOR.isSelected() && enabled);
        CB_WALK_TIME_ENA.setEnabled(CB_BRAKE_SENSOR.isSelected() && enabled);
        CB_STREET_WALK.setEnabled(enabled);
    }

    private void setLightsFieldsEnabled() {
        boolean enabled = CB_LIGHTS.isSelected();
        TF_LIGHT_MODE_ON_START.setEnabled(enabled);
        TF_LIGHT_MODE_1.setEnabled(enabled);
        TF_LIGHT_MODE_2.setEnabled(enabled);
        TF_LIGHT_MODE_3.setEnabled(enabled);
    }

    private void setMaxSpeedOffroadEnabled() {
        TF_MAX_SPEED.setEnabled(!CB_MAX_SPEED_DISPLAY.isSelected());
    }

    private void setStreetPowerLimitEnabled() {
        TF_STREET_POWER_LIM.setEnabled(CB_STREET_POWER_LIM.isSelected());
    }

    private void setAssistWithoutPedThresholdEnabled() {
        TF_ASS_WITHOUT_PED_THRES.setEnabled(CB_ASS_WITHOUT_PED.isSelected());
    }

    private void setNumDataAutoDisplayEnabled() {
        TF_NUM_DATA_AUTO_DISPLAY.setEnabled(CB_AUTO_DISPLAY_DATA.isSelected());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        groupMotorV = new javax.swing.ButtonGroup();
        groupOptAdc = new javax.swing.ButtonGroup();
        groupDisplayMode = new javax.swing.ButtonGroup();
        groupAssistOnStart = new javax.swing.ButtonGroup();
        groupUnits = new javax.swing.ButtonGroup();
        groupStartupDisplay = new javax.swing.ButtonGroup();
        groupStartupSoc = new javax.swing.ButtonGroup();
        groupStartupBoost = new javax.swing.ButtonGroup();
        labelTitle = new java.awt.Label();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelBasicSettings = new javax.swing.JPanel();
        subPanelMotorSettings = new javax.swing.JPanel();
        headingMotorSettings = new javax.swing.JLabel();
        jLabel_MOTOR_V = new javax.swing.JLabel();
        jLabel_MOTOR_ACC = new javax.swing.JLabel();
        TF_MOTOR_ACC = new components.IntField();
        jLabel_MOTOR_FAST_STOP = new javax.swing.JLabel();
        TF_MOTOR_DEC = new components.IntField();
        CB_ASS_WITHOUT_PED = new javax.swing.JCheckBox();
        TF_ASS_WITHOUT_PED_THRES = new components.IntField();
        TF_TORQ_PER_ADC_STEP = new components.IntField();
        jLabel_TORQ_PER_ADC_STEP_ADV = new javax.swing.JLabel();
        TF_TORQ_PER_ADC_STEP_ADV = new components.IntField();
        jLabel_TORQ_ADC_OFFSET_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_OFFSET_ADJ = new components.IntField();
        jLabel_TORQ_ADC_RANGE_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_RANGE_ADJ = new components.IntField();
        jLabel_TORQ_ADC_ANGLE_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_ANGLE_ADJ = new components.IntField();
        jLabel_TORQ_ADC_OFFSET = new javax.swing.JLabel();
        TF_TORQ_ADC_OFFSET = new components.IntField();
        jLabel_TORQ_ADC_MAX = new javax.swing.JLabel();
        TF_TORQUE_ADC_MAX = new components.IntField();
        jLabel_BOOST_TORQUE_FACTOR = new javax.swing.JLabel();
        TF_BOOST_TORQUE_FACTOR = new components.IntField();
        jLabel_BOOST_CADENCE_STEP = new javax.swing.JLabel();
        TF_BOOST_CADENCE_STEP = new components.IntField();
        jLabel_BOOST_AT_ZERO = new javax.swing.JLabel();
        jPanel_BOOST_AT_ZERO = new javax.swing.JPanel();
        RB_BOOST_AT_ZERO_CADENCE = new javax.swing.JRadioButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_BOOST_AT_ZERO_SPEED = new javax.swing.JRadioButton();
        jPanel_MOTOR_V = new javax.swing.JPanel();
        RB_MOTOR_36V = new javax.swing.JRadioButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_MOTOR_48V = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel_TORQ_PER_ADC_STEP = new javax.swing.JLabel();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        CB_ADC_STEP_ESTIM = new javax.swing.JCheckBox();
        subPanelBatterySettings = new javax.swing.JPanel();
        headerBatterySettings = new javax.swing.JLabel();
        jLabel_BAT_CUR_MAX = new javax.swing.JLabel();
        TF_BAT_CUR_MAX = new components.FloatField();
        jLabel_BATT_POW_MAX = new javax.swing.JLabel();
        TF_BATT_POW_MAX = new components.IntField();
        jLabel_BATT_CAPACITY = new javax.swing.JLabel();
        TF_BATT_CAPACITY = new components.IntField();
        jLabel_BATT_NUM_CELLS = new javax.swing.JLabel();
        TF_BATT_NUM_CELLS = new components.IntField();
        jLabel_BATT_VOLT_CAL = new javax.swing.JLabel();
        TF_BATT_VOLT_CAL = new components.IntField();
        jLabel_BATT_CAPACITY_CAL = new javax.swing.JLabel();
        TF_BATT_CAPACITY_CAL = new components.IntField();
        jLabel_BATT_VOLT_CUT_OFF = new javax.swing.JLabel();
        TF_BATT_VOLT_CUT_OFF = new components.IntField();
        headerDisplaySettings = new javax.swing.JLabel();
        jLabelDisplayType = new javax.swing.JLabel();
        CMB_DISPLAY_TYPE = new JComboBox<>(DisplayType.values());
        jLabelDisplayMode = new javax.swing.JLabel();
        rowDisplayMode = new javax.swing.JPanel();
        RB_DISPLAY_ALWAY_ON = new javax.swing.JRadioButton();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_DISPLAY_WORK_ON = new javax.swing.JRadioButton();
        labelUnits = new javax.swing.JLabel();
        rowUnits = new javax.swing.JPanel();
        RB_UNIT_KILOMETERS = new javax.swing.JRadioButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_UNIT_MILES = new javax.swing.JRadioButton();
        headerBikeSettings = new javax.swing.JLabel();
        TF_WHEEL_CIRCUMF = new components.IntField();
        TF_MAX_SPEED = new components.IntField();
        jLabel_MAX_SPEED = new javax.swing.JLabel();
        jLabel_WHEEL_CIRCUMF = new javax.swing.JLabel();
        subPanelFunctionSettings = new javax.swing.JPanel();
        headerFunctionSettings = new javax.swing.JLabel();
        CB_LIGHTS = new javax.swing.JCheckBox();
        CB_WALK_ASSIST = new javax.swing.JCheckBox();
        CB_BRAKE_SENSOR = new javax.swing.JCheckBox();
        CB_COASTER_BRAKE = new javax.swing.JCheckBox();
        jLabelOptADC = new javax.swing.JLabel();
        rowOptADC = new javax.swing.JPanel();
        RB_TEMP_LIMIT = new javax.swing.JRadioButton();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_THROTTLE = new javax.swing.JRadioButton();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_ADC_OPTION_DIS = new javax.swing.JRadioButton();
        CB_STREET_MODE_ON_START = new javax.swing.JCheckBox();
        CB_STARTUP_BOOST_ON_START = new javax.swing.JCheckBox();
        rowTorSensorAdv = new javax.swing.JPanel();
        CB_TOR_SENSOR_ADV = new javax.swing.JCheckBox();
        CB_TORQUE_CALIBRATION = new javax.swing.JCheckBox();
        CB_FIELD_WEAKENING_ENABLED = new javax.swing.JCheckBox();
        CB_STARTUP_ASSIST_ENABLED = new javax.swing.JCheckBox();
        CB_ODO_COMPENSATION = new javax.swing.JCheckBox();
        CB_SET_PARAM_ON_START = new javax.swing.JCheckBox();
        CB_AUTO_DISPLAY_DATA = new javax.swing.JCheckBox();
        CB_MAX_SPEED_DISPLAY = new javax.swing.JCheckBox();
        panelAssistanceSettings = new javax.swing.JPanel();
        subPanelPowerAssist = new javax.swing.JPanel();
        headerPowerAssist = new javax.swing.JLabel();
        jLabel_TF_POWER_ASS_1 = new javax.swing.JLabel();
        TF_POWER_ASS_1 = new components.IntField();
        jLabel_TF_POWER_ASS_2 = new javax.swing.JLabel();
        TF_POWER_ASS_2 = new components.IntField();
        jLabel_TF_POWER_ASS_3 = new javax.swing.JLabel();
        TF_POWER_ASS_3 = new components.IntField();
        jLabel_POWER_ASS_4 = new javax.swing.JLabel();
        TF_POWER_ASS_4 = new components.IntField();
        RB_POWER_ON_START = new javax.swing.JRadioButton();
        subPanelTorqueAssist = new javax.swing.JPanel();
        headerTorqueAssist = new javax.swing.JLabel();
        jLabel_TORQUE_ASS_1 = new javax.swing.JLabel();
        TF_TORQUE_ASS_1 = new components.IntField();
        jLabel_TORQUE_ASS_2 = new javax.swing.JLabel();
        TF_TORQUE_ASS_2 = new components.IntField();
        jLabel_TORQUE_ASS_3 = new javax.swing.JLabel();
        TF_TORQUE_ASS_3 = new components.IntField();
        jLabel_TORQUE_ASS_4 = new javax.swing.JLabel();
        TF_TORQUE_ASS_4 = new components.IntField();
        RB_TORQUE_ON_START = new javax.swing.JRadioButton();
        subPanelCadenceAssist = new javax.swing.JPanel();
        headerCadenceAssist = new javax.swing.JLabel();
        jLabel_CADENCE_ASS_1 = new javax.swing.JLabel();
        TF_CADENCE_ASS_1 = new components.IntField();
        jLabel_CADENCE_ASS_2 = new javax.swing.JLabel();
        TF_CADENCE_ASS_2 = new components.IntField();
        jLabel_CADENCE_ASS_3 = new javax.swing.JLabel();
        TF_CADENCE_ASS_3 = new components.IntField();
        jLabel_CADENCE_ASS_4 = new javax.swing.JLabel();
        TF_CADENCE_ASS_4 = new components.IntField();
        RB_CADENCE_ON_START = new javax.swing.JRadioButton();
        subPanelEmtbAssist = new javax.swing.JPanel();
        headerEmtbAssist = new javax.swing.JLabel();
        jLabel_EMTB_ASS_1 = new javax.swing.JLabel();
        TF_EMTB_ASS_1 = new components.IntField();
        jLabel_EMTB_ASS_2 = new javax.swing.JLabel();
        TF_EMTB_ASS_2 = new components.IntField();
        jLabel_EMTB_ASS_3 = new javax.swing.JLabel();
        TF_EMTB_ASS_3 = new components.IntField();
        jLabel_EMTB_ASS_4 = new javax.swing.JLabel();
        TF_EMTB_ASS_4 = new components.IntField();
        RB_EMTB_ON_START = new javax.swing.JRadioButton();
        subPanelWalkAssist = new javax.swing.JPanel();
        headerWalkAssist = new javax.swing.JLabel();
        jLabelWalkSpeedUnits = new javax.swing.JLabel();
        jLabel_WALK_ASS_SPEED_1 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_1 = new components.IntField();
        jLabel_WALK_ASS_SPEED_2 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_2 = new components.IntField();
        jLabel_WALK_ASS_SPEED_3 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_3 = new components.IntField();
        jLabel_WALK_ASS_SPEED_4 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_4 = new components.IntField();
        jLabel_WALK_ASS_SPEED_LIMIT = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_LIMIT = new components.IntField();
        jLabel_WALK_ASS_TIME = new javax.swing.JLabel();
        TF_WALK_ASS_TIME = new components.IntField();
        CB_WALK_TIME_ENA = new javax.swing.JCheckBox();
        subPanelStreetMode = new javax.swing.JPanel();
        headerStreetMode = new javax.swing.JLabel();
        jLabel_STREET_SPEED_LIM = new javax.swing.JLabel();
        TF_STREET_SPEED_LIM = new components.IntField();
        jLabel_STREET_POWER_LIM = new javax.swing.JLabel();
        TF_STREET_POWER_LIM = new components.IntField();
        CB_STREET_POWER_LIM = new javax.swing.JCheckBox();
        CB_STREET_THROTTLE = new javax.swing.JCheckBox();
        CB_THROTTLE_LEGAL = new javax.swing.JCheckBox();
        CB_STREET_CRUISE = new javax.swing.JCheckBox();
        CB_STREET_WALK = new javax.swing.JCheckBox();
        subPanelCruiseMode = new javax.swing.JPanel();
        headerCruiseMode = new javax.swing.JLabel();
        jLabelCruiseSpeedUnits = new javax.swing.JLabel();
        jLabel_CRUISE_ASS_1 = new javax.swing.JLabel();
        TF_CRUISE_ASS_1 = new components.IntField();
        jLabel_CRUISE_ASS_2 = new javax.swing.JLabel();
        TF_CRUISE_ASS_2 = new components.IntField();
        jLabel_CRUISE_ASS_3 = new javax.swing.JLabel();
        TF_CRUISE_ASS_3 = new components.IntField();
        jLabel_CRUISE_ASS_4 = new javax.swing.JLabel();
        TF_CRUISE_ASS_4 = new components.IntField();
        jLabel_CRUISE_SPEED_ENA = new javax.swing.JLabel();
        TF_CRUISE_SPEED_ENA = new components.IntField();
        CB_CRUISE_WHITOUT_PED = new javax.swing.JCheckBox();
        subPanelLightsHybrid = new javax.swing.JPanel();
        headerLights = new javax.swing.JLabel();
        jLabel_LIGHT_MODE_ON_START = new javax.swing.JLabel();
        TF_LIGHT_MODE_ON_START = new components.IntField();
        jLabel_LIGHT_MODE_1 = new javax.swing.JLabel();
        TF_LIGHT_MODE_1 = new components.IntField();
        jLabel_LIGHT_MODE_2 = new javax.swing.JLabel();
        TF_LIGHT_MODE_2 = new components.IntField();
        jLabel_LIGHT_MODE_3 = new javax.swing.JLabel();
        TF_LIGHT_MODE_3 = new components.IntField();
        headerHybridAssist = new javax.swing.JLabel();
        RB_HYBRID_ON_START = new javax.swing.JRadioButton();
        panelAdvancedSettings = new javax.swing.JPanel();
        subPanelBatteryCells = new javax.swing.JPanel();
        headerBatteryCells = new javax.swing.JLabel();
        jLabel_BAT_CELL_OVER = new javax.swing.JLabel();
        TF_BAT_CELL_OVER = new components.FloatField();
        jLabel_BAT_CELL_SOC = new javax.swing.JLabel();
        TF_BAT_CELL_SOC = new components.FloatField();
        jLabel_BAT_CELL_FULL = new javax.swing.JLabel();
        TF_BAT_CELL_FULL = new components.FloatField();
        jLabel_BAT_CELL_3_4 = new javax.swing.JLabel();
        TF_BAT_CELL_3_4 = new components.FloatField();
        jLabel_BAT_CELL_2_4 = new javax.swing.JLabel();
        TF_BAT_CELL_2_4 = new components.FloatField();
        jLabel_BAT_CELL_1_4 = new javax.swing.JLabel();
        TF_BAT_CELL_1_4 = new components.FloatField();
        jLabel_BAT_CELL_5_6 = new javax.swing.JLabel();
        TF_BAT_CELL_5_6 = new components.FloatField();
        jLabel_BAT_CELL_4_6 = new javax.swing.JLabel();
        TF_BAT_CELL_4_6 = new components.FloatField();
        jLabel_BAT_CELL_3_6 = new javax.swing.JLabel();
        TF_BAT_CELL_3_6 = new components.FloatField();
        jLabel_BAT_CELL_2_6 = new javax.swing.JLabel();
        TF_BAT_CELL_2_6 = new components.FloatField();
        jLabel_BAT_CELL_1_6 = new javax.swing.JLabel();
        TF_BAT_CELL_1_6 = new components.FloatField();
        jLabel_BAT_CELL_EMPTY = new javax.swing.JLabel();
        TF_BAT_CELL_EMPTY = new components.FloatField();
        subPanelDisplayAdvanced = new javax.swing.JPanel();
        headerDisplayAdvanced = new javax.swing.JLabel();
        jLabelData1 = new javax.swing.JLabel();
        TF_DATA_1 = new components.IntField();
        jLabelData2 = new javax.swing.JLabel();
        TF_DATA_2 = new components.IntField();
        jLabelData3 = new javax.swing.JLabel();
        TF_DATA_3 = new components.IntField();
        jLabelData4 = new javax.swing.JLabel();
        TF_DATA_4 = new components.IntField();
        jLabelData5 = new javax.swing.JLabel();
        TF_DATA_5 = new components.IntField();
        jLabelData6 = new javax.swing.JLabel();
        TF_DATA_6 = new components.IntField();
        jLabel_DELAY_DATA_1 = new javax.swing.JLabel();
        TF_DELAY_DATA_1 = new components.IntField();
        jLabel_DELAY_DATA_2 = new javax.swing.JLabel();
        TF_DELAY_DATA_2 = new components.IntField();
        jLabel_DELAY_DATA_3 = new javax.swing.JLabel();
        TF_DELAY_DATA_3 = new components.IntField();
        jLabel_DELAY_DATA_4 = new javax.swing.JLabel();
        TF_DELAY_DATA_4 = new components.IntField();
        jLabel_DELAY_DATA_5 = new javax.swing.JLabel();
        TF_DELAY_DATA_5 = new components.IntField();
        jLabel_DELAY_DATA_6 = new javax.swing.JLabel();
        TF_DELAY_DATA_6 = new components.IntField();
        subPanelDataOther = new javax.swing.JPanel();
        jLabel_NUM_DATA_AUTO_DISPLAY = new javax.swing.JLabel();
        TF_NUM_DATA_AUTO_DISPLAY = new components.IntField();
        jLabel_DELAY_MENU = new javax.swing.JLabel();
        TF_DELAY_MENU = new components.IntField();
        jLabelStartupData = new javax.swing.JLabel();
        rowStartupData = new javax.swing.JPanel();
        RB_STARTUP_SOC = new javax.swing.JRadioButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_STARTUP_VOLTS = new javax.swing.JRadioButton();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_STARTUP_NONE = new javax.swing.JRadioButton();
        jLabelSocCalc = new javax.swing.JLabel();
        rowSocCalc = new javax.swing.JPanel();
        RB_SOC_WH = new javax.swing.JRadioButton();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_SOC_AUTO = new javax.swing.JRadioButton();
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_SOC_VOLTS = new javax.swing.JRadioButton();
        headerOtherSettings = new javax.swing.JLabel();
        jLabel_COASTER_BRAKE_THRESHOLD = new javax.swing.JLabel();
        TF_COASTER_BRAKE_THRESHOLD = new components.IntField();
        jLabel_ADC_THROTTLE = new javax.swing.JLabel();
        jLabel_ADC_THROTTLE_MIN = new javax.swing.JLabel();
        TF_ADC_THROTTLE_MIN = new components.IntField();
        jLabel_ADC_THROTTLE_MAX = new javax.swing.JLabel();
        TF_ADC_THROTTLE_MAX = new components.IntField();
        jLabel_ASSIST_THROTTLE = new javax.swing.JLabel();
        jLabel_ASSIST_THROTTLE_MIN = new javax.swing.JLabel();
        TF_ASSIST_THROTTLE_MIN = new components.IntField();
        jLabel_ASSIST_THROTTLE_MAX = new javax.swing.JLabel();
        TF_ASSIST_THROTTLE_MAX = new components.IntField();
        CB_TEMP_ERR_MIN_LIM = new javax.swing.JCheckBox();
        jLabel_TEMP_MIN_LIM = new javax.swing.JLabel();
        TF_TEMP_MIN_LIM = new components.IntField();
        jLabel_TEMP_MAX_LIM = new javax.swing.JLabel();
        TF_TEMP_MAX_LIM = new components.IntField();
        jLabel_MOTOR_BLOCK_TIME = new javax.swing.JLabel();
        TF_MOTOR_BLOCK_TIME = new components.IntField();
        jLabel_MOTOR_BLOCK_CURR = new javax.swing.JLabel();
        TF_MOTOR_BLOCK_CURR = new components.IntField();
        jLabel_MOTOR_BLOCK_ERPS = new javax.swing.JLabel();
        TF_MOTOR_BLOCK_ERPS = new components.IntField();
        panelRightColumn = new javax.swing.JPanel();
        jLabelExpSettings = new javax.swing.JLabel();
        scrollExpSettings = new javax.swing.JScrollPane();
        expSet = new javax.swing.JList<>();
        jLabelProvenSettings = new javax.swing.JLabel();
        scrollProvenSettings = new javax.swing.JScrollPane();
        provSet = new javax.swing.JList<>();
        jLabelVersion = new javax.swing.JLabel();
        LB_LAST_COMMIT = new javax.swing.JLabel();
        rowCompileActions = new javax.swing.JPanel();
        BTN_COMPILE = new javax.swing.JButton();
        BTN_CANCEL = new javax.swing.JButton();
        LB_COMPILE_OUTPUT = new javax.swing.JLabel();
        scrollCompileOutput = new javax.swing.JScrollPane();
        TA_COMPILE_OUTPUT = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("TSDZ2 Parameter Configurator 4.3 for Open Source Firmware v20.1C.2-2");
        setMinimumSize(new java.awt.Dimension(1196, 758));
        setResizable(false);
        setSize(new java.awt.Dimension(1196, 758));

        labelTitle.setFont(new java.awt.Font("SansSerif", 1, 20)); // NOI18N
        labelTitle.setText("TSDZ2 Parameter Configurator");

        jTabbedPane1.setPreferredSize(new java.awt.Dimension(894, 513));

        java.awt.GridBagLayout panelBasicSettingsLayout = new java.awt.GridBagLayout();
        panelBasicSettingsLayout.columnWidths = new int[] {0, 20, 0, 20, 0};
        panelBasicSettingsLayout.rowHeights = new int[] {0};
        panelBasicSettings.setLayout(panelBasicSettingsLayout);

        java.awt.GridBagLayout subPanelMotorSettingsLayout = new java.awt.GridBagLayout();
        subPanelMotorSettingsLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        subPanelMotorSettingsLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelMotorSettings.setLayout(subPanelMotorSettingsLayout);

        headingMotorSettings.setFont(headingMotorSettings.getFont().deriveFont(headingMotorSettings.getFont().getStyle() | java.awt.Font.BOLD, headingMotorSettings.getFont().getSize()+2));
        headingMotorSettings.setText("Motor settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(headingMotorSettings, gridBagConstraints);

        jLabel_MOTOR_V.setFont(jLabel_MOTOR_V.getFont().deriveFont(jLabel_MOTOR_V.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_MOTOR_V.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_V.setText("Motor type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_V, gridBagConstraints);

        jLabel_MOTOR_ACC.setFont(jLabel_MOTOR_ACC.getFont().deriveFont(jLabel_MOTOR_ACC.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_MOTOR_ACC.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_ACC.setText("Motor acceleration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_ACC, gridBagConstraints);

        TF_MOTOR_ACC.setToolTipText("<html>MAX VALUE<br>\n36 volt motor, 36 volt battery = 35<br>\n36 volt motor, 48 volt battery = 5<br>\n36 volt motor, 52 volt battery = 0<br>\n48 volt motor, 36 volt battery = 45<br>\n48 volt motor, 48 volt battery = 35<br>\n48 volt motor, 52 volt battery = 30\n</html>");
        TF_MOTOR_ACC.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_MOTOR_ACC, gridBagConstraints);

        jLabel_MOTOR_FAST_STOP.setFont(jLabel_MOTOR_FAST_STOP.getFont().deriveFont(jLabel_MOTOR_FAST_STOP.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_MOTOR_FAST_STOP.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_FAST_STOP.setText("Motor deceleration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_FAST_STOP, gridBagConstraints);

        TF_MOTOR_DEC.setToolTipText("<html>Max value 100<br>\nRecommended range 0 to 50\n</html>");
        TF_MOTOR_DEC.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_MOTOR_DEC, gridBagConstraints);

        CB_ASS_WITHOUT_PED.setText("Startup assist without pedaling thres.");
        CB_ASS_WITHOUT_PED.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_ASS_WITHOUT_PEDStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(CB_ASS_WITHOUT_PED, gridBagConstraints);

        TF_ASS_WITHOUT_PED_THRES.setToolTipText("<html>Max value 100<br>\nRecommended range 10 to 30\n</html>");
        TF_ASS_WITHOUT_PED_THRES.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_ASS_WITHOUT_PED_THRES, gridBagConstraints);

        TF_TORQ_PER_ADC_STEP.setToolTipText("<html>\nDefault value 67<br>\nOptional calibration\n</html>");
        TF_TORQ_PER_ADC_STEP.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_PER_ADC_STEP, gridBagConstraints);

        jLabel_TORQ_PER_ADC_STEP_ADV.setText("Pedal torque ADC step advanced");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_PER_ADC_STEP_ADV, gridBagConstraints);

        TF_TORQ_PER_ADC_STEP_ADV.setToolTipText("<html>\nDefault value 34<br>\nOptional calibration\n</html>");
        TF_TORQ_PER_ADC_STEP_ADV.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_PER_ADC_STEP_ADV, gridBagConstraints);

        jLabel_TORQ_ADC_OFFSET_ADJ.setText("Pedal torque ADC offset adjustment");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_ADC_OFFSET_ADJ, gridBagConstraints);

        TF_TORQ_ADC_OFFSET_ADJ.setToolTipText("<html>\nValue -20 to 14<br>\nDefault 0\n</html>");
        TF_TORQ_ADC_OFFSET_ADJ.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_TORQ_ADC_OFFSET_ADJ.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_TORQ_ADC_OFFSET_ADJKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_ADC_OFFSET_ADJ, gridBagConstraints);

        jLabel_TORQ_ADC_RANGE_ADJ.setText("Pedal torque ADC range adjustment");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_ADC_RANGE_ADJ, gridBagConstraints);

        TF_TORQ_ADC_RANGE_ADJ.setToolTipText("<html>\nValue -20 to 20<br>\nDefault 0\n</html>");
        TF_TORQ_ADC_RANGE_ADJ.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_TORQ_ADC_RANGE_ADJ.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_TORQ_ADC_RANGE_ADJKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_ADC_RANGE_ADJ, gridBagConstraints);

        jLabel_TORQ_ADC_ANGLE_ADJ.setText("Pedal torque ADC angle adjustment");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_ADC_ANGLE_ADJ, gridBagConstraints);

        TF_TORQ_ADC_ANGLE_ADJ.setToolTipText("<html>\nValue -20 to 20<br>\nDefault 0\n</html>");
        TF_TORQ_ADC_ANGLE_ADJ.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_TORQ_ADC_ANGLE_ADJ.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_TORQ_ADC_ANGLE_ADJKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_ADC_ANGLE_ADJ, gridBagConstraints);

        jLabel_TORQ_ADC_OFFSET.setText("Pedal torque ADC offset (no weight)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_ADC_OFFSET, gridBagConstraints);

        TF_TORQ_ADC_OFFSET.setToolTipText("<html>\nInsert value read on calibration<br>\nMax 250\n</html>");
        TF_TORQ_ADC_OFFSET.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_TORQ_ADC_OFFSET.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_TORQ_ADC_OFFSETKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQ_ADC_OFFSET, gridBagConstraints);

        jLabel_TORQ_ADC_MAX.setText("Pedal torque ADC max (max weight)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_TORQ_ADC_MAX, gridBagConstraints);

        TF_TORQUE_ADC_MAX.setToolTipText("<html>\nInsert value read on calibration<br>\nMax 500\n</html>");
        TF_TORQUE_ADC_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_TORQUE_ADC_MAX.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_TORQUE_ADC_MAXKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_TORQUE_ADC_MAX, gridBagConstraints);

        jLabel_BOOST_TORQUE_FACTOR.setText("Startup boost torque factor (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_BOOST_TORQUE_FACTOR, gridBagConstraints);

        TF_BOOST_TORQUE_FACTOR.setToolTipText("<html>Max value 500<br>\nRecommended range 200 to 300\n</html>");
        TF_BOOST_TORQUE_FACTOR.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_BOOST_TORQUE_FACTOR, gridBagConstraints);

        jLabel_BOOST_CADENCE_STEP.setText("Startup boost cadence step (decr.)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_BOOST_CADENCE_STEP, gridBagConstraints);

        TF_BOOST_CADENCE_STEP.setToolTipText("<html>Max value 50<br>\nRecommended range 20 to 30<br>\n(high values short effect)\n</html>");
        TF_BOOST_CADENCE_STEP.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_BOOST_CADENCE_STEP, gridBagConstraints);

        jLabel_BOOST_AT_ZERO.setText("Startup boost at zero");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_BOOST_AT_ZERO, gridBagConstraints);

        jPanel_BOOST_AT_ZERO.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        groupStartupBoost.add(RB_BOOST_AT_ZERO_CADENCE);
        RB_BOOST_AT_ZERO_CADENCE.setSelected(true);
        RB_BOOST_AT_ZERO_CADENCE.setText("cadence");
        jPanel_BOOST_AT_ZERO.add(RB_BOOST_AT_ZERO_CADENCE);
        jPanel_BOOST_AT_ZERO.add(filler2);

        groupStartupBoost.add(RB_BOOST_AT_ZERO_SPEED);
        RB_BOOST_AT_ZERO_SPEED.setText("speed");
        jPanel_BOOST_AT_ZERO.add(RB_BOOST_AT_ZERO_SPEED);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(jPanel_BOOST_AT_ZERO, gridBagConstraints);

        jPanel_MOTOR_V.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        groupMotorV.add(RB_MOTOR_36V);
        RB_MOTOR_36V.setText("36V");
        jPanel_MOTOR_V.add(RB_MOTOR_36V);
        jPanel_MOTOR_V.add(filler1);

        groupMotorV.add(RB_MOTOR_48V);
        RB_MOTOR_48V.setText("48V");
        jPanel_MOTOR_V.add(RB_MOTOR_48V);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(jPanel_MOTOR_V, gridBagConstraints);

        jPanel2.setLayout(new javax.swing.BoxLayout(jPanel2, javax.swing.BoxLayout.LINE_AXIS));

        jLabel_TORQ_PER_ADC_STEP.setText("Pedal torque ADC step");
        jPanel2.add(jLabel_TORQ_PER_ADC_STEP);
        jPanel2.add(filler5);

        CB_ADC_STEP_ESTIM.setText("Estimated");
        CB_ADC_STEP_ESTIM.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_ADC_STEP_ESTIMStateChanged(evt);
            }
        });
        jPanel2.add(CB_ADC_STEP_ESTIM);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        panelBasicSettings.add(subPanelMotorSettings, gridBagConstraints);

        java.awt.GridBagLayout subPanelBatterySettingsLayout = new java.awt.GridBagLayout();
        subPanelBatterySettingsLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        subPanelBatterySettingsLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelBatterySettings.setLayout(subPanelBatterySettingsLayout);

        headerBatterySettings.setFont(headerBatterySettings.getFont().deriveFont(headerBatterySettings.getFont().getStyle() | java.awt.Font.BOLD, headerBatterySettings.getFont().getSize()+2));
        headerBatterySettings.setText("Battery settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerBatterySettings, gridBagConstraints);

        jLabel_BAT_CUR_MAX.setFont(jLabel_BAT_CUR_MAX.getFont().deriveFont(jLabel_BAT_CUR_MAX.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_BAT_CUR_MAX.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BAT_CUR_MAX.setText("Battery current max (A)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BAT_CUR_MAX, gridBagConstraints);

        TF_BAT_CUR_MAX.setToolTipText("<html>Max value<br>\n17 A for 36 V<br>\n12 A for 48 V\n</html>");
        TF_BAT_CUR_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BAT_CUR_MAX, gridBagConstraints);

        jLabel_BATT_POW_MAX.setFont(jLabel_BATT_POW_MAX.getFont().deriveFont(jLabel_BATT_POW_MAX.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_BATT_POW_MAX.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_POW_MAX.setText("Battery power max (W)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_POW_MAX, gridBagConstraints);

        TF_BATT_POW_MAX.setToolTipText("<html>Motor power limit in offroad mode<br>\nMax value depends on the rated<br>\nmotor power and the battery capacity\n</html>");
        TF_BATT_POW_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_POW_MAX, gridBagConstraints);

        jLabel_BATT_CAPACITY.setFont(jLabel_BATT_CAPACITY.getFont().deriveFont(jLabel_BATT_CAPACITY.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_BATT_CAPACITY.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_CAPACITY.setText("Battery capacity (Wh)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_CAPACITY, gridBagConstraints);

        TF_BATT_CAPACITY.setToolTipText("<html>To calculate<br>\nBattery Volt x Ah\n</html>\n");
        TF_BATT_CAPACITY.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_CAPACITY, gridBagConstraints);

        jLabel_BATT_NUM_CELLS.setFont(jLabel_BATT_NUM_CELLS.getFont().deriveFont(jLabel_BATT_NUM_CELLS.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_BATT_NUM_CELLS.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_NUM_CELLS.setText("Battery cells number");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_NUM_CELLS, gridBagConstraints);

        TF_BATT_NUM_CELLS.setToolTipText("<html> 7 for 24 V battery<br>\n10 for 36 V battery<br>\n13 for 48 V battery<br>\n14 for 52 V battery\n</html>");
        TF_BATT_NUM_CELLS.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_NUM_CELLS, gridBagConstraints);

        jLabel_BATT_VOLT_CAL.setText("Battery voltage calibration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_VOLT_CAL, gridBagConstraints);

        TF_BATT_VOLT_CAL.setToolTipText("<html>For calibrate voltage displayed<br>\nIndicative value 95 to 105\n</html>");
        TF_BATT_VOLT_CAL.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_VOLT_CAL, gridBagConstraints);

        jLabel_BATT_CAPACITY_CAL.setText("Battery capacity calibration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_CAPACITY_CAL, gridBagConstraints);

        TF_BATT_CAPACITY_CAL.setToolTipText("<html>Starting to 100%<br>\nwith the% remaining when battery is low<br>\ncalculate the actual%\n</html>");
        TF_BATT_CAPACITY_CAL.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_CAPACITY_CAL, gridBagConstraints);

        jLabel_BATT_VOLT_CUT_OFF.setFont(jLabel_BATT_VOLT_CUT_OFF.getFont().deriveFont(jLabel_BATT_VOLT_CUT_OFF.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_BATT_VOLT_CUT_OFF.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_VOLT_CUT_OFF.setText("Battery voltage cut off (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_VOLT_CUT_OFF, gridBagConstraints);

        TF_BATT_VOLT_CUT_OFF.setToolTipText("<html>Indicative value 29 for 36 V<br>\n38 for 48 V, It depends on the<br>\ncharacteristics of the battery\n</html>");
        TF_BATT_VOLT_CUT_OFF.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_VOLT_CUT_OFF, gridBagConstraints);

        headerDisplaySettings.setFont(headerDisplaySettings.getFont().deriveFont(headerDisplaySettings.getFont().getStyle() | java.awt.Font.BOLD, headerDisplaySettings.getFont().getSize()+2));
        headerDisplaySettings.setText("Display settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerDisplaySettings, gridBagConstraints);

        jLabelDisplayType.setFont(jLabelDisplayType.getFont().deriveFont(jLabelDisplayType.getFont().getStyle() | java.awt.Font.BOLD));
        jLabelDisplayType.setForeground(new java.awt.Color(255, 0, 0));
        jLabelDisplayType.setText("Type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabelDisplayType, gridBagConstraints);

        CMB_DISPLAY_TYPE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CMB_DISPLAY_TYPEActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(CMB_DISPLAY_TYPE, gridBagConstraints);

        jLabelDisplayMode.setText("Mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabelDisplayMode, gridBagConstraints);

        rowDisplayMode.setLayout(new javax.swing.BoxLayout(rowDisplayMode, javax.swing.BoxLayout.LINE_AXIS));

        groupDisplayMode.add(RB_DISPLAY_ALWAY_ON);
        RB_DISPLAY_ALWAY_ON.setText("Always on");
        rowDisplayMode.add(RB_DISPLAY_ALWAY_ON);
        rowDisplayMode.add(filler3);

        groupDisplayMode.add(RB_DISPLAY_WORK_ON);
        RB_DISPLAY_WORK_ON.setSelected(true);
        RB_DISPLAY_WORK_ON.setText("Working on");
        rowDisplayMode.add(RB_DISPLAY_WORK_ON);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(rowDisplayMode, gridBagConstraints);

        labelUnits.setText("Units");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(labelUnits, gridBagConstraints);

        rowUnits.setLayout(new javax.swing.BoxLayout(rowUnits, javax.swing.BoxLayout.LINE_AXIS));

        groupUnits.add(RB_UNIT_KILOMETERS);
        RB_UNIT_KILOMETERS.setText("km/h");
        RB_UNIT_KILOMETERS.setToolTipText("<html>Also set on the display<br>\nIf you set miles in display<br>\nset max wheel available\n</html>");
        RB_UNIT_KILOMETERS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_UNIT_KILOMETERSStateChanged(evt);
            }
        });
        rowUnits.add(RB_UNIT_KILOMETERS);
        rowUnits.add(filler4);

        groupUnits.add(RB_UNIT_MILES);
        RB_UNIT_MILES.setText("mph");
        RB_UNIT_MILES.setToolTipText("<html>Also set on the display<br>\nIf you set miles in display<br>\nset max wheel available\n</html>");
        RB_UNIT_MILES.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_UNIT_MILESStateChanged(evt);
            }
        });
        rowUnits.add(RB_UNIT_MILES);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(rowUnits, gridBagConstraints);

        headerBikeSettings.setFont(headerBikeSettings.getFont().deriveFont(headerBikeSettings.getFont().getStyle() | java.awt.Font.BOLD, headerBikeSettings.getFont().getSize()+2));
        headerBikeSettings.setText("Bike settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerBikeSettings, gridBagConstraints);

        TF_WHEEL_CIRCUMF.setToolTipText("<html>Indicative values:<br>\n26-inch wheel = 2050 mm<br>\n27-inch wheel = 2150 mm<br>\n27.5 inch wheel = 2215 mm<br>\n28-inch wheel = 2250 mm<br>\n29-inch wheel = 2300 mmV\n</html>");
        TF_WHEEL_CIRCUMF.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_WHEEL_CIRCUMF, gridBagConstraints);

        TF_MAX_SPEED.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_MAX_SPEED.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_MAX_SPEED.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_MAX_SPEEDKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_MAX_SPEED, gridBagConstraints);

        jLabel_MAX_SPEED.setText("Max speed offroad mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_MAX_SPEED, gridBagConstraints);

        jLabel_WHEEL_CIRCUMF.setFont(jLabel_WHEEL_CIRCUMF.getFont().deriveFont(jLabel_WHEEL_CIRCUMF.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_WHEEL_CIRCUMF.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_WHEEL_CIRCUMF.setText("Wheel circumference (mm)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_WHEEL_CIRCUMF, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        panelBasicSettings.add(subPanelBatterySettings, gridBagConstraints);

        java.awt.GridBagLayout subPanelFunctionSettingsLayout = new java.awt.GridBagLayout();
        subPanelFunctionSettingsLayout.columnWidths = new int[] {0};
        subPanelFunctionSettingsLayout.rowHeights = new int[] {0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0, 7, 0};
        subPanelFunctionSettings.setLayout(subPanelFunctionSettingsLayout);

        headerFunctionSettings.setFont(headerFunctionSettings.getFont().deriveFont(headerFunctionSettings.getFont().getStyle() | java.awt.Font.BOLD, headerFunctionSettings.getFont().getSize()+2));
        headerFunctionSettings.setText("Function settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(headerFunctionSettings, gridBagConstraints);

        CB_LIGHTS.setText("Lights");
        CB_LIGHTS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_LIGHTSStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_LIGHTS, gridBagConstraints);

        CB_WALK_ASSIST.setText("Walk assist");
        CB_WALK_ASSIST.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_WALK_ASSISTStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_WALK_ASSIST, gridBagConstraints);

        CB_BRAKE_SENSOR.setText("Brake sensor");
        CB_BRAKE_SENSOR.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_BRAKE_SENSORStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_BRAKE_SENSOR, gridBagConstraints);

        CB_COASTER_BRAKE.setText("Coaster brake");
        CB_COASTER_BRAKE.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_COASTER_BRAKEStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_COASTER_BRAKE, gridBagConstraints);

        jLabelOptADC.setText("Optional ADC function");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(jLabelOptADC, gridBagConstraints);

        rowOptADC.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        groupOptAdc.add(RB_TEMP_LIMIT);
        RB_TEMP_LIMIT.setText("Temp. sensor");
        RB_TEMP_LIMIT.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_TEMP_LIMITStateChanged(evt);
            }
        });
        rowOptADC.add(RB_TEMP_LIMIT);
        rowOptADC.add(filler6);

        groupOptAdc.add(RB_THROTTLE);
        RB_THROTTLE.setText("Throttle");
        RB_THROTTLE.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_THROTTLEStateChanged(evt);
            }
        });
        rowOptADC.add(RB_THROTTLE);
        rowOptADC.add(filler7);

        groupOptAdc.add(RB_ADC_OPTION_DIS);
        RB_ADC_OPTION_DIS.setSelected(true);
        RB_ADC_OPTION_DIS.setText("None");
        RB_ADC_OPTION_DIS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_ADC_OPTION_DISStateChanged(evt);
            }
        });
        rowOptADC.add(RB_ADC_OPTION_DIS);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(rowOptADC, gridBagConstraints);

        CB_STREET_MODE_ON_START.setText("Street mode enabled on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_STREET_MODE_ON_START, gridBagConstraints);

        CB_STARTUP_BOOST_ON_START.setText("Startup boost enabled  on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_STARTUP_BOOST_ON_START, gridBagConstraints);

        rowTorSensorAdv.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 0, 0));

        CB_TOR_SENSOR_ADV.setText("Torque sensor adv.");
        rowTorSensorAdv.add(CB_TOR_SENSOR_ADV);

        CB_TORQUE_CALIBRATION.setText("Calibrated");
        CB_TORQUE_CALIBRATION.setToolTipText("Enable after calibration");
        CB_TORQUE_CALIBRATION.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_TORQUE_CALIBRATIONStateChanged(evt);
            }
        });
        rowTorSensorAdv.add(CB_TORQUE_CALIBRATION);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(rowTorSensorAdv, gridBagConstraints);

        CB_FIELD_WEAKENING_ENABLED.setText("Field weakening enabled");
        CB_FIELD_WEAKENING_ENABLED.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_FIELD_WEAKENING_ENABLEDStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_FIELD_WEAKENING_ENABLED, gridBagConstraints);

        CB_STARTUP_ASSIST_ENABLED.setText("Startup assist enabled");
        CB_STARTUP_ASSIST_ENABLED.setToolTipText("");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_STARTUP_ASSIST_ENABLED, gridBagConstraints);

        CB_ODO_COMPENSATION.setText("Odometer compensation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_ODO_COMPENSATION, gridBagConstraints);

        CB_SET_PARAM_ON_START.setText("Set parameters on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_SET_PARAM_ON_START, gridBagConstraints);

        CB_AUTO_DISPLAY_DATA.setText("Auto display data with lights on");
        CB_AUTO_DISPLAY_DATA.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_AUTO_DISPLAY_DATAStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_AUTO_DISPLAY_DATA, gridBagConstraints);

        CB_MAX_SPEED_DISPLAY.setText("Set max speed from display");
        CB_MAX_SPEED_DISPLAY.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_MAX_SPEED_DISPLAYStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 30;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(CB_MAX_SPEED_DISPLAY, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 10);
        panelBasicSettings.add(subPanelFunctionSettings, gridBagConstraints);

        jTabbedPane1.addTab("Basic settings", panelBasicSettings);

        panelAssistanceSettings.setPreferredSize(new java.awt.Dimension(844, 552));
        java.awt.GridBagLayout panelAssistanceSettingsLayout = new java.awt.GridBagLayout();
        panelAssistanceSettingsLayout.columnWidths = new int[] {0, 20, 0, 20, 0, 20, 0};
        panelAssistanceSettingsLayout.rowHeights = new int[] {0, 10, 0};
        panelAssistanceSettings.setLayout(panelAssistanceSettingsLayout);

        java.awt.GridBagLayout jPanelPowerAssistLayout = new java.awt.GridBagLayout();
        jPanelPowerAssistLayout.columnWidths = new int[] {0, 8, 0};
        jPanelPowerAssistLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelPowerAssist.setLayout(jPanelPowerAssistLayout);

        headerPowerAssist.setFont(headerPowerAssist.getFont().deriveFont(headerPowerAssist.getFont().getStyle() | java.awt.Font.BOLD, headerPowerAssist.getFont().getSize()+1));
        headerPowerAssist.setText("Power assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(headerPowerAssist, gridBagConstraints);

        jLabel_TF_POWER_ASS_1.setText("Assist level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(jLabel_TF_POWER_ASS_1, gridBagConstraints);

        TF_POWER_ASS_1.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
        TF_POWER_ASS_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_POWER_ASS_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelPowerAssist.add(TF_POWER_ASS_1, gridBagConstraints);

        jLabel_TF_POWER_ASS_2.setText("Assist level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(jLabel_TF_POWER_ASS_2, gridBagConstraints);

        TF_POWER_ASS_2.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
        TF_POWER_ASS_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_POWER_ASS_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelPowerAssist.add(TF_POWER_ASS_2, gridBagConstraints);

        jLabel_TF_POWER_ASS_3.setText("Assist level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(jLabel_TF_POWER_ASS_3, gridBagConstraints);

        TF_POWER_ASS_3.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
        TF_POWER_ASS_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_POWER_ASS_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelPowerAssist.add(TF_POWER_ASS_3, gridBagConstraints);

        jLabel_POWER_ASS_4.setText("Assist level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(jLabel_POWER_ASS_4, gridBagConstraints);

        TF_POWER_ASS_4.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
        TF_POWER_ASS_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_POWER_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelPowerAssist.add(TF_POWER_ASS_4, gridBagConstraints);

        groupAssistOnStart.add(RB_POWER_ON_START);
        RB_POWER_ON_START.setSelected(true);
        RB_POWER_ON_START.setText("Enable on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelPowerAssist.add(RB_POWER_ON_START, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        panelAssistanceSettings.add(subPanelPowerAssist, gridBagConstraints);

        java.awt.GridBagLayout jPanel12Layout = new java.awt.GridBagLayout();
        jPanel12Layout.columnWidths = new int[] {0, 8, 0};
        jPanel12Layout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelTorqueAssist.setLayout(jPanel12Layout);

        headerTorqueAssist.setFont(headerTorqueAssist.getFont().deriveFont(headerTorqueAssist.getFont().getStyle() | java.awt.Font.BOLD, headerTorqueAssist.getFont().getSize()+1));
        headerTorqueAssist.setText("Torque assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(headerTorqueAssist, gridBagConstraints);

        jLabel_TORQUE_ASS_1.setText("Assist level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(jLabel_TORQUE_ASS_1, gridBagConstraints);

        TF_TORQUE_ASS_1.setToolTipText("Max value 254");
        TF_TORQUE_ASS_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TORQUE_ASS_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelTorqueAssist.add(TF_TORQUE_ASS_1, gridBagConstraints);

        jLabel_TORQUE_ASS_2.setText("Assist level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(jLabel_TORQUE_ASS_2, gridBagConstraints);

        TF_TORQUE_ASS_2.setToolTipText("Max value 254");
        TF_TORQUE_ASS_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TORQUE_ASS_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelTorqueAssist.add(TF_TORQUE_ASS_2, gridBagConstraints);

        jLabel_TORQUE_ASS_3.setText("Assist level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(jLabel_TORQUE_ASS_3, gridBagConstraints);

        TF_TORQUE_ASS_3.setToolTipText("Max value 254");
        TF_TORQUE_ASS_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TORQUE_ASS_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelTorqueAssist.add(TF_TORQUE_ASS_3, gridBagConstraints);

        jLabel_TORQUE_ASS_4.setText("Assist level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(jLabel_TORQUE_ASS_4, gridBagConstraints);

        TF_TORQUE_ASS_4.setToolTipText("Max value 254");
        TF_TORQUE_ASS_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TORQUE_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelTorqueAssist.add(TF_TORQUE_ASS_4, gridBagConstraints);

        groupAssistOnStart.add(RB_TORQUE_ON_START);
        RB_TORQUE_ON_START.setText("Enable on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelTorqueAssist.add(RB_TORQUE_ON_START, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        panelAssistanceSettings.add(subPanelTorqueAssist, gridBagConstraints);

        java.awt.GridBagLayout subPanelCadenceAssistLayout = new java.awt.GridBagLayout();
        subPanelCadenceAssistLayout.columnWidths = new int[] {0, 8, 0};
        subPanelCadenceAssistLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelCadenceAssist.setLayout(subPanelCadenceAssistLayout);

        headerCadenceAssist.setFont(headerCadenceAssist.getFont().deriveFont(headerCadenceAssist.getFont().getStyle() | java.awt.Font.BOLD, headerCadenceAssist.getFont().getSize()+1));
        headerCadenceAssist.setText("Cadence assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(headerCadenceAssist, gridBagConstraints);

        jLabel_CADENCE_ASS_1.setText("Assist level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(jLabel_CADENCE_ASS_1, gridBagConstraints);

        TF_CADENCE_ASS_1.setToolTipText("Max value 254");
        TF_CADENCE_ASS_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CADENCE_ASS_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelCadenceAssist.add(TF_CADENCE_ASS_1, gridBagConstraints);

        jLabel_CADENCE_ASS_2.setText("Assist level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(jLabel_CADENCE_ASS_2, gridBagConstraints);

        TF_CADENCE_ASS_2.setToolTipText("Max value 254");
        TF_CADENCE_ASS_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CADENCE_ASS_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelCadenceAssist.add(TF_CADENCE_ASS_2, gridBagConstraints);

        jLabel_CADENCE_ASS_3.setText("Assist level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(jLabel_CADENCE_ASS_3, gridBagConstraints);

        TF_CADENCE_ASS_3.setToolTipText("Max value 254");
        TF_CADENCE_ASS_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CADENCE_ASS_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelCadenceAssist.add(TF_CADENCE_ASS_3, gridBagConstraints);

        jLabel_CADENCE_ASS_4.setText("Assist level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(jLabel_CADENCE_ASS_4, gridBagConstraints);

        TF_CADENCE_ASS_4.setToolTipText("Max value 254");
        TF_CADENCE_ASS_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CADENCE_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelCadenceAssist.add(TF_CADENCE_ASS_4, gridBagConstraints);

        groupAssistOnStart.add(RB_CADENCE_ON_START);
        RB_CADENCE_ON_START.setText("Enable on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCadenceAssist.add(RB_CADENCE_ON_START, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        panelAssistanceSettings.add(subPanelCadenceAssist, gridBagConstraints);

        java.awt.GridBagLayout subPanelEmtbAssistLayout = new java.awt.GridBagLayout();
        subPanelEmtbAssistLayout.columnWidths = new int[] {0, 8, 0};
        subPanelEmtbAssistLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelEmtbAssist.setLayout(subPanelEmtbAssistLayout);

        headerEmtbAssist.setFont(headerEmtbAssist.getFont().deriveFont(headerEmtbAssist.getFont().getStyle() | java.awt.Font.BOLD, headerEmtbAssist.getFont().getSize()+1));
        headerEmtbAssist.setText("eMTB assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(headerEmtbAssist, gridBagConstraints);

        jLabel_EMTB_ASS_1.setText("Assist level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(jLabel_EMTB_ASS_1, gridBagConstraints);

        TF_EMTB_ASS_1.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
        TF_EMTB_ASS_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_EMTB_ASS_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelEmtbAssist.add(TF_EMTB_ASS_1, gridBagConstraints);

        jLabel_EMTB_ASS_2.setText("Assist level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(jLabel_EMTB_ASS_2, gridBagConstraints);

        TF_EMTB_ASS_2.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
        TF_EMTB_ASS_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_EMTB_ASS_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelEmtbAssist.add(TF_EMTB_ASS_2, gridBagConstraints);

        jLabel_EMTB_ASS_3.setText("Assist level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(jLabel_EMTB_ASS_3, gridBagConstraints);

        TF_EMTB_ASS_3.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
        TF_EMTB_ASS_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_EMTB_ASS_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelEmtbAssist.add(TF_EMTB_ASS_3, gridBagConstraints);

        jLabel_EMTB_ASS_4.setText("Assist level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(jLabel_EMTB_ASS_4, gridBagConstraints);

        TF_EMTB_ASS_4.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
        TF_EMTB_ASS_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_EMTB_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelEmtbAssist.add(TF_EMTB_ASS_4, gridBagConstraints);

        groupAssistOnStart.add(RB_EMTB_ON_START);
        RB_EMTB_ON_START.setText("Enable on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelEmtbAssist.add(RB_EMTB_ON_START, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 10);
        panelAssistanceSettings.add(subPanelEmtbAssist, gridBagConstraints);

        java.awt.GridBagLayout subPanelWalkAssistLayout = new java.awt.GridBagLayout();
        subPanelWalkAssistLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        subPanelWalkAssistLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelWalkAssist.setLayout(subPanelWalkAssistLayout);

        headerWalkAssist.setFont(headerWalkAssist.getFont().deriveFont(headerWalkAssist.getFont().getStyle() | java.awt.Font.BOLD, headerWalkAssist.getFont().getSize()+1));
        headerWalkAssist.setText("Walk assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(headerWalkAssist, gridBagConstraints);

        jLabelWalkSpeedUnits.setText("units x10");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(jLabelWalkSpeedUnits, gridBagConstraints);

        jLabel_WALK_ASS_SPEED_1.setText("Speed level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_SPEED_1, gridBagConstraints);

        TF_WALK_ASS_SPEED_1.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_1.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_WALK_ASS_SPEED_1KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_SPEED_1, gridBagConstraints);

        jLabel_WALK_ASS_SPEED_2.setText("Speed level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_SPEED_2, gridBagConstraints);

        TF_WALK_ASS_SPEED_2.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_2.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_WALK_ASS_SPEED_2KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_SPEED_2, gridBagConstraints);

        jLabel_WALK_ASS_SPEED_3.setText("Speed level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_SPEED_3, gridBagConstraints);

        TF_WALK_ASS_SPEED_3.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_3.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_WALK_ASS_SPEED_3KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_SPEED_3, gridBagConstraints);

        jLabel_WALK_ASS_SPEED_4.setText("Speed level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_SPEED_4, gridBagConstraints);

        TF_WALK_ASS_SPEED_4.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_4.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_WALK_ASS_SPEED_4KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_SPEED_4, gridBagConstraints);

        jLabel_WALK_ASS_SPEED_LIMIT.setText("Walk assist speed limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_SPEED_LIMIT, gridBagConstraints);

        TF_WALK_ASS_SPEED_LIMIT.setToolTipText("<html>km/h x10 or mph x10<br>\nMax value 60 (in EU 6 km/h)\n</html>");
        TF_WALK_ASS_SPEED_LIMIT.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_LIMIT.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_SPEED_LIMIT.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_WALK_ASS_SPEED_LIMITKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_SPEED_LIMIT, gridBagConstraints);

        jLabel_WALK_ASS_TIME.setText("Walk assist deb. time");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(jLabel_WALK_ASS_TIME, gridBagConstraints);

        TF_WALK_ASS_TIME.setToolTipText("Max value 255 (0.1 s)\n\n");
        TF_WALK_ASS_TIME.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_WALK_ASS_TIME.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_TIME, gridBagConstraints);

        CB_WALK_TIME_ENA.setText("Walk assist debounce time");
        CB_WALK_TIME_ENA.setToolTipText("Only with brake sensors enabled");
        CB_WALK_TIME_ENA.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_WALK_TIME_ENAStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelWalkAssist.add(CB_WALK_TIME_ENA, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        panelAssistanceSettings.add(subPanelWalkAssist, gridBagConstraints);

        java.awt.GridBagLayout subPanelStreetModeLayout = new java.awt.GridBagLayout();
        subPanelStreetModeLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        subPanelStreetModeLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelStreetMode.setLayout(subPanelStreetModeLayout);

        headerStreetMode.setFont(headerStreetMode.getFont().deriveFont(headerStreetMode.getFont().getStyle() | java.awt.Font.BOLD, headerStreetMode.getFont().getSize()+1));
        headerStreetMode.setText("Street mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(headerStreetMode, gridBagConstraints);

        jLabel_STREET_SPEED_LIM.setText("Street speed limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelStreetMode.add(jLabel_STREET_SPEED_LIM, gridBagConstraints);

        TF_STREET_SPEED_LIM.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_STREET_SPEED_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_STREET_SPEED_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_STREET_SPEED_LIM.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_STREET_SPEED_LIMKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelStreetMode.add(TF_STREET_SPEED_LIM, gridBagConstraints);

        jLabel_STREET_POWER_LIM.setFont(jLabel_STREET_POWER_LIM.getFont().deriveFont(jLabel_STREET_POWER_LIM.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel_STREET_POWER_LIM.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_STREET_POWER_LIM.setText("Street power limit (W)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelStreetMode.add(jLabel_STREET_POWER_LIM, gridBagConstraints);

        TF_STREET_POWER_LIM.setToolTipText("<html>Max nominal value in EU 250 W<br>\nMax peak value approx. 500 W\n</html>");
        TF_STREET_POWER_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_STREET_POWER_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelStreetMode.add(TF_STREET_POWER_LIM, gridBagConstraints);

        CB_STREET_POWER_LIM.setText("Street power limit enabled");
        CB_STREET_POWER_LIM.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                CB_STREET_POWER_LIMStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(CB_STREET_POWER_LIM, gridBagConstraints);

        CB_STREET_THROTTLE.setText("Throttle on street");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(CB_STREET_THROTTLE, gridBagConstraints);

        CB_THROTTLE_LEGAL.setText("Legal");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(CB_THROTTLE_LEGAL, gridBagConstraints);

        CB_STREET_CRUISE.setText("Cruise on street mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(CB_STREET_CRUISE, gridBagConstraints);

        CB_STREET_WALK.setText("Walk assist on street mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelStreetMode.add(CB_STREET_WALK, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelAssistanceSettings.add(subPanelStreetMode, gridBagConstraints);

        java.awt.GridBagLayout subPanelCruiseModeLayout = new java.awt.GridBagLayout();
        subPanelCruiseModeLayout.columnWidths = new int[] {0, 8, 0};
        subPanelCruiseModeLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelCruiseMode.setLayout(subPanelCruiseModeLayout);

        headerCruiseMode.setFont(headerCruiseMode.getFont().deriveFont(headerCruiseMode.getFont().getStyle() | java.awt.Font.BOLD, headerCruiseMode.getFont().getSize()+1));
        headerCruiseMode.setText("Cruise mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(headerCruiseMode, gridBagConstraints);

        jLabelCruiseSpeedUnits.setText("km/h");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(jLabelCruiseSpeedUnits, gridBagConstraints);

        jLabel_CRUISE_ASS_1.setText("Speed level 1 - ECO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(jLabel_CRUISE_ASS_1, gridBagConstraints);

        TF_CRUISE_ASS_1.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_CRUISE_ASS_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_1.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_CRUISE_ASS_1KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(TF_CRUISE_ASS_1, gridBagConstraints);

        jLabel_CRUISE_ASS_2.setText("Speed level 2 - TOUR");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(jLabel_CRUISE_ASS_2, gridBagConstraints);

        TF_CRUISE_ASS_2.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_CRUISE_ASS_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_2.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_CRUISE_ASS_2KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(TF_CRUISE_ASS_2, gridBagConstraints);

        jLabel_CRUISE_ASS_3.setText("Speed level 3 - SPORT");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(jLabel_CRUISE_ASS_3, gridBagConstraints);

        TF_CRUISE_ASS_3.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_CRUISE_ASS_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_3.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_CRUISE_ASS_3KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(TF_CRUISE_ASS_3, gridBagConstraints);

        jLabel_CRUISE_ASS_4.setText("Speed level 4 -TURBO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(jLabel_CRUISE_ASS_4, gridBagConstraints);

        TF_CRUISE_ASS_4.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_CRUISE_ASS_4.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_ASS_4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_CRUISE_ASS_4KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(TF_CRUISE_ASS_4, gridBagConstraints);

        jLabel_CRUISE_SPEED_ENA.setText("Speed cruise enabled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(jLabel_CRUISE_SPEED_ENA, gridBagConstraints);

        TF_CRUISE_SPEED_ENA.setToolTipText("Min speed to enable cruise (km/h or mph)");
        TF_CRUISE_SPEED_ENA.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_SPEED_ENA.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_CRUISE_SPEED_ENA.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_CRUISE_SPEED_ENAKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelCruiseMode.add(TF_CRUISE_SPEED_ENA, gridBagConstraints);

        CB_CRUISE_WHITOUT_PED.setText("Cruise without pedaling");
        CB_CRUISE_WHITOUT_PED.setToolTipText("Only with brake sensors enabled");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelCruiseMode.add(CB_CRUISE_WHITOUT_PED, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelAssistanceSettings.add(subPanelCruiseMode, gridBagConstraints);

        java.awt.GridBagLayout subPanelLightsHybridLayout = new java.awt.GridBagLayout();
        subPanelLightsHybridLayout.columnWidths = new int[] {0, 8, 0};
        subPanelLightsHybridLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelLightsHybrid.setLayout(subPanelLightsHybridLayout);

        headerLights.setFont(headerLights.getFont().deriveFont(headerLights.getFont().getStyle() | java.awt.Font.BOLD, headerLights.getFont().getSize()+1));
        headerLights.setText("Lights configuration");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(headerLights, gridBagConstraints);

        jLabel_LIGHT_MODE_ON_START.setText("<html>Lights mode on startup<br/>mode name");
        jLabel_LIGHT_MODE_ON_START.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel_LIGHT_MODE_ON_START.setMinimumSize(new java.awt.Dimension(160, 34));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_ON_START, gridBagConstraints);

        TF_LIGHT_MODE_ON_START.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF\n</html>");
        TF_LIGHT_MODE_ON_START.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_ON_START.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_ON_START.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_LIGHT_MODE_ON_STARTKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_ON_START, gridBagConstraints);

        jLabel_LIGHT_MODE_1.setText("<html>Mode 1 -<br/>mode name very long goes here");
        jLabel_LIGHT_MODE_1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel_LIGHT_MODE_1.setMinimumSize(new java.awt.Dimension(160, 34));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_1, gridBagConstraints);

        TF_LIGHT_MODE_1.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF\n</html>");
        TF_LIGHT_MODE_1.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_1.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_LIGHT_MODE_1KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_1, gridBagConstraints);

        jLabel_LIGHT_MODE_2.setText("<html>Mode 2 -<br/>mode name very long goes here");
        jLabel_LIGHT_MODE_2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel_LIGHT_MODE_2.setMinimumSize(new java.awt.Dimension(160, 34));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_2, gridBagConstraints);

        TF_LIGHT_MODE_2.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF<br>\nor alternative option settings<br>\n9 - assistance without pedal rotation\n</html>");
        TF_LIGHT_MODE_2.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_2.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_LIGHT_MODE_2KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_2, gridBagConstraints);

        jLabel_LIGHT_MODE_3.setText("<html>Mode 3 -<br/>mode name very long goes here");
        jLabel_LIGHT_MODE_3.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        jLabel_LIGHT_MODE_3.setMinimumSize(new java.awt.Dimension(160, 34));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.5;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_3, gridBagConstraints);

        TF_LIGHT_MODE_3.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF<br>\nor alternative option settings<br>\n10 - assistance with sensors error\n</html>");
        TF_LIGHT_MODE_3.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_3.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_LIGHT_MODE_3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_LIGHT_MODE_3KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.weightx = 0.5;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_3, gridBagConstraints);

        headerHybridAssist.setFont(headerHybridAssist.getFont().deriveFont(headerHybridAssist.getFont().getStyle() | java.awt.Font.BOLD, headerHybridAssist.getFont().getSize()+1));
        headerHybridAssist.setText("Hybrid assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        subPanelLightsHybrid.add(headerHybridAssist, gridBagConstraints);

        groupAssistOnStart.add(RB_HYBRID_ON_START);
        RB_HYBRID_ON_START.setText("Enable on startup");
        RB_HYBRID_ON_START.setToolTipText("Torque & Power");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(RB_HYBRID_ON_START, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        panelAssistanceSettings.add(subPanelLightsHybrid, gridBagConstraints);

        jTabbedPane1.addTab("Assistance settings", panelAssistanceSettings);

        panelAdvancedSettings.setPreferredSize(new java.awt.Dimension(800, 486));
        java.awt.GridBagLayout panelAdvancedSettingsLayout = new java.awt.GridBagLayout();
        panelAdvancedSettingsLayout.columnWidths = new int[] {0, 20, 0, 20, 0};
        panelAdvancedSettingsLayout.rowHeights = new int[] {0};
        panelAdvancedSettings.setLayout(panelAdvancedSettingsLayout);

        java.awt.GridBagLayout jPanel11Layout = new java.awt.GridBagLayout();
        jPanel11Layout.columnWidths = new int[] {0, 8, 0};
        jPanel11Layout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelBatteryCells.setLayout(jPanel11Layout);

        headerBatteryCells.setFont(headerBatteryCells.getFont().deriveFont(headerBatteryCells.getFont().getStyle() | java.awt.Font.BOLD, headerBatteryCells.getFont().getSize()+2));
        headerBatteryCells.setText("Battery cells settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(headerBatteryCells, gridBagConstraints);

        jLabel_BAT_CELL_OVER.setText("Overvoltage (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_OVER, gridBagConstraints);

        TF_BAT_CELL_OVER.setToolTipText("Value 4.25 to 4.35");
        TF_BAT_CELL_OVER.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_OVER, gridBagConstraints);

        jLabel_BAT_CELL_SOC.setText("Reset SOC percentage (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_SOC, gridBagConstraints);

        TF_BAT_CELL_SOC.setToolTipText("Value 4.00 to 4.10");
        TF_BAT_CELL_SOC.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_SOC, gridBagConstraints);

        jLabel_BAT_CELL_FULL.setText("Cell voltage full (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_FULL, gridBagConstraints);

        TF_BAT_CELL_FULL.setToolTipText("Value 3.90 to 4.00");
        TF_BAT_CELL_FULL.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_FULL, gridBagConstraints);

        jLabel_BAT_CELL_3_4.setText("Cell voltage 3/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_3_4, gridBagConstraints);

        TF_BAT_CELL_3_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_3_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_3_4, gridBagConstraints);

        jLabel_BAT_CELL_2_4.setText("Cell voltage 2/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_2_4, gridBagConstraints);

        TF_BAT_CELL_2_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_2_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_2_4, gridBagConstraints);

        jLabel_BAT_CELL_1_4.setText("Cell voltage 1/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_1_4, gridBagConstraints);

        TF_BAT_CELL_1_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_1_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_1_4, gridBagConstraints);

        jLabel_BAT_CELL_5_6.setText("Cell voltage 5/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_5_6, gridBagConstraints);

        TF_BAT_CELL_5_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_5_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_5_6, gridBagConstraints);

        jLabel_BAT_CELL_4_6.setText("Cell voltage 4/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_4_6, gridBagConstraints);

        TF_BAT_CELL_4_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_4_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_4_6, gridBagConstraints);

        jLabel_BAT_CELL_3_6.setText("Cell voltage 3/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_3_6, gridBagConstraints);

        TF_BAT_CELL_3_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_3_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_3_6, gridBagConstraints);

        jLabel_BAT_CELL_2_6.setText("Cell voltage 2/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_2_6, gridBagConstraints);

        TF_BAT_CELL_2_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_2_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_2_6, gridBagConstraints);

        jLabel_BAT_CELL_1_6.setText("Cell voltage 1/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_1_6, gridBagConstraints);

        TF_BAT_CELL_1_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_1_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_1_6, gridBagConstraints);

        jLabel_BAT_CELL_EMPTY.setText("Cell voltage empty (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelBatteryCells.add(jLabel_BAT_CELL_EMPTY, gridBagConstraints);

        TF_BAT_CELL_EMPTY.setToolTipText("<html>Indicative value 2.90<br>\nIt depends on the<br>\ncharacteristics of the cells\n</html>");
        TF_BAT_CELL_EMPTY.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatteryCells.add(TF_BAT_CELL_EMPTY, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        panelAdvancedSettings.add(subPanelBatteryCells, gridBagConstraints);

        java.awt.GridBagLayout jPanel18Layout = new java.awt.GridBagLayout();
        jPanel18Layout.columnWidths = new int[] {0, 8, 0};
        jPanel18Layout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelDisplayAdvanced.setLayout(jPanel18Layout);

        headerDisplayAdvanced.setFont(headerDisplayAdvanced.getFont().deriveFont(headerDisplayAdvanced.getFont().getStyle() | java.awt.Font.BOLD, headerDisplayAdvanced.getFont().getSize()+2));
        headerDisplayAdvanced.setText("Display advanced settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(headerDisplayAdvanced, gridBagConstraints);

        jLabelData1.setText("Data 1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData1, gridBagConstraints);

        TF_DATA_1.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_1.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_1KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_1, gridBagConstraints);

        jLabelData2.setText("Data 2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData2, gridBagConstraints);

        TF_DATA_2.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_2.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_2.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_2KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_2, gridBagConstraints);

        jLabelData3.setText("Data 3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData3, gridBagConstraints);

        TF_DATA_3.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_3.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_3.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_3KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_3, gridBagConstraints);

        jLabelData4.setText("Data 4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData4, gridBagConstraints);

        TF_DATA_4.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_4.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_4.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_4KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_4, gridBagConstraints);

        jLabelData5.setText("Data 5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData5, gridBagConstraints);

        TF_DATA_5.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_5.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_5.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_5KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_5, gridBagConstraints);

        jLabelData6.setText("Data 6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabelData6, gridBagConstraints);

        TF_DATA_6.setToolTipText("<html>0 - motor temperature (°C)<br>\n  1 - battery SOC remaining (%)<br>\n  2 - battery voltage (V)<br>\n  3 - battery current (A)<br>\n  4 - motor power (Watt/10)<br>\n  5 - adc throttle (8 bit)<br>\n  6 - adc torque sensor (10 bit)<br>\n  7 - pedal cadence (rpm)<br>\n  8 - human power(W/10)<br>\n  9 - pedal torque adc delta<br>\n10 - consumed Wh/10\n</html>");
        TF_DATA_6.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_DATA_6.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_DATA_6KeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DATA_6, gridBagConstraints);

        jLabel_DELAY_DATA_1.setText("Time to displayed data 1 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_1, gridBagConstraints);

        TF_DELAY_DATA_1.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_1, gridBagConstraints);

        jLabel_DELAY_DATA_2.setText("Time to displayed data 2 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_2, gridBagConstraints);

        TF_DELAY_DATA_2.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_2, gridBagConstraints);

        jLabel_DELAY_DATA_3.setText("Time to displayed data 3 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_3, gridBagConstraints);

        TF_DELAY_DATA_3.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_3, gridBagConstraints);

        jLabel_DELAY_DATA_4.setText("Time to displayed data 4 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_4, gridBagConstraints);

        TF_DELAY_DATA_4.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_4, gridBagConstraints);

        jLabel_DELAY_DATA_5.setText("Time to displayed data 5 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_5, gridBagConstraints);

        TF_DELAY_DATA_5.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_5.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_5, gridBagConstraints);

        jLabel_DELAY_DATA_6.setText("Time to displayed data 6 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDisplayAdvanced.add(jLabel_DELAY_DATA_6, gridBagConstraints);

        TF_DELAY_DATA_6.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDisplayAdvanced.add(TF_DELAY_DATA_6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        panelAdvancedSettings.add(subPanelDisplayAdvanced, gridBagConstraints);

        java.awt.GridBagLayout subPanelDataOtherLayout = new java.awt.GridBagLayout();
        subPanelDataOtherLayout.columnWidths = new int[] {0, 8, 0, 8, 0, 8, 0, 8, 0};
        subPanelDataOtherLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelDataOther.setLayout(subPanelDataOtherLayout);

        jLabel_NUM_DATA_AUTO_DISPLAY.setText("Number of data displayed at lights on");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_NUM_DATA_AUTO_DISPLAY, gridBagConstraints);

        TF_NUM_DATA_AUTO_DISPLAY.setToolTipText("Value 1 to 6");
        TF_NUM_DATA_AUTO_DISPLAY.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_NUM_DATA_AUTO_DISPLAY.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_NUM_DATA_AUTO_DISPLAY.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                TF_NUM_DATA_AUTO_DISPLAYKeyReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_NUM_DATA_AUTO_DISPLAY, gridBagConstraints);

        jLabel_DELAY_MENU.setText("Time to menu items (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_DELAY_MENU, gridBagConstraints);

        TF_DELAY_MENU.setToolTipText("Max value 60 (0.1 s)");
        TF_DELAY_MENU.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_DELAY_MENU.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_DELAY_MENU, gridBagConstraints);

        jLabelStartupData.setText("Data displayed on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabelStartupData, gridBagConstraints);

        rowStartupData.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        groupStartupDisplay.add(RB_STARTUP_SOC);
        RB_STARTUP_SOC.setSelected(true);
        RB_STARTUP_SOC.setText("Soc %");
        RB_STARTUP_SOC.setToolTipText("");
        rowStartupData.add(RB_STARTUP_SOC);
        rowStartupData.add(filler8);

        groupStartupDisplay.add(RB_STARTUP_VOLTS);
        RB_STARTUP_VOLTS.setText("Volts");
        RB_STARTUP_VOLTS.setToolTipText("");
        rowStartupData.add(RB_STARTUP_VOLTS);
        rowStartupData.add(filler9);

        groupStartupDisplay.add(RB_STARTUP_NONE);
        RB_STARTUP_NONE.setText("None");
        RB_STARTUP_NONE.setToolTipText("");
        rowStartupData.add(RB_STARTUP_NONE);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 4, 0);
        subPanelDataOther.add(rowStartupData, gridBagConstraints);

        jLabelSocCalc.setText("Soc % calculation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabelSocCalc, gridBagConstraints);

        rowSocCalc.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        groupStartupSoc.add(RB_SOC_WH);
        RB_SOC_WH.setText("Wh");
        RB_SOC_WH.setToolTipText("");
        rowSocCalc.add(RB_SOC_WH);
        rowSocCalc.add(filler10);

        groupStartupSoc.add(RB_SOC_AUTO);
        RB_SOC_AUTO.setSelected(true);
        RB_SOC_AUTO.setText("Auto");
        RB_SOC_AUTO.setToolTipText("");
        rowSocCalc.add(RB_SOC_AUTO);
        rowSocCalc.add(filler11);

        groupStartupSoc.add(RB_SOC_VOLTS);
        RB_SOC_VOLTS.setText("Volts");
        RB_SOC_VOLTS.setToolTipText("");
        rowSocCalc.add(RB_SOC_VOLTS);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(rowSocCalc, gridBagConstraints);

        headerOtherSettings.setFont(headerOtherSettings.getFont().deriveFont(headerOtherSettings.getFont().getStyle() | java.awt.Font.BOLD, headerOtherSettings.getFont().getSize()+2));
        headerOtherSettings.setText("Other function settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        subPanelDataOther.add(headerOtherSettings, gridBagConstraints);

        jLabel_COASTER_BRAKE_THRESHOLD.setText("Coaster brake torque threshold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_COASTER_BRAKE_THRESHOLD, gridBagConstraints);

        TF_COASTER_BRAKE_THRESHOLD.setToolTipText("Max value 255 (s)");
        TF_COASTER_BRAKE_THRESHOLD.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_COASTER_BRAKE_THRESHOLD.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_COASTER_BRAKE_THRESHOLD, gridBagConstraints);

        jLabel_ADC_THROTTLE.setText("ADC throttle value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_ADC_THROTTLE, gridBagConstraints);

        jLabel_ADC_THROTTLE_MIN.setText("min");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel_ADC_THROTTLE_MIN, gridBagConstraints);

        TF_ADC_THROTTLE_MIN.setToolTipText("Value 40 to 50");
        TF_ADC_THROTTLE_MIN.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ADC_THROTTLE_MIN.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ADC_THROTTLE_MIN, gridBagConstraints);

        jLabel_ADC_THROTTLE_MAX.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel_ADC_THROTTLE_MAX, gridBagConstraints);

        TF_ADC_THROTTLE_MAX.setToolTipText("Value 170 to 180");
        TF_ADC_THROTTLE_MAX.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ADC_THROTTLE_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ADC_THROTTLE_MAX, gridBagConstraints);

        jLabel_ASSIST_THROTTLE.setText("Throttle assist value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_ASSIST_THROTTLE, gridBagConstraints);

        jLabel_ASSIST_THROTTLE_MIN.setText("min");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel_ASSIST_THROTTLE_MIN, gridBagConstraints);

        TF_ASSIST_THROTTLE_MIN.setToolTipText("Value 0 to 100");
        TF_ASSIST_THROTTLE_MIN.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ASSIST_THROTTLE_MIN.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ASSIST_THROTTLE_MIN, gridBagConstraints);

        jLabel_ASSIST_THROTTLE_MAX.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel_ASSIST_THROTTLE_MAX, gridBagConstraints);

        TF_ASSIST_THROTTLE_MAX.setToolTipText("Value MIN to 255");
        TF_ASSIST_THROTTLE_MAX.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ASSIST_THROTTLE_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ASSIST_THROTTLE_MAX, gridBagConstraints);

        CB_TEMP_ERR_MIN_LIM.setText("Temperature error with min limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(CB_TEMP_ERR_MIN_LIM, gridBagConstraints);

        jLabel_TEMP_MIN_LIM.setText("Motor temperature min limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_TEMP_MIN_LIM, gridBagConstraints);

        TF_TEMP_MIN_LIM.setToolTipText("Max value 75 (°C)");
        TF_TEMP_MIN_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TEMP_MIN_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_TEMP_MIN_LIM, gridBagConstraints);

        jLabel_TEMP_MAX_LIM.setText("Motor temperature max limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_TEMP_MAX_LIM, gridBagConstraints);

        TF_TEMP_MAX_LIM.setToolTipText("Max value 85 (°C)");
        TF_TEMP_MAX_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TEMP_MAX_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_TEMP_MAX_LIM, gridBagConstraints);

        jLabel_MOTOR_BLOCK_TIME.setText("Motor blocked error - threshold time");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_MOTOR_BLOCK_TIME, gridBagConstraints);

        TF_MOTOR_BLOCK_TIME.setToolTipText("Value 1 to 10 (0.1 s)");
        TF_MOTOR_BLOCK_TIME.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_TIME.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_TIME, gridBagConstraints);

        jLabel_MOTOR_BLOCK_CURR.setText("Motor blocked error - threshold current");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_MOTOR_BLOCK_CURR, gridBagConstraints);

        TF_MOTOR_BLOCK_CURR.setToolTipText("Value 1 to 5 (0.1 A)");
        TF_MOTOR_BLOCK_CURR.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_CURR.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_CURR, gridBagConstraints);

        jLabel_MOTOR_BLOCK_ERPS.setText("Motor blocked error - threshold ERPS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel_MOTOR_BLOCK_ERPS, gridBagConstraints);

        TF_MOTOR_BLOCK_ERPS.setToolTipText("Value 10 to 30 (ERPS)");
        TF_MOTOR_BLOCK_ERPS.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_ERPS.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_ERPS, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 10);
        panelAdvancedSettings.add(subPanelDataOther, gridBagConstraints);

        jTabbedPane1.addTab("Advanced settings", panelAdvancedSettings);

        java.awt.GridBagLayout panelRightColumnLayout = new java.awt.GridBagLayout();
        panelRightColumnLayout.columnWidths = new int[] {0};
        panelRightColumnLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        panelRightColumn.setLayout(panelRightColumnLayout);

        jLabelExpSettings.setFont(jLabelExpSettings.getFont().deriveFont(jLabelExpSettings.getFont().getStyle() | java.awt.Font.BOLD, jLabelExpSettings.getFont().getSize()+2));
        jLabelExpSettings.setText("Proven Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(jLabelExpSettings, gridBagConstraints);

        expSet.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        expSet.setFocusCycleRoot(true);
        scrollExpSettings.setViewportView(expSet);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelRightColumn.add(scrollExpSettings, gridBagConstraints);

        jLabelProvenSettings.setFont(jLabelProvenSettings.getFont().deriveFont(jLabelProvenSettings.getFont().getStyle() | java.awt.Font.BOLD, jLabelProvenSettings.getFont().getSize()+2));
        jLabelProvenSettings.setText("Experimental Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
        panelRightColumn.add(jLabelProvenSettings, gridBagConstraints);

        scrollProvenSettings.setViewportView(provSet);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        panelRightColumn.add(scrollProvenSettings, gridBagConstraints);

        jLabelVersion.setText("Version (last commits)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(jLabelVersion, gridBagConstraints);

        LB_LAST_COMMIT.setText("<html>Last commit</html>");
        LB_LAST_COMMIT.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(LB_LAST_COMMIT, gridBagConstraints);

        java.awt.GridBagLayout rowCompileActionsLayout = new java.awt.GridBagLayout();
        rowCompileActionsLayout.columnWidths = new int[] {0, 8, 0};
        rowCompileActionsLayout.rowHeights = new int[] {0};
        rowCompileActions.setLayout(rowCompileActionsLayout);

        BTN_COMPILE.setFont(BTN_COMPILE.getFont().deriveFont(BTN_COMPILE.getFont().getStyle() | java.awt.Font.BOLD));
        BTN_COMPILE.setText("Compile & Flash");
        BTN_COMPILE.setMargin(new java.awt.Insets(4, 8, 4, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        rowCompileActions.add(BTN_COMPILE, gridBagConstraints);

        BTN_CANCEL.setText("Cancel");
        BTN_CANCEL.setEnabled(false);
        BTN_CANCEL.setMargin(new java.awt.Insets(4, 8, 4, 8));
        BTN_CANCEL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BTN_CANCELActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        rowCompileActions.add(BTN_CANCEL, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        panelRightColumn.add(rowCompileActions, gridBagConstraints);

        LB_COMPILE_OUTPUT.setFont(LB_COMPILE_OUTPUT.getFont().deriveFont(LB_COMPILE_OUTPUT.getFont().getStyle() | java.awt.Font.BOLD, LB_COMPILE_OUTPUT.getFont().getSize()+3));
        LB_COMPILE_OUTPUT.setText("Output from flashing");

        scrollCompileOutput.setHorizontalScrollBar(null);

        TA_COMPILE_OUTPUT.setEditable(false);
        TA_COMPILE_OUTPUT.setBackground(new java.awt.Color(255, 255, 255));
        TA_COMPILE_OUTPUT.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        TA_COMPILE_OUTPUT.setLineWrap(true);
        TA_COMPILE_OUTPUT.setWrapStyleWord(true);
        scrollCompileOutput.setViewportView(TA_COMPILE_OUTPUT);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollCompileOutput)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(LB_COMPILE_OUTPUT)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 885, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(panelRightColumn, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelRightColumn, javax.swing.GroupLayout.PREFERRED_SIZE, 481, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(labelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 456, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(LB_COMPILE_OUTPUT)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollCompileOutput, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                .addContainerGap())
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("MotorConfiguration");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void CB_COASTER_BRAKEStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_COASTER_BRAKEStateChanged
        setBrakeSensorFieldsEnabled();
    }//GEN-LAST:event_CB_COASTER_BRAKEStateChanged

    private void CB_WALK_TIME_ENAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_WALK_TIME_ENAStateChanged
        setBrakeSensorFieldsEnabled();
    }//GEN-LAST:event_CB_WALK_TIME_ENAStateChanged

    private void CB_BRAKE_SENSORStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_BRAKE_SENSORStateChanged
        setBrakeSensorFieldsEnabled();
    }//GEN-LAST:event_CB_BRAKE_SENSORStateChanged

    private void RB_THROTTLEStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_THROTTLEStateChanged
        setAdcOptionFieldsEnabled();
    }//GEN-LAST:event_RB_THROTTLEStateChanged

    private void RB_TEMP_LIMITStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_TEMP_LIMITStateChanged
        setAdcOptionFieldsEnabled();
    }//GEN-LAST:event_RB_TEMP_LIMITStateChanged

    private void RB_ADC_OPTION_DISStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_ADC_OPTION_DISStateChanged
        setAdcOptionFieldsEnabled();
    }//GEN-LAST:event_RB_ADC_OPTION_DISStateChanged

    private void TF_DATA_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_1KeyReleased
        updateDataLabel(TF_DATA_1, jLabelData1, 1);
    }//GEN-LAST:event_TF_DATA_1KeyReleased

    private void TF_DATA_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_2KeyReleased
        updateDataLabel(TF_DATA_2, jLabelData2, 2);
    }//GEN-LAST:event_TF_DATA_2KeyReleased

    private void TF_DATA_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_3KeyReleased
        updateDataLabel(TF_DATA_3, jLabelData3, 3);
    }//GEN-LAST:event_TF_DATA_3KeyReleased

    private void TF_DATA_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_4KeyReleased
        updateDataLabel(TF_DATA_4, jLabelData4, 4);
    }//GEN-LAST:event_TF_DATA_4KeyReleased

    private void TF_DATA_5KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_5KeyReleased
        updateDataLabel(TF_DATA_5, jLabelData5, 5);
    }//GEN-LAST:event_TF_DATA_5KeyReleased

    private void TF_DATA_6KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_6KeyReleased
        updateDataLabel(TF_DATA_6, jLabelData6, 6);
    }//GEN-LAST:event_TF_DATA_6KeyReleased

    private void TF_LIGHT_MODE_ON_STARTKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_ON_STARTKeyReleased
        updateLightsFieldLabel(TF_LIGHT_MODE_ON_START, jLabel_LIGHT_MODE_ON_START, 0);
    }//GEN-LAST:event_TF_LIGHT_MODE_ON_STARTKeyReleased

    private void TF_LIGHT_MODE_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_1KeyReleased
        updateLightsFieldLabel(TF_LIGHT_MODE_1, jLabel_LIGHT_MODE_1, 1);
    }//GEN-LAST:event_TF_LIGHT_MODE_1KeyReleased

    private void TF_LIGHT_MODE_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_2KeyReleased
        updateLightsFieldLabel(TF_LIGHT_MODE_1, jLabel_LIGHT_MODE_2, 2);
    }//GEN-LAST:event_TF_LIGHT_MODE_2KeyReleased

    private void TF_LIGHT_MODE_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_3KeyReleased
        updateLightsFieldLabel(TF_LIGHT_MODE_1, jLabel_LIGHT_MODE_3, 3);
    }//GEN-LAST:event_TF_LIGHT_MODE_3KeyReleased

    private void CB_STREET_POWER_LIMStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_STREET_POWER_LIMStateChanged
       setStreetPowerLimitEnabled();
    }//GEN-LAST:event_CB_STREET_POWER_LIMStateChanged

    private void CB_ASS_WITHOUT_PEDStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_ASS_WITHOUT_PEDStateChanged
        setAssistWithoutPedThresholdEnabled();
    }//GEN-LAST:event_CB_ASS_WITHOUT_PEDStateChanged

    private void CB_MAX_SPEED_DISPLAYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_MAX_SPEED_DISPLAYStateChanged
        setMaxSpeedOffroadEnabled();
    }//GEN-LAST:event_CB_MAX_SPEED_DISPLAYStateChanged

    private void CB_LIGHTSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_LIGHTSStateChanged
        setLightsFieldsEnabled();
    }//GEN-LAST:event_CB_LIGHTSStateChanged

    private void CB_WALK_ASSISTStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_WALK_ASSISTStateChanged
        setWalkAssistFieldsEnabled();
    }//GEN-LAST:event_CB_WALK_ASSISTStateChanged

    private void CB_AUTO_DISPLAY_DATAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_AUTO_DISPLAY_DATAStateChanged
        setNumDataAutoDisplayEnabled();
    }//GEN-LAST:event_CB_AUTO_DISPLAY_DATAStateChanged

    private void TF_MAX_SPEEDKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_MAX_SPEEDKeyReleased
        saveFieldInKm(TF_MAX_SPEED, intMaxSpeed, 0);
    }//GEN-LAST:event_TF_MAX_SPEEDKeyReleased

    private void TF_STREET_SPEED_LIMKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_STREET_SPEED_LIMKeyReleased
        saveFieldInKm(TF_STREET_SPEED_LIM, intMaxSpeed, 1);
    }//GEN-LAST:event_TF_STREET_SPEED_LIMKeyReleased

    private void TF_NUM_DATA_AUTO_DISPLAYKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_NUM_DATA_AUTO_DISPLAYKeyReleased
        ensureFieldInRange(TF_NUM_DATA_AUTO_DISPLAY, 1, 6);
    }//GEN-LAST:event_TF_NUM_DATA_AUTO_DISPLAYKeyReleased

    private void CB_TORQUE_CALIBRATIONStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_TORQUE_CALIBRATIONStateChanged
        updateTorqueCalibrationValues();
    }//GEN-LAST:event_CB_TORQUE_CALIBRATIONStateChanged

    private void TF_WALK_ASS_SPEED_LIMITKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_LIMITKeyReleased
        saveFieldInKm(TF_WALK_ASS_SPEED_LIMIT, intWalkSpeed, 0);
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_LIMITKeyReleased

    private void TF_CRUISE_ASS_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_1KeyReleased
        saveFieldInKm(TF_CRUISE_ASS_1, intCruiseSpeed, 1);
    }//GEN-LAST:event_TF_CRUISE_ASS_1KeyReleased

    private void TF_CRUISE_ASS_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_2KeyReleased
        saveFieldInKm(TF_CRUISE_ASS_2, intCruiseSpeed, 2);
    }//GEN-LAST:event_TF_CRUISE_ASS_2KeyReleased

    private void TF_CRUISE_ASS_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_3KeyReleased
        saveFieldInKm(TF_CRUISE_ASS_3, intCruiseSpeed, 3);
    }//GEN-LAST:event_TF_CRUISE_ASS_3KeyReleased

    private void TF_CRUISE_ASS_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_4KeyReleased
        saveFieldInKm(TF_CRUISE_ASS_4, intCruiseSpeed, 4);
    }//GEN-LAST:event_TF_CRUISE_ASS_4KeyReleased

    private void TF_CRUISE_SPEED_ENAKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_SPEED_ENAKeyReleased
        saveFieldInKm(TF_CRUISE_SPEED_ENA, intCruiseSpeed, 0);
    }//GEN-LAST:event_TF_CRUISE_SPEED_ENAKeyReleased

    private void TF_TORQ_ADC_OFFSET_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_OFFSET_ADJKeyReleased
        intTorqueOffsetAdj = handleFieldValueWithAdjustment(TF_TORQ_ADC_OFFSET_ADJ, MIDDLE_OFFSET_ADJ);
        if (intTorqueOffsetAdj > OFFSET_MAX_VALUE) {
            intTorqueOffsetAdj = OFFSET_MAX_VALUE;
            TF_TORQ_ADC_OFFSET_ADJ.setText(String.valueOf(OFFSET_MAX_VALUE - MIDDLE_OFFSET_ADJ));
        }
    }//GEN-LAST:event_TF_TORQ_ADC_OFFSET_ADJKeyReleased

    private void CB_FIELD_WEAKENING_ENABLEDStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_FIELD_WEAKENING_ENABLEDStateChanged
        /*    if (CB_FIELD_WEAKENING_ENABLED.isSelected()) {
            CB_FIELD_WEAKENING_ENABLED.setText("Field weakening enabled - PWM 18.0 kHz"); }
        else {
            CB_FIELD_WEAKENING_ENABLED.setText("Field weakening disabled - PWM 15.6 kHz"); }
         */
    }//GEN-LAST:event_CB_FIELD_WEAKENING_ENABLEDStateChanged

    private void TF_WALK_ASS_SPEED_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_1KeyReleased
        saveFieldInKm(TF_WALK_ASS_SPEED_1, intWalkSpeed, 1);
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_1KeyReleased

    private void TF_WALK_ASS_SPEED_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_2KeyReleased
        saveFieldInKm(TF_WALK_ASS_SPEED_2, intWalkSpeed, 2);
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_2KeyReleased

    private void TF_WALK_ASS_SPEED_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_3KeyReleased
        saveFieldInKm(TF_WALK_ASS_SPEED_3, intWalkSpeed, 3);
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_3KeyReleased

    private void TF_WALK_ASS_SPEED_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_4KeyReleased
        saveFieldInKm(TF_WALK_ASS_SPEED_4, intWalkSpeed, 4);
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_4KeyReleased

    private void RB_UNIT_MILESStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_UNIT_MILESStateChanged
        updateUnits();
    }//GEN-LAST:event_RB_UNIT_MILESStateChanged

    private void TF_TORQ_ADC_RANGE_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_RANGE_ADJKeyReleased
        intTorqueRangeAdj = handleFieldValueWithAdjustment(TF_TORQ_ADC_RANGE_ADJ, MIDDLE_RANGE_ADJ);
    }//GEN-LAST:event_TF_TORQ_ADC_RANGE_ADJKeyReleased

    private void TF_TORQ_ADC_OFFSETKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_OFFSETKeyReleased
        updateTorqueCalibrationValues();
    }//GEN-LAST:event_TF_TORQ_ADC_OFFSETKeyReleased

    private void TF_TORQUE_ADC_MAXKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQUE_ADC_MAXKeyReleased
        updateTorqueCalibrationValues();
    }//GEN-LAST:event_TF_TORQUE_ADC_MAXKeyReleased

    private void TF_TORQ_ADC_ANGLE_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_ANGLE_ADJKeyReleased
        intTorqueAngleAdj = handleFieldValueWithAdjustment(TF_TORQ_ADC_ANGLE_ADJ, MIDDLE_ANGLE_ADJ);
    }//GEN-LAST:event_TF_TORQ_ADC_ANGLE_ADJKeyReleased

    private void RB_UNIT_KILOMETERSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_UNIT_KILOMETERSStateChanged
        updateUnits();
    }//GEN-LAST:event_RB_UNIT_KILOMETERSStateChanged

    private void BTN_CANCELActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTN_CANCELActionPerformed
        if (compileWorker != null) {
            compileWorker.cancel(true);
            compileDone();
        }
    }//GEN-LAST:event_BTN_CANCELActionPerformed

    private void CB_ADC_STEP_ESTIMStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_ADC_STEP_ESTIMStateChanged
        updateTorqueCalibrationValues();
    }//GEN-LAST:event_CB_ADC_STEP_ESTIMStateChanged

    private void CMB_DISPLAY_TYPEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CMB_DISPLAY_TYPEActionPerformed
        setBatteryFieldsEnabled((DisplayType) CMB_DISPLAY_TYPE.getSelectedItem());
    }//GEN-LAST:event_CMB_DISPLAY_TYPEActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BTN_CANCEL;
    private javax.swing.JButton BTN_COMPILE;
    private javax.swing.JCheckBox CB_ADC_STEP_ESTIM;
    private javax.swing.JCheckBox CB_ASS_WITHOUT_PED;
    private javax.swing.JCheckBox CB_AUTO_DISPLAY_DATA;
    private javax.swing.JCheckBox CB_BRAKE_SENSOR;
    private javax.swing.JCheckBox CB_COASTER_BRAKE;
    private javax.swing.JCheckBox CB_CRUISE_WHITOUT_PED;
    private javax.swing.JCheckBox CB_FIELD_WEAKENING_ENABLED;
    private javax.swing.JCheckBox CB_LIGHTS;
    private javax.swing.JCheckBox CB_MAX_SPEED_DISPLAY;
    private javax.swing.JCheckBox CB_ODO_COMPENSATION;
    private javax.swing.JCheckBox CB_SET_PARAM_ON_START;
    private javax.swing.JCheckBox CB_STARTUP_ASSIST_ENABLED;
    private javax.swing.JCheckBox CB_STARTUP_BOOST_ON_START;
    private javax.swing.JCheckBox CB_STREET_CRUISE;
    private javax.swing.JCheckBox CB_STREET_MODE_ON_START;
    private javax.swing.JCheckBox CB_STREET_POWER_LIM;
    private javax.swing.JCheckBox CB_STREET_THROTTLE;
    private javax.swing.JCheckBox CB_STREET_WALK;
    private javax.swing.JCheckBox CB_TEMP_ERR_MIN_LIM;
    private javax.swing.JCheckBox CB_THROTTLE_LEGAL;
    private javax.swing.JCheckBox CB_TORQUE_CALIBRATION;
    private javax.swing.JCheckBox CB_TOR_SENSOR_ADV;
    private javax.swing.JCheckBox CB_WALK_ASSIST;
    private javax.swing.JCheckBox CB_WALK_TIME_ENA;
    private javax.swing.JComboBox<DisplayType> CMB_DISPLAY_TYPE;
    private javax.swing.JLabel LB_COMPILE_OUTPUT;
    private javax.swing.JLabel LB_LAST_COMMIT;
    private javax.swing.JRadioButton RB_ADC_OPTION_DIS;
    private javax.swing.JRadioButton RB_BOOST_AT_ZERO_CADENCE;
    private javax.swing.JRadioButton RB_BOOST_AT_ZERO_SPEED;
    private javax.swing.JRadioButton RB_CADENCE_ON_START;
    private javax.swing.JRadioButton RB_DISPLAY_ALWAY_ON;
    private javax.swing.JRadioButton RB_DISPLAY_WORK_ON;
    private javax.swing.JRadioButton RB_EMTB_ON_START;
    private javax.swing.JRadioButton RB_HYBRID_ON_START;
    private javax.swing.JRadioButton RB_MOTOR_36V;
    private javax.swing.JRadioButton RB_MOTOR_48V;
    private javax.swing.JRadioButton RB_POWER_ON_START;
    private javax.swing.JRadioButton RB_SOC_AUTO;
    private javax.swing.JRadioButton RB_SOC_VOLTS;
    private javax.swing.JRadioButton RB_SOC_WH;
    private javax.swing.JRadioButton RB_STARTUP_NONE;
    private javax.swing.JRadioButton RB_STARTUP_SOC;
    private javax.swing.JRadioButton RB_STARTUP_VOLTS;
    private javax.swing.JRadioButton RB_TEMP_LIMIT;
    private javax.swing.JRadioButton RB_THROTTLE;
    private javax.swing.JRadioButton RB_TORQUE_ON_START;
    private javax.swing.JRadioButton RB_UNIT_KILOMETERS;
    private javax.swing.JRadioButton RB_UNIT_MILES;
    private javax.swing.JTextArea TA_COMPILE_OUTPUT;
    private components.IntField TF_ADC_THROTTLE_MAX;
    private components.IntField TF_ADC_THROTTLE_MIN;
    private components.IntField TF_ASSIST_THROTTLE_MAX;
    private components.IntField TF_ASSIST_THROTTLE_MIN;
    private components.IntField TF_ASS_WITHOUT_PED_THRES;
    private components.IntField TF_BATT_CAPACITY;
    private components.IntField TF_BATT_CAPACITY_CAL;
    private components.IntField TF_BATT_NUM_CELLS;
    private components.IntField TF_BATT_POW_MAX;
    private components.IntField TF_BATT_VOLT_CAL;
    private components.IntField TF_BATT_VOLT_CUT_OFF;
    private components.FloatField TF_BAT_CELL_1_4;
    private components.FloatField TF_BAT_CELL_1_6;
    private components.FloatField TF_BAT_CELL_2_4;
    private components.FloatField TF_BAT_CELL_2_6;
    private components.FloatField TF_BAT_CELL_3_4;
    private components.FloatField TF_BAT_CELL_3_6;
    private components.FloatField TF_BAT_CELL_4_6;
    private components.FloatField TF_BAT_CELL_5_6;
    private components.FloatField TF_BAT_CELL_EMPTY;
    private components.FloatField TF_BAT_CELL_FULL;
    private components.FloatField TF_BAT_CELL_OVER;
    private components.FloatField TF_BAT_CELL_SOC;
    private components.FloatField TF_BAT_CUR_MAX;
    private components.IntField TF_BOOST_CADENCE_STEP;
    private components.IntField TF_BOOST_TORQUE_FACTOR;
    private components.IntField TF_CADENCE_ASS_1;
    private components.IntField TF_CADENCE_ASS_2;
    private components.IntField TF_CADENCE_ASS_3;
    private components.IntField TF_CADENCE_ASS_4;
    private components.IntField TF_COASTER_BRAKE_THRESHOLD;
    private components.IntField TF_CRUISE_ASS_1;
    private components.IntField TF_CRUISE_ASS_2;
    private components.IntField TF_CRUISE_ASS_3;
    private components.IntField TF_CRUISE_ASS_4;
    private components.IntField TF_CRUISE_SPEED_ENA;
    private components.IntField TF_DATA_1;
    private components.IntField TF_DATA_2;
    private components.IntField TF_DATA_3;
    private components.IntField TF_DATA_4;
    private components.IntField TF_DATA_5;
    private components.IntField TF_DATA_6;
    private components.IntField TF_DELAY_DATA_1;
    private components.IntField TF_DELAY_DATA_2;
    private components.IntField TF_DELAY_DATA_3;
    private components.IntField TF_DELAY_DATA_4;
    private components.IntField TF_DELAY_DATA_5;
    private components.IntField TF_DELAY_DATA_6;
    private components.IntField TF_DELAY_MENU;
    private components.IntField TF_EMTB_ASS_1;
    private components.IntField TF_EMTB_ASS_2;
    private components.IntField TF_EMTB_ASS_3;
    private components.IntField TF_EMTB_ASS_4;
    private components.IntField TF_LIGHT_MODE_1;
    private components.IntField TF_LIGHT_MODE_2;
    private components.IntField TF_LIGHT_MODE_3;
    private components.IntField TF_LIGHT_MODE_ON_START;
    private components.IntField TF_MAX_SPEED;
    private components.IntField TF_MOTOR_ACC;
    private components.IntField TF_MOTOR_BLOCK_CURR;
    private components.IntField TF_MOTOR_BLOCK_ERPS;
    private components.IntField TF_MOTOR_BLOCK_TIME;
    private components.IntField TF_MOTOR_DEC;
    private components.IntField TF_NUM_DATA_AUTO_DISPLAY;
    private components.IntField TF_POWER_ASS_1;
    private components.IntField TF_POWER_ASS_2;
    private components.IntField TF_POWER_ASS_3;
    private components.IntField TF_POWER_ASS_4;
    private components.IntField TF_STREET_POWER_LIM;
    private components.IntField TF_STREET_SPEED_LIM;
    private components.IntField TF_TEMP_MAX_LIM;
    private components.IntField TF_TEMP_MIN_LIM;
    private components.IntField TF_TORQUE_ADC_MAX;
    private components.IntField TF_TORQUE_ASS_1;
    private components.IntField TF_TORQUE_ASS_2;
    private components.IntField TF_TORQUE_ASS_3;
    private components.IntField TF_TORQUE_ASS_4;
    private components.IntField TF_TORQ_ADC_ANGLE_ADJ;
    private components.IntField TF_TORQ_ADC_OFFSET;
    private components.IntField TF_TORQ_ADC_OFFSET_ADJ;
    private components.IntField TF_TORQ_ADC_RANGE_ADJ;
    private components.IntField TF_TORQ_PER_ADC_STEP;
    private components.IntField TF_TORQ_PER_ADC_STEP_ADV;
    private components.IntField TF_WALK_ASS_SPEED_1;
    private components.IntField TF_WALK_ASS_SPEED_2;
    private components.IntField TF_WALK_ASS_SPEED_3;
    private components.IntField TF_WALK_ASS_SPEED_4;
    private components.IntField TF_WALK_ASS_SPEED_LIMIT;
    private components.IntField TF_WALK_ASS_TIME;
    private components.IntField TF_WHEEL_CIRCUMF;
    private javax.swing.JList<FileContainer> expSet;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler11;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.Box.Filler filler9;
    private javax.swing.ButtonGroup groupAssistOnStart;
    private javax.swing.ButtonGroup groupDisplayMode;
    private javax.swing.ButtonGroup groupMotorV;
    private javax.swing.ButtonGroup groupOptAdc;
    private javax.swing.ButtonGroup groupStartupBoost;
    private javax.swing.ButtonGroup groupStartupDisplay;
    private javax.swing.ButtonGroup groupStartupSoc;
    private javax.swing.ButtonGroup groupUnits;
    private javax.swing.JLabel headerBatteryCells;
    private javax.swing.JLabel headerBatterySettings;
    private javax.swing.JLabel headerBikeSettings;
    private javax.swing.JLabel headerCadenceAssist;
    private javax.swing.JLabel headerCruiseMode;
    private javax.swing.JLabel headerDisplayAdvanced;
    private javax.swing.JLabel headerDisplaySettings;
    private javax.swing.JLabel headerEmtbAssist;
    private javax.swing.JLabel headerFunctionSettings;
    private javax.swing.JLabel headerHybridAssist;
    private javax.swing.JLabel headerLights;
    private javax.swing.JLabel headerOtherSettings;
    private javax.swing.JLabel headerPowerAssist;
    private javax.swing.JLabel headerStreetMode;
    private javax.swing.JLabel headerTorqueAssist;
    private javax.swing.JLabel headerWalkAssist;
    private javax.swing.JLabel headingMotorSettings;
    private javax.swing.JLabel jLabelCruiseSpeedUnits;
    private javax.swing.JLabel jLabelData1;
    private javax.swing.JLabel jLabelData2;
    private javax.swing.JLabel jLabelData3;
    private javax.swing.JLabel jLabelData4;
    private javax.swing.JLabel jLabelData5;
    private javax.swing.JLabel jLabelData6;
    private javax.swing.JLabel jLabelDisplayMode;
    private javax.swing.JLabel jLabelDisplayType;
    private javax.swing.JLabel jLabelExpSettings;
    private javax.swing.JLabel jLabelOptADC;
    private javax.swing.JLabel jLabelProvenSettings;
    private javax.swing.JLabel jLabelSocCalc;
    private javax.swing.JLabel jLabelStartupData;
    private javax.swing.JLabel jLabelVersion;
    private javax.swing.JLabel jLabelWalkSpeedUnits;
    private javax.swing.JLabel jLabel_ADC_THROTTLE;
    private javax.swing.JLabel jLabel_ADC_THROTTLE_MAX;
    private javax.swing.JLabel jLabel_ADC_THROTTLE_MIN;
    private javax.swing.JLabel jLabel_ASSIST_THROTTLE;
    private javax.swing.JLabel jLabel_ASSIST_THROTTLE_MAX;
    private javax.swing.JLabel jLabel_ASSIST_THROTTLE_MIN;
    private javax.swing.JLabel jLabel_BATT_CAPACITY;
    private javax.swing.JLabel jLabel_BATT_CAPACITY_CAL;
    private javax.swing.JLabel jLabel_BATT_NUM_CELLS;
    private javax.swing.JLabel jLabel_BATT_POW_MAX;
    private javax.swing.JLabel jLabel_BATT_VOLT_CAL;
    private javax.swing.JLabel jLabel_BATT_VOLT_CUT_OFF;
    private javax.swing.JLabel jLabel_BAT_CELL_1_4;
    private javax.swing.JLabel jLabel_BAT_CELL_1_6;
    private javax.swing.JLabel jLabel_BAT_CELL_2_4;
    private javax.swing.JLabel jLabel_BAT_CELL_2_6;
    private javax.swing.JLabel jLabel_BAT_CELL_3_4;
    private javax.swing.JLabel jLabel_BAT_CELL_3_6;
    private javax.swing.JLabel jLabel_BAT_CELL_4_6;
    private javax.swing.JLabel jLabel_BAT_CELL_5_6;
    private javax.swing.JLabel jLabel_BAT_CELL_EMPTY;
    private javax.swing.JLabel jLabel_BAT_CELL_FULL;
    private javax.swing.JLabel jLabel_BAT_CELL_OVER;
    private javax.swing.JLabel jLabel_BAT_CELL_SOC;
    private javax.swing.JLabel jLabel_BAT_CUR_MAX;
    private javax.swing.JLabel jLabel_BOOST_AT_ZERO;
    private javax.swing.JLabel jLabel_BOOST_CADENCE_STEP;
    private javax.swing.JLabel jLabel_BOOST_TORQUE_FACTOR;
    private javax.swing.JLabel jLabel_CADENCE_ASS_1;
    private javax.swing.JLabel jLabel_CADENCE_ASS_2;
    private javax.swing.JLabel jLabel_CADENCE_ASS_3;
    private javax.swing.JLabel jLabel_CADENCE_ASS_4;
    private javax.swing.JLabel jLabel_COASTER_BRAKE_THRESHOLD;
    private javax.swing.JLabel jLabel_CRUISE_ASS_1;
    private javax.swing.JLabel jLabel_CRUISE_ASS_2;
    private javax.swing.JLabel jLabel_CRUISE_ASS_3;
    private javax.swing.JLabel jLabel_CRUISE_ASS_4;
    private javax.swing.JLabel jLabel_CRUISE_SPEED_ENA;
    private javax.swing.JLabel jLabel_DELAY_DATA_1;
    private javax.swing.JLabel jLabel_DELAY_DATA_2;
    private javax.swing.JLabel jLabel_DELAY_DATA_3;
    private javax.swing.JLabel jLabel_DELAY_DATA_4;
    private javax.swing.JLabel jLabel_DELAY_DATA_5;
    private javax.swing.JLabel jLabel_DELAY_DATA_6;
    private javax.swing.JLabel jLabel_DELAY_MENU;
    private javax.swing.JLabel jLabel_EMTB_ASS_1;
    private javax.swing.JLabel jLabel_EMTB_ASS_2;
    private javax.swing.JLabel jLabel_EMTB_ASS_3;
    private javax.swing.JLabel jLabel_EMTB_ASS_4;
    private javax.swing.JLabel jLabel_LIGHT_MODE_1;
    private javax.swing.JLabel jLabel_LIGHT_MODE_2;
    private javax.swing.JLabel jLabel_LIGHT_MODE_3;
    private javax.swing.JLabel jLabel_LIGHT_MODE_ON_START;
    private javax.swing.JLabel jLabel_MAX_SPEED;
    private javax.swing.JLabel jLabel_MOTOR_ACC;
    private javax.swing.JLabel jLabel_MOTOR_BLOCK_CURR;
    private javax.swing.JLabel jLabel_MOTOR_BLOCK_ERPS;
    private javax.swing.JLabel jLabel_MOTOR_BLOCK_TIME;
    private javax.swing.JLabel jLabel_MOTOR_FAST_STOP;
    private javax.swing.JLabel jLabel_MOTOR_V;
    private javax.swing.JLabel jLabel_NUM_DATA_AUTO_DISPLAY;
    private javax.swing.JLabel jLabel_POWER_ASS_4;
    private javax.swing.JLabel jLabel_STREET_POWER_LIM;
    private javax.swing.JLabel jLabel_STREET_SPEED_LIM;
    private javax.swing.JLabel jLabel_TEMP_MAX_LIM;
    private javax.swing.JLabel jLabel_TEMP_MIN_LIM;
    private javax.swing.JLabel jLabel_TF_POWER_ASS_1;
    private javax.swing.JLabel jLabel_TF_POWER_ASS_2;
    private javax.swing.JLabel jLabel_TF_POWER_ASS_3;
    private javax.swing.JLabel jLabel_TORQUE_ASS_1;
    private javax.swing.JLabel jLabel_TORQUE_ASS_2;
    private javax.swing.JLabel jLabel_TORQUE_ASS_3;
    private javax.swing.JLabel jLabel_TORQUE_ASS_4;
    private javax.swing.JLabel jLabel_TORQ_ADC_ANGLE_ADJ;
    private javax.swing.JLabel jLabel_TORQ_ADC_MAX;
    private javax.swing.JLabel jLabel_TORQ_ADC_OFFSET;
    private javax.swing.JLabel jLabel_TORQ_ADC_OFFSET_ADJ;
    private javax.swing.JLabel jLabel_TORQ_ADC_RANGE_ADJ;
    private javax.swing.JLabel jLabel_TORQ_PER_ADC_STEP;
    private javax.swing.JLabel jLabel_TORQ_PER_ADC_STEP_ADV;
    private javax.swing.JLabel jLabel_WALK_ASS_SPEED_1;
    private javax.swing.JLabel jLabel_WALK_ASS_SPEED_2;
    private javax.swing.JLabel jLabel_WALK_ASS_SPEED_3;
    private javax.swing.JLabel jLabel_WALK_ASS_SPEED_4;
    private javax.swing.JLabel jLabel_WALK_ASS_SPEED_LIMIT;
    private javax.swing.JLabel jLabel_WALK_ASS_TIME;
    private javax.swing.JLabel jLabel_WHEEL_CIRCUMF;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel_BOOST_AT_ZERO;
    private javax.swing.JPanel jPanel_MOTOR_V;
    private javax.swing.JTabbedPane jTabbedPane1;
    private java.awt.Label labelTitle;
    private javax.swing.JLabel labelUnits;
    private javax.swing.JPanel panelAdvancedSettings;
    private javax.swing.JPanel panelAssistanceSettings;
    private javax.swing.JPanel panelBasicSettings;
    private javax.swing.JPanel panelRightColumn;
    private javax.swing.JList<FileContainer> provSet;
    private javax.swing.JPanel rowCompileActions;
    private javax.swing.JPanel rowDisplayMode;
    private javax.swing.JPanel rowOptADC;
    private javax.swing.JPanel rowSocCalc;
    private javax.swing.JPanel rowStartupData;
    private javax.swing.JPanel rowTorSensorAdv;
    private javax.swing.JPanel rowUnits;
    private javax.swing.JScrollPane scrollCompileOutput;
    private javax.swing.JScrollPane scrollExpSettings;
    private javax.swing.JScrollPane scrollProvenSettings;
    private javax.swing.JPanel subPanelBatteryCells;
    private javax.swing.JPanel subPanelBatterySettings;
    private javax.swing.JPanel subPanelCadenceAssist;
    private javax.swing.JPanel subPanelCruiseMode;
    private javax.swing.JPanel subPanelDataOther;
    private javax.swing.JPanel subPanelDisplayAdvanced;
    private javax.swing.JPanel subPanelEmtbAssist;
    private javax.swing.JPanel subPanelFunctionSettings;
    private javax.swing.JPanel subPanelLightsHybrid;
    private javax.swing.JPanel subPanelMotorSettings;
    private javax.swing.JPanel subPanelPowerAssist;
    private javax.swing.JPanel subPanelStreetMode;
    private javax.swing.JPanel subPanelTorqueAssist;
    private javax.swing.JPanel subPanelWalkAssist;
    // End of variables declaration//GEN-END:variables
}
