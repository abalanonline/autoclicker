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

import ab.gpio.Gpio;
import ab.tui.Tui;

import java.awt.Dimension;
import java.util.function.Consumer;

public class Tm1638 implements Tui {

  private static final int PWCLK_NS = 400;
  private final Gpio stb;
  private final Gpio clk;
  private final Gpio dio;
  private Thread thread;
  private boolean open;
  private Consumer<String> keyListener;

  private int brightness;
  private final boolean[] led = new boolean[8];
  private final byte[] digit = new byte[8];
  private final byte[] frontBuffer = new byte[8];
  private final boolean[] button = new boolean[8];
  public final int latencySize = 10001;
  public int latencyPtr;
  public long[] latency = new long[latencySize];

  public Tm1638(Gpio stb, Gpio clk, Gpio dio) {
    this.stb = stb;
    this.clk = clk;
    this.dio = dio;
  }

  @Override
  public Tm1638 open() {
    if (open) throw new IllegalStateException("not closed");
    //for (int i = 0; i < 8; i++) button[i] = false;
    // button state must not be cleared, or else button hold between close and open will be reported as clicked again
    open = true;
    brightness = 7;
    stb.open();
    clk.open();
    dio.open();
    thread = new Thread(this::run);
    thread.start();
    return this;
  }

  @Override
  public void close() {
    if (!open) return;
    for (int i = 0; i < 8; i++) {
      digit[i] = 0;
      frontBuffer[i] = 0;
      led[i] = false;
    }
    try {
      synchronized (this) {
        this.wait();
      }
      open = false;
      if (!thread.equals(Thread.currentThread())) thread.join();
    } catch (InterruptedException ignore) {
    }
    stb.close();
    clk.close();
    dio.close();
  }

  @Override
  public Dimension getSize() {
    return new Dimension(8, 1);
  }

  /**
   * Leds located above the line of 7 segment displays, so they have y == -1.
   * They are enabled by '1' and disabled by '0' or the low bit of any other symbol.
   * Brightness can be changed by writing "0" - "7" to x == -1, y == -1.
   */
  @Override
  public void print(int x, int y, String s, int attr) {
    if (y == -1) {
      if (x == -1) {
        brightness = s.charAt(0) - '0';
        return;
      }
      for (int i = 0; x < 8 && i < s.length();) led[x++] = (s.charAt(i++) & 1) > 0;
      return;
    }
    print(s, x);
  }

  @Override
  public void update() {
    System.arraycopy(digit, 0, frontBuffer, 0, 8);
  }

  @Override
  public void setKeyListener(Consumer<String> keyListener) {
    this.keyListener = keyListener;
  }

  private void run() {
    int[] command = new int[17];
    while (open) {
      long nanoTime = System.nanoTime();
      synchronized (this) {
        this.notifyAll();
      }
      writeCommand(0x40);
      command[0] = 0xC0;
      for (int i = 0, j = 0; i < 8; i++) {
        command[++j] = this.frontBuffer[i];
        command[++j] = this.led[i] ? 0xFF : 0;
      }
      writeCommand(command);
      writeCommand(0x88 + Math.min(Math.max(0, brightness), 7));

      strobeLow();
      writeByte(0x42);
      dio.set(true);
      int btnByte = 0;
      for (int i = 0; i < 4; i++) btnByte |= readByte() << i;
      for (int i = 0; i < 8; i++) {
        boolean newButton = (1 << i & btnByte) != 0;
        Consumer<String> keyListener = this.keyListener;
        if (this.button[i] != newButton && keyListener != null) {
          String key = String.format("%d", i + 1);
          if (newButton) {
            keyListener.accept("+" + key);
            keyListener.accept(key);
          } else {
            keyListener.accept("-" + key);
          }
        }
        this.button[i] = newButton;
      }
      strobeHigh();
      latency[latencyPtr] = System.nanoTime() - nanoTime;
      if (++latencyPtr >= latencySize) latencyPtr = 0;
    }
  }

  private static void sleep() {
    long nanoTime = System.nanoTime() + PWCLK_NS;
    while (System.nanoTime() < nanoTime) ;
  }

  private void writeByte(int data) {
    for (int i = 0; i < 8; i++) {
      clk.set(false);
      dio.set((1 << i & data) != 0);
      sleep();
      clk.set(true);
      sleep();
    }
  }

  private int readByte() {
    int data = 0;
    for (int i = 0; i < 8; i++) {
      clk.set(false);
      sleep();
      data |= (dio.get() ? 1 : 0) << i;
      clk.set(true);
      sleep();
    }
    return data;
  }

  private void strobeLow() {
    stb.set(true);
    clk.set(true);
    sleep();
    stb.set(false);
    sleep();
  }

  private void strobeHigh() {
    stb.set(true);
    sleep();
  }

  private void writeCommand(int... data) {
    strobeLow();
    for (int i = 0; i < data.length; i++) {
      writeByte(data[i]);
    }
    strobeHigh();
  }

  private static int to7(char c) {
    int[] digit = {
        0b0111111, 0b0000110, 0b1011011, 0b1001111, 0b1100110, 0b1101101, 0b1111101, 0b0000111,
        0b1111111, 0b1101111, 0b1110111, 0b1111100, 0b0111001, 0b1011110, 0b1111001, 0b1110001};
    int[] letter = {
        0b1110111, 0b1111100, 0b0111001, 0b1011110, 0b1111001, 0b1110001, 0b1101111, 0b1110110, // ABCDEFGH
        0b0000110, 0b0011110, 0b1111010, 0b0111000, 0b0110111, 0b1010100, 0b0111111, 0b1110011, // IJKLMNOP
        0b1100111, 0b1010000, 0b1101101, 0b1111000, 0b0111110, 0b0011100, 0b0000000, 0b1110110, // QRSTUVWX
        0x1101110, 0b1011011}; // YZ
    if (c >= '0' && c <= '9') return digit[c - '0'];
    if (c >= 'a' && c <= 'z') return letter[c - 'a'];
    if (c >= 'A' && c <= 'Z') return letter[c - 'A'];
    switch (c) {
      case ' ': return 0;
      case '_': return 0b0001000;
      case '-': return 0b1000000;
      case '\'': return 0b0100000;
      case '"': return 0b0100010;
      case '?': return 0b1010011;
      default:
        return 0b1010011; // question mark
    }
  }

  private void print(String s, int x) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == '.') {
        this.digit[x - 1] |= 0b10000000;
        continue;
      }
      if (x >= 8) break;
      this.digit[x++] = (c & 0xFF00) == 0xE100 ? (byte) c : (byte) to7(c);
    }
  }

}
