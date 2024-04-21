package ab;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Main implements KeyListener {

  private final Tm1638 tm1638;

  public Main(Tm1638 tm1638) {
    this.tm1638 = tm1638;
    tm1638.keyListener = this;
  }

  public void test() {
    tm1638.brightness = 0;
    tm1638.print("        ");
    for (int i = 0; i < 8; i++) {
      tm1638.print(String.format("push %d", i + 1));
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
      case '3': break;
    }

  }

  @Override
  public void keyReleased(KeyEvent e) {

  }

  public static void main(String[] args) throws Exception {
    try (Tm1638 tm1638 = new Tm1638(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
      new Main(tm1638).test();
    }
  }

}
