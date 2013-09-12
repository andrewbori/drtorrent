
package hu.bute.daai.amorg.drtorrent.util.bencode;

import java.util.Stack;

public abstract class Bencoded {

    // Bencode types
    public final static int BENCODED_STRING 	= 0;
    public final static int BENCODED_INTEGER	= 1;
    public final static int BENCODED_LIST 	  	= 2;    
    public final static int BENCODED_DICTIONARY = 3;
    
    private final static int STATE_IN_PROGRESS = 0;
    private final static int STATE_FAILED 	   = 1;
    private final static int STATE_SUCCESS	   = 2;
    
    /** Creates a new instance of Bencode */
    public Bencoded() {
    }
    
    /** Returns the Bencode type */     
    public abstract int type();
    
    /** Returns the bencoded data */ 
    public abstract byte[] Bencode();    
    
    /**
     * Parses a bencoded string. It only decodes the first bencoded element of the string
     * (of course, if the element is a list or dictionary, it parses the contained elements too)!
     *
     * @param buff data to be parsed
     * @returns on succces a bencoded data class, otherwise null
     */    
    public static Bencoded parse(byte[] buff) {
        boolean itemParsed = false;
        Stack<Bencoded> parseStack = new Stack<Bencoded>();
        int parseState = STATE_IN_PROGRESS;
        int parseIndex = 0;
        int dataLength = buff.length;                 
        
        while (parseIndex < dataLength && parseState == STATE_IN_PROGRESS) {
            //System.gc();
            // bencoded string
            if (Character.isDigit((char)buff[parseIndex])) {
                String temp = "";
                char c = (char)buff[parseIndex];
                while (c != ':') {
                    temp = temp + c;
                    parseIndex++;
                    if (parseIndex > dataLength) {
                        parseState = STATE_FAILED;
                        continue;
                    }
                    c = (char)buff[parseIndex];
                }
                
                parseIndex++; // jump the ':'
                
                int stringEndIndex = 0;
                /** I had a an exception here (Bori Andras 2012-12-10): java.lang.NumberFormatExcetion: Invalid int: "140733193388032" */
                try {
                	stringEndIndex = parseIndex + Integer.parseInt(temp);
                } catch (Exception e) {
                	return null;
                }

                byte[] tempChars = new byte[stringEndIndex - parseIndex];
                System.arraycopy(buff, parseIndex, tempChars, 0, stringEndIndex - parseIndex);

                parseStack.push(new BencodedString(tempChars));
                
                parseIndex = stringEndIndex;
                
                itemParsed = true;
            } else if ((char)buff[parseIndex] == 'i') {
                String temp = "";
                parseIndex++;
                char c = (char)buff[parseIndex];
                while(c != 'e') {
                    temp = temp + c;
                    parseIndex++;
                    if (parseIndex > dataLength) {
                        parseState = STATE_FAILED;
                        continue;
                    }
                    c = (char)buff[parseIndex];
                }
                
                parseStack.push(new BencodedInteger(Long.parseLong(temp)));
            } else if ((char)buff[parseIndex] == 'l') {
                parseStack.push(new BencodedList());                
                parseIndex++;
            } else if ((char)buff[parseIndex] == 'd') {
                parseStack.push(new BencodedDictionary());                
                parseIndex++;
            } else if ((char)buff[parseIndex] == 'e') {
                itemParsed = true;
                parseIndex++;
            } else {
                parseState = STATE_FAILED;
            }
            
            if (itemParsed) {
                itemParsed = false;
                
                if (parseStack.size() == 1) {
                    parseState = STATE_SUCCESS;
                } else {
                    int type = ((Bencoded)parseStack.elementAt(parseStack.size() - 2)).type();
                    switch (type) {
                        case BENCODED_LIST:
                            Bencoded item = (Bencoded)parseStack.pop();
                            ((BencodedList)parseStack.peek()).append(item);                                                 
                        	break;
                        
                        case BENCODED_STRING:
                            if (((Bencoded)parseStack.elementAt(parseStack.size() - 3)).type() == Bencoded.BENCODED_DICTIONARY) {
                                Bencoded value = (Bencoded)parseStack.pop();
                                BencodedString key = (BencodedString)parseStack.pop();
                                ((BencodedDictionary)parseStack.peek()).addEntry(key, value);					
                            } else {
                                parseState = STATE_FAILED;
                            }
                            break;                        
                        
                        case BENCODED_DICTIONARY:
                            break;

                        default:
                            parseState = STATE_FAILED;												
                            break;	                        
                    }                                        
                }
            }            
        }

        Bencoded parsedItem = null;
        if (parseState == STATE_SUCCESS) {
            parsedItem = (Bencoded)parseStack.pop();
        }
			                
        return parsedItem;
    }
}