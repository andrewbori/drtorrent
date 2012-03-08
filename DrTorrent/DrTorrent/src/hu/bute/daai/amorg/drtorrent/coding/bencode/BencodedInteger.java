/*
 * MTBencodedInteger.java
 *
 * Created on 2007. február 8., 19:19
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package hu.bute.daai.amorg.drtorrent.coding.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author Tyrial
 */
public class BencodedInteger extends Bencoded {
    /**
     * TODOAKOS: according to spec 64 bit long would be better
     * e.g.: hd movies size may be larger than 2 GB
     */
    private int value;
    
    /** Creates a new instance of MTBencodedInteger */
    public BencodedInteger(int aValue)
    {
        value=aValue;
    }

    public void toLog(int aIndentation)
    {        
        //MTLogger.write(getValue());
    }
    
    public int type()
    {
        return Bencoded.BencodedInteger;
    }

    public byte[] Bencode()
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            os.write("i".getBytes());
            String intValue = ""+value;
            os.write(intValue.getBytes());
            os.write("e".getBytes());
        }
        catch(IOException e){
            e.printStackTrace();
        }
        
        return os.toByteArray();                
    }  

    public int getValue() {
        return value;
    }
}
