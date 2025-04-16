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
      0, 0.008, 0.016, 0.032, 0.064, 0.125, 0.250, 0.500,
      1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
  public static final int FINE = 8;
  private int rate = 8 * FINE;
  private int hold;
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

  protected double getRate() {
    int rc = rate / FINE;
    int rf = rate % FINE;
    double r0 = RATES[rc];
    if (rf == 0) return r0;
    double r1 = RATES[rc + 1];
    if (rc < 2) return (r1 - r0) * rf / FINE + r0; // linear part
    // linear interpolation
    return Math.exp((Math.log(r0) * (FINE - rf) + Math.log(r1) * rf) / FINE);
  }

  public void printRate() {
    double rate = getRate();
    int precision = 3;
    if (rate > 0.125) precision = 2;
    if (rate >= 1) precision = 1;
    device.print(3, 0, String.format("%6s", String.format("%." + precision + "f", rate)));
  }

  @Override
  public void run() {
    device.setBrightness(0);
    device.print(0, 0, "\uE158\uE134\uE158");
    printRate();
    while (!(exit1 && exit2)) {
      double rate = getRate();
      boolean click = true;
      if (rate > 0) {
        long period = (long) (1_000_000_000 / rate);
        final long p1 = Math.min(period / 2, 200_000_000);
        click = (System.nanoTime() - nano) % period < p1;
      }
      if (hold > 0) {
        device.click(hold - 1, click);
        device.print(4, -1, click ? "1" : "0");
      }
      device.update();
      try {
        Thread.sleep(1);
      } catch (InterruptedException ignore) {
      }
    }
  }

  protected void setRate(int rate) {
    nano = System.nanoTime();
    this.rate = Math.min(Math.max(0, rate), (RATES.length - 1) * FINE);
    printRate();
  }

  public void keyPressed(String s) {
    switch (s) {
      case "1": brightness = Math.max(0, brightness - 1); device.setBrightness(brightness); device.move(-16, 0); break;
      case "2": brightness = Math.min(brightness + 1, 3); device.setBrightness(brightness); device.move(16, 0); break;
      case "3": setRate(rate - 1); break;
      case "4": setRate(rate + 1); break;
      case "5": setRate(rate - (rate - 1) % FINE - 1); break;
      case "6": setRate(rate - (rate % FINE) + FINE); break;
      case "7":
        nano = System.nanoTime();
        if (hold > 0) {
          device.click(hold - 1, false);
          device.print(5 + hold, -1, "0");
        }
        if (hold != 1) {
          hold = 1;
          device.print(5 + hold, -1, "1");
        } else {
          hold = 0;
        }
        device.update();
        break;
      case "8":
        nano = System.nanoTime();
        if (hold > 0) {
          device.click(hold - 1, false);
          device.print(5 + hold, -1, "0");
        }
        if (hold != 2) {
          hold = 2;
          device.print(5 + hold, -1, "1");
        } else {
          hold = 0;
        }
        device.update();
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
