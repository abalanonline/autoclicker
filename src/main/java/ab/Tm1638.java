package ab;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputOutputDevice;
import com.diozero.api.DigitalOutputDevice;

public class Tm1638 implements AutoCloseable {

	private final DigitalOutputDevice stb;
	private final DigitalOutputDevice clk;
	private final DigitalInputOutputDevice dio;
	public int brightness = 7;
	public boolean[] leds = new boolean[8];

	public Tm1638(int stbGpio, int clkGpio, int dioGpio) {
		this.stb = new DigitalOutputDevice(stbGpio);
		this.clk = new DigitalOutputDevice(clkGpio);
		this.dio = new DigitalInputOutputDevice(dioGpio, DeviceMode.DIGITAL_OUTPUT);
		print("        ");
	}

	public static void sleep() {
		try {
			System.nanoTime();
			Thread.sleep(0, 100_000);
		} catch (InterruptedException ignore) {
		}
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
			sleep();
		}
		strobeHigh();
	}

	public int inkey() {
		strobeLow();
		writeByte(0x42);
		dio.setValue(true);
		dio.setMode(DeviceMode.DIGITAL_INPUT);
		int result = 0;
		for (int i = 0; i < 4; i++) {
			result |= readByte() << i;
		}
		dio.setMode(DeviceMode.DIGITAL_OUTPUT);
		//dio.setValue(false);
		strobeHigh();
		return result;
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
		writeCommand(0x40);
		s += "        ";
		int[] command = new int[17];
		command[0] = 0xC0;
		for (int i = 0, j = 0; i < 8; i++) {
			command[++j] = to7(s.charAt(i));
			command[++j] = leds[i] ? 0xFF : 0;
		}
		writeCommand(command);
		writeCommand(0x88 + Math.min(Math.max(0, brightness), 7));
	}

	@Override
	public void close() {
		stb.close();
		clk.close();
		dio.close();
	}

}
