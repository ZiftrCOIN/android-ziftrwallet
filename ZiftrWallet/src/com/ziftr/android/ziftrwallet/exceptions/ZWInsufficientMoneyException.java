/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ziftr.android.ziftrwallet.exceptions;

import java.math.BigInteger;

import javax.annotation.Nullable;

import com.ziftr.android.ziftrwallet.crypto.ZWCoin;

/**
 * Thrown to indicate that you don't have enough money available 
 * to perform the requested operation.
 */
@SuppressWarnings("serial")
public class ZWInsufficientMoneyException extends Exception {
    
	/**
	 * The type of coin that this exception was thrown for.
	 */
	private final ZWCoin coinId;
	
	/** 
     * Contains the number of satoshis that would have been required to 
     * complete the operation. 
     */
    @Nullable
    private final BigInteger insufficientByAmount;

	protected ZWInsufficientMoneyException(ZWCoin coinId) {
		this.coinId = coinId;
        this.insufficientByAmount = null;
    }

    public ZWInsufficientMoneyException(ZWCoin coinId, BigInteger missing) {
        this(coinId, missing, "Insufficient money,  missing " + missing + " atomic units");
    }

    public ZWInsufficientMoneyException(ZWCoin coinId, BigInteger missing, String message) {
        super(message);
        this.coinId = coinId;
        this.insufficientByAmount = missing;
    }

	public ZWCoin getCoinId() {
		return this.coinId;
	}
	
	/**
	 * @return the insufficientBy
	 */
	public BigInteger getInsufficientBy() {
		return insufficientByAmount;
	}

}
