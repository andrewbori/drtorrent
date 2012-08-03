/*
 * MTBencodedList.java
 *
 * Created on 2007. február 8., 19:46
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package hu.bute.daai.amorg.drtorrent.coding.bencode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 *
 * @author Tyrial
 */
public class BencodedList extends Bencoded{
    
    private Vector<Bencoded> items;
    
    /** Creates a new instance of MTBencodedList */
    public BencodedList() 
    {
        items = new Vector<Bencoded>();
    }  
    
    public Bencoded item(int aIndex)
    {
        return (Bencoded)items.elementAt(aIndex);
    }
    
    public int count()
    {
        return items.size();
    }
    
    public void append(Bencoded aItem)
    {
        items.addElement(aItem);
    }
    
    public int type() 
    {
        return Bencoded.BencodedList;
    }

    public byte[] Bencode() 
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            os.write("l".getBytes());        
            for (int i=0; i<items.size(); i++)
               os.write(((Bencoded)items.elementAt(i)).Bencode());
            os.write("e".getBytes());                  
        }
        catch(IOException e){
            e.printStackTrace();
        }        
        return os.toByteArray();        
    }
    
}
