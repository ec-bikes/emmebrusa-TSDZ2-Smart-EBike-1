package components;

import java.beans.JavaBean;

@JavaBean
public class FloatField extends NumberField<Float> {
    public FloatField() {
        this(0, Float.MAX_VALUE);
    }

    public FloatField(float min, float max) {
        super(Float.class, min, max, 0f, Float.MAX_VALUE);
    }

    @Override
    public void setValueFromString(String value) {
        this.setValueManual(Float.parseFloat(value));
    }
}
