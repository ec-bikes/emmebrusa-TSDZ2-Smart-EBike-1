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
import util.CompileThread;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.ListSelectionModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.JOptionPane;
import javax.swing.JList;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

public class TSDZ2_Configurator extends javax.swing.JFrame {

    private File experimentalSettingsDir;
    private File lastSettingsFile = null;

    DefaultListModel provenSettingsFilesModel = new DefaultListModel();
    DefaultListModel experimentalSettingsFilesModel = new DefaultListModel();
    JList experimentalSettingsList = new JList(experimentalSettingsFilesModel);
    CompileThread compileWorker;

    public class FileContainer {

        public FileContainer(File file) {
            this.file = file;
        }
        public File file;

        @Override
        public String toString() {
            return file.getName();
        }

        public File getFile() {
            return file;
        }
    }

    String[] displayDataArray = {"motor temperature", "battery SOC rem. %", "battery voltage", "battery current", "motor power", "adc throttle 8b", "adc torque sensor 10b", "pedal cadence rpm", "human power", "adc pedal torque delta", "consumed Wh"};
    String[] lightModeArray = {"<br>lights ON", "<br>lights FLASHING", "lights ON and BRAKE-FLASHING brak.", "lights FLASHING and ON when braking", "lights FLASHING BRAKE-FLASHING brak.", "lights ON and ON always braking", "lights ON and BRAKE-FLASHING alw.br.", "lights FLASHING and ON always braking", "lights FLASHING BRAKE-FLASHING alw.br.", "assist without pedal rotation", "assist with sensors error", "field weakening"};

    public int[] intAdcPedalTorqueAngleAdjArray = {160, 138, 120, 107, 96, 88, 80, 74, 70, 66, 63, 59, 56, 52, 50, 47, 44, 42, 39, 37, 36, 35, 34, 33, 32, 31, 30, 29, 28, 27, 26, 25, 24, 23, 22, 21, 20, 19, 18, 17, 16};

    public int intMaxSpeed;
    public int intStreetSpeed;
    public int intWalkSpeed1;
    public int intWalkSpeed2;
    public int intWalkSpeed3;
    public int intWalkSpeed4;
    public int intWalkSpeedLimit;
    public int intCruiseSpeed;
    public int intCruiseSpeed1;
    public int intCruiseSpeed2;
    public int intCruiseSpeed3;
    public int intCruiseSpeed4;
    public int intTorqueAdcStep;
    public int intTorqueAdcStepCalc;
    public int intTorqueAdcOffset;
    public int intTorqueAdcMax;
    public int intTorqueAdcOnWeight;
    public int intTorqueOffsetAdj;
    public int intTorqueRangeAdj;
    public int intTorqueAngleAdj;
    String strTorqueOffsetAdj;
    String strTorqueRangeAdj;
    String strTorqueAngleAdj;

    public static final int WEIGHT_ON_PEDAL = 25; // kg
    public static final int MIDDLE_OFFSET_ADJ = 20;
    public static final int OFFSET_MAX_VALUE = 34; // MIDDLE_OFFSET_ADJ * 2 - 6 (ADC_TORQUE_SENSOR_CALIBRATION_OFFSET)
    public static final int MIDDLE_RANGE_ADJ = 20;
    public static final int MIDDLE_ANGLE_ADJ = 20;

