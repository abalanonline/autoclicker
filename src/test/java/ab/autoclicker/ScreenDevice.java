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

import ab.jnc3.Screen;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.util.function.Consumer;

public class ScreenDevice extends Device {

  private final int[] colorMap = new int[]{
      0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xAA5500, 0xAAAAAA,
      0x221111, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
  private final int c0 = 8;
  private final int[] bc = new int[]{4, 6, 12, 14};
  private int brightness = 0;
  private Screen screen;
  private ScreenTm1638 tm1638;
  private Consumer<String> keyListener;
  private boolean[] click = new boolean[3];

  @Override
  public void setKeyListener(Consumer<String> keyListener) {
    this.keyListener = keyListener;
  }

  private void keyListener(String s) {
    if (s.length() == 1 && s.charAt(0) >= '1' && s.charAt(0) <= '8') keyListener.accept(s);
    if (s.length() == 2 && (s.charAt(0) == '-' || s.charAt(0) == '+')
        && s.charAt(1) >= '1' && s.charAt(1) <= '8') keyListener.accept(s);
  }

  private void dot(int x, int y, int c) {
    screen.image.setRGB(x, y, colorMap[c]);
  }

  private final int digitSize = 4;
  private void line(int x, int y, boolean v, int on) {
    for (int i = 0; i < digitSize; i++) {
      dot(x, y, on == 0 ? c0 : bc[brightness]);
      if (v) y++; else x++;
    }
  }

  @Override
  public void update() {
    int s = digitSize;
    for (int i = 0; i < 8; i++) {
      int y = 8;
      int x = (s + 4) * i + 2;
      int lc = tm1638.getLed(i) ? bc[0] : c0;
      for (int iy = 1; iy < s - 1; iy++) for (int ix = 1; ix < s - 1; ix++) dot(x + ix, y + iy, lc);

      byte b = tm1638.getDigit(i);
      y += s + 1;
      line(x, y - 1, false, b & 0x01);
      line(x - 1, y, true, b & 0x20);
      line(x + s, y, true, b & 0x02);
      y += s + 1;
      line(x, y - 1, false, b & 0x40);
      line(x - 1, y, true, b & 0x10);
      line(x + s, y, true, b & 0x04);
      y += s + 1;
      line(x, y - 1, false, b & 0x08);
      dot(x + s + 1, y - 1, (b & 0x80) == 0 ? c0 : bc[brightness]);
      if (i < 3) for (int ix = 0; ix < s; ix++) dot(x++, y + 1, click[i] ? 15 : 0);
    }
    screen.update();
  }

  @Override
  public void setBrightness(int brightness) {
    this.brightness = Math.min(Math.max(0, brightness), 3);
  }

  @Override
  public void click(int button, boolean press) {
    click[button] = press;
  }

  @Override
  public void move(int x, int y) {
  }

  @Override
  public Device open() {
    int width = 64;
    int height = 36;
    screen = new Screen();
    screen.preferredSize = new Dimension(width, height);
    screen.image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED,
        new IndexColorModel(8, colorMap.length, colorMap, 0, false, -1, DataBuffer.TYPE_BYTE));
    screen.keyListener = this::keyListener;
    screen.gameController = true;
    tm1638 = new ScreenTm1638();
    super.tm1638 = tm1638;
    return this;
  }

  @Override
  public void close() {

//    super.close();
  }

}
