package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.JSONObject;

public class ZWRawTransaction  {
	
	public static int ERROR_CODE_BAD_DATA = -2;
	
	private JSONObject rawData;
	
	private BigInteger change;
	private BigInteger spentCoins;
	private BigInteger inputCoins;
	private String toAddress;
	private boolean usingUnconfirmedInputs;
	
	private ZWCoin coin;
	
	private BigInteger totalFee;
	private BigInteger totalSpent;
	
	private int errorCode;
	private String errorMessage;
	
	public ZWRawTransaction(ZWCoin coin, BigInteger inputCoins, BigInteger spentCoins, BigInteger change, String toAddress) {
		this.coin = coin;
		this.toAddress = toAddress;
		
		this.inputCoins = inputCoins;
		this.spentCoins = spentCoins;
		this.change = change;
		
		this.totalFee = inputCoins.subtract(spentCoins).subtract(change);
		this.totalSpent = totalFee.add(spentCoins);
	}
	
	
	public ZWRawTransaction(ZWCoin coin, String toAddress, int errorCode, String errorMessage) {
		this.coin = coin;
		this.toAddress = toAddress;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
	
	
	public void setRawData(JSONObject rawData) {
		this.rawData = rawData;
	}
	
	public String getTotalSpent() {
		BigDecimal totalDecimal = coin.getAmount(this.totalSpent);
		return totalDecimal.stripTrailingZeros().toPlainString();
	}
	
	
	public String getFee() {
		if(this.totalFee.compareTo(BigInteger.ZERO) == 0) {
			//if there is no fee, return null so calling code can easily tell
			return null;
		}
		else {
			BigDecimal feeDecimal = coin.getAmount(this.totalFee);
			return feeDecimal.stripTrailingZeros().toPlainString();
		}
	}
	
	
	public int getErrorCode() {
		return this.errorCode;
	}
	
	
	public String getErrorMessage() {
		return this.errorMessage;
	}

	public void setUsingUnconfirmedInputs(boolean usingUnconfirmedInputs) {
		this.usingUnconfirmedInputs = usingUnconfirmedInputs;
	}
	
	
	public boolean usingUnconfirmedInputs() {
		return this.usingUnconfirmedInputs;
	}
	
	public String getAddress() {
		return this.toAddress;
	}
	
	
	public ZWCoin getCoin() {
		return this.coin;
	}
	
	public JSONObject getRawData() {
		return this.rawData;
	}
}




