
package hu.bute.daai.amorg.drtorrent.util.bencode;

import java.util.Stack;

public abstract class Bencoded {

    // Bencode types
    public final static int BENCODED_STRING 	= 0;
    public final static int BENCODED_INTEGER	= 1;
    public final static int BENCODED_LIST 	  	= 2;    
    public final static int BENCODED_DICTIONARY = 3;
    
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
     * @param buff data to be parsed.
     * @returns on succces a bencoded data class, otherwise null.
     * @throws BencodedException when a parsing error happens.
     */    
    public static Bencoded parse(byte[] buff) throws BencodedException {
        try {
        	boolean itemParsed = false;
            final Stack<Bencoded> parseStack = new Stack<Bencoded>();
            int parseIndex = 0;
            
	        while (parseIndex < buff.length) {
	            // Read Bencoded String: [str.length]:[str]
	            if (Character.isDigit((char)buff[parseIndex])) {
	                final String temp = readFromBufferUntil(buff, parseIndex, ':');
	                parseIndex += temp.length();
	                parseIndex++;	// jump after the ':'
	                
	                int stringEndIndex = parseIndex + Integer.parseInt(temp);

	                // Read the string value
	                byte[] tempChars = new byte[stringEndIndex - parseIndex];
	                System.arraycopy(buff, parseIndex, tempChars, 0, stringEndIndex - parseIndex);
	                parseStack.push(new BencodedString(tempChars));
	                
	                parseIndex = stringEndIndex;
	                itemParsed = true;
                
                // Read Bencoded Integer: i[value]e
	            } else if ((char)buff[parseIndex] == 'i') {
	                parseIndex++;
	                String temp = readFromBufferUntil(buff, parseIndex, 'e');
	                parseIndex += temp.length();
	                
	                parseStack.push(new BencodedInteger(Long.parseLong(temp)));
	            
                // Read Bencoded List: l[elements]e
	            } else if ((char)buff[parseIndex] == 'l') {
	                parseStack.push(new BencodedList());                
	                parseIndex++;
	                
                // Read Bencoded Dictionary: d[key-value pairs]e
	            } else if ((char)buff[parseIndex] == 'd') {
	                parseStack.push(new BencodedDictionary());                
	                parseIndex++;
	                
                // Read end of Bencoded List or Dictionary
	            } else if ((char)buff[parseIndex] == 'e') {
	                itemParsed = true;
	                parseIndex++;
	                
	            } else {
	            	throw new BencodedException("Invalid type marker: " + (char)buff[parseIndex]);
	            }
	            
	            if (itemParsed) {
	                itemParsed = false;
	                
	                if (parseStack.size() == 1) {
	                    break;	// SUCCESS
	                    
	                } else {
	                	// Get the type of the element that is under the top
	                    int type = ((Bencoded)parseStack.elementAt(parseStack.size() - 2)).type();
	                    switch (type) {
	                        case BENCODED_LIST:
	                        	// Append the item to the list
	                            Bencoded item = (Bencoded)parseStack.pop();
	                            ((BencodedList)parseStack.peek()).append(item);                                                 
	                        	break;
	                        
	                        case BENCODED_STRING:
	                        	// If the third element is a dictionary then we got a key-value pair
	                            if (((Bencoded)parseStack.elementAt(parseStack.size() - 3)).type() == Bencoded.BENCODED_DICTIONARY) {
	                                // Add the key-value pair to the dictionary
	                            	Bencoded value = (Bencoded)parseStack.pop();
	                                BencodedString key = (BencodedString)parseStack.pop();
	                                ((BencodedDictionary)parseStack.peek()).addEntry(key, value);					
	                            } else {
	                            	throw new BencodedException("Unexpected parsing error!");
	                            }
	                            break;                        
	                        
	                        case BENCODED_DICTIONARY:
	                        	// The top element is a key, go to the next iteration and read its value
	                            break;
	
	                        default:
	                        	throw new BencodedException("Unexpected parsing error!");                      
	                    }                                        
	                }
	            }            
	        }
	        Bencoded parsedItem = (Bencoded)parseStack.pop();
	        return parsedItem;
        } catch (ArrayIndexOutOfBoundsException e) {
        	throw new BencodedException("Array out of boundary. Cannot find the end of the bencoded value!");
        } catch (NumberFormatException e) {
        	throw new BencodedException("Invalid number format!");
        } catch (BencodedException e) {
        	throw e;
        } catch (Exception e) {
        	throw new BencodedException("Unexpected parsing error!", e);
        }
    }
    
    /** Reads from the buffer from the specified index until the given marker is not reached. */
    private static String readFromBufferUntil(final byte[] buffer, final int index, final char marker) {
    	int parseIndex = index;
    	String temp = "";
        char c = (char)buffer[parseIndex];
        while (c != marker) {
            temp = temp + c;
            parseIndex++;
            c = (char)buffer[parseIndex];
        }
        return temp;
    }
}