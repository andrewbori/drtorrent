package hu.bute.daai.amorg.drtorrent.coding.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BencodedString extends Bencoded {

	private byte[] value_;

	/** Creates a new instance of MTBencodedString */
	public BencodedString(byte[] value) {
		setValue(value);
	}

	public int type() {
		return Bencoded.BENCODED_STRING;
	}

	public void setValue(byte[] value) {
		this.value_ = value;
	}

	public byte[] getValue() {
		return value_;
	}

	public String getStringValue() {
		return new String(value_);
	}

	public byte[] Bencode() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		String pre = "" + getValue().length + ":";
		try {
			os.write(pre.getBytes());
			os.write(getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return os.toByteArray();
	}
}
