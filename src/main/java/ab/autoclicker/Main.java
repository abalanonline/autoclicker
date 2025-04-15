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

package ab.autoclicker;

public class Main implements AutoCloseable, Runnable {

  public static final double[] RATES = {
      0.001, 0.002, 0.004, 0.008, 0.016, 0.032, 0.064, 0.125, 0.250, 0.500,
      1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
  private int rate = 10;
  private boolean[] hold = new boolean[3];
  private long nano;
  private boolean exit1;
  private boolean exit2;
  private int brightness;
  private final Device device;

  public Main(Device device) {
    this.device = device;
    this.device.setKeyListener(this::keyPressed);
  }

  @Override
  public void close() {
    device.close();
  }

  public void printRate() {
    device.print(3, 0, String.format("%6s",
        String.format("%f", RATES[rate]).replaceAll("0+$", "").replaceAll("\\.$", ".0")));
  }

  @Override
  public void run() {
    device.setBrightness(0);
    device.print(0, 0, "\uE158\uE134\uE158");
    printRate();
    while (!(exit1 && exit2)) {
      long period = (long) (1_000_000_000 / RATES[rate]);
      final long p1 = Math.min(period / 2, 200_000_000);
      final boolean click = (System.nanoTime() - nano) % period < p1 && hold[0];
      device.click(0, click);
      device.print(4, -1, click ? "1" : "0");
      device.update();
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
    }
  }

  public void keyPressed(String s) {
    switch (s) {
      case "1": brightness = Math.max(0, brightness - 1); device.setBrightness(brightness); break;
      case "2": brightness = Math.min(brightness + 1, 3); device.setBrightness(brightness); break;
      case "3": break;
      case "4": device.move(-20, -10); break;
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
        device.print(6, -1, l ? "1" : "0");
        device.update();
        break;
      case "8":
        final boolean r = !hold[1];
        hold[1] = r;
        device.print(7, -1, r ? "1" : "0");
        device.update();
        device.click(1, r);
        break;
      case "+1": exit1 = true; break;
      case "+2": exit2 = true; break;
      case "-1": exit1 = false; break;
      case "-2": exit2 = false; break;
    }
  }

  public static void main(String[] args) {
    try (final Main main = new Main(new Device().open())) {
      main.run();
    }
  }

}
