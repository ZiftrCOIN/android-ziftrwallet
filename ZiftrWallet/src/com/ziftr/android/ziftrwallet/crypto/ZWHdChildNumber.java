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
		this.num = Integer.parseInt(s);
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
	
}
