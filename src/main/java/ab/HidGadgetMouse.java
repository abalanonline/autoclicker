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
