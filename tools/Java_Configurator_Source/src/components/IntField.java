package components;

import java.beans.JavaBean;

@JavaBean
public class IntField extends NumberField<Integer> {
    public IntField() {
        this(0, Integer.MAX_VALUE);
    }

    public IntField(int min, int max) {
        super(Integer.class, min, max, 0, Integer.MAX_VALUE);
    }

    @Override
    public void setValueFromString(String value) {
        this.setValueManual(Integer.parseInt(value));
    }
}
