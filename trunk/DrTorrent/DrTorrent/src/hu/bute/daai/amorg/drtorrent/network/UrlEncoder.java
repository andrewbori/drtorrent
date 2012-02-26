package hu.bute.daai.amorg.drtorrent.network;


public class UrlEncoder {

	public static String encode(String s) {
		if (s == null) return s;
		
        StringBuffer sb = new StringBuffer(s.length() * 3);
        try {
            char c;
            for (int i = 0; i < s.length(); i++)  {
                c = s.charAt(i);
                /*if (c == '&') {
                    sb.append("&");
                } else*/ 
                if (c == ' ') {
                    sb.append('+');
                } else if (
                    (c >= ',' && c <= ';')
                        || (c >= 'A' && c <= 'Z')
                        || (c >= 'a' && c <= 'z')
                        || c == '_'
                        || c == '?') {
                    sb.append(c);
                } else {
                    sb.append('%');
                    if (c > 15) { // is it a non-control char, ie. >x0F so 2 chars
                        sb.append(Integer.toHexString((int)c)); // just add % and the string
                    } else {
                        sb.append("0" + Integer.toHexString((int)c));
                        // otherwise need to add a leading 0
                    }
                }
            }

        } catch (Exception ex) {
                return null;
        }
        
        return (sb.toString());
	}
}
