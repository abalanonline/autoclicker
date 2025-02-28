/*
 * Copyright (C) 2024 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab;

public class Main implements AutoCloseable, Runnable {

  public static final double[] RATES = {
      0.001, 0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.125, 0.250, 0.500,
      1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
  private final Tm1638 tm1638;
  private final HidGadgetMouse mouse;
  private int rate = 10;
  private boolean[] hold = new boolean[3];
  private long nano;
  private boolean[] button = new boolean[8];
  private int brightness;

  public Main() {
    mouse = new HidGadgetMouse("/dev/hidg0");
    tm1638 = new Tm1638(17, 27, 22);
    tm1638.setKeyListener(this::keyPressed);
    tm1638.open();
  }

  @Override
  public void close() {
    mouse.click(0, false);
    mouse.click(1, false);
    tm1638.close();
    mouse.close();
  }

  public void printRate() {
    tm1638.print(3, 0, String.format("%6s",
        String.format("%f", RATES[rate]).replaceAll("0+$", "").replaceAll("\\.$", ".0")), 1);
  }

  @Override
  public void run() {
    tm1638.print(-1, -1, "0", 1);
    tm1638.print(0, 0, "\uE158\uE134\uE158", 1);
    printRate();
    for (int buttons = 0; buttons != 7;) {
      long period = (long) (1_000_000_000 / RATES[rate]);
      final long p1 = Math.min(period / 2, 200_000_000);
      final boolean click = (System.nanoTime() - nano) % period < p1 && hold[0];
      mouse.click(0, click);
      tm1638.print(4, -1, click ? "1" : "0", 1);
      tm1638.update();
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
      buttons = 0;
      for (int i = 0; i < 8; i++) buttons |= this.button[i] ? 1 << i : 0;
    }
  }

  public void test() {
    tm1638.print(-1, -1, "0", 1);
    tm1638.print(0, 0, "        ", 1);
    for (int i = 0; i < 8; i++) {
      tm1638.print(0, 0, String.format("push %d  ", i + 1), 1);
      while (!this.button[i]) {
        for (int j = 0; j < 8; j++) {
          tm1638.print(j, -1, this.button[j] ? "1" : "0", 1);
        }
        tm1638.update();
      }
    }
  }

  public void keyPressed(String s) {
    switch (s) {
      case "1": brightness = Math.max(0, brightness - 1); tm1638.print(-1, -1, "" + brightness, 1); break;
      case "2": brightness = Math.min(brightness + 1, 7); tm1638.print(-1, -1, "" + brightness, 1); break;
      case "3": break;
      case "4": mouse.move(-20, -10); break;
      case "5":
        nano = System.nanoTime();
        rate = Math.max(0, rate - 1);
        printRate();
        break;
      case "6":
        nano = System.nanoTime();
        rate = Math.min(rate + 1, RATES.length - 1);
        printRate();
        break;
      case "7":
        nano = System.nanoTime();
        final boolean l = !hold[0];
        hold[0] = l;
        tm1638.print(6, -1, l ? "1" : "0", 1);
        tm1638.update();
        break;
      case "8":
        final boolean r = !hold[1];
        hold[1] = r;
        tm1638.print(7, -1, r ? "1" : "0", 1);
        tm1638.update();
        mouse.click(1, r);
        break;
      case "+1": button[0] = true; break;
      case "+2": button[1] = true; break;
      case "+3": button[2] = true; break;
      case "+4": button[3] = true; break;
      case "+5": button[4] = true; break;
      case "+6": button[5] = true; break;
      case "+7": button[6] = true; break;
      case "+8": button[7] = true; break;
      case "-1": button[0] = false; break;
      case "-2": button[1] = false; break;
      case "-3": button[2] = false; break;
      case "-4": button[3] = false; break;
      case "-5": button[4] = false; break;
      case "-6": button[5] = false; break;
      case "-7": button[6] = false; break;
      case "-8": button[7] = false; break;
    }
  }

  public static void main(String[] args) {
    try (final Main main = new Main()) {
      main.run();
    }
  }

}
