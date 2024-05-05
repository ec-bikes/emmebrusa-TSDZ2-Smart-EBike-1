package components;

import java.beans.JavaBean;

@JavaBean
public class IntField extends NumberField<Integer> {
  public IntField() {
    this(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  public IntField(int min, int max) {
    super(Integer.class, min, max);
  }

  public int getIntValue() {
    return (int) this.getValue();
  }

  public boolean setIntStringValue(String value) {
    try {
      this.setValue(Integer.parseInt(value));
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }
}
