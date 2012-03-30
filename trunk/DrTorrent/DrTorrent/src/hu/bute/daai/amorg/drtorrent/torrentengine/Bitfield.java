package hu.bute.daai.amorg.drtorrent.torrentengine;

/** Class determining which pieces the torrent already has. */
public class Bitfield {
    private byte[] bitfield_;
    private int lengthInBits_;
    
    /** Creates a new instance of Bitfield.
     * 
     * length     the length of the bitfield.
     * setAllBits whether or not all bits are true by default. 
     */    
    public Bitfield(int length, boolean setAllBits) {
    	lengthInBits_ = length;
		int lengthInBytes = length / 8;
		if ((length % 8) > 0) lengthInBytes++;
		
        bitfield_ = new byte[lengthInBytes];
		
		byte value;
		if (setAllBits) value = (byte) 255;
		else value = (byte) 0;
		
		for (int i = 0; i < bitfield_.length; i++) bitfield_[i] = value;
    }
    
    /** Copy constructor. */
    public Bitfield(byte[] bitField) {
        bitfield_ = bitField;
    }
    
    /** Sets the bit. */
    public void setBit(int index) {
        bitfield_[index / 8] |= (128 >> (index % 8));	
    }

	/** Unsets the bit. */
    public void unsetBit(int index) {
        bitfield_[index / 8] &= (~(128 >> (index % 8)));	
    }
    
    /** Returns whether all bits are unsetted or not. */
    public boolean isNull() {
        for (int i = 0; i < bitfield_.length; i++)
            if (bitfield_[i] != 0)
                return false;
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

    public Bitfield clone() {
        byte[] clonedBitField = new byte[bitfield_.length];        
        System.arraycopy(bitfield_,0,clonedBitField,0,bitfield_.length);        
        return new Bitfield(clonedBitField);                
    }
    
    /** Inverts all bits. */
    public void bitwiseNot() {
        for (int i=0; i<bitfield_.length; i++)
            bitfield_[i] = (byte)(~(bitfield_[i]));
    }

    public void bitwiseAnd(Bitfield bitField) {
        for (int i = 0; i < bitfield_.length; i++)
            bitfield_[i] = (byte)(bitfield_[i] & bitField.data()[i]);
    }
    
    public void set(byte[] bitField) {
        bitfield_ = bitField;
    }
        
    public boolean isBitSet(int index) {
        return ((128 >> (index % 8)) & (bitfield_[index/8]))>0;
    }

    public byte[] data()  {
        return bitfield_;	
    }
    
    public String getLogData() {
        String result = "";
        
        for (int i = 0; i < bitfield_.length * 8; i++) {
            if (isBitSet(i)) result+="1";
            else result+="0";
        }
        
        return result;
    }

    public int getLengthInBytes() {
        return bitfield_.length;
    }

    public int getLengthInBits() {
        return lengthInBits_;
    }
}
