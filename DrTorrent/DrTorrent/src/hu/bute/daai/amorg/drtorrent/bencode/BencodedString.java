/*
 * MTBencodedString.java
 *
 * Created on 2007. február 8., 19:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package hu.bute.daai.amorg.drtorrent.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 *
 * @author Tyrial
 */
public class BencodedString extends Bencoded{
    
    private byte[] value;
    
    /** Creates a new instance of MTBencodedString */
    public BencodedString(byte[] aValue)
    {
        setValue(aValue);
    }
    
    public void toLog(int aIndentation)
    {        
        //MTLogger.write(new String(getValue()));
    }
    
    public int type()
    {
        return Bencoded.BencodedString;
    }   
    
    public void setValue(byte[] value) {
        this.value = value;
    }    
    
    public byte[] getValue() {
        return value;
    }    
    
    public String getStringValue() {
        return new String(value);
    }     
    
    public byte[] Bencode()
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        String pre = ""+getValue().length+":";
        try
        {
            os.write(pre.getBytes());
            os.write(getValue());   
        }
        catch(IOException e){
            e.printStackTrace();
        }        
        return os.toByteArray();
    }        
}
