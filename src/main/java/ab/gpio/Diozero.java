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

import com.diozero.api.DeviceInterface;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputOutputDevice;

/**
 * 733 fps
 */
public class Diozero implements Gpio {
  private final int offset;
  private DigitalInputOutputDevice device;

  public Diozero(int offset) {
    this.offset = offset;
  }

  @Override
  public void open() {
    device = new DigitalInputOutputDevice(offset, DeviceMode.DIGITAL_OUTPUT);
  }

  @Override
  public void close() {
    DeviceInterface device = this.device;
    if (device != null) device.close();
  }

  @Override
  public void set(boolean v) {
    device.setMode(DeviceMode.DIGITAL_OUTPUT);
    device.setValue(v);
  }

  @Override
  public boolean get() {
    device.setMode(DeviceMode.DIGITAL_INPUT);
    return device.getValue();
  }

}
