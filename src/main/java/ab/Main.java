package ab;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Main implements KeyListener, AutoCloseable, Runnable {

  public static final double[] RATES = {
      0.001, 0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.125, 0.250, 0.500,
      1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
  private final Tm1638 tm1638;
  private final HidGadgetMouse hidGadgetMouse;
  private int rate = 10;

  public Main() {
    hidGadgetMouse = new HidGadgetMouse("/dev/hidg0");
    tm1638 = new Tm1638(17, 27, 22);
    tm1638.keyListener = this;
  }

  @Override
  public void close() {
    tm1638.close();
    hidGadgetMouse.close();
  }

  public void printRate() {
    tm1638.print(String.format("%6s", String.format("%f", RATES[rate]).replaceAll("0+$", "")), 3);
  }

  @Override
  public void run() {
    tm1638.brightness = 0;
    tm1638.digit[0] = 0b1011000;
    tm1638.digit[1] = 0b0110100;
    tm1638.digit[2] = 0b1011000;
    printRate();
    for (int buttons = 0; buttons != 7;) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
      buttons = 0;
      for (int i = 0; i < 8; i++) buttons |= tm1638.button[i] ? 1 << i : 0;
    }
    tm1638.print("        ");
  }

  public void test() {
    tm1638.brightness = 0;
    tm1638.print("        ");
    for (int i = 0; i < 8; i++) {
      tm1638.print(String.format("push %d  ", i + 1));
      while (!tm1638.button[i]) {
        for (int j = 0; j < 8; j++) {
          tm1638.led[j] = tm1638.button[j];
        }
      }
    }
    tm1638.print("        ");
  }

  @Override
  public void keyTyped(KeyEvent e) {
  }

  @Override
  public void keyPressed(KeyEvent e) {
    switch (e.getKeyCode()) {
      case '0': tm1638.brightness = Math.max(0, tm1638.brightness - 1); break;
      case '1': tm1638.brightness = Math.min(tm1638.brightness + 1, 7); break;
      case '2': break;
      case '3': hidGadgetMouse.move(-20, -10); break;
      case '4': if (rate > 0) rate--; printRate(); break;
      case '5': if (rate < RATES.length - 1) rate++; printRate(); break;
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
  }

  public static void main(String[] args) throws Exception {
    try (final Main main = new Main()) {
      main.run();
    }
//    try (HidGadgetMouse hidGadgetMouse = new HidGadgetMouse()) {
//      Thread.sleep(500);
//      hidGadgetMouse.move(50, -20);
//    }
//    try (Tm1638 tm1638 = new Tm1638(17, 27, 22)) {
//      new Main(tm1638).test();
//    }
  }

}
