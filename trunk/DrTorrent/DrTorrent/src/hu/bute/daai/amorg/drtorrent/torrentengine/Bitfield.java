package hu.bute.daai.amorg.drtorrent.torrentengine;

/** Class determining which pieces the torrent already has. */
public class Bitfield {
    private byte[] bitField_;
    
    /** Creates a new instance of Bitfield.
     * 
     * length     the length of the bitfield.
     * setAllBits whether or not all bits are true by default. 
     */    
    public Bitfield(int length, boolean setAllBits) {
		int lengthInBytes = length / 8;
		if ((length % 8)>0) lengthInBytes++;
		
        bitField_ = new byte[lengthInBytes];
		
		byte value;
		if (setAllBits) value = (byte) 255;
		else value = (byte) 0;
		
		for (int i=0; i<bitField_.length; i++) bitField_[i] = value;
    }
    
    /** Copy constructor. */
    public Bitfield(byte[] bitField) {
        bitField_ = bitField;
    }
    
    /** Sets the bit. */
    public void setBit(int index) {
        bitField_[index / 8] |= (128 >> (index % 8));	
    }

	/** Unsets the bit. */
    public void unsetBit(int index) {
        bitField_[index / 8] &= (~(128 >> (index % 8)));	
    }
    
    public boolean isNull() {
        for (int i = 0; i < bitField_.length; i++)
            if (bitField_[i] != 0)
                return false;
        return true;
    }

    public Bitfield clone() {
        byte[] clonedBitField = new byte[bitField_.length];        
        System.arraycopy(bitField_,0,clonedBitField,0,bitField_.length);        
        return new Bitfield(clonedBitField);                
    }
    
    public void bitwiseNot() {
        for (int i=0; i<bitField_.length; i++)
            bitField_[i] = (byte)(~(bitField_[i]));
    }

    public void bitwiseAnd(Bitfield bitField) {
        for (int i = 0; i < bitField_.length; i++)
            bitField_[i] = (byte)(bitField_[i] & bitField.data()[i]);
    }

    public void set(byte[] bitField) {
        bitField_ = bitField;
    }
        
    public boolean isBitSet(int index) {
        return ((128 >> (index % 8)) & (bitField_[index/8]))>0;
    }

    public byte[] data()  {
        return bitField_;	
    }
    
    public String getLogData() {
        String result = "";
        
        for (int i = 0; i < bitField_.length * 8; i++) {
            if (isBitSet(i)) result+="1";
            else result+="0";
        }
        
        return result;
    }

    public int lengthInBytes() {
        return bitField_.length;
    }

    public int getLengthInBites() {
        return bitField_.length;
    }
}
