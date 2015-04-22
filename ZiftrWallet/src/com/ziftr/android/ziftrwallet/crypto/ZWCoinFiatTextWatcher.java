package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigDecimal;

import com.ziftr.android.ziftrwallet.ZWPreferences;
import com.ziftr.android.ziftrwallet.util.ZLog;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class ZWCoinFiatTextWatcher implements TextWatcher {

	ZWCoin coin;
	EditText coinEdit;
	EditText fiatEdit;
	boolean userEdited = true;
	
	public ZWCoinFiatTextWatcher(ZWCoin coin, EditText coinEdit, EditText fiatEdit) {
		this.coin = coin;
		this.coinEdit = coinEdit;
		this.fiatEdit = fiatEdit;
	}
	
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		//no op
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		//no op
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(s == coinEdit.getEditableText()) {
			//user edited the coin edit, so update the fiat edit accordingly
			ZWFiat selectedFiat = ZWPreferences.getFiatCurrency();
			
			if(userEdited) {
				try {
					String coinValueString = s.toString();
					BigDecimal coinValue = new BigDecimal(coinValueString);
					BigDecimal fiatValue = ZWConverter.convert(coinValue, coin, selectedFiat);
					
					userEdited = false;
					fiatEdit.setText(selectedFiat.getFormattedAmount(fiatValue));
				} 
				catch(NumberFormatException nfe) {
					// If a value can't be parsed then we just do nothing
					// and leave the other box with what was already in it.
				} 
				catch (ArithmeticException ae) {
					// Shouldn't happen
					ZLog.log("User entered non-sensical value. Returning for now.");
				}
			}
			
			userEdited = true; //always consider it edited by the user unless the code explicitly changes it
		}
		else if(s == fiatEdit.getEditableText()) {
			if(userEdited) {
				try {
					ZWFiat selectedFiat = ZWPreferences.getFiatCurrency();
					String fiatValueString = s.toString();
					BigDecimal fiatValue = new BigDecimal(fiatValueString);
					BigDecimal coinValue = ZWConverter.convert(fiatValue, selectedFiat, coin);
					
					userEdited = false;
					coinEdit.setText(coin.getFormattedAmount(coinValue));
				} 
				catch(NumberFormatException nfe) {
					// If a value can't be parsed then we just do nothing
					// and leave the other box with what was already in it.
					return;
				} 
				catch (ArithmeticException ae) {
					// Shouldn't happen
					ZLog.log("User entered non-sensical value. Returning for now.");
					return;
				}
			}
			
			userEdited = true;
		}
		
	}

}
