package components;

import java.beans.JavaBean;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.text.NumberFormatter;

@JavaBean
public abstract class NumberField<T extends Comparable<T>> extends JFormattedTextField {
    private T min;
    private T max;
    protected final NumberFormatter formatter;
    private boolean isDefaultToolTip = true;

    public NumberField(Class<?> valueClass, T min, T max) {
        super(NumberFormat.getInstance());
        this.formatter = (NumberFormatter) this.getFormatter();
        this.formatter.setValueClass(valueClass);
        // necessary to allow invalid intermediate values (will be committed or reverted
        // on focus out)
        this.formatter.setAllowsInvalid(true);

        this.setMinMax(min, max);
    }

    public T getMin() {
        return min;
    }

    public void setMin(T min) {
        this.setMinMax(min, this.max);
    }

    public T getMax() {
        return max;
    }

    public void setMax(T max) {
        this.setMinMax(this.min, max);
    }

    public void setMinMax(T min, T max) {
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min must be less than or equal to max");
        }

        this.min = min;
        this.max = max;
        this.formatter.setMinimum(min);
        this.formatter.setMaximum(max);
        if (this.isDefaultToolTip) {
            super.setToolTipText("min " + min + ", max " + max);
        }

        @SuppressWarnings("unchecked")
        T oldValue = (T) this.getValue();
        if (oldValue != null) {
            if (oldValue.compareTo(min) < 0)
                this.setValue(min);
            else if (oldValue.compareTo(max) > 0)
                this.setValue(max);
        }
    }

    @Override
    public void setToolTipText(String text) {
        this.isDefaultToolTip = false;
        super.setToolTipText(text);
    }
}
