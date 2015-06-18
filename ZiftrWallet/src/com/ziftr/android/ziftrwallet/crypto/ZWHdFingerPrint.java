package com.ziftr.android.ziftrwallet.crypto;

import java.util.Arrays;


public class ZWHdFingerPrint {

	byte[] fp = new byte[4];

	public ZWHdFingerPrint() {
		Arrays.fill(this.fp, (byte)0);
	}

	public ZWHdFingerPrint(byte[] bytes) { 
		if (bytes == null || bytes.length < 4) {
			throw new ZWHdWalletException("Finger prints must have at least 4 bytes of data");
		}
		System.arraycopy(bytes, 0, fp, 0, 4);
	}

	public byte[] serialize() {
		return Arrays.copyOf(fp, 4);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(fp);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!this.getClass().equals(obj.getClass()))
			return false;
		ZWHdFingerPrint other = (ZWHdFingerPrint) obj;
		return Arrays.equals(fp, other.fp);
	}

}
