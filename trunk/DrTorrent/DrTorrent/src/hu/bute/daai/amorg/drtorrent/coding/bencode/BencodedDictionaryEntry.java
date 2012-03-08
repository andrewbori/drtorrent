/*
 * MTBencodedDictionaryEntry.java
 *
 * Created on 2007. február 8., 20:09
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package hu.bute.daai.amorg.drtorrent.coding.bencode;

/**
 *
 * @author Tyrial
 */
public class BencodedDictionaryEntry {
    
    private BencodedString key;
    private Bencoded value;
    
    /** Creates a new instance of MTBencodedDictionaryEntry */
    public BencodedDictionaryEntry(BencodedString aKey, Bencoded aValue) 
    {
        key=aKey;
        value=aValue;
    }        

    public BencodedString getKey() {
        return key;
    }

    public Bencoded getValue() {
        return value;
    }
    
    public static int compare(BencodedDictionaryEntry first, BencodedDictionaryEntry second)
    {       
	if (first.getKey().getValue()==second.getKey().getValue())
		return 0;
        
	if (new String(first.getKey().getValue()).compareTo(new String(second.getKey().getValue())) < 0)
		return -1;
	else
		return 1;        
    }        
}
