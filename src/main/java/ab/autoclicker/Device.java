/*
 * Copyright (C) 2025 Aleksei Balan
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

import ab.gpio.Pin;
import ab.gpio.Tm1638;

import java.util.function.Consumer;

public class Device implements AutoCloseable {

  protected Tm1638 tm1638;
  private HidGadgetMouse mouse;

  public void setKeyListener(Consumer<String> keyListener) {
    tm1638.setKeyListener(keyListener);
  }

  public void print(int x, int y, String s) {
    tm1638.print(x, y, s, 1);
  }

  public void update() {
    tm1638.update();
  }

  public void setBrightness(int brightness) {
    tm1638.setBrightness(brightness);
  }

  public void click(int button, boolean press) {
    mouse.click(button, press);
  }

  public void move(int x, int y) {
    mouse.move(x, y);
  }

  public Device open() {
    mouse = new HidGadgetMouse("/dev/hidg0");
    tm1638 = new Tm1638(new Pin(0, 17), new Pin(0, 27), new Pin(0, 22));
    tm1638.open();
    return this;
  }

  @Override
  public void close() {
    mouse.click(0, false);
    mouse.click(1, false);
    tm1638.close();
    mouse.close();
  }

}
