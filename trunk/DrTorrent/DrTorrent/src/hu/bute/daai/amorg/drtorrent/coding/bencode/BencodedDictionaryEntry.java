package hu.bute.daai.amorg.drtorrent.coding.bencode;

public class BencodedDictionaryEntry {

	private BencodedString key_;
	private Bencoded value_;

	/** Creates a new instance of MTBencodedDictionaryEntry */
	public BencodedDictionaryEntry(BencodedString key, Bencoded value) {
		key_ = key;
		value_ = value;
	}

	public BencodedString getKey() {
		return key_;
	}

	public Bencoded getValue() {
		return value_;
	}

	public static int compare(BencodedDictionaryEntry first, BencodedDictionaryEntry second) {
		if (first.getKey().getValue() == second.getKey().getValue())
			return 0;

		if (new String(first.getKey().getValue()).compareTo(new String(second.getKey().getValue())) < 0)
			return -1;
		else
			return 1;
	}
}
