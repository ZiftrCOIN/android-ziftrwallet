package com.ziftr.android.ziftrwallet.crypto;

public class ZWTransactionOutput {
	
	private String address;
	private String transactionId;
	private int index;
	private long value;
	
	public ZWTransactionOutput(String address, String txId, int index, long value) {
		this.address = address;
		this.transactionId = txId;
		this.index = index;
		this.value = value;
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

	public long getValue() {
		return value;
	}
	
	

}
