package hu.bute.daai.amorg.drtorrent.sha1;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-1 hasher class.
 * @author Ludanyi Akos
 */
final public class SHA1 {
    private MessageDigest hasher;

    public SHA1()
    {
        try {
			hasher = MessageDigest.getInstance("SHA1");
		}
        catch (NoSuchAlgorithmException e)
        {
		}
    	reset();
    }

    public void reset()
    {
    	hasher.reset();
    }

    /**
     * Add data to hash.
     * @param inp data
     */
    public void update(byte [] inp)
    {
       hasher.update(inp);
    }

    /**
     * Add data to hash.
     * @param inp data
     * @param  offset offset
     * @param inpLength lenth
     */

    public void update(byte [] inp, int offset, int inpLength)
    {
        hasher.update(inp, offset, inpLength);
    }

    /**
     * Get the result of the hash, and reset SHA-1
     * @return result of the hashing
     */
    public int [] digest()
    {
       byte [] val = hasher.digest();
       int [] ret = new int[5];
       
       try
       {
    	   DataInputStream ds = new DataInputStream(new ByteArrayInputStream(val));
    	   for(int i = 0; i < 5; i++)
    	   {
    		   ret[i] = ds.readInt();
    	   }
       }
       catch(Exception e)
       {
       }
       return ret;

    }

    /**
     * Convertes result to a hex string.
     */
    static public String resultToString(int [] inp)
    {
        StringBuffer r = new StringBuffer(8 * 5);
        
        for(int i = 0; i < 5; i++)
        {
            final String tmp = Integer.toHexString(inp[i]).toUpperCase();
            for(int j = 0; j < 8 - tmp.length(); j++)
                r.append('0');
            r.append(tmp);
        }

        return r.toString();
    }

    /**
     * Convertes result to a hex string.
     */
    static public String resultToString(byte [] inp)
    {
        StringBuffer r = new StringBuffer(8 * 5);

        for(int i = 0; i < 20; i++)
        {
            final String digit = Integer.toHexString(inp[i] & 0xff).toUpperCase();
            
            if(digit.length() < 2)
            {
                r.append('0');
            }

            r.append(digit);
        }

        return r.toString();
    }

    /**
     * Converts result to a byte array.
     * @param inp SHA-1 result
     * @return byte array
     */
    static public byte [] resultToByte(int [] inp)
    {
        byte [] ret = null;
        try
        {
            ByteArrayOutputStream s = new ByteArrayOutputStream(32 * 5);
            DataOutputStream ds = new DataOutputStream(s);

            for(int i = 0; i < 5; i++)
                ds.writeInt(inp[i]);

            ret = s.toByteArray();
            ds.close();
            s.close();
        }
        catch(IOException e)
        {
        }
        return ret;
    }

    static public String resultToByteString(int [] inp)
    {
        StringBuffer ret = new StringBuffer(20);
        byte [] b = resultToByte(inp);

        for(int i = 0; i < 20; i++)
        {
            final int c = b[i] & 0xff;
            ret.append((char)c);
        }

        return ret.toString();

    } 
}
