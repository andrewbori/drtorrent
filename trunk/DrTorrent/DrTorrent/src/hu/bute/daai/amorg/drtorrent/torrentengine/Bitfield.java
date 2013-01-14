package hu.bute.daai.amorg.drtorrent.torrentengine;

import java.io.Serializable;

/** Class determining which pieces the torrent already has. */
public class Bitfield implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private byte[] bitfield_;
    private int lengthInBits_;
    
    private boolean isChanged_ = true;
    
    /** Creates a new instance of Bitfield.
     * 
     * length     the length of the bitfield.
     * setAllBits whether or not all bits are true by default. 
     */    
    public Bitfield(int length, boolean setAllBits) {
    	lengthInBits_ = length;
		int lengthInBytes = length / 8;
		if ((length % 8) > 0) {
			lengthInBytes++;
		}
		
        bitfield_ = new byte[lengthInBytes];
		
		byte value;
		if (setAllBits) {
			value = (byte) 255;
		} else {
			value = (byte) 0;
		}
		
		for (int i = 0; i < bitfield_.length; i++) bitfield_[i] = value;
    }
    
    /** Copy constructor. */
    public Bitfield(byte[] bitField) {
        bitfield_ = bitField;
        lengthInBits_ = bitfield_.length * 8;
    }
    
    /** Sets the bit. */
    public void setBit(int index) {
        bitfield_[index / 8] |= (128 >> (index % 8));
        isChanged_ = true;
    }

	/** Unsets the bit. */
    public void unsetBit(int index) {
        bitfield_[index / 8] &= (~(128 >> (index % 8)));
        isChanged_ = true;
    }
    
    /** Returns whether all bits are unsetted or not. */
    public boolean isNull() {
        for (int i = 0; i < bitfield_.length; i++) {
            if (bitfield_[i] != 0) {
                return false;
            }
        }
        return true;
    }
    
    /** Returns whether all bits are setted or not. */
    public boolean isFull() {
        for (int i = 0; i < bitfield_.length; i++) {
            if (i+1 < lengthInBits_) {
            	if (bitfield_[i] != 255) return false;
            } else {
            	byte lastFull = (byte) 255;
            	lastFull <<= (8 - lengthInBits_ % 8);
            	if (bitfield_[i] != lastFull) return false;
            }
        }
        return true;
    }
    
    /** Returns the count of the setted bits. */
    public int countOfSet() {
    	int n = 0;
    	for (int i = 0; i < lengthInBits_; i++) {
           if (isBitSet(i)) n++; 
        }
    	return n;
    }

    public Bitfield clone() {
        final byte[] clonedBitField = new byte[bitfield_.length];        
        System.arraycopy(bitfield_,0,clonedBitField,0,bitfield_.length);        
        return new Bitfield(clonedBitField);                
    }
    
    /** Inverts all bits. */
    public void bitwiseNot() {
        for (int i=0; i<bitfield_.length; i++) {
        	bitfield_[i] = (byte)(~(bitfield_[i]));
        }
        isChanged_ = true;
    }

    public void bitwiseAnd(final Bitfield bitField) {
        for (int i = 0; i < bitfield_.length; i++) {
        	bitfield_[i] = (byte)(bitfield_[i] & bitField.data()[i]);
        }
        isChanged_ = true;
    }
    
    public Bitfield getBitfieldAnd(final Bitfield other) {
        Bitfield newBitfield = new Bitfield(lengthInBits_, false);
    	for (int i = 0; i < bitfield_.length && i < other.bitfield_.length; i++) {
        	newBitfield.bitfield_[i] = (byte)(bitfield_[i] & other.bitfield_[i]);
        }
        return newBitfield;
    }
    
    public void set(final byte[] bitField) {
        bitfield_ = bitField;
    }
        
    public boolean isBitSet(final int index) {
    	try {
    		return ((128 >> (index % 8)) & (bitfield_[index / 8])) > 0;
    	} catch (Exception e) {
    		return false;
    	}
    }

    public byte[] data()  {
        return bitfield_;	
    }

    public int getLengthInBytes() {
        return bitfield_.length;
    }

    public int getLengthInBits() {
        return lengthInBits_;
    }
    
    public boolean isChanged() {
    	return isChanged_;
    }
    
    public void setChanged(boolean isChanged) {
    	isChanged_ = isChanged;
    }
    
    public boolean hasSettedCompearedTo(final Bitfield bitfield) {
    	for (int i = 0; i < bitfield_.length && i < bitfield.bitfield_.length; i++) {
    		if ((byte)((bitfield_[i] ^ bitfield.bitfield_[i]) & bitfield_[i]) != 0) {
    			return true;
    		}
        }
    	
    	return false;
    }
    
    @Override
    public boolean equals(final Object o) {
    	final Bitfield other = (Bitfield) o;
    	if (this.lengthInBits_ != other.lengthInBits_) {
    		return false;
    	}
    	for (int i = 0; i < bitfield_.length; i++) {
    		if (this.bitfield_[i] != other.bitfield_[i]) {
    			return false;
    		}
    	}
    	
    	return true;
    }
}
