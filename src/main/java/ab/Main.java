package ab;

public class Main {

  public void test(Tm1638 tm1638) {
    tm1638.brightness = 0;
    tm1638.print("        ");
    for (int i = 0; i < 8; i++) {
      tm1638.print(String.format("push %d", i + 1));
      while ((1 << i) != tm1638.inkey()) ;
    }
    tm1638.print("        ");
  }

  public static void main(String[] args) throws Exception {
    try (Tm1638 tm1638 = new Tm1638(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]))) {
      new Main().test(tm1638);
    }
  }

}
