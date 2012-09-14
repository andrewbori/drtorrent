package hu.bute.daai.amorg.drtorrent.coding.bencode;

import hu.bute.daai.amorg.drtorrent.Tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * TODO binary search instead of linear
 */
public class BencodedDictionary extends Bencoded {

	private Vector<BencodedDictionaryEntry> entries_;

	/** Creates a new instance of MTBencodedDictionary */
	public BencodedDictionary() {
		entries_ = new Vector<BencodedDictionaryEntry>();
	}

	public int count() {
		return entries_.size();
	}

	public int type() {
		return Bencoded.BENCODED_DICTIONARY;
	}

	public void addEntry(BencodedDictionaryEntry entry) {
		if (entries_.size() == 0)
			entries_.addElement(entry);
		else {
			boolean inserted = false;
			for (int i = 0; i < entries_.size(); i++) {
				BencodedDictionaryEntry current = (BencodedDictionaryEntry) entries_.elementAt(i);
				if (BencodedDictionaryEntry.compare(current, entry) > 0) {
					entries_.insertElementAt(entry, i);
					inserted = true;
					break;
				}
			}

			if (!inserted)
				entries_.addElement(entry);
		}
	}

	public void addEntry(BencodedString key, Bencoded value) {
		addEntry(new BencodedDictionaryEntry(key, value));
	}

	public void addEntry(byte[] key, Bencoded value) {
		addEntry(new BencodedDictionaryEntry(new BencodedString(key), value));
	}

	public Bencoded entryValue(String key) {
		for (int i = 0; i < entries_.size(); i++) {
			BencodedDictionaryEntry current = (BencodedDictionaryEntry) entries_.elementAt(i);

			if (new String(current.getKey().getValue()).equals(key)) {
				return current.getValue();
			}
		}
		return null;
	}

	public Bencoded entryValue(byte[] key) {
		for (int i = 0; i < entries_.size(); i++) {
			BencodedDictionaryEntry current = (BencodedDictionaryEntry) entries_.elementAt(i);

			if (Tools.byteArrayEqual(current.getKey().getValue(), key)) {
				return current.getValue();
			}
		}
		return null;
	}

	public Bencoded entryValue(BencodedString key) {
		return entryValue(key.getValue());
	}

	public BencodedDictionaryEntry entry(int index) {
		return (BencodedDictionaryEntry) entries_.elementAt(index);
	}

	public byte[] Bencode() {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		try {
			os.write("d".getBytes());
			for (int i = 0; i < entries_.size(); i++) {
				os.write(((BencodedDictionaryEntry) entries_.elementAt(i)).getKey().Bencode());
				os.write(((BencodedDictionaryEntry) entries_.elementAt(i)).getValue().Bencode());
			}
			os.write("e".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return os.toByteArray();
	}
}
