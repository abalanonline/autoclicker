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

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputOutputDevice;
import com.diozero.api.DigitalOutputDevice;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Tm1638 extends Component implements AutoCloseable, Runnable {

  public static final int PWCLK_NS = 400;
  private final DigitalOutputDevice stb;
  private final DigitalOutputDevice clk;
  private final DigitalInputOutputDevice dio;
  private boolean stop;

  public int brightness = 7;
  public boolean[] led = new boolean[8];
  public byte[] digit = new byte[8];
  public boolean[] button = new boolean[8];
  public KeyListener keyListener;

  public Tm1638(int stbGpio, int clkGpio, int dioGpio) {
    this.stb = new DigitalOutputDevice(stbGpio);
    this.clk = new DigitalOutputDevice(clkGpio);
    this.dio = new DigitalInputOutputDevice(dioGpio, DeviceMode.DIGITAL_OUTPUT);
    new Thread(this).start();
  }

  @Override
  public void run() {
    int[] command = new int[17];
    while (!stop) {
      writeCommand(0x40);
      command[0] = 0xC0;
      for (int i = 0, j = 0; i < 8; i++) {
        command[++j] = this.digit[i];
        command[++j] = this.led[i] ? 0xFF : 0;
      }
      writeCommand(command);
      writeCommand(0x88 + Math.min(Math.max(0, brightness), 7));

      strobeLow();
      writeByte(0x42);
      dio.setValue(true);
      dio.setMode(DeviceMode.DIGITAL_INPUT);
      int btnByte = 0;
      for (int i = 0; i < 4; i++) btnByte |= readByte() << i;
      for (int i = 0; i < 8; i++) {
        boolean newButton = (1 << i & btnByte) != 0;
        KeyListener keyListener = this.keyListener;
        if (this.button[i] != newButton && keyListener != null) {
          KeyEvent keyEvent = new KeyEvent(this, 0, 0, 0, KeyEvent.VK_0 + i, (char) ('0' + i));
          if (newButton) {
            keyListener.keyPressed(keyEvent);
          } else {
            keyListener.keyReleased(keyEvent);
          }
        }
        this.button[i] = newButton;
      }
      dio.setMode(DeviceMode.DIGITAL_OUTPUT);
      strobeHigh();
      synchronized (this) {
        this.notifyAll();
      }
    }
    stop = false;
  }

  private static void sleep() {
    long nanoTime = System.nanoTime() + PWCLK_NS;
    while (System.nanoTime() < nanoTime) ;
  }

  private void writeByte(int data) {
    for (int i = 0; i < 8; i++) {
      clk.off();
      dio.setValue((1 << i & data) != 0);
      sleep();
      clk.on();
      sleep();
    }
  }

  private int readByte() {
    int data = 0;
    for (int i = 0; i < 8; i++) {
      clk.off();
      sleep();
      data |= (dio.getValue() ? 1 : 0) << i;
      clk.on();
      sleep();
    }
    return data;
  }

  private void strobeLow() {
    stb.on();
    clk.on();
    sleep();
    stb.off();
    sleep();
  }

  private void strobeHigh() {
    stb.on();
    sleep();
  }

  private void writeCommand(int... data) {
    strobeLow();
    for (int i = 0; i < data.length; i++) {
      writeByte(data[i]);
    }
    strobeHigh();
  }

  public static int to7(char c) {
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

  public void print(String s) {
    print(s, 0);
  }

  public void print(String s, int x) {
    for (int i = 0; i < s.length(); i++) {
      final char c = s.charAt(i);
      if (c == '.') {
        this.digit[x - 1] |= 0b10000000;
        continue;
      }
      if (x >= 8) break;
      this.digit[x++] = (byte) to7(c);
    }
  }

  @Override
  public void close() {
    try {
      synchronized (this) {
        this.wait();
        this.wait();
      }
      this.stop = true;
      while (this.stop) Thread.sleep(1);
    } catch (InterruptedException ignore) {
    }
    stb.close();
    clk.close();
    dio.close();
  }

}
