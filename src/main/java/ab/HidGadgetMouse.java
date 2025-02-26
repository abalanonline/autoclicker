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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class HidGadgetMouse implements AutoCloseable {

  private final Process process;
  private final InputStream inputStream;
  private final OutputStream outputStream;

  public HidGadgetMouse(String devHid) {
    if (devHid == null) devHid = "/dev/hidg0";
    ProcessBuilder processBuilder = new ProcessBuilder("./hid_gadget_test", devHid, "mouse");
    processBuilder.redirectErrorStream(true);
    try {
      process = processBuilder.start();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    inputStream = process.getInputStream();
    outputStream = process.getOutputStream();
  }

  protected String readInput() {
    return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
  }

  protected void writeOutput(String s) {
    try {
      synchronized (outputStream) {
        outputStream.write(s.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(readInput(), e);
    }
  }

  @Override
  public void close() {
    writeOutput("--quit\n");
    try {
      process.waitFor();
    } catch (InterruptedException e) {
      throw new IllegalStateException(e);
    }
  }

  public void move(int x, int y) {
    writeOutput(String.format("%d %d\n", x, y));
  }

  private boolean[] hold = new boolean[3];

  public void click(int button, boolean press) {
    if (hold[button] == press) return;
    hold[button] = press;
    writeOutput(String.format("--b%d%s\n", button + 1, press ? " --hold" : ""));
  }

}
