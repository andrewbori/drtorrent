package hu.bute.daai.amorg.drtorrent.util.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BencodedInteger extends Bencoded {

	private long value_;

	/** Creates a new instance of BencodedInteger */
	public BencodedInteger(long value) {
		value_ = value;
	}

	public int type() {
		return Bencoded.BENCODED_INTEGER;
	}

	public byte[] Bencode() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			os.write("i".getBytes());
			String intValue = "" + value_;
			os.write(intValue.getBytes());
			os.write("e".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return os.toByteArray();
	}

	public long getValue() {
		return value_;
	}
}
