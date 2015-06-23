package com.ziftr.android.ziftrwallet.crypto;

import java.nio.ByteBuffer;

public class ZWHdChildNumber {
	
	public static final String HD_HARDENED_KEY_IDENTIFIER = "'";
	
	private Integer num;
	private boolean hardened = false;

	public ZWHdChildNumber(int num, boolean hardened) {
		this.checkIndex(num);
		this.num = num;
		this.hardened = hardened;
	}
	
	public ZWHdChildNumber(byte[] data) {
		if (data == null || data.length != 4) {
			throw new ZWHdWalletException("Cannot construct a child index number without byte information.");
		}
		this.hardened = (data[0] & 0x80) != 0;
		data[0] = (byte)(data[0] & 0x7f);
		this.num = ByteBuffer.wrap(data).getInt();
	}
	
	public ZWHdChildNumber(String s) {
		if (s == null || s.isEmpty())
			throw new ZWHdWalletException("The path cannot be empty.");
		if (s.endsWith(HD_HARDENED_KEY_IDENTIFIER)) {
			this.hardened = true;
			s = s.substring(0, s.length()-1);
		}
		try {
			this.num = Integer.parseInt(s);
		} catch(NumberFormatException nfe) {
			throw new ZWHdWalletException("Child number cannot be parsed.");
		}
		this.checkIndex(this.num);
	}
	
	@Override
	public String toString() {
		return num.toString() + (hardened ? HD_HARDENED_KEY_IDENTIFIER : "");
	}
	
	public boolean isHardened() {
		return hardened;
	}

	public byte[] serialize() {
		// BIP32 indices are serialized as unsigned integers
		ByteBuffer b = ByteBuffer.allocate(4);
		b.put((byte) ((num >> 24) & 0x7F | (hardened ? 0x80 : 0x00)));
		b.put((byte) ((num >> 16) & 0xFF));
		b.put((byte) ((num >>  8) & 0xFF));
		b.put((byte) ((num >>  0) & 0xFF));
		return b.array();
	}
	
	private void checkIndex(int i) {
		if (i < 0) throw new ZWHdWalletException("Illegal index");
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		ZWHdChildNumber other = (ZWHdChildNumber) obj;
		if (hardened != other.hardened)
			return false;
		if (num == null) {
			if (other.num != null)
				return false;
		} else if (!num.equals(other.num)) {
			return false;
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (hardened ? 1231 : 1237);
		result = prime * result + ((num == null) ? 0 : num.hashCode());
		return result;
	}

}
