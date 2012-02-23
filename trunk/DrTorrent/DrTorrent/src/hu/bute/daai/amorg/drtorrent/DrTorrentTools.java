package hu.bute.daai.amorg.drtorrent;

public class DrTorrentTools {

	public static boolean byteArrayEqual(byte[] array1, byte[] array2) {
		if (array1.length != array2.length)
			return false;

		for (int i = 0; i < array1.length; i++) {
			if (array1[i] != array2[i])
				return false;
		}

		return true;
	}

}
