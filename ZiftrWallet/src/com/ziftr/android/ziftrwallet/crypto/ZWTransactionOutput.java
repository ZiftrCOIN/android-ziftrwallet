package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;

public class ZWTransactionOutput {
	
	private String address;
	private String transactionId;
	private int index;
	private BigInteger value;
	private boolean isMultiSig = false;

	
	public ZWTransactionOutput(String address, String txId, int index, String value, boolean isMultiSig) {
		this(address, txId, index, new BigInteger(value), isMultiSig);
	}
	
	
	public ZWTransactionOutput(String address, String txId, int index, BigInteger value, boolean isMultiSig) {
		this.address = address;
		this.transactionId = txId;
		this.index = index;
		this.value = value;
		this.isMultiSig = isMultiSig;
	}

	
	public String getAddress() {
		return address;
	}

	public String getTransactionId() {
		return transactionId;
	}

	public int getIndex() {
		return index;
	}

	public BigInteger getValue() {
		return value;
	}
	
	public String getValueString() {
		return value.toString();
	}
	
	public boolean isMultiSig() {
		return isMultiSig;
	}
	
	

}
