package hu.bute.daai.amorg.drtorrent.coding.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

public class BencodedList extends Bencoded {

	private Vector<Bencoded> items_;

	/** Creates a new instance of BencodedList */
	public BencodedList() {
		items_ = new Vector<Bencoded>();
	}

	public Bencoded item(int index) {
		return (Bencoded) items_.elementAt(index);
	}

	public int count() {
		return items_.size();
	}

	public void append(Bencoded aItem) {
		items_.addElement(aItem);
	}

	public int type() {
		return Bencoded.BENCODED_LIST;
	}

	public byte[] Bencode() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			os.write("l".getBytes());
			for (int i = 0; i < items_.size(); i++)
				os.write(((Bencoded) items_.elementAt(i)).Bencode());
			os.write("e".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return os.toByteArray();
	}

}
