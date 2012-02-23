/*
 * MTBencodedDictionary.java
 *
 * Created on 2007. február 8., 20:21
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package hu.bute.daai.amorg.drtorrent.bencode;

import hu.bute.daai.amorg.drtorrent.DrTorrentTools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 *
 * @author Tyrial
 *
 * TODOAKOS: binary search instead of linear
 */
public class BencodedDictionary extends Bencoded{
    
    private Vector<BencodedDictionaryEntry> entries;
    
    /** Creates a new instance of MTBencodedDictionary */
    public BencodedDictionary() {
        entries = new Vector<BencodedDictionaryEntry>();
    }

    public int count()
    {
        return entries.size();
    }
    
    public int type()
    {
        return Bencoded.BencodedDictionary;        
    }
    
    public void addEntry(BencodedDictionaryEntry aEntry)
    {
        if (entries.size()==0)
            entries.addElement(aEntry);
        else
        {
            boolean inserted = false;
            for (int i=0; i<entries.size(); i++) 
            {
                BencodedDictionaryEntry current = 
                        (BencodedDictionaryEntry)entries.elementAt(i);
                if (BencodedDictionaryEntry.compare(current,aEntry)>0)
                {                    
                    entries.insertElementAt(aEntry,i);
                    inserted = true;
                    break;
                }
            }
            
            if (!inserted)
                entries.addElement(aEntry);
        }
    }
    
    public void addEntry(BencodedString aKey, Bencoded aValue)
    {
        addEntry(new BencodedDictionaryEntry(aKey,aValue));
    }
    
    public void addEntry(byte[] aKey, Bencoded aValue)
    {
        addEntry(new BencodedDictionaryEntry(new BencodedString(aKey),aValue));
    }
    
    public Bencoded entryValue(String aKey)
    {
        for (int i=0; i<entries.size(); i++)
        {
            BencodedDictionaryEntry current = 
                (BencodedDictionaryEntry)entries.elementAt(i);            
            
            if (new String(current.getKey().getValue()).equals(aKey))
            {
                return current.getValue();
            }
        }
        return null;
    }    
    
    public Bencoded entryValue(byte[] aKey)
    {
        for (int i=0; i<entries.size(); i++)
        {
            BencodedDictionaryEntry current = 
                (BencodedDictionaryEntry)entries.elementAt(i);            
            
            if (DrTorrentTools.byteArrayEqual(current.getKey().getValue(), aKey))
            {
                return current.getValue();
            }
        }
        return null;
    }
    
    public Bencoded entryValue(BencodedString aKey)
    {
        return entryValue(aKey.getValue());
    }
    
    public BencodedDictionaryEntry entry(int aIndex)
    {
        return (BencodedDictionaryEntry)entries.elementAt(aIndex);
    }
    
    public void toLog(int aIndentation)
    {        
	for (int i=0; i<entries.size(); i++)
	{
            ((BencodedDictionaryEntry)entries.elementAt(i)).getKey().toLog(aIndentation+1);
            //MTLogger.write(" :");
            String tempKey = new String(((BencodedDictionaryEntry)entries.elementAt(i)).getKey().getValue());
            if (!tempKey.equals("pieces"))
            {
                ((BencodedDictionaryEntry)entries.elementAt(i)).getValue().toLog(aIndentation + 2);
            }
            /*else
                MTLogger.write("[pieces]");
            MTLogger.writeLine("");*/
	}
    }    
    
    public byte[] Bencode() 
    {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try
        {
            os.write("d".getBytes());        
            for (int i=0; i<entries.size(); i++)
            {
                os.write(((BencodedDictionaryEntry)entries.elementAt(i)).getKey().Bencode());
                os.write(((BencodedDictionaryEntry)entries.elementAt(i)).getValue().Bencode());           
            }
            os.write("e".getBytes());       
        }
        catch(IOException e){
            e.printStackTrace();
        }
        return os.toByteArray();            
    }
    
}
