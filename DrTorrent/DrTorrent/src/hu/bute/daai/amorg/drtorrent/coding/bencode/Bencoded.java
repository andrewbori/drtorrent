
package hu.bute.daai.amorg.drtorrent.coding.bencode;

import java.util.Stack;

public abstract class Bencoded {

    // Bencode types
    public static int BENCODED_STRING 	  = 0;
    public static int BENCODED_INTEGER	  = 1;
    public static int BENCODED_LIST 	  = 2;    
    public static int BENCODED_DICTIONARY = 3; 
    
    /** Creates a new instance of Bencode */
    public Bencoded() {
    }
    
    /** Returns the Bencode type */     
    public abstract int type();
    
    /** Returns the bencoded data */ 
    public abstract byte[] Bencode();    
    
    /**
     * Parses a bencoded string. It only decodes the first
     * bencoded element of the string (of course, if the element
     * is a list or dictionary, it parses the contained elements too)!
     *
     * return on succces a bencoded data class,
     * otherwise null
     */    
    public static Bencoded parse(byte[] buff)
    {
        boolean itemParsed = false;
        Stack<Bencoded> parseStack = new Stack<Bencoded>();
        int parseState = 0; // 0: in progress; 1: failed; 2: success
        int parseIndex = 0;
        int dataLength = buff.length;                 
        
        while (parseIndex<dataLength && parseState==0)
        {
            //System.gc();
            // bencoded string
            if (Character.isDigit((char)buff[parseIndex]))
            {
                String temp="";
                char c = (char)buff[parseIndex];
                while(c!=':')
                {
                    temp=temp+c;
                    parseIndex++;
                    if (parseIndex>dataLength)
                    {
                        parseState = 1; // failed
                        continue;
                    }
                    c = (char)buff[parseIndex];
                }
                
                parseIndex++; // jump the ':'
                
                int stringEndIndex = parseIndex+Integer.parseInt(temp);

                byte[] tempChars=new byte[stringEndIndex-parseIndex];
                System.arraycopy(buff,parseIndex,tempChars,0,stringEndIndex-parseIndex);

                parseStack.push(new BencodedString(tempChars));
                
                parseIndex = stringEndIndex;
                
                itemParsed = true;
            }
            else if ((char)buff[parseIndex]=='i')
            {
                String temp="";
                parseIndex++;
                char c = (char)buff[parseIndex];
                while(c!='e')
                {
                    temp=temp+c;
                    parseIndex++;
                    if (parseIndex>dataLength)
                    {
                        parseState = 1; // failed
                        continue;
                    }
                    c = (char)buff[parseIndex];
                }
                
                parseStack.push(new BencodedInteger(Integer.parseInt(temp)));
            }
            else if ((char)buff[parseIndex]=='l')
            {
                parseStack.push(new BencodedList());                
                parseIndex++;
            }
            else if ((char)buff[parseIndex]=='d')
            {
                parseStack.push(new BencodedDictionary());                
                parseIndex++;
            }            
            else if ((char)buff[parseIndex]=='e')
            {
                itemParsed = true;
                parseIndex++;
            }
            else
                parseState = 1; // failed
            
            if (itemParsed)
            {
                itemParsed = false;
                
                if (parseStack.size()==1)
                    parseState = 2; // success
                else
                {
                    int type = ((Bencoded)parseStack.elementAt(parseStack.size()-2)).type();
                    switch (type)
                    {
                        case 2: // bencodedList
                        {
                            Bencoded item = (Bencoded)parseStack.pop();
                            ((BencodedList)parseStack.peek()).append(item);                                                 
                        }
                        break;
                        
                        case 0: // bencodedString
                        {
                            if (((Bencoded)parseStack.elementAt(parseStack.size()-3)).type() == Bencoded.BENCODED_DICTIONARY)
                            {
                                Bencoded value = (Bencoded)parseStack.pop();
                                BencodedString key = (BencodedString)parseStack.pop();
                                ((BencodedDictionary)parseStack.peek()).addEntry(key,value);					
                            }
                            else
                                parseState = 1;	// failed											
                        }
                        break;                        
                        
                        case 3: // BencodedDictionary
                            break;

                        default:
                        {
                            parseState = 1;												
                        }
                        break;	                        
                    }                                        
                }
            }            
        }

        Bencoded parsedItem = null;
        if (parseState == 2) // success
            parsedItem = (Bencoded)parseStack.pop();		
			                
        return parsedItem;
    }
}
