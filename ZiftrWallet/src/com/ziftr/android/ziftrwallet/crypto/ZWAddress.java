package com.ziftr.android.ziftrwallet.crypto;

import java.util.Arrays;
import java.util.Locale;

import javax.annotation.Nullable;

import android.annotation.SuppressLint;

import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.fragment.ZWSearchableListItem;
import com.ziftr.android.ziftrwallet.util.ZiftrBase58;

public abstract class ZWAddress implements ZWSearchableListItem {
	
	///////////////////////////////////////
	//////////  Database Fields  //////////
	///////////////////////////////////////
	
	/**
	* The note that the user has applied to this given address. 
	*/
	private String label = "";
	
	/**
	* The last known balance for this address. May not be up to date.
	* 
	* TODO do we really need this for all addresses or can this go in the ECKey class?
	*/
	private long lastKnownBalance;
	
	/**
	* The most recent time that this address had a transaction made using it. May not be up to date.
	*/
	private long lastTimeModifiedSeconds;
	
	//////////////////////////////////////////////
	//////////  Address Content Fields  //////////
	//////////////////////////////////////////////
	
	/**
	* An address is a RIPEMD160 hash of a public key, therefore is always 
	* 160 bits, or 20 bytes.
	*/
	public static final int LENGTH = 20;
	
	/** The type of coin that this is an address for. */
	private ZWCoin coin;
	
	/** The version byte specifies what type of address it is (P2SH, P2PubKey, etc). */
	private byte versionByte;
	
	/** 
	* Addresses are ripemd(sha256( pub_key )) encoded in base 58. This is the holder
	* for the result of that computation. 
	*/
	private byte[] hash160;
	
	
	/**
	 * A private helper method for initialization that prevents code duplication. 
	 * 
	 * @param coin
	 * @param versionByte
	 * @param hash160
	 */
	protected void initialize(ZWCoin coin, byte versionByte, byte[] hash160) throws ZWAddressFormatException {
		if (hash160 == null || hash160.length != 20) {
			throw new ZWAddressFormatException("Addresses are 160-bit hashes, so you must provide 20 bytes");
		}

		if (coin != null) {
			if (!isAcceptableVersion(coin, versionByte)) {
				String exceptionMessage = "Version code of address did not match acceptable versions for network: " + 
						String.valueOf(versionByte) + " not in " +
						Arrays.toString(coin.getAcceptableAddressCodes());
				throw new ZWAddressFormatException(exceptionMessage);
			}
		} else {
			// If null then we need to infer what the coinType is 
			coin = getCoinTypeFromVersionByte(versionByte);
		}

		if (coin == null) {
			throw new ZWAddressFormatException("Version byte does not match any supported coin types");
		}

		this.coin = coin;
		this.versionByte = versionByte;
		this.hash160 = hash160;
		// Default, should be overridden with the setter method
		lastTimeModifiedSeconds = System.currentTimeMillis() / 1000;
	}

	public ZWCoin getCoin() {
		return this.coin;
	}

	/**
	 * @return the versionByte
	 */
	public byte getVersionByte() {
		return versionByte;
	}

	
	/** The (big endian) 20 byte hash that is the core of an address. */
	public byte[] getHash160() {
		return hash160;
	}

	/**
	 * Returns true if this address is a Pay-To-Script-Hash (P2SH) address.
	 * See also https://github.com/bitcoin/bips/blob/master/bip-0013.mediawiki: 
	 * Address Format for pay-to-script-hash.
	 */
	public boolean isP2SHAddress() {
		return coin != null && this.versionByte == this.coin.getScriptHashPrefix();
	}

	@Override
	public String toString() {
		return this.getAddress();
	}
	
	///////////////////////////////////////////
	//////////  Getters and Setters  //////////
	///////////////////////////////////////////

	public abstract boolean isOwnedAddress();

	/**
	 * @return the note
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * Gets the publicly displayed base58 encoded version of this address
	 * @return the public address as a String
	 */
	public String getAddress() {
		return ZiftrBase58.encode(this.versionByte, this.hash160);
	}

	/**
	 * @param note the note to set
	 */
	public void setLabel(String note) {
		this.label = note;
	}

	/**
	 * @return the lastKnownBalance
	 */
	public long getLastKnownBalance() {
		return lastKnownBalance;
	}

	/**
	 * @param lastKnownBalance the lastKnownBalance to set
	 */
	public void setLastKnownBalance(long lastKnownBalance) {
		this.lastKnownBalance = lastKnownBalance;
	}

	/**
	 * @return the lastTimeModifiedSeconds
	 */
	public long getLastTimeModifiedSeconds() {
		return lastTimeModifiedSeconds;
	}

	/**
	 * @param lastTimeModifiedSeconds the lastTimeModifiedSeconds to set
	 */
	public void setLastTimeModifiedSeconds(long lastTimeModifiedSeconds) {
		this.lastTimeModifiedSeconds = lastTimeModifiedSeconds;
	}

	public static boolean isAcceptableVersion(ZWCoin coinId, byte b) {
		return coinId.getPubKeyHashPrefix() == b || coinId.getScriptHashPrefix() == b;
	}

	/**
	 * Given an address, examines the version byte and attempts to find a matching NetworkParameters. If you aren't sure
	 * which network the address is intended for (eg, it was provided by a user), you can use this to decide if it is
	 * compatible with the current wallet.
	 * @return a NetworkParameters or null if the string wasn't of a known version.
	 */
	@Nullable
	public static ZWCoin getCoinTypeFromAddress(String address) throws ZWAddressFormatException {
		return getCoinTypeFromDecodedAddress(ZiftrBase58.decodeChecked(address));
	}

	@Nullable
	private static ZWCoin getCoinTypeFromDecodedAddress(byte[] allData) {
		return getCoinTypeFromVersionByte(allData[0]);
	}

	@Nullable
	private static ZWCoin getCoinTypeFromVersionByte(byte version) {
		for (ZWCoin coin : ZWCoin.getAllCoins()) {
			if (isAcceptableVersion(coin, version)) {
				if(coin.getChain().equals("main")) {
					//never attempt to load testnet coins from version bytes (too much ambiguity between different testnets)
					return coin;
				}
			}
		}
		return null;
	}

	
	@SuppressLint("DefaultLocale")
	@Override
	public boolean matches(CharSequence constraint, ZWSearchableListItem nextItem) {
		boolean addressMatches = this.toString().toLowerCase(Locale.ENGLISH).contains(constraint.toString().toLowerCase());
		boolean labelMatches = this.getLabel().toLowerCase(Locale.ENGLISH).contains(constraint.toString().toLowerCase());
		return addressMatches || labelMatches;
	}

}
