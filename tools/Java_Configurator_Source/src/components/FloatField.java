package components;

import java.beans.JavaBean;

@JavaBean
public class FloatField extends NumberField<Float> {
  public FloatField() {
    this(-Float.MAX_VALUE, Float.MAX_VALUE);
  }

  public FloatField(float min, float max) {
    super(Float.class, min, max);
  }

  public float getFloatValue() {
    return (float) this.getValue();
  }

  public boolean setFloatStringValue(String value) {
    try {
      this.setValue(Float.parseFloat(value));
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
