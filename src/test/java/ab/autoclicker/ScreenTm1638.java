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

import ab.gpio.Tm1638;

public class ScreenTm1638 extends Tm1638 {

  public ScreenTm1638() {
    super(null, null, null);
  }

  public boolean getLed(int i) {
    return super.led[i];
  }

  public byte getDigit(int i) {
    return super.digit[i];
  }

}
