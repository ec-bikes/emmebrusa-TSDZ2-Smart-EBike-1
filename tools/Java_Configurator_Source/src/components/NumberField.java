package components;

import java.beans.PropertyChangeEvent;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

public abstract class NumberField<T extends Number & Comparable<T>> extends JFormattedTextField {
    /** Property name for committed changes to the user-entered value. */
    public static final String USER_VALUE_PROPERTY = "userValue";

    private T min;
    private T max;
    private T defaultMin;
    private T defaultMax;
    private T toolTipRecommendedMin;
    private T toolTipRecommendedMax;
    private T toolTipDefaultValue;
    private String toolTipExtra;
    private boolean isDefaultToolTip = true;
    private boolean eventsPaused = false;
    private final NumberFormatter formatter;

    /**
     * Create a new NumberField with the given min and max values.
     * @param valueClass Value class passed to the formatter
     * @param min Min value enforced by the formatter
     * @param max Max value enforced by the formatter
     * @param defaultMin Default min value used to determine if the current min should be shown in the tooltip
     * @param defaultMax Default max value used to determine if the current max should be shown in the tooltip
     */
    protected NumberField(Class<T> valueClass, T min, T max, T defaultMin, T defaultMax) {
        super(NumberFormat.getInstance());
        this.defaultMin = defaultMin;
        this.defaultMax = defaultMax;

        this.formatter = (NumberFormatter) this.getFormatter();
        this.formatter.setValueClass(valueClass);
        // don't use commas in the number
        ((NumberFormat) this.formatter.getFormat()).setGroupingUsed(false);
        // necessary to allow invalid intermediate values (will be committed or reverted
        // on focus out)
        this.formatter.setAllowsInvalid(true);

        this.setMinMax(min, max);

        // Fire a custom property change event for only user-entered committed values.
        this.addPropertyChangeListener("value", (PropertyChangeEvent e) -> {
            if (!eventsPaused) {
                this.firePropertyChange(USER_VALUE_PROPERTY, e.getOldValue(), e.getNewValue());
            }
        });
    }

    /** Get the current value as a number type. */
    @SuppressWarnings("unchecked")
    public T getNumberValue() {
        return (T) this.getValue();
    }

    /**
     * Parse and set a value from a string.
     * (The implementation MUST use setValueManual to avoid loops.)
     */
    public abstract void setValueFromString(String value);

    /**
     * Set the value manually from code, not from a user action.
     * This pauses event firing to avoid loops.
     */
    public void setValueManual(T value) {
        eventsPaused = true;
        this.setValue(value);
        eventsPaused = false;
    }

    /** Min value enforced by the formatter */
    public T getMin() { return min; }
    public void setMin(T min) {
        this.setMinMax(min, this.max);
    }

    /** Max value enforced by the formatter */
    public T getMax() { return max; }
    public void setMax(T max) {
        this.setMinMax(this.min, max);
    }

    /** Recommended min used ONLY in the default tooltip */
    public T getToolTipRecommendedMin() { return toolTipRecommendedMin; }
    public void setToolTipRecommendedMin(T recommendedMin) {
        this.toolTipRecommendedMin = recommendedMin;
        this.updateDefaultToolTip();
    }

    /** Recommended max used ONLY in the default tooltip */
    public T getToolTipRecommendedMax() { return toolTipRecommendedMax; }
    public void setToolTipRecommendedMax(T recommendedMax) {
        this.toolTipRecommendedMax = recommendedMax;
        this.updateDefaultToolTip();
    }

    /** Default value used ONLY in the default tooltip */
    public T getToolTipDefaultValue() { return toolTipDefaultValue; }
    public void setToolTipDefaultValue(T defaultValue) {
        this.toolTipDefaultValue = defaultValue;
        this.updateDefaultToolTip();
    }

    /** Extra text used at the start of the default tooltip */
    public String getToolTipExtra() { return toolTipExtra; }
    public void setToolTipExtra(String extraText) {
        this.toolTipExtra = extraText;
        this.updateDefaultToolTip();
    }

    /** Set the min and max together */
    public void setMinMax(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must be less than or equal to max");
        }

        this.min = min;
        this.max = max;
        this.formatter.setMinimum(min);
        this.formatter.setMaximum(max);
        this.updateDefaultToolTip();

        @SuppressWarnings("unchecked")
        T oldValue = (T) this.getValue();
        if (oldValue != null) {
            if (oldValue.compareTo(min) < 0)
                this.setValueManual(min);
            else if (oldValue.compareTo(max) > 0)
                this.setValueManual(max);
        }
    }

    /** Set tooltip text. This prevents the default min/max tooltip from being used. */
    @Override
    public void setToolTipText(String text) {
        this.isDefaultToolTip = false;
        super.setToolTipText(text);
    }

    private void updateDefaultToolTip() {
        if (!this.isDefaultToolTip) return;

        ArrayList<String> parts = new ArrayList<>();

        if (toolTipExtra != null) {
            parts.add(toolTipExtra);
        }

        if (toolTipRecommendedMin != null && toolTipRecommendedMax != null) {
            parts.add("Recommended range " + toolTipRecommendedMin + " to " + toolTipRecommendedMax);
        } else if (toolTipRecommendedMin != null) {
            parts.add("Recommended min " + toolTipRecommendedMin);
        } else if (toolTipRecommendedMax != null) {
            parts.add("Recommended max " + toolTipRecommendedMax);
        }

        boolean isDefaultMin = min.equals(defaultMin);
        boolean isDefaultMax = max.equals(defaultMax);
        if (!isDefaultMin && !isDefaultMax) {
            parts.add("Range " + min + " to " + max);
        } else if (isDefaultMin && !isDefaultMax) {
            parts.add("Max value " + max);
        } else if (!isDefaultMin && isDefaultMax) {
            parts.add("Min value " + min);
        }

        if (toolTipDefaultValue != null) {
            parts.add("Default value " + toolTipDefaultValue);
        }

        if (!parts.isEmpty()) {
            super.setToolTipText("<html>" + String.join("<br>", parts) + "</html>");
        }
    }
}
