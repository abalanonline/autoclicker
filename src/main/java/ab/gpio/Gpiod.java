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

package ab.gpio;

import java.io.IOException;

/**
 * sudo apt install gpiod
 * sudo groupadd gpio
 * sudo usermod -aG gpio $USER
 * sudo vi /etc/udev/rules.d/99-com.rules
 * SUBSYSTEM=="gpio", GROUP="gpio", MODE="0660"
 * ls -l /dev/gpiochip*
 *
 * 0.3 fps
 */
public class Gpiod implements Gpio {
  private final int chip;
  private final int offset;
  private final Runtime runtime;

  public Gpiod(int chip, int offset) {
    this.chip = chip;
    this.offset = offset;
    this.runtime = Runtime.getRuntime();
  }

  @Override
  public void open() {

  }

  @Override
  public void close() {

  }

  @Override
  public void set(boolean v) {
    try {
      Process process = runtime.exec(String.format("gpioset %d %d=%d", chip, offset, v ? 1 : 0));
      if (process.waitFor() != 0) throw new IllegalStateException(new String(process.getErrorStream().readAllBytes()));
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public boolean get() {
    try {
      Process process = runtime.exec(String.format("gpioget %d %d", chip, offset));
      if (process.waitFor() != 0) throw new IllegalStateException(new String(process.getErrorStream().readAllBytes()));
      byte[] bytes = process.getInputStream().readAllBytes();
      return bytes.length > 0 && bytes[0] == '1';
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

}