    public void loadSettings(File f) throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(f));
        RB_MOTOR_36V.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_MOTOR_48V.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_TORQUE_CALIBRATION.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_MOTOR_ACC.setText(in.readLine());
        CB_ASS_WITHOUT_PED.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ASS_WITHOUT_PED_THRES.setText(in.readLine());
        intTorqueAdcStep = Integer.parseInt(in.readLine());
        //TF_TORQ_PER_ADC_STEP.setText(in.readLine());
        TF_TORQUE_ADC_MAX.setText(in.readLine());
        TF_BOOST_TORQUE_FACTOR.setText(in.readLine());
        TF_MOTOR_BLOCK_TIME.setText(in.readLine());
        TF_MOTOR_BLOCK_CURR.setText(in.readLine());
        TF_MOTOR_BLOCK_ERPS.setText(in.readLine());
        TF_BOOST_CADENCE_STEP.setText(in.readLine());
        TF_BAT_CUR_MAX.setText(in.readLine());
        TF_BATT_POW_MAX.setText(in.readLine());
        TF_BATT_CAPACITY.setText(in.readLine());
        TF_BATT_NUM_CELLS.setText(in.readLine());
        TF_MOTOR_DEC.setText(in.readLine());
        TF_BATT_VOLT_CUT_OFF.setText(in.readLine());
        TF_BATT_VOLT_CAL.setText(in.readLine());
        TF_BATT_CAPACITY_CAL.setText(in.readLine());
        TF_BAT_CELL_OVER.setText(in.readLine());
        TF_BAT_CELL_SOC.setText(in.readLine());
        TF_BAT_CELL_FULL.setText(in.readLine());
        TF_BAT_CELL_3_4.setText(in.readLine());
        TF_BAT_CELL_2_4.setText(in.readLine());
        TF_BAT_CELL_1_4.setText(in.readLine());
        TF_BAT_CELL_5_6.setText(in.readLine());
        TF_BAT_CELL_4_6.setText(in.readLine());
        TF_BAT_CELL_3_6.setText(in.readLine());
        TF_BAT_CELL_2_6.setText(in.readLine());
        TF_BAT_CELL_1_6.setText(in.readLine());
        TF_BAT_CELL_EMPTY.setText(in.readLine());
        TF_WHEEL_CIRCUMF.setText(in.readLine());
        intMaxSpeed = Integer.parseInt(in.readLine());
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
        TF_LIGHT_MODE_ON_START.setText(in.readLine());
        RB_POWER_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_TORQUE_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_CADENCE_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_EMTB_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_LIGHT_MODE_1.setText(in.readLine());
        TF_LIGHT_MODE_2.setText(in.readLine());
        TF_LIGHT_MODE_3.setText(in.readLine());
        CB_STREET_POWER_LIM.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_STREET_POWER_LIM.setText(in.readLine());
        intStreetSpeed = Integer.parseInt(in.readLine());
        CB_STREET_THROTTLE.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STREET_CRUISE.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ADC_THROTTLE_MIN.setText(in.readLine());
        TF_ADC_THROTTLE_MAX.setText(in.readLine());
        TF_TEMP_MIN_LIM.setText(in.readLine());
        TF_TEMP_MAX_LIM.setText(in.readLine());
        CB_TEMP_ERR_MIN_LIM.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_VLCD6.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_VLCD5.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_XH18.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_DISPLAY_WORK_ON.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_DISPLAY_ALWAY_ON.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_MAX_SPEED_DISPLAY.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_DELAY_MENU.setText(in.readLine());
        CB_COASTER_BRAKE.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_COASTER_BRAKE_THRESHOLD.setText(in.readLine());
        CB_AUTO_DISPLAY_DATA.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_STARTUP_ASSIST_ENABLED.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_DELAY_DATA_1.setText(in.readLine());
        TF_DELAY_DATA_2.setText(in.readLine());
        TF_DELAY_DATA_3.setText(in.readLine());
        TF_DELAY_DATA_4.setText(in.readLine());
        TF_DELAY_DATA_5.setText(in.readLine());
        TF_DELAY_DATA_6.setText(in.readLine());
        TF_DATA_1.setText(in.readLine());
        TF_DATA_2.setText(in.readLine());
        TF_DATA_3.setText(in.readLine());
        TF_DATA_4.setText(in.readLine());
        TF_DATA_5.setText(in.readLine());
        TF_DATA_6.setText(in.readLine());
        TF_POWER_ASS_1.setText(in.readLine());
        TF_POWER_ASS_2.setText(in.readLine());
        TF_POWER_ASS_3.setText(in.readLine());
        TF_POWER_ASS_4.setText(in.readLine());
        TF_TORQUE_ASS_1.setText(in.readLine());
        TF_TORQUE_ASS_2.setText(in.readLine());
        TF_TORQUE_ASS_3.setText(in.readLine());
        TF_TORQUE_ASS_4.setText(in.readLine());
        TF_CADENCE_ASS_1.setText(in.readLine());
        TF_CADENCE_ASS_2.setText(in.readLine());
        TF_CADENCE_ASS_3.setText(in.readLine());
        TF_CADENCE_ASS_4.setText(in.readLine());
        TF_EMTB_ASS_1.setText(in.readLine());
        TF_EMTB_ASS_2.setText(in.readLine());
        TF_EMTB_ASS_3.setText(in.readLine());
        TF_EMTB_ASS_4.setText(in.readLine());
        //TF_WALK_ASS_SPEED_1.setText(in.readLine());
        //TF_WALK_ASS_SPEED_2.setText(in.readLine());
        //TF_WALK_ASS_SPEED_3.setText(in.readLine());
        //TF_WALK_ASS_SPEED_4.setText(in.readLine());
        intWalkSpeed1 = Integer.parseInt(in.readLine());
        intWalkSpeed2 = Integer.parseInt(in.readLine());
        intWalkSpeed3 = Integer.parseInt(in.readLine());
        intWalkSpeed4 = Integer.parseInt(in.readLine());
        intWalkSpeedLimit = Integer.parseInt(in.readLine());
        CB_WALK_TIME_ENA.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_WALK_ASS_TIME.setText(in.readLine());
        intCruiseSpeed1 = Integer.parseInt(in.readLine());
        intCruiseSpeed2 = Integer.parseInt(in.readLine());
        intCruiseSpeed3 = Integer.parseInt(in.readLine());
        intCruiseSpeed4 = Integer.parseInt(in.readLine());
        CB_CRUISE_WHITOUT_PED.setSelected(Boolean.parseBoolean(in.readLine()));
        intCruiseSpeed = Integer.parseInt(in.readLine());
        TF_TORQ_ADC_OFFSET.setText(in.readLine());
        TF_NUM_DATA_AUTO_DISPLAY.setText(in.readLine());
        RB_UNIT_KILOMETERS.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_UNIT_MILES.setSelected(Boolean.parseBoolean(in.readLine()));
        TF_ASSIST_THROTTLE_MIN.setText(in.readLine());
        TF_ASSIST_THROTTLE_MAX.setText(in.readLine());
        CB_STREET_WALK.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_HYBRID_ON_START.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_NONE.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_SOC.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_STARTUP_VOLTS.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_FIELD_WEAKENING_ENABLED.setSelected(Boolean.parseBoolean(in.readLine()));
        strTorqueOffsetAdj = in.readLine();
        strTorqueRangeAdj = in.readLine();
        strTorqueAngleAdj = in.readLine();
        TF_TORQ_PER_ADC_STEP_ADV.setText(in.readLine());
        RB_SOC_AUTO.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_SOC_WH.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_SOC_VOLTS.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_ADC_STEP_ESTIM.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_BOOST_AT_ZERO_CADENCE.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_BOOST_AT_ZERO_SPEED.setSelected(Boolean.parseBoolean(in.readLine()));
        RB_850C.setSelected(Boolean.parseBoolean(in.readLine()));
        CB_THROTTLE_LEGAL.setSelected(Boolean.parseBoolean(in.readLine()));

        in.close();

        if ((!RB_BOOST_AT_ZERO_CADENCE.isSelected()) && (!RB_BOOST_AT_ZERO_SPEED.isSelected())) {
            RB_BOOST_AT_ZERO_CADENCE.setSelected(true);
        }

        TF_TORQ_PER_ADC_STEP.setText(String.valueOf(intTorqueAdcStep));

        if (strTorqueOffsetAdj != null) {
            intTorqueOffsetAdj = Integer.parseInt(strTorqueOffsetAdj);
        } else {
            intTorqueOffsetAdj = MIDDLE_OFFSET_ADJ;
        }

        if (intTorqueOffsetAdj == MIDDLE_OFFSET_ADJ) {
            TF_TORQ_ADC_OFFSET_ADJ.setText("0");
        } else if (intTorqueOffsetAdj < MIDDLE_OFFSET_ADJ) {
            TF_TORQ_ADC_OFFSET_ADJ.setText("-" + String.valueOf(MIDDLE_OFFSET_ADJ - intTorqueOffsetAdj));
        } else {
            TF_TORQ_ADC_OFFSET_ADJ.setText(String.valueOf(intTorqueOffsetAdj - MIDDLE_OFFSET_ADJ));
        }

        if (strTorqueRangeAdj != null) {
            intTorqueRangeAdj = Integer.parseInt(strTorqueRangeAdj);
        } else {
            intTorqueRangeAdj = MIDDLE_RANGE_ADJ;
        }

        if (intTorqueRangeAdj == MIDDLE_RANGE_ADJ) {
            TF_TORQ_ADC_RANGE_ADJ.setText("0");
        } else if (intTorqueRangeAdj < MIDDLE_RANGE_ADJ) {
            TF_TORQ_ADC_RANGE_ADJ.setText("-" + String.valueOf(MIDDLE_RANGE_ADJ - intTorqueRangeAdj));
        } else {
            TF_TORQ_ADC_RANGE_ADJ.setText(String.valueOf(intTorqueRangeAdj - MIDDLE_RANGE_ADJ));
        }

        if (strTorqueAngleAdj != null) {
            intTorqueAngleAdj = Integer.parseInt(strTorqueAngleAdj);
        } else {
            intTorqueAngleAdj = MIDDLE_ANGLE_ADJ;
        }

        if (intTorqueAngleAdj == MIDDLE_ANGLE_ADJ) {
            TF_TORQ_ADC_ANGLE_ADJ.setText("0");
        } else if (intTorqueAngleAdj < MIDDLE_ANGLE_ADJ) {
            TF_TORQ_ADC_ANGLE_ADJ.setText("-" + String.valueOf(MIDDLE_ANGLE_ADJ - intTorqueAngleAdj));
        } else {
            TF_TORQ_ADC_ANGLE_ADJ.setText(String.valueOf(intTorqueAngleAdj - MIDDLE_ANGLE_ADJ));
        }
        /*
                if (CB_FIELD_WEAKENING_ENABLED.isSelected()) {
                    CB_FIELD_WEAKENING_ENABLED.setText("Field weakening enabled - PWM 18.0 kHz"); }
                else {
                    CB_FIELD_WEAKENING_ENABLED.setText("Field weakening disabled - PWM 15.6 kHz"); }
         */
        jLabelMOTOR_BLOCK_CURR.setVisible(false);
        jLabelMOTOR_BLOCK_ERPS.setVisible(false);
        TF_MOTOR_BLOCK_CURR.setVisible(false);
        TF_MOTOR_BLOCK_ERPS.setVisible(false);

        jLabelData1.setText("Data 1 - " + displayDataArray[Integer.parseInt(TF_DATA_1.getText())]);
        jLabelData2.setText("Data 2 - " + displayDataArray[Integer.parseInt(TF_DATA_2.getText())]);
        jLabelData3.setText("Data 3 - " + displayDataArray[Integer.parseInt(TF_DATA_3.getText())]);
        jLabelData4.setText("Data 4 - " + displayDataArray[Integer.parseInt(TF_DATA_4.getText())]);
        jLabelData5.setText("Data 5 - " + displayDataArray[Integer.parseInt(TF_DATA_5.getText())]);
        jLabelData6.setText("Data 6 - " + displayDataArray[Integer.parseInt(TF_DATA_6.getText())]);

        jLabel_LIGHT_MODE_ON_START.setText("<html>Lights mode on startup " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_ON_START.getText())] + "</html>");
        jLabel_LIGHT_MODE_1.setText("<html>Mode 1 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_1.getText())] + "</html>");
        jLabel_LIGHT_MODE_2.setText("<html>Mode 2 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_2.getText())] + "</html>");
        jLabel_LIGHT_MODE_3.setText("<html>Mode 3 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_3.getText())] + "</html>");

        if (RB_UNIT_KILOMETERS.isSelected()) {
            jLabel_MAX_SPEED.setText("Max speed offroad mode (km/h)");
            jLabel_STREET_SPEED_LIM.setText("Street speed limit (km/h)");
            jLabelCruiseSpeedUnits.setText("km/h");
            jLabelWalkSpeedUnits.setText("km/h x10");
            TF_MAX_SPEED.setText(String.valueOf(intMaxSpeed));
            TF_STREET_SPEED_LIM.setText(String.valueOf(intStreetSpeed));
            TF_WALK_ASS_SPEED_1.setText(String.valueOf(intWalkSpeed1));
            TF_WALK_ASS_SPEED_2.setText(String.valueOf(intWalkSpeed2));
            TF_WALK_ASS_SPEED_3.setText(String.valueOf(intWalkSpeed3));
            TF_WALK_ASS_SPEED_4.setText(String.valueOf(intWalkSpeed4));
            TF_WALK_ASS_SPEED_LIMIT.setText(String.valueOf(intWalkSpeedLimit));
            TF_CRUISE_SPEED_ENA.setText(String.valueOf(intCruiseSpeed));
            TF_CRUISE_ASS_1.setText(String.valueOf(intCruiseSpeed1));
            TF_CRUISE_ASS_2.setText(String.valueOf(intCruiseSpeed2));
            TF_CRUISE_ASS_3.setText(String.valueOf(intCruiseSpeed3));
            TF_CRUISE_ASS_4.setText(String.valueOf(intCruiseSpeed4));
        }

        if (RB_UNIT_MILES.isSelected()) {
            jLabel_MAX_SPEED.setText("Max speed offroad mode (mph)");
            jLabel_STREET_SPEED_LIM.setText("Street speed limit (mph)");
            jLabelCruiseSpeedUnits.setText("mph");
            jLabelWalkSpeedUnits.setText("mph x10");
            TF_MAX_SPEED.setText(String.valueOf((intMaxSpeed * 10 + 5) / 16));
            TF_STREET_SPEED_LIM.setText(String.valueOf((intStreetSpeed * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_1.setText(String.valueOf((intWalkSpeed1 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_2.setText(String.valueOf((intWalkSpeed2 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_3.setText(String.valueOf((intWalkSpeed3 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_4.setText(String.valueOf((intWalkSpeed4 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_LIMIT.setText(String.valueOf((intWalkSpeedLimit * 10 + 5) / 16));
            TF_CRUISE_SPEED_ENA.setText(String.valueOf((intCruiseSpeed * 10 + 5) / 16));
            TF_CRUISE_ASS_1.setText(String.valueOf((intCruiseSpeed1 * 10 + 5) / 16));
            TF_CRUISE_ASS_2.setText(String.valueOf((intCruiseSpeed2 * 10 + 5) / 16));
            TF_CRUISE_ASS_3.setText(String.valueOf((intCruiseSpeed3 * 10 + 5) / 16));
            TF_CRUISE_ASS_4.setText(String.valueOf((intCruiseSpeed4 * 10 + 5) / 16));
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader("commits.txt"));
            LB_LAST_COMMIT.setText("<html>" + br.readLine() + "</html>");
            br.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, " " + ex);
        }
    }

    public void AddListItem(File newFile) {

        experimentalSettingsFilesModel.add(0, new FileContainer(newFile));

        expSet.setSelectedIndex(0);
        expSet.repaint();
        // JOptionPane.showMessageDialog(null,experimentalSettingsFilesModel.toString(),"Titel", JOptionPane.PLAIN_MESSAGE);
    }

    public TSDZ2_Configurator() {
        //  Font defaultFont = (Font) UIManager.getLookAndFeelDefaults().get("Label.font");
//         this.boldFont = new Font(defaultFont.getName(), Font.BOLD, defaultFont.getSize());
//         this.redColor = new Color(255,0,0);
//         UIManager.getLookAndFeelDefaults().put("defaultFont", new Font(defaultFont.getName(), Font.PLAIN, defaultFont.getSize()));
//         UIManager.getLookAndFeelDefaults().put("defaultFont", new Font("Tahoma", Font.PLAIN, defaultFont.getSize()));

        // UIManager.getLookAndFeelDefaults().put("Label.font", new Font(defaultFont.getName(), Font.PLAIN, 12));
//        Font defaultFont = new Font("Tahoma", Font.PLAIN, 13);
//        Enumeration keys = UIManager.getDefaults().keys();
//        while (keys.hasMoreElements()) {
//          Object key = keys.nextElement();
//          Object value = UIManager.get(key);
//          if (value instanceof javax.swing.plaf.FontUIResource) {
//            UIManager.put(key, defaultFont);
//          }
//        }
        initComponents();

        this.setLocationRelativeTo(null);

        // update lists
        experimentalSettingsDir = new File(Paths.get(".").toAbsolutePath().normalize().toString());

        while (!Arrays.asList(experimentalSettingsDir.list()).contains("experimental settings")) {
            experimentalSettingsDir = experimentalSettingsDir.getParentFile();
        }
        File provenSettingsDir = new File(experimentalSettingsDir.getAbsolutePath() + File.separator + "proven settings");
        experimentalSettingsDir = new File(experimentalSettingsDir.getAbsolutePath() + File.separator + "experimental settings");

        File[] provenSettingsFiles = provenSettingsDir.listFiles();
        Arrays.sort(provenSettingsFiles);
        for (File file : provenSettingsFiles) {
            provenSettingsFilesModel.addElement(new TSDZ2_Configurator.FileContainer(file));

            if (lastSettingsFile == null) {
                lastSettingsFile = file;
            } else {
                if (file.lastModified() > lastSettingsFile.lastModified()) {
                    lastSettingsFile = file;
                }
            }
        }

        File[] experimentalSettingsFiles = experimentalSettingsDir.listFiles();
        Arrays.sort(experimentalSettingsFiles, Collections.reverseOrder());
        for (File file : experimentalSettingsFiles) {
            experimentalSettingsFilesModel.addElement(new TSDZ2_Configurator.FileContainer(file));
            if (lastSettingsFile == null) {
                lastSettingsFile = file;
            } else {
                if (file.lastModified() > lastSettingsFile.lastModified()) {
                    lastSettingsFile = file;
                }
            }
        }

        experimentalSettingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        experimentalSettingsList.setLayoutOrientation(JList.VERTICAL);

        experimentalSettingsList.setVisibleRowCount(-1);

        expSet.setModel(experimentalSettingsFilesModel);

        JList provenSettingsList = new JList(provenSettingsFilesModel);
        provenSettingsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        provenSettingsList.setLayoutOrientation(JList.VERTICAL);
        provenSettingsList.setVisibleRowCount(-1);

        provSet.setModel(provenSettingsFilesModel);
        scrollProvenSettings.setViewportView(provSet);

        expSet.setSelectedIndex(0);

        expSet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int selectedIndex = expSet.getSelectedIndex();
                    experimentalSettingsList.setSelectedIndex(selectedIndex);
                    loadSettings(((FileContainer) experimentalSettingsList.getSelectedValue()).file);
                    experimentalSettingsList.clearSelection();
                } catch (IOException ex) {
                    Logger.getLogger(TSDZ2_Configurator.class.getName()).log(Level.SEVERE, null, ex);
                }
                experimentalSettingsList.clearSelection();

                //updateDependiencies(false);
            }
        });

        provSet.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    int selectedIndex = provSet.getSelectedIndex();
                    provenSettingsList.setSelectedIndex(selectedIndex);
                    loadSettings(((FileContainer) provenSettingsList.getSelectedValue()).file);
                    provenSettingsList.clearSelection();
                } catch (IOException ex) {
                    Logger.getLogger(TSDZ2_Configurator.class.getName()).log(Level.SEVERE, null, ex);
                }
                provenSettingsList.clearSelection();
                //updateDependiencies(false);
            }
        });

        BTN_COMPILE.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                BTN_COMPILE.setEnabled(false);

                PrintWriter iWriter = null;
                PrintWriter pWriter = null;

                File newFile = new File(experimentalSettingsDir + File.separator + new SimpleDateFormat("yyyyMMdd-HHmmssz").format(new Date()) + ".ini");
                try {
                    AddListItem(newFile);

                    iWriter = new PrintWriter(new BufferedWriter(new FileWriter(newFile)));
                    pWriter = new PrintWriter(new BufferedWriter(new FileWriter("src/controller/config.h")));
                    pWriter.println("/*\r\n"
                            + " * config.h\r\n"
                            + " *\r\n"
                            + " *  Automatically created by TSDS2 Parameter Configurator\r\n"
                            + " *  Author: stancecoke\r\n"
                            + " */\r\n"
                            + "\r\n"
                            + "#ifndef CONFIG_H_\r\n"
                            + "#define CONFIG_H_\r\n");
                    String text_to_save = "";

                    if (RB_MOTOR_36V.isSelected()) {
                        text_to_save = "#define MOTOR_TYPE 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_MOTOR_36V.isSelected());

                    if (RB_MOTOR_48V.isSelected()) {
                        text_to_save = "#define MOTOR_TYPE 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_MOTOR_48V.isSelected());

                    if (CB_TORQUE_CALIBRATION.isSelected()) {
                        text_to_save = "#define TORQUE_SENSOR_CALIBRATED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define TORQUE_SENSOR_CALIBRATED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_TORQUE_CALIBRATION.isSelected());

                    text_to_save = "#define MOTOR_ACCELERATION  " + TF_MOTOR_ACC.getText();
                    iWriter.println(TF_MOTOR_ACC.getText());
                    pWriter.println(text_to_save);

                    if (CB_ASS_WITHOUT_PED.isSelected()) {
                        text_to_save = "#define MOTOR_ASSISTANCE_WITHOUT_PEDAL_ROTATION 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define MOTOR_ASSISTANCE_WITHOUT_PEDAL_ROTATION 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_ASS_WITHOUT_PED.isSelected());

                    text_to_save = "#define ASSISTANCE_WITHOUT_PEDAL_ROTATION_THRESHOLD " + TF_ASS_WITHOUT_PED_THRES.getText();
                    iWriter.println(TF_ASS_WITHOUT_PED_THRES.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_PER_10_BIT_ADC_STEP_X100 " + TF_TORQ_PER_ADC_STEP.getText();
                    iWriter.println(TF_TORQ_PER_ADC_STEP.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_ADC_MAX " + TF_TORQUE_ADC_MAX.getText();
                    iWriter.println(TF_TORQUE_ADC_MAX.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define STARTUP_BOOST_TORQUE_FACTOR " + TF_BOOST_TORQUE_FACTOR.getText();
                    iWriter.println(TF_BOOST_TORQUE_FACTOR.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_BLOCKED_COUNTER_THRESHOLD " + TF_MOTOR_BLOCK_TIME.getText();
                    iWriter.println(TF_MOTOR_BLOCK_TIME.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_BLOCKED_BATTERY_CURRENT_THRESHOLD_X10 " + TF_MOTOR_BLOCK_CURR.getText();
                    iWriter.println(TF_MOTOR_BLOCK_CURR.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_BLOCKED_ERPS_THRESHOLD " + TF_MOTOR_BLOCK_ERPS.getText();
                    iWriter.println(TF_MOTOR_BLOCK_ERPS.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define STARTUP_BOOST_CADENCE_STEP " + TF_BOOST_CADENCE_STEP.getText();
                    iWriter.println(TF_BOOST_CADENCE_STEP.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define BATTERY_CURRENT_MAX " + TF_BAT_CUR_MAX.getText();
                    iWriter.println(TF_BAT_CUR_MAX.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TARGET_MAX_BATTERY_POWER " + TF_BATT_POW_MAX.getText();
                    iWriter.println(TF_BATT_POW_MAX.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TARGET_MAX_BATTERY_CAPACITY " + TF_BATT_CAPACITY.getText();
                    iWriter.println(TF_BATT_CAPACITY.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define BATTERY_CELLS_NUMBER " + TF_BATT_NUM_CELLS.getText();
                    iWriter.println(TF_BATT_NUM_CELLS.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_DECELERATION " + TF_MOTOR_DEC.getText();
                    iWriter.println(TF_MOTOR_DEC.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define BATTERY_LOW_VOLTAGE_CUT_OFF " + TF_BATT_VOLT_CUT_OFF.getText();
                    iWriter.println(TF_BATT_VOLT_CUT_OFF.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define ACTUAL_BATTERY_VOLTAGE_PERCENT " + TF_BATT_VOLT_CAL.getText();
                    iWriter.println(TF_BATT_VOLT_CAL.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define ACTUAL_BATTERY_CAPACITY_PERCENT " + TF_BATT_CAPACITY_CAL.getText();
                    iWriter.println(TF_BATT_CAPACITY_CAL.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_OVERVOLT " + TF_BAT_CELL_OVER.getText();
                    iWriter.println(TF_BAT_CELL_OVER.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_RESET_SOC_PERCENT " + TF_BAT_CELL_SOC.getText();
                    iWriter.println(TF_BAT_CELL_SOC.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_FULL " + TF_BAT_CELL_FULL.getText();
                    iWriter.println(TF_BAT_CELL_FULL.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_3_OF_4 " + TF_BAT_CELL_3_4.getText();
                    iWriter.println(TF_BAT_CELL_3_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_2_OF_4 " + TF_BAT_CELL_2_4.getText();
                    iWriter.println(TF_BAT_CELL_2_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_1_OF_4 " + TF_BAT_CELL_1_4.getText();
                    iWriter.println(TF_BAT_CELL_1_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_5_OF_6 " + TF_BAT_CELL_5_6.getText();
                    iWriter.println(TF_BAT_CELL_5_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_4_OF_6 " + TF_BAT_CELL_4_6.getText();
                    iWriter.println(TF_BAT_CELL_4_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_3_OF_6 " + TF_BAT_CELL_3_6.getText();
                    iWriter.println(TF_BAT_CELL_3_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_2_OF_6 " + TF_BAT_CELL_2_6.getText();
                    iWriter.println(TF_BAT_CELL_2_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_1_OF_6 " + TF_BAT_CELL_1_6.getText();
                    iWriter.println(TF_BAT_CELL_1_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LI_ION_CELL_VOLTS_EMPTY " + TF_BAT_CELL_EMPTY.getText();
                    iWriter.println(TF_BAT_CELL_EMPTY.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define WHEEL_PERIMETER " + TF_WHEEL_CIRCUMF.getText();
                    iWriter.println(TF_WHEEL_CIRCUMF.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define WHEEL_MAX_SPEED " + String.valueOf(intMaxSpeed);
                    iWriter.println(String.valueOf(intMaxSpeed));
                    pWriter.println(text_to_save);

                    if (CB_LIGHTS.isSelected()) {
                        text_to_save = "#define ENABLE_LIGHTS 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_LIGHTS 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_LIGHTS.isSelected());

                    if (CB_WALK_ASSIST.isSelected()) {
                        text_to_save = "#define ENABLE_WALK_ASSIST 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_WALK_ASSIST 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_WALK_ASSIST.isSelected());

                    if (CB_BRAKE_SENSOR.isSelected()) {
                        text_to_save = "#define ENABLE_BRAKE_SENSOR 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_BRAKE_SENSOR 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_BRAKE_SENSOR.isSelected());

                    if (RB_ADC_OPTION_DIS.isSelected()) {
                        text_to_save = "#define ENABLE_THROTTLE 0" + System.getProperty("line.separator") + "#define ENABLE_TEMPERATURE_LIMIT 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_ADC_OPTION_DIS.isSelected());

                    if (RB_THROTTLE.isSelected()) {
                        text_to_save = "#define ENABLE_THROTTLE 1" + System.getProperty("line.separator") + "#define ENABLE_TEMPERATURE_LIMIT 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_THROTTLE.isSelected());

                    if (RB_TEMP_LIMIT.isSelected()) {
                        text_to_save = "#define ENABLE_THROTTLE 0" + System.getProperty("line.separator") + "#define ENABLE_TEMPERATURE_LIMIT 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_TEMP_LIMIT.isSelected());

                    if (CB_STREET_MODE_ON_START.isSelected()) {
                        text_to_save = "#define ENABLE_STREET_MODE_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_STREET_MODE_ON_STARTUP 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STREET_MODE_ON_START.isSelected());

                    if (CB_SET_PARAM_ON_START.isSelected()) {
                        text_to_save = "#define ENABLE_SET_PARAMETER_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_SET_PARAMETER_ON_STARTUP 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_SET_PARAM_ON_START.isSelected());

                    if (CB_ODO_COMPENSATION.isSelected()) {
                        text_to_save = "#define ENABLE_ODOMETER_COMPENSATION 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_ODOMETER_COMPENSATION 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_ODO_COMPENSATION.isSelected());

                    if (CB_STARTUP_BOOST_ON_START.isSelected()) {
                        text_to_save = "#define STARTUP_BOOST_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STARTUP_BOOST_ON_STARTUP 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STARTUP_BOOST_ON_START.isSelected());

                    if (CB_TOR_SENSOR_ADV.isSelected()) {
                        text_to_save = "#define TORQUE_SENSOR_ADV_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define TORQUE_SENSOR_ADV_ON_STARTUP 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_TOR_SENSOR_ADV.isSelected());

                    text_to_save = "#define LIGHTS_CONFIGURATION_ON_STARTUP " + TF_LIGHT_MODE_ON_START.getText();
                    iWriter.println(TF_LIGHT_MODE_ON_START.getText());
                    pWriter.println(text_to_save);

                    if (RB_POWER_ON_START.isSelected()) {
                        text_to_save = "#define RIDING_MODE_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_POWER_ON_START.isSelected());

                    if (RB_TORQUE_ON_START.isSelected()) {
                        text_to_save = "#define RIDING_MODE_ON_STARTUP 2";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_TORQUE_ON_START.isSelected());

                    if (RB_CADENCE_ON_START.isSelected()) {
                        text_to_save = "#define RIDING_MODE_ON_STARTUP 3";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_CADENCE_ON_START.isSelected());

                    if (RB_EMTB_ON_START.isSelected()) {
                        text_to_save = "#define RIDING_MODE_ON_STARTUP 4";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_EMTB_ON_START.isSelected());

                    text_to_save = "#define LIGHTS_CONFIGURATION_1 " + TF_LIGHT_MODE_1.getText();
                    iWriter.println(TF_LIGHT_MODE_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LIGHTS_CONFIGURATION_2 " + TF_LIGHT_MODE_2.getText();
                    iWriter.println(TF_LIGHT_MODE_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define LIGHTS_CONFIGURATION_3 " + TF_LIGHT_MODE_3.getText();
                    iWriter.println(TF_LIGHT_MODE_3.getText());
                    pWriter.println(text_to_save);

                    if (CB_STREET_POWER_LIM.isSelected()) {
                        text_to_save = "#define STREET_MODE_POWER_LIMIT_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STREET_MODE_POWER_LIMIT_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STREET_POWER_LIM.isSelected());

                    text_to_save = "#define STREET_MODE_POWER_LIMIT " + TF_STREET_POWER_LIM.getText();
                    iWriter.println(TF_STREET_POWER_LIM.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define STREET_MODE_SPEED_LIMIT " + String.valueOf(intStreetSpeed);
                    iWriter.println(String.valueOf(intStreetSpeed));
                    pWriter.println(text_to_save);

                    if (CB_STREET_THROTTLE.isSelected()) {
                        text_to_save = "#define STREET_MODE_THROTTLE_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STREET_MODE_THROTTLE_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STREET_THROTTLE.isSelected());

                    if (CB_STREET_CRUISE.isSelected()) {
                        text_to_save = "#define STREET_MODE_CRUISE_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STREET_MODE_CRUISE_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STREET_CRUISE.isSelected());

                    text_to_save = "#define ADC_THROTTLE_MIN_VALUE " + TF_ADC_THROTTLE_MIN.getText();
                    iWriter.println(TF_ADC_THROTTLE_MIN.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define ADC_THROTTLE_MAX_VALUE " + TF_ADC_THROTTLE_MAX.getText();
                    iWriter.println(TF_ADC_THROTTLE_MAX.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_TEMPERATURE_MIN_VALUE_LIMIT " + TF_TEMP_MIN_LIM.getText();
                    iWriter.println(TF_TEMP_MIN_LIM.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define MOTOR_TEMPERATURE_MAX_VALUE_LIMIT " + TF_TEMP_MAX_LIM.getText();
                    iWriter.println(TF_TEMP_MAX_LIM.getText());
                    pWriter.println(text_to_save);

                    if (CB_TEMP_ERR_MIN_LIM.isSelected()) {
                        text_to_save = "#define ENABLE_TEMPERATURE_ERROR_MIN_LIMIT 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_TEMPERATURE_ERROR_MIN_LIMIT 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_TEMP_ERR_MIN_LIM.isSelected());

                    if (RB_VLCD6.isSelected()) {
                        text_to_save = "#define ENABLE_VLCD6 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_VLCD6 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_VLCD6.isSelected());

                    if (RB_VLCD5.isSelected()) {
                        text_to_save = "#define ENABLE_VLCD5 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_VLCD5 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_VLCD5.isSelected());

                    if (RB_XH18.isSelected()) {
                        text_to_save = "#define ENABLE_XH18 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_XH18 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_XH18.isSelected());

                    if (RB_DISPLAY_WORK_ON.isSelected()) {
                        text_to_save = "#define ENABLE_DISPLAY_WORKING_FLAG 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_DISPLAY_WORKING_FLAG 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_DISPLAY_WORK_ON.isSelected());

                    if (RB_DISPLAY_ALWAY_ON.isSelected()) {
                        text_to_save = "#define ENABLE_DISPLAY_ALWAYS_ON 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_DISPLAY_ALWAYS_ON 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_DISPLAY_ALWAY_ON.isSelected());

                    if (CB_MAX_SPEED_DISPLAY.isSelected()) {
                        text_to_save = "#define ENABLE_WHEEL_MAX_SPEED_FROM_DISPLAY 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_WHEEL_MAX_SPEED_FROM_DISPLAY 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_MAX_SPEED_DISPLAY.isSelected());

                    text_to_save = "#define DELAY_MENU_ON " + TF_DELAY_MENU.getText();
                    iWriter.println(TF_DELAY_MENU.getText());
                    pWriter.println(text_to_save);

                    if (CB_COASTER_BRAKE.isSelected()) {
                        text_to_save = "#define COASTER_BRAKE_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define COASTER_BRAKE_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_COASTER_BRAKE.isSelected());

                    text_to_save = "#define COASTER_BRAKE_TORQUE_THRESHOLD " + TF_COASTER_BRAKE_THRESHOLD.getText();
                    iWriter.println(TF_COASTER_BRAKE_THRESHOLD.getText());
                    pWriter.println(text_to_save);

                    if (CB_AUTO_DISPLAY_DATA.isSelected()) {
                        text_to_save = "#define ENABLE_AUTO_DATA_DISPLAY 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_AUTO_DATA_DISPLAY 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_AUTO_DISPLAY_DATA.isSelected());

                    if (CB_STARTUP_ASSIST_ENABLED.isSelected()) {
                        text_to_save = "#define STARTUP_ASSIST_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STARTUP_ASSIST_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STARTUP_ASSIST_ENABLED.isSelected());

                    text_to_save = "#define DELAY_DISPLAY_DATA_1 " + TF_DELAY_DATA_1.getText();
                    iWriter.println(TF_DELAY_DATA_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DELAY_DISPLAY_DATA_2 " + TF_DELAY_DATA_2.getText();
                    iWriter.println(TF_DELAY_DATA_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DELAY_DISPLAY_DATA_3 " + TF_DELAY_DATA_3.getText();
                    iWriter.println(TF_DELAY_DATA_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DELAY_DISPLAY_DATA_4 " + TF_DELAY_DATA_4.getText();
                    iWriter.println(TF_DELAY_DATA_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DELAY_DISPLAY_DATA_5 " + TF_DELAY_DATA_5.getText();
                    iWriter.println(TF_DELAY_DATA_5.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DELAY_DISPLAY_DATA_6 " + TF_DELAY_DATA_6.getText();
                    iWriter.println(TF_DELAY_DATA_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_1 " + TF_DATA_1.getText();
                    iWriter.println(TF_DATA_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_2 " + TF_DATA_2.getText();
                    iWriter.println(TF_DATA_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_3 " + TF_DATA_3.getText();
                    iWriter.println(TF_DATA_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_4 " + TF_DATA_4.getText();
                    iWriter.println(TF_DATA_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_5 " + TF_DATA_5.getText();
                    iWriter.println(TF_DATA_5.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define DISPLAY_DATA_6 " + TF_DATA_6.getText();
                    iWriter.println(TF_DATA_6.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define POWER_ASSIST_LEVEL_1 " + TF_POWER_ASS_1.getText();
                    iWriter.println(TF_POWER_ASS_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define POWER_ASSIST_LEVEL_2 " + TF_POWER_ASS_2.getText();
                    iWriter.println(TF_POWER_ASS_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define POWER_ASSIST_LEVEL_3 " + TF_POWER_ASS_3.getText();
                    iWriter.println(TF_POWER_ASS_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define POWER_ASSIST_LEVEL_4 " + TF_POWER_ASS_4.getText();
                    iWriter.println(TF_POWER_ASS_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TORQUE_ASSIST_LEVEL_1 " + TF_TORQUE_ASS_1.getText();
                    iWriter.println(TF_TORQUE_ASS_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TORQUE_ASSIST_LEVEL_2 " + TF_TORQUE_ASS_2.getText();
                    iWriter.println(TF_TORQUE_ASS_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TORQUE_ASSIST_LEVEL_3 " + TF_TORQUE_ASS_3.getText();
                    iWriter.println(TF_TORQUE_ASS_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define TORQUE_ASSIST_LEVEL_4 " + TF_TORQUE_ASS_4.getText();
                    iWriter.println(TF_TORQUE_ASS_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define CADENCE_ASSIST_LEVEL_1 " + TF_CADENCE_ASS_1.getText();
                    iWriter.println(TF_CADENCE_ASS_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define CADENCE_ASSIST_LEVEL_2 " + TF_CADENCE_ASS_2.getText();
                    iWriter.println(TF_CADENCE_ASS_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define CADENCE_ASSIST_LEVEL_3 " + TF_CADENCE_ASS_3.getText();
                    iWriter.println(TF_CADENCE_ASS_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define CADENCE_ASSIST_LEVEL_4 " + TF_CADENCE_ASS_4.getText();
                    iWriter.println(TF_CADENCE_ASS_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define EMTB_ASSIST_LEVEL_1 " + TF_EMTB_ASS_1.getText();
                    iWriter.println(TF_EMTB_ASS_1.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define EMTB_ASSIST_LEVEL_2 " + TF_EMTB_ASS_2.getText();
                    iWriter.println(TF_EMTB_ASS_2.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define EMTB_ASSIST_LEVEL_3 " + TF_EMTB_ASS_3.getText();
                    iWriter.println(TF_EMTB_ASS_3.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define EMTB_ASSIST_LEVEL_4 " + TF_EMTB_ASS_4.getText();
                    iWriter.println(TF_EMTB_ASS_4.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define WALK_ASSIST_LEVEL_1 " + String.valueOf(intWalkSpeed1);
                    iWriter.println(String.valueOf(intWalkSpeed1));
                    pWriter.println(text_to_save);

                    text_to_save = "#define WALK_ASSIST_LEVEL_2 " + String.valueOf(intWalkSpeed2);
                    iWriter.println(String.valueOf(intWalkSpeed2));
                    pWriter.println(text_to_save);

                    text_to_save = "#define WALK_ASSIST_LEVEL_3 " + String.valueOf(intWalkSpeed3);
                    iWriter.println(String.valueOf(intWalkSpeed3));
                    pWriter.println(text_to_save);

                    text_to_save = "#define WALK_ASSIST_LEVEL_4 " + String.valueOf(intWalkSpeed4);
                    iWriter.println(String.valueOf(intWalkSpeed4));
                    pWriter.println(text_to_save);

                    text_to_save = "#define WALK_ASSIST_THRESHOLD_SPEED " + String.valueOf(intWalkSpeedLimit);
                    iWriter.println(String.valueOf(intWalkSpeedLimit));
                    pWriter.println(text_to_save);

                    if (CB_WALK_TIME_ENA.isSelected()) {
                        text_to_save = "#define WALK_ASSIST_DEBOUNCE_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define WALK_ASSIST_DEBOUNCE_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_WALK_TIME_ENA.isSelected());

                    text_to_save = "#define WALK_ASSIST_DEBOUNCE_TIME " + TF_WALK_ASS_TIME.getText();
                    iWriter.println(TF_WALK_ASS_TIME.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define CRUISE_TARGET_SPEED_LEVEL_1 " + String.valueOf(intCruiseSpeed1);
                    iWriter.println(String.valueOf(intCruiseSpeed1));
                    pWriter.println(text_to_save);

                    text_to_save = "#define CRUISE_TARGET_SPEED_LEVEL_2 " + String.valueOf(intCruiseSpeed2);
                    iWriter.println(String.valueOf(intCruiseSpeed2));
                    pWriter.println(text_to_save);

                    text_to_save = "#define CRUISE_TARGET_SPEED_LEVEL_3 " + String.valueOf(intCruiseSpeed3);
                    iWriter.println(String.valueOf(intCruiseSpeed3));
                    pWriter.println(text_to_save);

                    text_to_save = "#define CRUISE_TARGET_SPEED_LEVEL_4 " + String.valueOf(intCruiseSpeed4);
                    iWriter.println(String.valueOf(intCruiseSpeed4));
                    pWriter.println(text_to_save);

                    if (CB_CRUISE_WHITOUT_PED.isSelected()) {
                        text_to_save = "#define CRUISE_MODE_WALK_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define CRUISE_MODE_WALK_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_CRUISE_WHITOUT_PED.isSelected());

                    text_to_save = "#define CRUISE_THRESHOLD_SPEED " + String.valueOf(intCruiseSpeed);
                    iWriter.println(String.valueOf(intCruiseSpeed));
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_ADC_OFFSET " + TF_TORQ_ADC_OFFSET.getText();
                    iWriter.println(TF_TORQ_ADC_OFFSET.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define AUTO_DATA_NUMBER_DISPLAY " + TF_NUM_DATA_AUTO_DISPLAY.getText();
                    iWriter.println(TF_NUM_DATA_AUTO_DISPLAY.getText());
                    pWriter.println(text_to_save);

                    if (RB_UNIT_KILOMETERS.isSelected()) {
                        text_to_save = "#define UNITS_TYPE 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_UNIT_KILOMETERS.isSelected());

                    if (RB_UNIT_MILES.isSelected()) {
                        text_to_save = "#define UNITS_TYPE 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_UNIT_MILES.isSelected());

                    text_to_save = "#define ASSIST_THROTTLE_MIN_VALUE " + TF_ASSIST_THROTTLE_MIN.getText();
                    iWriter.println(TF_ASSIST_THROTTLE_MIN.getText());
                    pWriter.println(text_to_save);

                    text_to_save = "#define ASSIST_THROTTLE_MAX_VALUE " + TF_ASSIST_THROTTLE_MAX.getText();
                    iWriter.println(TF_ASSIST_THROTTLE_MAX.getText());
                    pWriter.println(text_to_save);

                    if (CB_STREET_WALK.isSelected()) {
                        text_to_save = "#define STREET_MODE_WALK_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STREET_MODE_WALK_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_STREET_WALK.isSelected());

                    if (RB_HYBRID_ON_START.isSelected()) {
                        text_to_save = "#define RIDING_MODE_ON_STARTUP 5";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_HYBRID_ON_START.isSelected());

                    if (RB_STARTUP_NONE.isSelected()) {
                        text_to_save = "#define DATA_DISPLAY_ON_STARTUP 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_STARTUP_NONE.isSelected());

                    if (RB_STARTUP_SOC.isSelected()) {
                        text_to_save = "#define DATA_DISPLAY_ON_STARTUP 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_STARTUP_SOC.isSelected());

                    if (RB_STARTUP_VOLTS.isSelected()) {
                        text_to_save = "#define DATA_DISPLAY_ON_STARTUP 2";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_STARTUP_VOLTS.isSelected());

                    if (CB_FIELD_WEAKENING_ENABLED.isSelected()) {
                        text_to_save = "#define FIELD_WEAKENING_ENABLED 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define FIELD_WEAKENING_ENABLED 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_FIELD_WEAKENING_ENABLED.isSelected());

                    text_to_save = "#define PEDAL_TORQUE_ADC_OFFSET_ADJ " + String.valueOf(intTorqueOffsetAdj);
                    iWriter.println(String.valueOf(intTorqueOffsetAdj));
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_ADC_RANGE_ADJ " + String.valueOf(intTorqueRangeAdj);
                    iWriter.println(String.valueOf(intTorqueRangeAdj));
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_ADC_ANGLE_ADJ " + String.valueOf(intAdcPedalTorqueAngleAdjArray[intTorqueAngleAdj]);
                    iWriter.println(String.valueOf(intTorqueAngleAdj));
                    pWriter.println(text_to_save);

                    text_to_save = "#define PEDAL_TORQUE_PER_10_BIT_ADC_STEP_ADV_X100 " + TF_TORQ_PER_ADC_STEP_ADV.getText();
                    iWriter.println(TF_TORQ_PER_ADC_STEP_ADV.getText());
                    pWriter.println(text_to_save);

                    if (RB_SOC_AUTO.isSelected()) {
                        text_to_save = "#define SOC_PERCENT_CALC 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_SOC_AUTO.isSelected());

                    if (RB_SOC_WH.isSelected()) {
                        text_to_save = "#define SOC_PERCENT_CALC 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_SOC_WH.isSelected());

                    if (RB_SOC_VOLTS.isSelected()) {
                        text_to_save = "#define SOC_PERCENT_CALC 2";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_SOC_VOLTS.isSelected());

                    /*
                                        if (CB_ADC_STEP_CALC.isSelected()) {
						text_to_save = "#define TORQUE_ADC_STEP_EXTIMATED 1";
						pWriter.println(text_to_save);
					}
                                        else {
						text_to_save = "#define TORQUE_ADC_STEP_EXTIMATED 0";
						pWriter.println(text_to_save);
					}
                     */
                    iWriter.println(CB_ADC_STEP_ESTIM.isSelected());

                    if (RB_BOOST_AT_ZERO_CADENCE.isSelected()) {
                        text_to_save = "#define STARTUP_BOOST_AT_ZERO 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_BOOST_AT_ZERO_CADENCE.isSelected());

                    if (RB_BOOST_AT_ZERO_SPEED.isSelected()) {
                        text_to_save = "#define STARTUP_BOOST_AT_ZERO 1";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_BOOST_AT_ZERO_SPEED.isSelected());

                    if (RB_850C.isSelected()) {
                        text_to_save = "#define ENABLE_850C 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define ENABLE_850C 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(RB_850C.isSelected());

                    if (CB_THROTTLE_LEGAL.isSelected()) {
                        text_to_save = "#define STREET_MODE_THROTTLE_LEGAL 1";
                        pWriter.println(text_to_save);
                    } else {
                        text_to_save = "#define STREET_MODE_THROTTLE_LEGAL 0";
                        pWriter.println(text_to_save);
                    }
                    iWriter.println(CB_THROTTLE_LEGAL.isSelected());

                    pWriter.println("\r\n#endif /* CONFIG_H_ */");

                    iWriter.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace(System.err);
                } finally {
                    if (pWriter != null) {
                        pWriter.flush();
                        pWriter.close();
                    }
                }

                compileAndFlash(newFile.getName());
            }
        }); // end of jButton1.addActionListener

        if (lastSettingsFile != null) {
            try {
                loadSettings(lastSettingsFile);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, " " + ex);
            }
            provenSettingsList.clearSelection();
            experimentalSettingsList.clearSelection();
            //updateDependiencies(false);
        }
    }

    private void compileAndFlash(String fileName) {
        BTN_COMPILE.setEnabled(false);
        BTN_CANCEL.setEnabled(true);

        compileWorker = new CompileThread(TA_COMPILE_OUTPUT, fileName);
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jRadioButton1 = new javax.swing.JRadioButton();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        buttonGroup6 = new javax.swing.ButtonGroup();
        buttonGroup7 = new javax.swing.ButtonGroup();
        buttonGroup8 = new javax.swing.ButtonGroup();
        buttonGroup9 = new javax.swing.ButtonGroup();
        labelTitle = new java.awt.Label();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        panelBasicSettings = new javax.swing.JPanel();
        subPanelMotorSettings = new javax.swing.JPanel();
        headingMotorSettings = new javax.swing.JLabel();
        jLabel_MOTOR_V = new javax.swing.JLabel();
        jLabel_MOTOR_ACC = new javax.swing.JLabel();
        TF_MOTOR_ACC = new javax.swing.JTextField();
        jLabel_MOTOR_FAST_STOP = new javax.swing.JLabel();
        TF_MOTOR_DEC = new javax.swing.JTextField();
        CB_ASS_WITHOUT_PED = new javax.swing.JCheckBox();
        TF_ASS_WITHOUT_PED_THRES = new javax.swing.JTextField();
        TF_TORQ_PER_ADC_STEP = new javax.swing.JTextField();
        jLabel_TORQ_PER_ADC_STEP_ADV = new javax.swing.JLabel();
        TF_TORQ_PER_ADC_STEP_ADV = new javax.swing.JTextField();
        jLabel_TORQ_ADC_OFFSET_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_OFFSET_ADJ = new javax.swing.JTextField();
        jLabel_TORQ_ADC_RANGE_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_RANGE_ADJ = new javax.swing.JTextField();
        jLabel_TORQ_ADC_ANGLE_ADJ = new javax.swing.JLabel();
        TF_TORQ_ADC_ANGLE_ADJ = new javax.swing.JTextField();
        jLabel_TORQ_ADC_OFFSET = new javax.swing.JLabel();
        TF_TORQ_ADC_OFFSET = new javax.swing.JTextField();
        jLabel_TORQ_ADC_MAX = new javax.swing.JLabel();
        TF_TORQUE_ADC_MAX = new javax.swing.JTextField();
        jLabel_BOOST_TORQUE_FACTOR = new javax.swing.JLabel();
        TF_BOOST_TORQUE_FACTOR = new javax.swing.JTextField();
        jLabel_BOOST_CADENCE_STEP = new javax.swing.JLabel();
        TF_BOOST_CADENCE_STEP = new javax.swing.JTextField();
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
        TF_BAT_CUR_MAX = new javax.swing.JTextField();
        jLabel_BATT_POW_MAX = new javax.swing.JLabel();
        TF_BATT_POW_MAX = new javax.swing.JTextField();
        jLabel_BATT_CAPACITY = new javax.swing.JLabel();
        TF_BATT_CAPACITY = new javax.swing.JTextField();
        jLabel_BATT_NUM_CELLS = new javax.swing.JLabel();
        TF_BATT_NUM_CELLS = new javax.swing.JTextField();
        jLabel_BATT_VOLT_CAL = new javax.swing.JLabel();
        TF_BATT_VOLT_CAL = new javax.swing.JTextField();
        jLabel_BATT_CAPACITY_CAL = new javax.swing.JLabel();
        TF_BATT_CAPACITY_CAL = new javax.swing.JTextField();
        jLabel_BATT_VOLT_CUT_OFF = new javax.swing.JLabel();
        TF_BATT_VOLT_CUT_OFF = new javax.swing.JTextField();
        headerDisplaySettings = new javax.swing.JLabel();
        jLabelDisplayType = new javax.swing.JLabel();
        rowDisplayType = new javax.swing.JPanel();
        RB_VLCD5 = new javax.swing.JRadioButton();
        RB_XH18 = new javax.swing.JRadioButton();
        RB_VLCD6 = new javax.swing.JRadioButton();
        RB_850C = new javax.swing.JRadioButton();
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
        TF_WHEEL_CIRCUMF = new javax.swing.JTextField();
        TF_MAX_SPEED = new javax.swing.JTextField();
        jLabel_MAX_SPEED = new javax.swing.JLabel();
        jLabel_WHEEL_CIRCUMF = new javax.swing.JLabel();
        subPanelFunctionSettings = new javax.swing.JPanel();
        jLabel39 = new javax.swing.JLabel();
        CB_LIGHTS = new javax.swing.JCheckBox();
        CB_WALK_ASSIST = new javax.swing.JCheckBox();
        CB_BRAKE_SENSOR = new javax.swing.JCheckBox();
        CB_COASTER_BRAKE = new javax.swing.JCheckBox();
        jLabelOptADC = new javax.swing.JLabel();
        rowOptADC = new javax.swing.JPanel();
        RB_TEMP_LIMIT = new javax.swing.JRadioButton();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_ADC_OPTION_DIS = new javax.swing.JRadioButton();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_THROTTLE = new javax.swing.JRadioButton();
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
        TF_POWER_ASS_1 = new javax.swing.JTextField();
        jLabel_TF_POWER_ASS_2 = new javax.swing.JLabel();
        TF_POWER_ASS_2 = new javax.swing.JTextField();
        jLabel_TF_POWER_ASS_3 = new javax.swing.JLabel();
        TF_POWER_ASS_3 = new javax.swing.JTextField();
        jLabel_POWER_ASS_4 = new javax.swing.JLabel();
        TF_POWER_ASS_4 = new javax.swing.JTextField();
        RB_POWER_ON_START = new javax.swing.JRadioButton();
        subPanelTorqueAssist = new javax.swing.JPanel();
        headerTorqueAssist = new javax.swing.JLabel();
        jLabel_TORQUE_ASS_1 = new javax.swing.JLabel();
        TF_TORQUE_ASS_1 = new javax.swing.JTextField();
        jLabel_TORQUE_ASS_2 = new javax.swing.JLabel();
        TF_TORQUE_ASS_2 = new javax.swing.JTextField();
        jLabel_TORQUE_ASS_3 = new javax.swing.JLabel();
        TF_TORQUE_ASS_3 = new javax.swing.JTextField();
        jLabel_TORQUE_ASS_4 = new javax.swing.JLabel();
        TF_TORQUE_ASS_4 = new javax.swing.JTextField();
        RB_TORQUE_ON_START = new javax.swing.JRadioButton();
        subPanelCadenceAssist = new javax.swing.JPanel();
        headerCadenceAssist = new javax.swing.JLabel();
        jLabel_CADENCE_ASS_1 = new javax.swing.JLabel();
        TF_CADENCE_ASS_1 = new javax.swing.JTextField();
        jLabel_CADENCE_ASS_2 = new javax.swing.JLabel();
        TF_CADENCE_ASS_2 = new javax.swing.JTextField();
        jLabel_CADENCE_ASS_3 = new javax.swing.JLabel();
        TF_CADENCE_ASS_3 = new javax.swing.JTextField();
        jLabel_CADENCE_ASS_4 = new javax.swing.JLabel();
        TF_CADENCE_ASS_4 = new javax.swing.JTextField();
        RB_CADENCE_ON_START = new javax.swing.JRadioButton();
        subPanelEmtbAssist = new javax.swing.JPanel();
        headerEmtbAssist = new javax.swing.JLabel();
        jLabel_EMTB_ASS_1 = new javax.swing.JLabel();
        TF_EMTB_ASS_1 = new javax.swing.JTextField();
        jLabel_EMTB_ASS_2 = new javax.swing.JLabel();
        TF_EMTB_ASS_2 = new javax.swing.JTextField();
        jLabel_EMTB_ASS_3 = new javax.swing.JLabel();
        TF_EMTB_ASS_3 = new javax.swing.JTextField();
        jLabel_EMTB_ASS_4 = new javax.swing.JLabel();
        TF_EMTB_ASS_4 = new javax.swing.JTextField();
        RB_EMTB_ON_START = new javax.swing.JRadioButton();
        subPanelWalkAssist = new javax.swing.JPanel();
        headerWalkAssist = new javax.swing.JLabel();
        jLabelWalkSpeedUnits = new javax.swing.JLabel();
        jLabel_WALK_ASS_SPEED_1 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_1 = new javax.swing.JTextField();
        jLabel_WALK_ASS_SPEED_2 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_2 = new javax.swing.JTextField();
        jLabel_WALK_ASS_SPEED_3 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_3 = new javax.swing.JTextField();
        jLabel_WALK_ASS_SPEED_4 = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_4 = new javax.swing.JTextField();
        jLabel_WALK_ASS_SPEED_LIMIT = new javax.swing.JLabel();
        TF_WALK_ASS_SPEED_LIMIT = new javax.swing.JTextField();
        jLabel_WALK_ASS_TIME = new javax.swing.JLabel();
        TF_WALK_ASS_TIME = new javax.swing.JTextField();
        CB_WALK_TIME_ENA = new javax.swing.JCheckBox();
        subPanelStreetMode = new javax.swing.JPanel();
        headerStreetMode = new javax.swing.JLabel();
        jLabel_STREET_SPEED_LIM = new javax.swing.JLabel();
        TF_STREET_SPEED_LIM = new javax.swing.JTextField();
        jLabel_STREET_POWER_LIM = new javax.swing.JLabel();
        TF_STREET_POWER_LIM = new javax.swing.JTextField();
        CB_STREET_POWER_LIM = new javax.swing.JCheckBox();
        CB_STREET_THROTTLE = new javax.swing.JCheckBox();
        CB_THROTTLE_LEGAL = new javax.swing.JCheckBox();
        CB_STREET_CRUISE = new javax.swing.JCheckBox();
        CB_STREET_WALK = new javax.swing.JCheckBox();
        subPanelCruiseMode = new javax.swing.JPanel();
        headerCruiseMode = new javax.swing.JLabel();
        jLabelCruiseSpeedUnits = new javax.swing.JLabel();
        jLabel_CRUISE_ASS_1 = new javax.swing.JLabel();
        TF_CRUISE_ASS_1 = new javax.swing.JTextField();
        jLabel_CRUISE_ASS_2 = new javax.swing.JLabel();
        TF_CRUISE_ASS_2 = new javax.swing.JTextField();
        jLabel_CRUISE_ASS_3 = new javax.swing.JLabel();
        TF_CRUISE_ASS_3 = new javax.swing.JTextField();
        jLabel_CRUISE_ASS_4 = new javax.swing.JLabel();
        TF_CRUISE_ASS_4 = new javax.swing.JTextField();
        jLabel_CRUISE_SPEED_ENA = new javax.swing.JLabel();
        TF_CRUISE_SPEED_ENA = new javax.swing.JTextField();
        CB_CRUISE_WHITOUT_PED = new javax.swing.JCheckBox();
        subPanelLightsHybrid = new javax.swing.JPanel();
        headerLights = new javax.swing.JLabel();
        jLabel_LIGHT_MODE_ON_START = new javax.swing.JLabel();
        TF_LIGHT_MODE_ON_START = new javax.swing.JTextField();
        jLabel_LIGHT_MODE_1 = new javax.swing.JLabel();
        TF_LIGHT_MODE_1 = new javax.swing.JTextField();
        jLabel_LIGHT_MODE_2 = new javax.swing.JLabel();
        TF_LIGHT_MODE_2 = new javax.swing.JTextField();
        jLabel_LIGHT_MODE_3 = new javax.swing.JLabel();
        TF_LIGHT_MODE_3 = new javax.swing.JTextField();
        headerHybridAssist = new javax.swing.JLabel();
        RB_HYBRID_ON_START = new javax.swing.JRadioButton();
        panelAdvancedSettings = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel35 = new javax.swing.JLabel();
        jLabel36 = new javax.swing.JLabel();
        TF_BAT_CELL_FULL = new javax.swing.JTextField();
        jLabel37 = new javax.swing.JLabel();
        TF_BAT_CELL_OVER = new javax.swing.JTextField();
        jLabel38 = new javax.swing.JLabel();
        TF_BAT_CELL_SOC = new javax.swing.JTextField();
        jLabel40 = new javax.swing.JLabel();
        TF_BAT_CELL_3_4 = new javax.swing.JTextField();
        jLabel41 = new javax.swing.JLabel();
        TF_BAT_CELL_2_4 = new javax.swing.JTextField();
        jLabel42 = new javax.swing.JLabel();
        TF_BAT_CELL_1_4 = new javax.swing.JTextField();
        jLabel75 = new javax.swing.JLabel();
        TF_BAT_CELL_5_6 = new javax.swing.JTextField();
        jLabel81 = new javax.swing.JLabel();
        TF_BAT_CELL_4_6 = new javax.swing.JTextField();
        jLabel82 = new javax.swing.JLabel();
        TF_BAT_CELL_3_6 = new javax.swing.JTextField();
        TF_BAT_CELL_2_6 = new javax.swing.JTextField();
        jLabel83 = new javax.swing.JLabel();
        TF_BAT_CELL_1_6 = new javax.swing.JTextField();
        jLabel84 = new javax.swing.JLabel();
        TF_BAT_CELL_EMPTY = new javax.swing.JTextField();
        jLabel85 = new javax.swing.JLabel();
        jPanel18 = new javax.swing.JPanel();
        jLabel86 = new javax.swing.JLabel();
        jLabel89 = new javax.swing.JLabel();
        TF_DELAY_DATA_1 = new javax.swing.JTextField();
        jLabelData1 = new javax.swing.JLabel();
        TF_DATA_1 = new javax.swing.JTextField();
        TF_DATA_2 = new javax.swing.JTextField();
        jLabelData2 = new javax.swing.JLabel();
        TF_DATA_3 = new javax.swing.JTextField();
        jLabelData3 = new javax.swing.JLabel();
        jLabelData4 = new javax.swing.JLabel();
        TF_DATA_4 = new javax.swing.JTextField();
        TF_DATA_5 = new javax.swing.JTextField();
        jLabelData5 = new javax.swing.JLabel();
        jLabelData6 = new javax.swing.JLabel();
        TF_DATA_6 = new javax.swing.JTextField();
        jLabel96 = new javax.swing.JLabel();
        TF_DELAY_DATA_2 = new javax.swing.JTextField();
        jLabel97 = new javax.swing.JLabel();
        TF_DELAY_DATA_3 = new javax.swing.JTextField();
        jLabel98 = new javax.swing.JLabel();
        TF_DELAY_DATA_4 = new javax.swing.JTextField();
        jLabel99 = new javax.swing.JLabel();
        TF_DELAY_DATA_5 = new javax.swing.JTextField();
        jLabel100 = new javax.swing.JLabel();
        TF_DELAY_DATA_6 = new javax.swing.JTextField();
        subPanelDataOther = new javax.swing.JPanel();
        jLabel101 = new javax.swing.JLabel();
        jLabel107 = new javax.swing.JLabel();
        jLabel102 = new javax.swing.JLabel();
        jLabel103 = new javax.swing.JLabel();
        jLabel108 = new javax.swing.JLabel();
        TF_ADC_THROTTLE_MIN = new javax.swing.JTextField();
        TF_ADC_THROTTLE_MAX = new javax.swing.JTextField();
        CB_TEMP_ERR_MIN_LIM = new javax.swing.JCheckBox();
        jLabel104 = new javax.swing.JLabel();
        TF_TEMP_MIN_LIM = new javax.swing.JTextField();
        jLabel105 = new javax.swing.JLabel();
        TF_TEMP_MAX_LIM = new javax.swing.JTextField();
        jLabel106 = new javax.swing.JLabel();
        TF_MOTOR_BLOCK_TIME = new javax.swing.JTextField();
        TF_DELAY_MENU = new javax.swing.JTextField();
        jLabel87 = new javax.swing.JLabel();
        jLabel90 = new javax.swing.JLabel();
        TF_NUM_DATA_AUTO_DISPLAY = new javax.swing.JTextField();
        jLabel109 = new javax.swing.JLabel();
        TF_ASSIST_THROTTLE_MIN = new javax.swing.JTextField();
        jLabel110 = new javax.swing.JLabel();
        TF_ASSIST_THROTTLE_MAX = new javax.swing.JTextField();
        jLabel91 = new javax.swing.JLabel();
        jLabelCoasterBrakeThreshld = new javax.swing.JLabel();
        TF_COASTER_BRAKE_THRESHOLD = new javax.swing.JTextField();
        jLabel94 = new javax.swing.JLabel();
        TF_MOTOR_BLOCK_ERPS = new javax.swing.JTextField();
        TF_MOTOR_BLOCK_CURR = new javax.swing.JTextField();
        jLabelMOTOR_BLOCK_CURR = new javax.swing.JLabel();
        jLabelMOTOR_BLOCK_ERPS = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        RB_STARTUP_SOC = new javax.swing.JRadioButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_STARTUP_VOLTS = new javax.swing.JRadioButton();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_STARTUP_NONE = new javax.swing.JRadioButton();
        jPanel3 = new javax.swing.JPanel();
        RB_SOC_WH = new javax.swing.JRadioButton();
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_SOC_AUTO = new javax.swing.JRadioButton();
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 0), new java.awt.Dimension(8, 32767));
        RB_SOC_VOLTS = new javax.swing.JRadioButton();
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
        BTN_SAVE = new javax.swing.JButton();
        BTN_COMPILE = new javax.swing.JButton();
        BTN_CANCEL = new javax.swing.JButton();
        LB_COMPILE_OUTPUT = new javax.swing.JLabel();
        scrollCompileOutput = new javax.swing.JScrollPane();
        TA_COMPILE_OUTPUT = new javax.swing.JTextArea();

        jRadioButton1.setText("jRadioButton1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("TSDZ2 Parameter Configurator 4.3 for Open Source Firmware v20.1C.2-2");
        setMaximumSize(new java.awt.Dimension(1196, 758));
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

        headingMotorSettings.setFont(headingMotorSettings.getFont().deriveFont(headingMotorSettings.getFont().getStyle() | java.awt.Font.BOLD));
        headingMotorSettings.setText("Motor settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(headingMotorSettings, gridBagConstraints);

        jLabel_MOTOR_V.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_V.setText("Motor type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_V, gridBagConstraints);

        jLabel_MOTOR_ACC.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_ACC.setText("Motor acceleration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_ACC, gridBagConstraints);

        TF_MOTOR_ACC.setText("25");
        TF_MOTOR_ACC.setToolTipText("<html>MAX VALUE<br>\n36 volt motor, 36 volt battery = 35<br>\n36 volt motor, 48 volt battery = 5<br>\n36 volt motor, 52 volt battery = 0<br>\n48 volt motor, 36 volt battery = 45<br>\n48 volt motor, 48 volt battery = 35<br>\n48 volt motor, 52 volt battery = 30\n</html>");
        TF_MOTOR_ACC.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_MOTOR_ACC, gridBagConstraints);

        jLabel_MOTOR_FAST_STOP.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_MOTOR_FAST_STOP.setText("Motor deceleration (%)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelMotorSettings.add(jLabel_MOTOR_FAST_STOP, gridBagConstraints);

        TF_MOTOR_DEC.setText("0");
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

        TF_ASS_WITHOUT_PED_THRES.setText("20");
        TF_ASS_WITHOUT_PED_THRES.setToolTipText("<html>Max value 100<br>\nRecommended range 10 to 30\n</html>");
        TF_ASS_WITHOUT_PED_THRES.setEnabled(CB_ASS_WITHOUT_PED.isSelected());
        TF_ASS_WITHOUT_PED_THRES.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(TF_ASS_WITHOUT_PED_THRES, gridBagConstraints);

        TF_TORQ_PER_ADC_STEP.setText("67");
        TF_TORQ_PER_ADC_STEP.setToolTipText("<html>\nDefault value 67<br>\nOptional calibration\n</html>");
        TF_TORQ_PER_ADC_STEP.setEnabled(!CB_ADC_STEP_ESTIM.isSelected());
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

        TF_TORQ_PER_ADC_STEP_ADV.setText("34");
        TF_TORQ_PER_ADC_STEP_ADV.setToolTipText("<html>\nDefault value 34<br>\nOptional calibration\n</html>");
        TF_TORQ_PER_ADC_STEP_ADV.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        TF_TORQ_ADC_OFFSET_ADJ.setText("0");
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

        TF_TORQ_ADC_RANGE_ADJ.setText("0");
        TF_TORQ_ADC_RANGE_ADJ.setToolTipText("<html>\nValue -20 to 20<br>\nDefault 0\n</html>");
        TF_TORQ_ADC_RANGE_ADJ.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        TF_TORQ_ADC_ANGLE_ADJ.setText("0");
        TF_TORQ_ADC_ANGLE_ADJ.setToolTipText("<html>\nValue -20 to 20<br>\nDefault 0\n</html>");
        TF_TORQ_ADC_ANGLE_ADJ.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        TF_TORQ_ADC_OFFSET.setText("150");
        TF_TORQ_ADC_OFFSET.setToolTipText("<html>\nInsert value read on calibration<br>\nMax 250\n</html>");
        TF_TORQ_ADC_OFFSET.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        TF_TORQUE_ADC_MAX.setText("300");
        TF_TORQUE_ADC_MAX.setToolTipText("<html>\nInsert value read on calibration<br>\nMax 500\n</html>");
        TF_TORQUE_ADC_MAX.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        TF_BOOST_TORQUE_FACTOR.setText("300");
        TF_BOOST_TORQUE_FACTOR.setToolTipText("<html>Max value 500<br>\nRecommended range 200 to 300\n</html>");
        TF_BOOST_TORQUE_FACTOR.setPreferredSize(new java.awt.Dimension(45, 23));
        TF_BOOST_TORQUE_FACTOR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                TF_BOOST_TORQUE_FACTORActionPerformed(evt);
            }
        });
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

        TF_BOOST_CADENCE_STEP.setText("20");
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

        buttonGroup9.add(RB_BOOST_AT_ZERO_CADENCE);
        RB_BOOST_AT_ZERO_CADENCE.setText("cadence");
        jPanel_BOOST_AT_ZERO.add(RB_BOOST_AT_ZERO_CADENCE);
        jPanel_BOOST_AT_ZERO.add(filler2);

        buttonGroup9.add(RB_BOOST_AT_ZERO_SPEED);
        RB_BOOST_AT_ZERO_SPEED.setText("speed");
        jPanel_BOOST_AT_ZERO.add(RB_BOOST_AT_ZERO_SPEED);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelMotorSettings.add(jPanel_BOOST_AT_ZERO, gridBagConstraints);

        jPanel_MOTOR_V.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        buttonGroup1.add(RB_MOTOR_36V);
        RB_MOTOR_36V.setText("36V");
        jPanel_MOTOR_V.add(RB_MOTOR_36V);
        jPanel_MOTOR_V.add(filler1);

        buttonGroup1.add(RB_MOTOR_48V);
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
        CB_ADC_STEP_ESTIM.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
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

        headerBatterySettings.setFont(headerBatterySettings.getFont().deriveFont(headerBatterySettings.getFont().getStyle() | java.awt.Font.BOLD));
        headerBatterySettings.setText("Battery settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerBatterySettings, gridBagConstraints);

        jLabel_BAT_CUR_MAX.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BAT_CUR_MAX.setText("Battery current max (A)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BAT_CUR_MAX, gridBagConstraints);

        TF_BAT_CUR_MAX.setText("17");
        TF_BAT_CUR_MAX.setToolTipText("<html>Max value<br>\n17 A for 36 V<br>\n12 A for 48 V\n</html>");
        TF_BAT_CUR_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BAT_CUR_MAX, gridBagConstraints);

        jLabel_BATT_POW_MAX.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_POW_MAX.setText("Battery power max (W)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_POW_MAX, gridBagConstraints);

        TF_BATT_POW_MAX.setText("500");
        TF_BATT_POW_MAX.setToolTipText("<html>Motor power limit in offroad mode<br>\nMax value depends on the rated<br>\nmotor power and the battery capacity\n</html>");
        TF_BATT_POW_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_POW_MAX, gridBagConstraints);

        jLabel_BATT_CAPACITY.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_CAPACITY.setText("Battery capacity (Wh)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_CAPACITY, gridBagConstraints);

        TF_BATT_CAPACITY.setText("630");
        TF_BATT_CAPACITY.setToolTipText("<html>To calculate<br>\nBattery Volt x Ah\n</html>\n");
        TF_BATT_CAPACITY.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_CAPACITY, gridBagConstraints);

        jLabel_BATT_NUM_CELLS.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_NUM_CELLS.setText("Battery cells number");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_NUM_CELLS, gridBagConstraints);

        TF_BATT_NUM_CELLS.setText("10");
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

        TF_BATT_VOLT_CAL.setText("100");
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

        TF_BATT_CAPACITY_CAL.setText("100");
        TF_BATT_CAPACITY_CAL.setToolTipText("<html>Starting to 100%<br>\nwith the% remaining when battery is low<br>\ncalculate the actual%\n</html>");
        TF_BATT_CAPACITY_CAL.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_CAPACITY_CAL, gridBagConstraints);

        jLabel_BATT_VOLT_CUT_OFF.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_BATT_VOLT_CUT_OFF.setText("Battery voltage cut off (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabel_BATT_VOLT_CUT_OFF, gridBagConstraints);

        TF_BATT_VOLT_CUT_OFF.setText("29");
        TF_BATT_VOLT_CUT_OFF.setToolTipText("<html>Indicative value 29 for 36 V<br>\n38 for 48 V, It depends on the<br>\ncharacteristics of the battery\n</html>");
        TF_BATT_VOLT_CUT_OFF.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_BATT_VOLT_CUT_OFF, gridBagConstraints);

        headerDisplaySettings.setFont(headerDisplaySettings.getFont().deriveFont(headerDisplaySettings.getFont().getStyle() | java.awt.Font.BOLD));
        headerDisplaySettings.setText("Display settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerDisplaySettings, gridBagConstraints);

        jLabelDisplayType.setForeground(new java.awt.Color(255, 0, 0));
        jLabelDisplayType.setText("Type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabelDisplayType, gridBagConstraints);

        java.awt.GridBagLayout rowDisplayTypeLayout = new java.awt.GridBagLayout();
        rowDisplayTypeLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        rowDisplayTypeLayout.rowHeights = new int[] {0, 4, 0};
        rowDisplayType.setLayout(rowDisplayTypeLayout);

        buttonGroup2.add(RB_VLCD5);
        RB_VLCD5.setText("VLCD5");
        RB_VLCD5.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_VLCD5StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rowDisplayType.add(RB_VLCD5, gridBagConstraints);

        buttonGroup2.add(RB_XH18);
        RB_XH18.setText("XH18");
        RB_XH18.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_XH18StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rowDisplayType.add(RB_XH18, gridBagConstraints);

        buttonGroup2.add(RB_VLCD6);
        RB_VLCD6.setText("VLCD6");
        RB_VLCD6.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_VLCD6StateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        rowDisplayType.add(RB_VLCD6, gridBagConstraints);

        buttonGroup2.add(RB_850C);
        RB_850C.setText("850C");
        RB_850C.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_850CStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        rowDisplayType.add(RB_850C, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(rowDisplayType, gridBagConstraints);

        jLabelDisplayMode.setText("Mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(jLabelDisplayMode, gridBagConstraints);

        rowDisplayMode.setLayout(new javax.swing.BoxLayout(rowDisplayMode, javax.swing.BoxLayout.LINE_AXIS));

        buttonGroup4.add(RB_DISPLAY_ALWAY_ON);
        RB_DISPLAY_ALWAY_ON.setText("Always on");
        rowDisplayMode.add(RB_DISPLAY_ALWAY_ON);
        rowDisplayMode.add(filler3);

        buttonGroup4.add(RB_DISPLAY_WORK_ON);
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

        buttonGroup6.add(RB_UNIT_KILOMETERS);
        RB_UNIT_KILOMETERS.setText("km/h");
        RB_UNIT_KILOMETERS.setToolTipText("<html>Also set on the display<br>\nIf you set miles in display<br>\nset max wheel available\n</html>");
        RB_UNIT_KILOMETERS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_UNIT_KILOMETERSStateChanged(evt);
            }
        });
        rowUnits.add(RB_UNIT_KILOMETERS);
        rowUnits.add(filler4);

        buttonGroup6.add(RB_UNIT_MILES);
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

        headerBikeSettings.setFont(headerBikeSettings.getFont().deriveFont(headerBikeSettings.getFont().getStyle() | java.awt.Font.BOLD));
        headerBikeSettings.setText("Bike settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelBatterySettings.add(headerBikeSettings, gridBagConstraints);

        TF_WHEEL_CIRCUMF.setText("2260");
        TF_WHEEL_CIRCUMF.setToolTipText("<html>Indicative values:<br>\n26-inch wheel = 2050 mm<br>\n27-inch wheel = 2150 mm<br>\n27.5 inch wheel = 2215 mm<br>\n28-inch wheel = 2250 mm<br>\n29-inch wheel = 2300 mmV\n</html>");
        TF_WHEEL_CIRCUMF.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelBatterySettings.add(TF_WHEEL_CIRCUMF, gridBagConstraints);

        TF_MAX_SPEED.setText("25");
        TF_MAX_SPEED.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
        TF_MAX_SPEED.setEnabled(!CB_MAX_SPEED_DISPLAY.isSelected());
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
        subPanelFunctionSettingsLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelFunctionSettings.setLayout(subPanelFunctionSettingsLayout);

        jLabel39.setFont(jLabel39.getFont().deriveFont(jLabel39.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel39.setText("Function settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        subPanelFunctionSettings.add(jLabel39, gridBagConstraints);

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

        buttonGroup3.add(RB_TEMP_LIMIT);
        RB_TEMP_LIMIT.setText("Temp. sensor");
        RB_TEMP_LIMIT.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_TEMP_LIMITStateChanged(evt);
            }
        });
        rowOptADC.add(RB_TEMP_LIMIT);
        rowOptADC.add(filler6);

        buttonGroup3.add(RB_ADC_OPTION_DIS);
        RB_ADC_OPTION_DIS.setText("None");
        RB_ADC_OPTION_DIS.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_ADC_OPTION_DISStateChanged(evt);
            }
        });
        rowOptADC.add(RB_ADC_OPTION_DIS);
        rowOptADC.add(filler7);

        buttonGroup3.add(RB_THROTTLE);
        RB_THROTTLE.setText("Throttle");
        RB_THROTTLE.setEnabled(CB_BRAKE_SENSOR.isSelected()&&(!(CB_COASTER_BRAKE.isSelected())));
        RB_THROTTLE.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                RB_THROTTLEStateChanged(evt);
            }
        });
        rowOptADC.add(RB_THROTTLE);

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

        headerPowerAssist.setFont(headerPowerAssist.getFont().deriveFont(headerPowerAssist.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_POWER_ASS_1.setText("70");
        TF_POWER_ASS_1.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
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

        TF_POWER_ASS_2.setText("120");
        TF_POWER_ASS_2.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
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

        TF_POWER_ASS_3.setText("210");
        TF_POWER_ASS_3.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
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

        TF_POWER_ASS_4.setText("300");
        TF_POWER_ASS_4.setToolTipText("<html>% Human power<br>\nMax value 500\n</html>");
        TF_POWER_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelPowerAssist.add(TF_POWER_ASS_4, gridBagConstraints);

        buttonGroup5.add(RB_POWER_ON_START);
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

        headerTorqueAssist.setFont(headerTorqueAssist.getFont().deriveFont(headerTorqueAssist.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_TORQUE_ASS_1.setText("70");
        TF_TORQUE_ASS_1.setToolTipText("Max value 254");
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

        TF_TORQUE_ASS_2.setText("100");
        TF_TORQUE_ASS_2.setToolTipText("Max value 254");
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

        TF_TORQUE_ASS_3.setText("130");
        TF_TORQUE_ASS_3.setToolTipText("Max value 254");
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

        TF_TORQUE_ASS_4.setText("160");
        TF_TORQUE_ASS_4.setToolTipText("Max value 254");
        TF_TORQUE_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelTorqueAssist.add(TF_TORQUE_ASS_4, gridBagConstraints);

        buttonGroup5.add(RB_TORQUE_ON_START);
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

        headerCadenceAssist.setFont(headerCadenceAssist.getFont().deriveFont(headerCadenceAssist.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_CADENCE_ASS_1.setText("70");
        TF_CADENCE_ASS_1.setToolTipText("Max value 254");
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

        TF_CADENCE_ASS_2.setText("100");
        TF_CADENCE_ASS_2.setToolTipText("Max value 254");
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

        TF_CADENCE_ASS_3.setText("130");
        TF_CADENCE_ASS_3.setToolTipText("Max value 254");
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

        TF_CADENCE_ASS_4.setText("160");
        TF_CADENCE_ASS_4.setToolTipText("Max value 254");
        TF_CADENCE_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelCadenceAssist.add(TF_CADENCE_ASS_4, gridBagConstraints);

        buttonGroup5.add(RB_CADENCE_ON_START);
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

        headerEmtbAssist.setFont(headerEmtbAssist.getFont().deriveFont(headerEmtbAssist.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_EMTB_ASS_1.setText("6");
        TF_EMTB_ASS_1.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
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

        TF_EMTB_ASS_2.setText("9");
        TF_EMTB_ASS_2.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
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

        TF_EMTB_ASS_3.setText("12");
        TF_EMTB_ASS_3.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
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

        TF_EMTB_ASS_4.setText("15");
        TF_EMTB_ASS_4.setToolTipText("<html>Sensitivity<br>\nbetween 0 to 20\n</html>");
        TF_EMTB_ASS_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelEmtbAssist.add(TF_EMTB_ASS_4, gridBagConstraints);

        buttonGroup5.add(RB_EMTB_ON_START);
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

        headerWalkAssist.setFont(headerWalkAssist.getFont().deriveFont(headerWalkAssist.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_WALK_ASS_SPEED_1.setText("35");
        TF_WALK_ASS_SPEED_1.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_1.setEnabled(CB_WALK_ASSIST.isSelected());
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

        TF_WALK_ASS_SPEED_2.setText("40");
        TF_WALK_ASS_SPEED_2.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_2.setEnabled(CB_WALK_ASSIST.isSelected());
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

        TF_WALK_ASS_SPEED_3.setText("45");
        TF_WALK_ASS_SPEED_3.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_3.setEnabled(CB_WALK_ASSIST.isSelected());
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

        TF_WALK_ASS_SPEED_4.setText("50");
        TF_WALK_ASS_SPEED_4.setToolTipText("<html>km/h x10 or mph x10<br>\nValue 35 to 50 (3.5 to 5.0 km/h)\n</html>");
        TF_WALK_ASS_SPEED_4.setEnabled(CB_WALK_ASSIST.isSelected());
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

        TF_WALK_ASS_SPEED_LIMIT.setText("60");
        TF_WALK_ASS_SPEED_LIMIT.setToolTipText("<html>km/h x10 or mph x10<br>\nMax value 60 (in EU 6 km/h)\n</html>");
        TF_WALK_ASS_SPEED_LIMIT.setEnabled(CB_WALK_ASSIST.isSelected());
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

        TF_WALK_ASS_TIME.setText("60");
        TF_WALK_ASS_TIME.setToolTipText("Max value 255 (0.1 s)\n\n");
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_TIME.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelWalkAssist.add(TF_WALK_ASS_TIME, gridBagConstraints);

        CB_WALK_TIME_ENA.setText("Walk assist debounce time");
        CB_WALK_TIME_ENA.setToolTipText("Only with brake sensors enabled");
        CB_WALK_TIME_ENA.setEnabled(CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
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

        headerStreetMode.setFont(headerStreetMode.getFont().deriveFont(headerStreetMode.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_STREET_SPEED_LIM.setText("25");
        TF_STREET_SPEED_LIM.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
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

        jLabel_STREET_POWER_LIM.setForeground(new java.awt.Color(255, 0, 0));
        jLabel_STREET_POWER_LIM.setText("Street power limit (W)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelStreetMode.add(jLabel_STREET_POWER_LIM, gridBagConstraints);

        TF_STREET_POWER_LIM.setText("500");
        TF_STREET_POWER_LIM.setToolTipText("<html>Max nominal value in EU 250 W<br>\nMax peak value approx. 500 W\n</html>");
        TF_STREET_POWER_LIM.setEnabled(CB_STREET_POWER_LIM.isSelected());
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
        CB_STREET_WALK.setEnabled(CB_WALK_ASSIST.isSelected());
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

        headerCruiseMode.setFont(headerCruiseMode.getFont().deriveFont(headerCruiseMode.getFont().getStyle() | java.awt.Font.BOLD));
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

        TF_CRUISE_ASS_1.setText("15");
        TF_CRUISE_ASS_1.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
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

        TF_CRUISE_ASS_2.setText("18");
        TF_CRUISE_ASS_2.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
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

        TF_CRUISE_ASS_3.setText("21");
        TF_CRUISE_ASS_3.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
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

        TF_CRUISE_ASS_4.setText("24");
        TF_CRUISE_ASS_4.setToolTipText("<html>km/h or mph<br>\nMax value in EU 25 km/h\n</html>");
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

        TF_CRUISE_SPEED_ENA.setText("10");
        TF_CRUISE_SPEED_ENA.setToolTipText("Min speed to enable cruise (km/h or mph)");
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
        CB_CRUISE_WHITOUT_PED.setEnabled(CB_BRAKE_SENSOR.isSelected());
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

        headerLights.setFont(headerLights.getFont().deriveFont(headerLights.getFont().getStyle() | java.awt.Font.BOLD));
        headerLights.setText("Lights configuration");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(headerLights, gridBagConstraints);

        jLabel_LIGHT_MODE_ON_START.setText("Lights mode on startup");
        jLabel_LIGHT_MODE_ON_START.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_ON_START, gridBagConstraints);

        TF_LIGHT_MODE_ON_START.setText("0");
        TF_LIGHT_MODE_ON_START.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF\n</html>");
        TF_LIGHT_MODE_ON_START.setEnabled(CB_LIGHTS.isSelected());
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
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_ON_START, gridBagConstraints);

        jLabel_LIGHT_MODE_1.setText("Lights mode 1");
        jLabel_LIGHT_MODE_1.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_1, gridBagConstraints);

        TF_LIGHT_MODE_1.setText("6");
        TF_LIGHT_MODE_1.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF\n</html>");
        TF_LIGHT_MODE_1.setEnabled(CB_LIGHTS.isSelected());
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
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_1, gridBagConstraints);

        jLabel_LIGHT_MODE_2.setText("Lights mode 2");
        jLabel_LIGHT_MODE_2.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_2, gridBagConstraints);

        TF_LIGHT_MODE_2.setText("7");
        TF_LIGHT_MODE_2.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF<br>\nor alternative option settings<br>\n9 - assistance without pedal rotation\n</html>");
        TF_LIGHT_MODE_2.setEnabled(CB_LIGHTS.isSelected());
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
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_2, gridBagConstraints);

        jLabel_LIGHT_MODE_3.setText("Lights mode 3");
        jLabel_LIGHT_MODE_3.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(jLabel_LIGHT_MODE_3, gridBagConstraints);

        TF_LIGHT_MODE_3.setText("1");
        TF_LIGHT_MODE_3.setToolTipText("<html>With lights button ON<br>\n0 - lights ON<br>\n1 - lights FLASHING<br>\n2 - lights ON and BRAKE-FLASHING when braking<br>\n3 - lights FLASHING and ON when braking<br>\n4 - lights FLASHING and BRAKE-FLASHING when braking<br>\n5 - lights ON and ON when braking, even with the light button OFF<br>\n6 - lights ON and BRAKE-FLASHING when braking, even with the light button OFF<br>\n7 - lights FLASHING and ON when braking, even with the light button OFF<br>\n8 - lights FLASHING and BRAKE-FLASHING when braking, even with the light button OFF<br>\nor alternative option settings<br>\n10 - assistance with sensors error\n</html>");
        TF_LIGHT_MODE_3.setEnabled(CB_LIGHTS.isSelected());
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
        gridBagConstraints.weightx = 1.0;
        subPanelLightsHybrid.add(TF_LIGHT_MODE_3, gridBagConstraints);

        headerHybridAssist.setFont(headerHybridAssist.getFont().deriveFont(headerHybridAssist.getFont().getStyle() | java.awt.Font.BOLD));
        headerHybridAssist.setText("Hybrid assist mode");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        subPanelLightsHybrid.add(headerHybridAssist, gridBagConstraints);

        buttonGroup5.add(RB_HYBRID_ON_START);
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
        jPanel11.setLayout(jPanel11Layout);

        jLabel35.setFont(jLabel35.getFont().deriveFont(jLabel35.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel35.setText("Battery cells settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel35, gridBagConstraints);

        jLabel36.setText("Cell voltage full (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel36, gridBagConstraints);

        TF_BAT_CELL_FULL.setText("3.95");
        TF_BAT_CELL_FULL.setToolTipText("Value 3.90 to 4.00");
        TF_BAT_CELL_FULL.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_FULL, gridBagConstraints);

        jLabel37.setText("Overvoltage (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel37, gridBagConstraints);

        TF_BAT_CELL_OVER.setText("4.35");
        TF_BAT_CELL_OVER.setToolTipText("Value 4.25 to 4.35");
        TF_BAT_CELL_OVER.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_OVER, gridBagConstraints);

        jLabel38.setText("Reset SOC percentage (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel38, gridBagConstraints);

        TF_BAT_CELL_SOC.setText("4.05");
        TF_BAT_CELL_SOC.setToolTipText("Value 4.00 to 4.10");
        TF_BAT_CELL_SOC.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_SOC, gridBagConstraints);

        jLabel40.setText("Cell voltage 3/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel40, gridBagConstraints);

        TF_BAT_CELL_3_4.setText("3.70");
        TF_BAT_CELL_3_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_3_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_3_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_3_4, gridBagConstraints);

        jLabel41.setText("Cell voltage 2/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel41, gridBagConstraints);

        TF_BAT_CELL_2_4.setText("3.45");
        TF_BAT_CELL_2_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_2_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_2_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_2_4, gridBagConstraints);

        jLabel42.setText("Cell voltage 1/4 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel42, gridBagConstraints);

        TF_BAT_CELL_1_4.setText("3.25");
        TF_BAT_CELL_1_4.setToolTipText("Value empty to full");
        TF_BAT_CELL_1_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_1_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_1_4, gridBagConstraints);

        jLabel75.setText("Cell voltage 5/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel75, gridBagConstraints);

        TF_BAT_CELL_5_6.setText("3.85");
        TF_BAT_CELL_5_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_5_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_5_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_5_6, gridBagConstraints);

        jLabel81.setText("Cell voltage 4/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel81, gridBagConstraints);

        TF_BAT_CELL_4_6.setText("3.70");
        TF_BAT_CELL_4_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_4_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_4_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_4_6, gridBagConstraints);

        jLabel82.setText("Cell voltage 3/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel82, gridBagConstraints);

        TF_BAT_CELL_3_6.setText("3.55");
        TF_BAT_CELL_3_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_3_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_3_6, gridBagConstraints);

        TF_BAT_CELL_2_6.setText("3.40");
        TF_BAT_CELL_2_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_2_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_2_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_2_6, gridBagConstraints);

        jLabel83.setText("Cell voltage 2/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel83, gridBagConstraints);

        TF_BAT_CELL_1_6.setText("3.25");
        TF_BAT_CELL_1_6.setToolTipText("Value empty to full");
        TF_BAT_CELL_1_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_1_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_1_6, gridBagConstraints);

        jLabel84.setText("Cell voltage 1/6 (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel84, gridBagConstraints);

        TF_BAT_CELL_EMPTY.setText("2.90");
        TF_BAT_CELL_EMPTY.setToolTipText("<html>Indicative value 2.90<br>\nIt depends on the<br>\ncharacteristics of the cells\n</html>");
        TF_BAT_CELL_EMPTY.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel11.add(TF_BAT_CELL_EMPTY, gridBagConstraints);

        jLabel85.setText("Cell voltage empty (V)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel11.add(jLabel85, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        panelAdvancedSettings.add(jPanel11, gridBagConstraints);

        java.awt.GridBagLayout jPanel18Layout = new java.awt.GridBagLayout();
        jPanel18Layout.columnWidths = new int[] {0, 8, 0};
        jPanel18Layout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        jPanel18.setLayout(jPanel18Layout);

        jLabel86.setFont(jLabel86.getFont().deriveFont(jLabel86.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel86.setText("Display advanced settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel86, gridBagConstraints);

        jLabel89.setText("Time to displayed data 1 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel89, gridBagConstraints);

        TF_DELAY_DATA_1.setText("50");
        TF_DELAY_DATA_1.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_1.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_1, gridBagConstraints);

        jLabelData1.setText("Data 1");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData1, gridBagConstraints);

        TF_DATA_1.setText("1");
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
        jPanel18.add(TF_DATA_1, gridBagConstraints);

        TF_DATA_2.setText("2");
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
        jPanel18.add(TF_DATA_2, gridBagConstraints);

        jLabelData2.setText("Data 2");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData2, gridBagConstraints);

        TF_DATA_3.setText("5");
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
        jPanel18.add(TF_DATA_3, gridBagConstraints);

        jLabelData3.setText("Data 3");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData3, gridBagConstraints);

        jLabelData4.setText("Data 4");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData4, gridBagConstraints);

        TF_DATA_4.setText("4");
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
        jPanel18.add(TF_DATA_4, gridBagConstraints);

        TF_DATA_5.setText("7");
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
        jPanel18.add(TF_DATA_5, gridBagConstraints);

        jLabelData5.setText("Data 5");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData5, gridBagConstraints);

        jLabelData6.setText("Data 6");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabelData6, gridBagConstraints);

        TF_DATA_6.setText("0");
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
        jPanel18.add(TF_DATA_6, gridBagConstraints);

        jLabel96.setText("Time to displayed data 2 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel96, gridBagConstraints);

        TF_DELAY_DATA_2.setText("50");
        TF_DELAY_DATA_2.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_2.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_2, gridBagConstraints);

        jLabel97.setText("Time to displayed data 3 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel97, gridBagConstraints);

        TF_DELAY_DATA_3.setText("50");
        TF_DELAY_DATA_3.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_3.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_3, gridBagConstraints);

        jLabel98.setText("Time to displayed data 4 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel98, gridBagConstraints);

        TF_DELAY_DATA_4.setText("50");
        TF_DELAY_DATA_4.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_4.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_4, gridBagConstraints);

        jLabel99.setText("Time to displayed data 5 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel99, gridBagConstraints);

        TF_DELAY_DATA_5.setText("50");
        TF_DELAY_DATA_5.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_5.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_5, gridBagConstraints);

        jLabel100.setText("Time to displayed data 6 (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel18.add(jLabel100, gridBagConstraints);

        TF_DELAY_DATA_6.setText("50");
        TF_DELAY_DATA_6.setToolTipText("<html>Max value 255 (0.1 sec)<br>\ncontinuous display at zero value\n</html>");
        TF_DELAY_DATA_6.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel18.add(TF_DELAY_DATA_6, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 0);
        panelAdvancedSettings.add(jPanel18, gridBagConstraints);

        java.awt.GridBagLayout subPanelDataOtherLayout = new java.awt.GridBagLayout();
        subPanelDataOtherLayout.columnWidths = new int[] {0, 8, 0, 8, 0, 8, 0, 8, 0};
        subPanelDataOtherLayout.rowHeights = new int[] {0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0, 4, 0};
        subPanelDataOther.setLayout(subPanelDataOtherLayout);

        jLabel101.setFont(jLabel101.getFont().deriveFont(jLabel101.getFont().getStyle() | java.awt.Font.BOLD));
        jLabel101.setText("Other function settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        subPanelDataOther.add(jLabel101, gridBagConstraints);

        jLabel107.setText("ADC throttle value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel107, gridBagConstraints);

        jLabel102.setText("min");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel102, gridBagConstraints);

        jLabel103.setText("Throttle assist value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel103, gridBagConstraints);

        jLabel108.setText("min");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel108, gridBagConstraints);

        TF_ADC_THROTTLE_MIN.setText("47");
        TF_ADC_THROTTLE_MIN.setToolTipText("Value 40 to 50");
        TF_ADC_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ADC_THROTTLE_MIN.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ADC_THROTTLE_MIN.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ADC_THROTTLE_MIN, gridBagConstraints);

        TF_ADC_THROTTLE_MAX.setText("176");
        TF_ADC_THROTTLE_MAX.setToolTipText("Value 170 to 180");
        TF_ADC_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
        TF_ADC_THROTTLE_MAX.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ADC_THROTTLE_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ADC_THROTTLE_MAX, gridBagConstraints);

        CB_TEMP_ERR_MIN_LIM.setText("Temperature error with min limit");
        CB_TEMP_ERR_MIN_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 18;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(CB_TEMP_ERR_MIN_LIM, gridBagConstraints);

        jLabel104.setText("Motor temperature min limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel104, gridBagConstraints);

        TF_TEMP_MIN_LIM.setText("65");
        TF_TEMP_MIN_LIM.setToolTipText("Max value 75 (°C)");
        TF_TEMP_MIN_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        TF_TEMP_MIN_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TEMP_MIN_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_TEMP_MIN_LIM, gridBagConstraints);

        jLabel105.setText("Motor temperature max limit");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel105, gridBagConstraints);

        TF_TEMP_MAX_LIM.setText("80");
        TF_TEMP_MAX_LIM.setToolTipText("Max value 85 (°C)");
        TF_TEMP_MAX_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        TF_TEMP_MAX_LIM.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_TEMP_MAX_LIM.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_TEMP_MAX_LIM, gridBagConstraints);

        jLabel106.setText("Motor blocked error - threshold time");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel106, gridBagConstraints);

        TF_MOTOR_BLOCK_TIME.setText("2");
        TF_MOTOR_BLOCK_TIME.setToolTipText("Value 1 to 10 (0.1 s)");
        TF_MOTOR_BLOCK_TIME.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_TIME.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 24;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_TIME, gridBagConstraints);

        TF_DELAY_MENU.setText("50");
        TF_DELAY_MENU.setToolTipText("Max value 60 (0.1 s)");
        TF_DELAY_MENU.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_DELAY_MENU.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_DELAY_MENU, gridBagConstraints);

        jLabel87.setText("Time to menu items (0.1 s)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel87, gridBagConstraints);

        jLabel90.setText("Number of data displayed at lights on");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel90, gridBagConstraints);

        TF_NUM_DATA_AUTO_DISPLAY.setText("2");
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

        jLabel109.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel109, gridBagConstraints);

        TF_ASSIST_THROTTLE_MIN.setText("0");
        TF_ASSIST_THROTTLE_MIN.setToolTipText("Value 0 to 100");
        TF_ASSIST_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ASSIST_THROTTLE_MIN.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ASSIST_THROTTLE_MIN.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ASSIST_THROTTLE_MIN, gridBagConstraints);

        jLabel110.setText("max");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 6;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jLabel110, gridBagConstraints);

        TF_ASSIST_THROTTLE_MAX.setText("255");
        TF_ASSIST_THROTTLE_MAX.setToolTipText("Value MIN to 255");
        TF_ASSIST_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
        TF_ASSIST_THROTTLE_MAX.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_ASSIST_THROTTLE_MAX.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_ASSIST_THROTTLE_MAX, gridBagConstraints);

        jLabel91.setText("Data displayed on startup");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel91, gridBagConstraints);

        jLabelCoasterBrakeThreshld.setText("Coaster brake torque threshold");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabelCoasterBrakeThreshld, gridBagConstraints);

        TF_COASTER_BRAKE_THRESHOLD.setText("30");
        TF_COASTER_BRAKE_THRESHOLD.setToolTipText("Max value 255 (s)");
        TF_COASTER_BRAKE_THRESHOLD.setEnabled(CB_COASTER_BRAKE.isSelected());
        TF_COASTER_BRAKE_THRESHOLD.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_COASTER_BRAKE_THRESHOLD.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_COASTER_BRAKE_THRESHOLD, gridBagConstraints);

        jLabel94.setText("Soc % calculation");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabel94, gridBagConstraints);

        TF_MOTOR_BLOCK_ERPS.setText("20");
        TF_MOTOR_BLOCK_ERPS.setToolTipText("Value 10 to 30 (ERPS)");
        TF_MOTOR_BLOCK_ERPS.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_ERPS.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_ERPS, gridBagConstraints);

        TF_MOTOR_BLOCK_CURR.setText("30");
        TF_MOTOR_BLOCK_CURR.setToolTipText("Value 1 to 5 (0.1 A)");
        TF_MOTOR_BLOCK_CURR.setMinimumSize(new java.awt.Dimension(45, 23));
        TF_MOTOR_BLOCK_CURR.setPreferredSize(new java.awt.Dimension(45, 23));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 8;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(TF_MOTOR_BLOCK_CURR, gridBagConstraints);

        jLabelMOTOR_BLOCK_CURR.setText("Motor blocked error - threshold current");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 26;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabelMOTOR_BLOCK_CURR, gridBagConstraints);

        jLabelMOTOR_BLOCK_ERPS.setText("Motor blocked error - threshold ERPS");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 28;
        gridBagConstraints.gridwidth = 7;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jLabelMOTOR_BLOCK_ERPS, gridBagConstraints);

        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        buttonGroup7.add(RB_STARTUP_SOC);
        RB_STARTUP_SOC.setText("Soc %");
        RB_STARTUP_SOC.setToolTipText("");
        jPanel1.add(RB_STARTUP_SOC);
        jPanel1.add(filler8);

        buttonGroup7.add(RB_STARTUP_VOLTS);
        RB_STARTUP_VOLTS.setText("Volts");
        RB_STARTUP_VOLTS.setToolTipText("");
        jPanel1.add(RB_STARTUP_VOLTS);
        jPanel1.add(filler9);

        buttonGroup7.add(RB_STARTUP_NONE);
        RB_STARTUP_NONE.setText("None");
        RB_STARTUP_NONE.setToolTipText("");
        jPanel1.add(RB_STARTUP_NONE);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.weightx = 1.0;
        subPanelDataOther.add(jPanel1, gridBagConstraints);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));

        buttonGroup8.add(RB_SOC_WH);
        RB_SOC_WH.setSelected(true);
        RB_SOC_WH.setText("Wh");
        RB_SOC_WH.setToolTipText("");
        jPanel3.add(RB_SOC_WH);
        jPanel3.add(filler10);

        buttonGroup8.add(RB_SOC_AUTO);
        RB_SOC_AUTO.setText("Auto");
        RB_SOC_AUTO.setToolTipText("");
        jPanel3.add(RB_SOC_AUTO);
        jPanel3.add(filler11);

        buttonGroup8.add(RB_SOC_VOLTS);
        RB_SOC_VOLTS.setText("Volts");
        RB_SOC_VOLTS.setToolTipText("");
        jPanel3.add(RB_SOC_VOLTS);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        subPanelDataOther.add(jPanel3, gridBagConstraints);

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

        jLabelExpSettings.setText("Proven Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(jLabelExpSettings, gridBagConstraints);

        expSet.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
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

        jLabelProvenSettings.setText("Experimental Settings");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(jLabelProvenSettings, gridBagConstraints);

        provSet.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
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
        rowCompileActionsLayout.columnWidths = new int[] {0, 8, 0, 8, 0};
        rowCompileActionsLayout.rowHeights = new int[] {0};
        rowCompileActions.setLayout(rowCompileActionsLayout);

        BTN_SAVE.setText("Save");
        BTN_SAVE.setEnabled(false);
        BTN_SAVE.setMargin(new java.awt.Insets(4, 8, 4, 8));
        BTN_SAVE.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BTN_SAVEActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        rowCompileActions.add(BTN_SAVE, gridBagConstraints);

        BTN_COMPILE.setFont(BTN_COMPILE.getFont().deriveFont(BTN_COMPILE.getFont().getStyle() | java.awt.Font.BOLD));
        BTN_COMPILE.setText("Compile & Flash");
        BTN_COMPILE.setMargin(new java.awt.Insets(4, 8, 4, 8));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
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
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        rowCompileActions.add(BTN_CANCEL, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        panelRightColumn.add(rowCompileActions, gridBagConstraints);

        LB_COMPILE_OUTPUT.setFont(LB_COMPILE_OUTPUT.getFont().deriveFont(LB_COMPILE_OUTPUT.getFont().getSize()+3f));
        LB_COMPILE_OUTPUT.setText("Output from flashing");

        scrollCompileOutput.setHorizontalScrollBar(null);

        TA_COMPILE_OUTPUT.setEditable(false);
        TA_COMPILE_OUTPUT.setBackground(new java.awt.Color(255, 255, 255));
        TA_COMPILE_OUTPUT.setColumns(20);
        TA_COMPILE_OUTPUT.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        TA_COMPILE_OUTPUT.setLineWrap(true);
        TA_COMPILE_OUTPUT.setRows(5);
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
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(LB_COMPILE_OUTPUT)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(labelTitle, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTabbedPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 870, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(panelRightColumn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 6, Short.MAX_VALUE)))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(LB_COMPILE_OUTPUT)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollCompileOutput, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jTabbedPane1.getAccessibleContext().setAccessibleName("MotorConfiguration");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void CB_COASTER_BRAKEStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_COASTER_BRAKEStateChanged
        TF_COASTER_BRAKE_THRESHOLD.setEnabled(CB_COASTER_BRAKE.isSelected());
        RB_THROTTLE.setEnabled(CB_BRAKE_SENSOR.isSelected() && (!(CB_COASTER_BRAKE.isSelected())));
        if (CB_COASTER_BRAKE.isSelected() && RB_THROTTLE.isSelected()) {
            RB_ADC_OPTION_DIS.setSelected(true);
        }
    }//GEN-LAST:event_CB_COASTER_BRAKEStateChanged

    private void CB_WALK_TIME_ENAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_WALK_TIME_ENAStateChanged
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
    }//GEN-LAST:event_CB_WALK_TIME_ENAStateChanged

    private void CB_BRAKE_SENSORStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_BRAKE_SENSORStateChanged
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
        CB_CRUISE_WHITOUT_PED.setEnabled(CB_BRAKE_SENSOR.isSelected());
        CB_WALK_TIME_ENA.setEnabled(CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
        RB_THROTTLE.setEnabled(CB_BRAKE_SENSOR.isSelected() && (!(CB_COASTER_BRAKE.isSelected())));
    }//GEN-LAST:event_CB_BRAKE_SENSORStateChanged

    private void RB_THROTTLEStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_THROTTLEStateChanged
        TF_ADC_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ADC_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
        TF_ASSIST_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ASSIST_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
    }//GEN-LAST:event_RB_THROTTLEStateChanged

    private void RB_TEMP_LIMITStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_TEMP_LIMITStateChanged
        TF_TEMP_MIN_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        TF_TEMP_MAX_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        CB_TEMP_ERR_MIN_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
    }//GEN-LAST:event_RB_TEMP_LIMITStateChanged

    private void RB_ADC_OPTION_DISStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_ADC_OPTION_DISStateChanged
        TF_ADC_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ADC_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
        TF_TEMP_MIN_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        TF_TEMP_MAX_LIM.setEnabled(RB_TEMP_LIMIT.isSelected());
        TF_ASSIST_THROTTLE_MIN.setEnabled(RB_THROTTLE.isSelected());
        TF_ASSIST_THROTTLE_MAX.setEnabled(RB_THROTTLE.isSelected());
    }//GEN-LAST:event_RB_ADC_OPTION_DISStateChanged

    private void TF_DATA_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_1KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_1.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData1.setText("Data 1 - " + displayDataArray[index]);
            } else {
                jLabelData1.setText("Data 1");
            }
        } catch (NumberFormatException ex) {
            jLabelData1.setText("Data 1");
        }
    }//GEN-LAST:event_TF_DATA_1KeyReleased

    private void TF_DATA_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_2KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_2.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData2.setText("Data 2 - " + displayDataArray[index]);
            } else {
                jLabelData2.setText("Data 2");
            }
        } catch (NumberFormatException ex) {
            jLabelData2.setText("Data 2");
        }
    }//GEN-LAST:event_TF_DATA_2KeyReleased

    private void TF_DATA_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_3KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_3.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData3.setText("Data 3 - " + displayDataArray[index]);
            } else {
                jLabelData3.setText("Data 3");
            }
        } catch (NumberFormatException ex) {
            jLabelData3.setText("Data 3");
        }
    }//GEN-LAST:event_TF_DATA_3KeyReleased

    private void TF_DATA_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_4KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_4.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData4.setText("Data 4 - " + displayDataArray[index]);
            } else {
                jLabelData4.setText("Data 4");
            }
        } catch (NumberFormatException ex) {
            jLabelData4.setText("Data 4");
        }
    }//GEN-LAST:event_TF_DATA_4KeyReleased

    private void TF_DATA_5KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_5KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_5.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData5.setText("Data 5 - " + displayDataArray[index]);
            } else {
                jLabelData5.setText("Data 5");
            }
        } catch (NumberFormatException ex) {
            jLabelData5.setText("Data 5");
        }
    }//GEN-LAST:event_TF_DATA_5KeyReleased

    private void TF_DATA_6KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_DATA_6KeyReleased
        try {
            int index = Integer.parseInt(TF_DATA_6.getText());
            if ((index >= 0) && (index <= 10)) {
                jLabelData6.setText("Data 6 - " + displayDataArray[index]);
            } else {
                jLabelData6.setText("Data 6");
            }
        } catch (NumberFormatException ex) {
            jLabelData6.setText("Data 6");
        }
    }//GEN-LAST:event_TF_DATA_6KeyReleased

    private void TF_LIGHT_MODE_ON_STARTKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_ON_STARTKeyReleased
        try {
            int index = Integer.parseInt(TF_LIGHT_MODE_ON_START.getText());
            if ((index >= 0) && (index <= 8)) {
                jLabel_LIGHT_MODE_ON_START.setText("<html>Lights mode on startup " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_ON_START.getText())] + "</html>");
            } else {
                jLabel_LIGHT_MODE_ON_START.setText("Lights mode on startup");
            }
        } catch (NumberFormatException ex) {
            jLabel_LIGHT_MODE_ON_START.setText("Lights mode on startup");
        }
    }//GEN-LAST:event_TF_LIGHT_MODE_ON_STARTKeyReleased

    private void TF_LIGHT_MODE_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_1KeyReleased
        try {
            int index = Integer.parseInt(TF_LIGHT_MODE_1.getText());
            if ((index >= 0) && (index <= 8)) {
                jLabel_LIGHT_MODE_1.setText("<html>Mode 1 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_1.getText())] + "</html>");
            } else {
                jLabel_LIGHT_MODE_1.setText("Mode 1");
            }
        } catch (NumberFormatException ex) {
            jLabel_LIGHT_MODE_1.setText("Mode 1");
        }
    }//GEN-LAST:event_TF_LIGHT_MODE_1KeyReleased

    private void TF_LIGHT_MODE_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_2KeyReleased
        try {
            int index = Integer.parseInt(TF_LIGHT_MODE_2.getText());
            if ((index >= 0) && (index <= 9)) {
                jLabel_LIGHT_MODE_2.setText("<html>Mode 2 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_2.getText())] + "</html>");
            } else {
                jLabel_LIGHT_MODE_2.setText("Mode 2");
            }
        } catch (NumberFormatException ex) {
            jLabel_LIGHT_MODE_2.setText("Mode 2");
        }
    }//GEN-LAST:event_TF_LIGHT_MODE_2KeyReleased

    private void TF_LIGHT_MODE_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_LIGHT_MODE_3KeyReleased
        try {
            int index = Integer.parseInt(TF_LIGHT_MODE_3.getText());
            if (((index >= 0) && (index <= 8)) || (index == 10)) {
                jLabel_LIGHT_MODE_3.setText("<html>Mode 3 - " + lightModeArray[Integer.parseInt(TF_LIGHT_MODE_3.getText())] + "</html>");
            } else {
                jLabel_LIGHT_MODE_3.setText("Mode 3");
            }
        } catch (NumberFormatException ex) {
            jLabel_LIGHT_MODE_3.setText("Mode 3");
        }
    }//GEN-LAST:event_TF_LIGHT_MODE_3KeyReleased

    private void CB_STREET_POWER_LIMStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_STREET_POWER_LIMStateChanged
        TF_STREET_POWER_LIM.setEnabled(CB_STREET_POWER_LIM.isSelected());
    }//GEN-LAST:event_CB_STREET_POWER_LIMStateChanged

    private void CB_ASS_WITHOUT_PEDStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_ASS_WITHOUT_PEDStateChanged
        TF_ASS_WITHOUT_PED_THRES.setEnabled(CB_ASS_WITHOUT_PED.isSelected());
    }//GEN-LAST:event_CB_ASS_WITHOUT_PEDStateChanged

    private void CB_MAX_SPEED_DISPLAYStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_MAX_SPEED_DISPLAYStateChanged
        TF_MAX_SPEED.setEnabled(!CB_MAX_SPEED_DISPLAY.isSelected());
    }//GEN-LAST:event_CB_MAX_SPEED_DISPLAYStateChanged

    private void CB_LIGHTSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_LIGHTSStateChanged
        TF_LIGHT_MODE_ON_START.setEnabled(CB_LIGHTS.isSelected());
        TF_LIGHT_MODE_1.setEnabled(CB_LIGHTS.isSelected());
        TF_LIGHT_MODE_2.setEnabled(CB_LIGHTS.isSelected());
        TF_LIGHT_MODE_3.setEnabled(CB_LIGHTS.isSelected());
    }//GEN-LAST:event_CB_LIGHTSStateChanged

    private void CB_WALK_ASSISTStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_WALK_ASSISTStateChanged
        TF_WALK_ASS_SPEED_1.setEnabled(CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_SPEED_2.setEnabled(CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_SPEED_3.setEnabled(CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_SPEED_4.setEnabled(CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_SPEED_LIMIT.setEnabled(CB_WALK_ASSIST.isSelected());
        TF_WALK_ASS_TIME.setEnabled(CB_WALK_TIME_ENA.isSelected() && CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
        CB_WALK_TIME_ENA.setEnabled(CB_BRAKE_SENSOR.isSelected() && CB_WALK_ASSIST.isSelected());
        CB_STREET_WALK.setEnabled(CB_WALK_ASSIST.isSelected());
    }//GEN-LAST:event_CB_WALK_ASSISTStateChanged

    private void CB_AUTO_DISPLAY_DATAStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_AUTO_DISPLAY_DATAStateChanged
        TF_NUM_DATA_AUTO_DISPLAY.setEnabled(CB_AUTO_DISPLAY_DATA.isSelected());
    }//GEN-LAST:event_CB_AUTO_DISPLAY_DATAStateChanged

    private void TF_MAX_SPEEDKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_MAX_SPEEDKeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intMaxSpeed = Integer.parseInt(TF_MAX_SPEED.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intMaxSpeed = Integer.parseInt(TF_MAX_SPEED.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_MAX_SPEEDKeyReleased

    private void TF_STREET_SPEED_LIMKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_STREET_SPEED_LIMKeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intStreetSpeed = Integer.parseInt(TF_STREET_SPEED_LIM.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intStreetSpeed = Integer.parseInt(TF_STREET_SPEED_LIM.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_STREET_SPEED_LIMKeyReleased

    private void TF_NUM_DATA_AUTO_DISPLAYKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_NUM_DATA_AUTO_DISPLAYKeyReleased
        if (Integer.parseInt(TF_NUM_DATA_AUTO_DISPLAY.getText()) > 6) {
            TF_NUM_DATA_AUTO_DISPLAY.setText("6");
        }
        if (Integer.parseInt(TF_NUM_DATA_AUTO_DISPLAY.getText()) == 0) {
            TF_NUM_DATA_AUTO_DISPLAY.setText("1");
        }
    }//GEN-LAST:event_TF_NUM_DATA_AUTO_DISPLAYKeyReleased

    private void CB_TORQUE_CALIBRATIONStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_TORQUE_CALIBRATIONStateChanged
        TF_TORQ_ADC_OFFSET.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
        TF_TORQ_ADC_RANGE_ADJ.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
        TF_TORQ_ADC_ANGLE_ADJ.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
        TF_TORQUE_ADC_MAX.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
        CB_ADC_STEP_ESTIM.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
        TF_TORQ_PER_ADC_STEP_ADV.setEnabled(CB_TORQUE_CALIBRATION.isSelected());
    }//GEN-LAST:event_CB_TORQUE_CALIBRATIONStateChanged

    private void TF_WALK_ASS_SPEED_LIMITKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_LIMITKeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intWalkSpeedLimit = Integer.parseInt(TF_WALK_ASS_SPEED_LIMIT.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intWalkSpeedLimit = Integer.parseInt(TF_WALK_ASS_SPEED_LIMIT.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_LIMITKeyReleased

    private void TF_CRUISE_ASS_1KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_1KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intCruiseSpeed1 = Integer.parseInt(TF_CRUISE_ASS_1.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intCruiseSpeed1 = Integer.parseInt(TF_CRUISE_ASS_1.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_CRUISE_ASS_1KeyReleased

    private void TF_CRUISE_ASS_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_2KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intCruiseSpeed2 = Integer.parseInt(TF_CRUISE_ASS_2.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intCruiseSpeed2 = Integer.parseInt(TF_CRUISE_ASS_2.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_CRUISE_ASS_2KeyReleased

    private void TF_CRUISE_ASS_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_3KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intCruiseSpeed3 = Integer.parseInt(TF_CRUISE_ASS_3.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intCruiseSpeed3 = Integer.parseInt(TF_CRUISE_ASS_3.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_CRUISE_ASS_3KeyReleased

    private void TF_CRUISE_ASS_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_ASS_4KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intCruiseSpeed4 = Integer.parseInt(TF_CRUISE_ASS_4.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intCruiseSpeed4 = Integer.parseInt(TF_CRUISE_ASS_4.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_CRUISE_ASS_4KeyReleased

    private void TF_CRUISE_SPEED_ENAKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_CRUISE_SPEED_ENAKeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intCruiseSpeed = Integer.parseInt(TF_CRUISE_SPEED_ENA.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intCruiseSpeed = Integer.parseInt(TF_CRUISE_SPEED_ENA.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_CRUISE_SPEED_ENAKeyReleased

    private void TF_TORQ_ADC_OFFSET_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_OFFSET_ADJKeyReleased
        int value = 0;
        int length = TF_TORQ_ADC_OFFSET_ADJ.getText().length();
        String sign = TF_TORQ_ADC_OFFSET_ADJ.getText().substring(0, 1);
        String empty = TF_TORQ_ADC_OFFSET_ADJ.getText();

        if (sign.equals("-")) {
            value = Integer.parseInt(TF_TORQ_ADC_OFFSET_ADJ.getText().substring(1, length));
        } else if (empty.equals("")) {
            value = 0;
        } else {
            value = Integer.parseInt(TF_TORQ_ADC_OFFSET_ADJ.getText());
        }

        if ((value >= 0) && (value <= MIDDLE_OFFSET_ADJ)) {
            if (empty.equals("")) {
                intTorqueOffsetAdj = MIDDLE_OFFSET_ADJ;
            } else if (sign.equals("-")) {
                intTorqueOffsetAdj = MIDDLE_OFFSET_ADJ - value;
            } else {
                intTorqueOffsetAdj = MIDDLE_OFFSET_ADJ + value;
                if (intTorqueOffsetAdj > OFFSET_MAX_VALUE) {
                    intTorqueOffsetAdj = OFFSET_MAX_VALUE;
                    TF_TORQ_ADC_OFFSET_ADJ.setText(String.valueOf(OFFSET_MAX_VALUE - MIDDLE_OFFSET_ADJ));
                }
            }
        } else {
            TF_TORQ_ADC_OFFSET_ADJ.setText("0");
            intTorqueOffsetAdj = MIDDLE_OFFSET_ADJ;
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
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intWalkSpeed1 = Integer.parseInt(TF_WALK_ASS_SPEED_1.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intWalkSpeed1 = Integer.parseInt(TF_WALK_ASS_SPEED_1.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_1KeyReleased

    private void TF_WALK_ASS_SPEED_2KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_2KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intWalkSpeed2 = Integer.parseInt(TF_WALK_ASS_SPEED_2.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intWalkSpeed2 = Integer.parseInt(TF_WALK_ASS_SPEED_2.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_2KeyReleased

    private void TF_WALK_ASS_SPEED_3KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_3KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intWalkSpeed3 = Integer.parseInt(TF_WALK_ASS_SPEED_3.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intWalkSpeed3 = Integer.parseInt(TF_WALK_ASS_SPEED_3.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_3KeyReleased

    private void TF_WALK_ASS_SPEED_4KeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_WALK_ASS_SPEED_4KeyReleased
        if (RB_UNIT_KILOMETERS.isSelected()) {
            intWalkSpeed4 = Integer.parseInt(TF_WALK_ASS_SPEED_4.getText());
        }
        if (RB_UNIT_MILES.isSelected()) {
            intWalkSpeed4 = Integer.parseInt(TF_WALK_ASS_SPEED_4.getText()) * 16 / 10;
        }
    }//GEN-LAST:event_TF_WALK_ASS_SPEED_4KeyReleased

    private void RB_UNIT_MILESStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_UNIT_MILESStateChanged
        if (RB_UNIT_MILES.isSelected()) {
            jLabel_MAX_SPEED.setText("Max speed offroad mode (mph)");
            jLabel_STREET_SPEED_LIM.setText("Street speed limit (mph)");
            jLabelCruiseSpeedUnits.setText("mph");
            jLabelWalkSpeedUnits.setText("mph x10");
            TF_MAX_SPEED.setText(String.valueOf((intMaxSpeed * 10 + 5) / 16));
            TF_STREET_SPEED_LIM.setText(String.valueOf((intStreetSpeed * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_1.setText(String.valueOf((intWalkSpeed1 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_2.setText(String.valueOf((intWalkSpeed2 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_3.setText(String.valueOf((intWalkSpeed3 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_4.setText(String.valueOf((intWalkSpeed4 * 10 + 5) / 16));
            TF_WALK_ASS_SPEED_LIMIT.setText(String.valueOf((intWalkSpeedLimit * 10 + 5) / 16));
            TF_CRUISE_SPEED_ENA.setText(String.valueOf((intCruiseSpeed * 10 + 5) / 16));
            TF_CRUISE_ASS_1.setText(String.valueOf((intCruiseSpeed1 * 10 + 5) / 16));
            TF_CRUISE_ASS_2.setText(String.valueOf((intCruiseSpeed2 * 10 + 5) / 16));
            TF_CRUISE_ASS_3.setText(String.valueOf((intCruiseSpeed3 * 10 + 5) / 16));
            TF_CRUISE_ASS_4.setText(String.valueOf((intCruiseSpeed4 * 10 + 5) / 16));
        }
    }//GEN-LAST:event_RB_UNIT_MILESStateChanged

    private void RB_XH18StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_XH18StateChanged
        TF_BAT_CELL_5_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_4_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_2_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_1_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_2_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_1_4.setEnabled(!(RB_VLCD5.isSelected()));
    }//GEN-LAST:event_RB_XH18StateChanged

    private void RB_VLCD5StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_VLCD5StateChanged
        TF_BAT_CELL_5_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_4_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_2_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_1_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_2_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_1_4.setEnabled(!(RB_VLCD5.isSelected()));
    }//GEN-LAST:event_RB_VLCD5StateChanged

    private void RB_VLCD6StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_VLCD6StateChanged
        TF_BAT_CELL_5_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_4_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_2_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_1_6.setEnabled(RB_VLCD5.isSelected());
        TF_BAT_CELL_3_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_2_4.setEnabled(!(RB_VLCD5.isSelected()));
        TF_BAT_CELL_1_4.setEnabled(!(RB_VLCD5.isSelected()));
    }//GEN-LAST:event_RB_VLCD6StateChanged

    private void TF_TORQ_ADC_RANGE_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_RANGE_ADJKeyReleased
        int value = 0;
        int length = TF_TORQ_ADC_RANGE_ADJ.getText().length();
        String sign = TF_TORQ_ADC_RANGE_ADJ.getText().substring(0, 1);
        String empty = TF_TORQ_ADC_RANGE_ADJ.getText();

        if (sign.equals("-")) {
            value = Integer.parseInt(TF_TORQ_ADC_RANGE_ADJ.getText().substring(1, length));
        } else if (empty.equals("")) {
            value = 0;
        } else {
            value = Integer.parseInt(TF_TORQ_ADC_RANGE_ADJ.getText());
        }

        if ((value >= 0) && (value <= MIDDLE_RANGE_ADJ)) {
            if (empty.equals("")) {
                intTorqueRangeAdj = MIDDLE_RANGE_ADJ;
            } else if (sign.equals("-")) {
                intTorqueRangeAdj = MIDDLE_RANGE_ADJ - value;
            } else {
                intTorqueRangeAdj = MIDDLE_RANGE_ADJ + value;
            }
        } else {
            TF_TORQ_ADC_RANGE_ADJ.setText("0");
            intTorqueRangeAdj = MIDDLE_RANGE_ADJ;
        }
    }//GEN-LAST:event_TF_TORQ_ADC_RANGE_ADJKeyReleased

    private void TF_TORQ_ADC_OFFSETKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_OFFSETKeyReleased
        if (CB_ADC_STEP_ESTIM.isSelected()) {
            intTorqueAdcOffset = Integer.parseInt(TF_TORQ_ADC_OFFSET.getText());
            intTorqueAdcMax = Integer.parseInt(TF_TORQUE_ADC_MAX.getText());
            intTorqueAdcOnWeight = intTorqueAdcOffset + ((intTorqueAdcMax - intTorqueAdcOffset) * 75) / 100;
            intTorqueAdcStepCalc = (WEIGHT_ON_PEDAL * 167) / (intTorqueAdcOnWeight - intTorqueAdcOffset);
            TF_TORQ_PER_ADC_STEP.setText(String.valueOf(intTorqueAdcStepCalc));
        }
    }//GEN-LAST:event_TF_TORQ_ADC_OFFSETKeyReleased

    private void TF_TORQUE_ADC_MAXKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQUE_ADC_MAXKeyReleased
        if (CB_ADC_STEP_ESTIM.isSelected()) {
            intTorqueAdcOffset = Integer.parseInt(TF_TORQ_ADC_OFFSET.getText());
            intTorqueAdcMax = Integer.parseInt(TF_TORQUE_ADC_MAX.getText());
            intTorqueAdcOnWeight = intTorqueAdcOffset + ((intTorqueAdcMax - intTorqueAdcOffset) * 75) / 100;
            intTorqueAdcStepCalc = (WEIGHT_ON_PEDAL * 167) / (intTorqueAdcOnWeight - intTorqueAdcOffset);
            TF_TORQ_PER_ADC_STEP.setText(String.valueOf(intTorqueAdcStepCalc));
        }
    }//GEN-LAST:event_TF_TORQUE_ADC_MAXKeyReleased

    private void TF_TORQ_ADC_ANGLE_ADJKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_TF_TORQ_ADC_ANGLE_ADJKeyReleased
        int value = 0;
        int length = TF_TORQ_ADC_ANGLE_ADJ.getText().length();
        String sign = TF_TORQ_ADC_ANGLE_ADJ.getText().substring(0, 1);
        String empty = TF_TORQ_ADC_ANGLE_ADJ.getText();

        if (sign.equals("-")) {
            value = Integer.parseInt(TF_TORQ_ADC_ANGLE_ADJ.getText().substring(1, length));
        } else if (empty.equals("")) {
            value = 0;
        } else {
            value = Integer.parseInt(TF_TORQ_ADC_ANGLE_ADJ.getText());
        }

        if ((value >= 0) && (value <= MIDDLE_ANGLE_ADJ)) {
            if (empty.equals("")) {
                intTorqueAngleAdj = MIDDLE_ANGLE_ADJ;
            } else if (sign.equals("-")) {
                intTorqueAngleAdj = MIDDLE_ANGLE_ADJ - value;
            } else {
                intTorqueAngleAdj = MIDDLE_ANGLE_ADJ + value;
            }
        } else {
            TF_TORQ_ADC_ANGLE_ADJ.setText("0");
            intTorqueAngleAdj = MIDDLE_ANGLE_ADJ;
        }
    }//GEN-LAST:event_TF_TORQ_ADC_ANGLE_ADJKeyReleased

    private void RB_UNIT_KILOMETERSStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_UNIT_KILOMETERSStateChanged
        if (RB_UNIT_KILOMETERS.isSelected()) {
            jLabel_MAX_SPEED.setText("Max speed offroad mode (km/h)");
            jLabel_STREET_SPEED_LIM.setText("Street speed limit (km/h)");
            jLabelCruiseSpeedUnits.setText("km/h");
            jLabelWalkSpeedUnits.setText("km/h x10");
            TF_MAX_SPEED.setText(String.valueOf(intMaxSpeed));
            TF_STREET_SPEED_LIM.setText(String.valueOf(intStreetSpeed));
            TF_WALK_ASS_SPEED_1.setText(String.valueOf(intWalkSpeed1));
            TF_WALK_ASS_SPEED_2.setText(String.valueOf(intWalkSpeed2));
            TF_WALK_ASS_SPEED_3.setText(String.valueOf(intWalkSpeed3));
            TF_WALK_ASS_SPEED_4.setText(String.valueOf(intWalkSpeed4));
            TF_WALK_ASS_SPEED_LIMIT.setText(String.valueOf(intWalkSpeedLimit));
            TF_CRUISE_SPEED_ENA.setText(String.valueOf(intCruiseSpeed));
            TF_CRUISE_ASS_1.setText(String.valueOf(intCruiseSpeed1));
            TF_CRUISE_ASS_2.setText(String.valueOf(intCruiseSpeed2));
            TF_CRUISE_ASS_3.setText(String.valueOf(intCruiseSpeed3));
            TF_CRUISE_ASS_4.setText(String.valueOf(intCruiseSpeed4));
        }
    }//GEN-LAST:event_RB_UNIT_KILOMETERSStateChanged

    private void RB_850CStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_RB_850CStateChanged
        TF_BAT_CELL_5_6.setEnabled(RB_850C.isSelected());
        TF_BAT_CELL_4_6.setEnabled(RB_850C.isSelected());
        TF_BAT_CELL_3_6.setEnabled(RB_850C.isSelected());
        TF_BAT_CELL_2_6.setEnabled(RB_850C.isSelected());
        TF_BAT_CELL_1_6.setEnabled(RB_850C.isSelected());
        TF_BAT_CELL_3_4.setEnabled(!(RB_850C.isSelected()));
        TF_BAT_CELL_2_4.setEnabled(!(RB_850C.isSelected()));
        TF_BAT_CELL_1_4.setEnabled(!(RB_850C.isSelected()));
    }//GEN-LAST:event_RB_850CStateChanged

    private void BTN_CANCELActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTN_CANCELActionPerformed
        if (compileWorker != null) {
            compileWorker.cancel(true);
            compileDone();
        }
    }//GEN-LAST:event_BTN_CANCELActionPerformed

    private void TF_BOOST_TORQUE_FACTORActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_TF_BOOST_TORQUE_FACTORActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_TF_BOOST_TORQUE_FACTORActionPerformed

    private void CB_ADC_STEP_ESTIMStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_CB_ADC_STEP_ESTIMStateChanged
        TF_TORQ_PER_ADC_STEP.setEnabled(!CB_ADC_STEP_ESTIM.isSelected());

        if (CB_ADC_STEP_ESTIM.isSelected()) {
            intTorqueAdcOffset = Integer.parseInt(TF_TORQ_ADC_OFFSET.getText());
            intTorqueAdcMax = Integer.parseInt(TF_TORQUE_ADC_MAX.getText());
            intTorqueAdcOnWeight = intTorqueAdcOffset + ((intTorqueAdcMax - intTorqueAdcOffset) * 75) / 100;
            intTorqueAdcStepCalc = (WEIGHT_ON_PEDAL * 167) / (intTorqueAdcOnWeight - intTorqueAdcOffset);
            TF_TORQ_PER_ADC_STEP.setText(String.valueOf(intTorqueAdcStepCalc));
        } else {
            TF_TORQ_PER_ADC_STEP.setText(String.valueOf(intTorqueAdcStep));
        }
    }//GEN-LAST:event_CB_ADC_STEP_ESTIMStateChanged

    private void BTN_SAVEActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BTN_SAVEActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_BTN_SAVEActionPerformed

    /*
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TSDZ2_Configurator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TSDZ2_Configurator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TSDZ2_Configurator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TSDZ2_Configurator.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TSDZ2_Configurator().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BTN_CANCEL;
    private javax.swing.JButton BTN_COMPILE;
    private javax.swing.JButton BTN_SAVE;
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
    private javax.swing.JLabel LB_COMPILE_OUTPUT;
    private javax.swing.JLabel LB_LAST_COMMIT;
    private javax.swing.JRadioButton RB_850C;
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
    private javax.swing.JRadioButton RB_VLCD5;
    private javax.swing.JRadioButton RB_VLCD6;
    private javax.swing.JRadioButton RB_XH18;
    private javax.swing.JTextArea TA_COMPILE_OUTPUT;
    private javax.swing.JTextField TF_ADC_THROTTLE_MAX;
    private javax.swing.JTextField TF_ADC_THROTTLE_MIN;
    private javax.swing.JTextField TF_ASSIST_THROTTLE_MAX;
    private javax.swing.JTextField TF_ASSIST_THROTTLE_MIN;
    private javax.swing.JTextField TF_ASS_WITHOUT_PED_THRES;
    private javax.swing.JTextField TF_BATT_CAPACITY;
    private javax.swing.JTextField TF_BATT_CAPACITY_CAL;
    private javax.swing.JTextField TF_BATT_NUM_CELLS;
    private javax.swing.JTextField TF_BATT_POW_MAX;
    private javax.swing.JTextField TF_BATT_VOLT_CAL;
    private javax.swing.JTextField TF_BATT_VOLT_CUT_OFF;
    private javax.swing.JTextField TF_BAT_CELL_1_4;
    private javax.swing.JTextField TF_BAT_CELL_1_6;
    private javax.swing.JTextField TF_BAT_CELL_2_4;
    private javax.swing.JTextField TF_BAT_CELL_2_6;
    private javax.swing.JTextField TF_BAT_CELL_3_4;
    private javax.swing.JTextField TF_BAT_CELL_3_6;
    private javax.swing.JTextField TF_BAT_CELL_4_6;
    private javax.swing.JTextField TF_BAT_CELL_5_6;
    private javax.swing.JTextField TF_BAT_CELL_EMPTY;
    private javax.swing.JTextField TF_BAT_CELL_FULL;
    private javax.swing.JTextField TF_BAT_CELL_OVER;
    private javax.swing.JTextField TF_BAT_CELL_SOC;
    private javax.swing.JTextField TF_BAT_CUR_MAX;
    private javax.swing.JTextField TF_BOOST_CADENCE_STEP;
    private javax.swing.JTextField TF_BOOST_TORQUE_FACTOR;
    private javax.swing.JTextField TF_CADENCE_ASS_1;
    private javax.swing.JTextField TF_CADENCE_ASS_2;
    private javax.swing.JTextField TF_CADENCE_ASS_3;
    private javax.swing.JTextField TF_CADENCE_ASS_4;
    private javax.swing.JTextField TF_COASTER_BRAKE_THRESHOLD;
    private javax.swing.JTextField TF_CRUISE_ASS_1;
    private javax.swing.JTextField TF_CRUISE_ASS_2;
    private javax.swing.JTextField TF_CRUISE_ASS_3;
    private javax.swing.JTextField TF_CRUISE_ASS_4;
    private javax.swing.JTextField TF_CRUISE_SPEED_ENA;
    private javax.swing.JTextField TF_DATA_1;
    private javax.swing.JTextField TF_DATA_2;
    private javax.swing.JTextField TF_DATA_3;
    private javax.swing.JTextField TF_DATA_4;
    private javax.swing.JTextField TF_DATA_5;
    private javax.swing.JTextField TF_DATA_6;
    private javax.swing.JTextField TF_DELAY_DATA_1;
    private javax.swing.JTextField TF_DELAY_DATA_2;
    private javax.swing.JTextField TF_DELAY_DATA_3;
    private javax.swing.JTextField TF_DELAY_DATA_4;
    private javax.swing.JTextField TF_DELAY_DATA_5;
    private javax.swing.JTextField TF_DELAY_DATA_6;
    private javax.swing.JTextField TF_DELAY_MENU;
    private javax.swing.JTextField TF_EMTB_ASS_1;
    private javax.swing.JTextField TF_EMTB_ASS_2;
    private javax.swing.JTextField TF_EMTB_ASS_3;
    private javax.swing.JTextField TF_EMTB_ASS_4;
    private javax.swing.JTextField TF_LIGHT_MODE_1;
    private javax.swing.JTextField TF_LIGHT_MODE_2;
    private javax.swing.JTextField TF_LIGHT_MODE_3;
    private javax.swing.JTextField TF_LIGHT_MODE_ON_START;
    private javax.swing.JTextField TF_MAX_SPEED;
    private javax.swing.JTextField TF_MOTOR_ACC;
    private javax.swing.JTextField TF_MOTOR_BLOCK_CURR;
    private javax.swing.JTextField TF_MOTOR_BLOCK_ERPS;
    private javax.swing.JTextField TF_MOTOR_BLOCK_TIME;
    private javax.swing.JTextField TF_MOTOR_DEC;
    private javax.swing.JTextField TF_NUM_DATA_AUTO_DISPLAY;
    private javax.swing.JTextField TF_POWER_ASS_1;
    private javax.swing.JTextField TF_POWER_ASS_2;
    private javax.swing.JTextField TF_POWER_ASS_3;
    private javax.swing.JTextField TF_POWER_ASS_4;
    private javax.swing.JTextField TF_STREET_POWER_LIM;
    private javax.swing.JTextField TF_STREET_SPEED_LIM;
    private javax.swing.JTextField TF_TEMP_MAX_LIM;
    private javax.swing.JTextField TF_TEMP_MIN_LIM;
    private javax.swing.JTextField TF_TORQUE_ADC_MAX;
    private javax.swing.JTextField TF_TORQUE_ASS_1;
    private javax.swing.JTextField TF_TORQUE_ASS_2;
    private javax.swing.JTextField TF_TORQUE_ASS_3;
    private javax.swing.JTextField TF_TORQUE_ASS_4;
    private javax.swing.JTextField TF_TORQ_ADC_ANGLE_ADJ;
    private javax.swing.JTextField TF_TORQ_ADC_OFFSET;
    private javax.swing.JTextField TF_TORQ_ADC_OFFSET_ADJ;
    private javax.swing.JTextField TF_TORQ_ADC_RANGE_ADJ;
    private javax.swing.JTextField TF_TORQ_PER_ADC_STEP;
    private javax.swing.JTextField TF_TORQ_PER_ADC_STEP_ADV;
    private javax.swing.JTextField TF_WALK_ASS_SPEED_1;
    private javax.swing.JTextField TF_WALK_ASS_SPEED_2;
    private javax.swing.JTextField TF_WALK_ASS_SPEED_3;
    private javax.swing.JTextField TF_WALK_ASS_SPEED_4;
    private javax.swing.JTextField TF_WALK_ASS_SPEED_LIMIT;
    private javax.swing.JTextField TF_WALK_ASS_TIME;
    private javax.swing.JTextField TF_WHEEL_CIRCUMF;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.ButtonGroup buttonGroup6;
    private javax.swing.ButtonGroup buttonGroup7;
    private javax.swing.ButtonGroup buttonGroup8;
    private javax.swing.ButtonGroup buttonGroup9;
    private javax.swing.JList<String> expSet;
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
    private javax.swing.JLabel headerBatterySettings;
    private javax.swing.JLabel headerBikeSettings;
    private javax.swing.JLabel headerCadenceAssist;
    private javax.swing.JLabel headerCruiseMode;
    private javax.swing.JLabel headerDisplaySettings;
    private javax.swing.JLabel headerEmtbAssist;
    private javax.swing.JLabel headerHybridAssist;
    private javax.swing.JLabel headerLights;
    private javax.swing.JLabel headerPowerAssist;
    private javax.swing.JLabel headerStreetMode;
    private javax.swing.JLabel headerTorqueAssist;
    private javax.swing.JLabel headerWalkAssist;
    private javax.swing.JLabel headingMotorSettings;
    private javax.swing.JLabel jLabel100;
    private javax.swing.JLabel jLabel101;
    private javax.swing.JLabel jLabel102;
    private javax.swing.JLabel jLabel103;
    private javax.swing.JLabel jLabel104;
    private javax.swing.JLabel jLabel105;
    private javax.swing.JLabel jLabel106;
    private javax.swing.JLabel jLabel107;
    private javax.swing.JLabel jLabel108;
    private javax.swing.JLabel jLabel109;
    private javax.swing.JLabel jLabel110;
    private javax.swing.JLabel jLabel35;
    private javax.swing.JLabel jLabel36;
    private javax.swing.JLabel jLabel37;
    private javax.swing.JLabel jLabel38;
    private javax.swing.JLabel jLabel39;
    private javax.swing.JLabel jLabel40;
    private javax.swing.JLabel jLabel41;
    private javax.swing.JLabel jLabel42;
    private javax.swing.JLabel jLabel75;
    private javax.swing.JLabel jLabel81;
    private javax.swing.JLabel jLabel82;
    private javax.swing.JLabel jLabel83;
    private javax.swing.JLabel jLabel84;
    private javax.swing.JLabel jLabel85;
    private javax.swing.JLabel jLabel86;
    private javax.swing.JLabel jLabel87;
    private javax.swing.JLabel jLabel89;
    private javax.swing.JLabel jLabel90;
    private javax.swing.JLabel jLabel91;
    private javax.swing.JLabel jLabel94;
    private javax.swing.JLabel jLabel96;
    private javax.swing.JLabel jLabel97;
    private javax.swing.JLabel jLabel98;
    private javax.swing.JLabel jLabel99;
    private javax.swing.JLabel jLabelCoasterBrakeThreshld;
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
    private javax.swing.JLabel jLabelMOTOR_BLOCK_CURR;
    private javax.swing.JLabel jLabelMOTOR_BLOCK_ERPS;
    private javax.swing.JLabel jLabelOptADC;
    private javax.swing.JLabel jLabelProvenSettings;
    private javax.swing.JLabel jLabelVersion;
    private javax.swing.JLabel jLabelWalkSpeedUnits;
    private javax.swing.JLabel jLabel_BATT_CAPACITY;
    private javax.swing.JLabel jLabel_BATT_CAPACITY_CAL;
    private javax.swing.JLabel jLabel_BATT_NUM_CELLS;
    private javax.swing.JLabel jLabel_BATT_POW_MAX;
    private javax.swing.JLabel jLabel_BATT_VOLT_CAL;
    private javax.swing.JLabel jLabel_BATT_VOLT_CUT_OFF;
    private javax.swing.JLabel jLabel_BAT_CUR_MAX;
    private javax.swing.JLabel jLabel_BOOST_AT_ZERO;
    private javax.swing.JLabel jLabel_BOOST_CADENCE_STEP;
    private javax.swing.JLabel jLabel_BOOST_TORQUE_FACTOR;
    private javax.swing.JLabel jLabel_CADENCE_ASS_1;
    private javax.swing.JLabel jLabel_CADENCE_ASS_2;
    private javax.swing.JLabel jLabel_CADENCE_ASS_3;
    private javax.swing.JLabel jLabel_CADENCE_ASS_4;
    private javax.swing.JLabel jLabel_CRUISE_ASS_1;
    private javax.swing.JLabel jLabel_CRUISE_ASS_2;
    private javax.swing.JLabel jLabel_CRUISE_ASS_3;
    private javax.swing.JLabel jLabel_CRUISE_ASS_4;
    private javax.swing.JLabel jLabel_CRUISE_SPEED_ENA;
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
    private javax.swing.JLabel jLabel_MOTOR_FAST_STOP;
    private javax.swing.JLabel jLabel_MOTOR_V;
    private javax.swing.JLabel jLabel_POWER_ASS_4;
    private javax.swing.JLabel jLabel_STREET_POWER_LIM;
    private javax.swing.JLabel jLabel_STREET_SPEED_LIM;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel_BOOST_AT_ZERO;
    private javax.swing.JPanel jPanel_MOTOR_V;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private java.awt.Label labelTitle;
    private javax.swing.JLabel labelUnits;
    private javax.swing.JPanel panelAdvancedSettings;
    private javax.swing.JPanel panelAssistanceSettings;
    private javax.swing.JPanel panelBasicSettings;
    private javax.swing.JPanel panelRightColumn;
    private javax.swing.JList<String> provSet;
    private javax.swing.JPanel rowCompileActions;
    private javax.swing.JPanel rowDisplayMode;
    private javax.swing.JPanel rowDisplayType;
    private javax.swing.JPanel rowOptADC;
    private javax.swing.JPanel rowTorSensorAdv;
    private javax.swing.JPanel rowUnits;
    private javax.swing.JScrollPane scrollCompileOutput;
    private javax.swing.JScrollPane scrollExpSettings;
    private javax.swing.JScrollPane scrollProvenSettings;
    private javax.swing.JPanel subPanelBatterySettings;
    private javax.swing.JPanel subPanelCadenceAssist;
    private javax.swing.JPanel subPanelCruiseMode;
    private javax.swing.JPanel subPanelDataOther;
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
