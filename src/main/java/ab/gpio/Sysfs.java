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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * sudo groupadd gpio
 * sudo usermod -aG gpio $USER
 * sudo vi /etc/udev/rules.d/99-com.rules
 * SUBSYSTEM=="gpio", ACTION=="add", PROGRAM="/bin/sh -c 'chgrp -R gpio /sys/${DEVPATH} && chmod -R g+w /sys/${DEVPATH}'"
 * https://stackoverflow.com/questions/30938991/access-gpio-sys-class-gpio-as-non-root
 * echo 485 | sudo tee -a /sys/class/gpio/export
 * ls -l /sys/class/gpio/gpio485/
 *
 * 39 fps
 */
public class Sysfs implements Gpio {
  private final int id;
  private final String fileName;
  private boolean in;

  public Sysfs(int id) {
    this.id = id;
    this.fileName = String.format("/sys/class/gpio/gpio%d/value", id);
  }

  @Override
  public void open() {
    in = true;
    in(false);
  }

  @Override
  public void close() {

  }

  protected void write(String fileName, String output) {
    try {
      Files.write(Paths.get(fileName), output.getBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(fileName, e);
    }
  }

  protected void in(boolean in) {
    if (this.in != in) {
      write(String.format("/sys/class/gpio/gpio%d/direction", id), in ? "in" : "out");
      this.in = in;
    }
  }

  @Override
  public void set(boolean v) {
    in(false);
    write(fileName, v ? "1" : "0");
  }

  @Override
  public boolean get() {
    in(true);
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(fileName));
      return bytes.length > 0 && bytes[0] == '1';
    } catch (IOException e) {
      throw new UncheckedIOException(fileName, e);
    }
  }

}
