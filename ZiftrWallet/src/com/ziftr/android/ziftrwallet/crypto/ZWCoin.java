package com.ziftr.android.ziftrwallet.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Charsets;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.Base58;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

/**
 * This class holds the Type enum. Each member of the enum can be thought of as an identifier for 
 * that cryptocurrency type. Abstract classes can define an abstract getCoinId() method and 
 * then subclasses can be made that implement this method, which is very useful for making classes 
 * applicable to many types of cryptocurrencies. 
 * 
 * TODO somehow incorporate a list of address prefixes for each coin type. 
 * e.g. bitcoin address start with a 1, bitcoin private keys start with 
 * 
 * Four leading prefixes are needed, currently, for each coin type:
 *     1. pubKeyHashPrefix
 *     2. scriptHashPrefix
 *     3. privKeyPrefix
 */
public class ZWCoin implements ZWCurrency {
	
	
	/** When using bundles, this can be used to store a specific coin type. */
	public static final String TYPE_KEY = "ZWCOIN_TYPE_KEY";


	//public static final ZWCoin[] TYPES = new ZWCoin[] {BTC, LTC, PPC, DOGE};
	//public static final ZWCoin[] TYPES_TEST = new ZWCoin[] {BTC_TEST, LTC_TEST, PPC_TEST, DOGE_TEST};
	
	public static HashMap<String, ZWCoin> coins = new HashMap<String, ZWCoin>();
	
	//use these until server is sending actual images (and for display before images are downloaded)
	private static HashMap<String, Integer> logoMap = new HashMap<String, Integer>();
	static {
		logoMap.put("btc", R.drawable.logo_bitcoin);
		logoMap.put("ltc", R.drawable.logo_litecoin);
		logoMap.put("doge", R.drawable.logo_dogecoin);
		logoMap.put("ppc", R.drawable.logo_peercoin);
	}
	
	public static void loadCoins(List<ZWCoin> loadedCoins) {		
		for(ZWCoin coin : loadedCoins) {
			coins.put(coin.getSymbol(), coin);
		}
	}
	
	public static final Collection<ZWCoin> getAllCoins() {
		return coins.values();
	}

	
	public static ZWCoin getCoin(String coinSymbol) {
		return coins.get(coinSymbol);
	}
	
	
	private boolean enabled = false;
	private BigInteger defaultFeePer;
	private byte pubKeyHashPrefix; 
	private byte scriptHashPrefix;  
	private byte privKeyPrefix; // TODO assume its pubKeyHashPrefix + 128
	private String chain;
	private int confirmationsNeeded; 
	private int blockTime;  
	private String type;
	private String name;
	private String scheme; 
	private int scale;
	private String logoUrl;
	
	
	public ZWCoin(String name, String type, String chain, String scheme, int scale, 
			String defaultFee, String logoUrl, byte pubKeyHashPrefix, byte scriptHashPrefix, 
			byte privKeyPrefix, int confirmationsNeeded, int blockTime, boolean enabled) {

		this.name = name;
		this.type = type;
		this.chain = chain;
		this.scheme = scheme;
		this.scale = scale;
		this.logoUrl = logoUrl;
		this.pubKeyHashPrefix = pubKeyHashPrefix;
		this.scriptHashPrefix = scriptHashPrefix;
		this.privKeyPrefix = privKeyPrefix;
		this.confirmationsNeeded = confirmationsNeeded;
		this.blockTime = blockTime;
		this.enabled = enabled;

		try {
			this.defaultFeePer = new BigInteger(defaultFee);
		}
		catch(Exception e) {
			ZLog.log("Exception setting default fee of ", defaultFee, "to coin ", name);
			this.defaultFeePer = BigInteger.ZERO;
		}
	}



	/**
	 * gets the default fee per kb for a transaction for this coin
	 * @return the default fee per kb
	 */
	public BigInteger getDefaultFee() {
		return this.defaultFeePer;
	}

	
	@Override
	public String getSymbol() {
		//for all known coins the symbol/ticker is just an all uppercase version of what the apis call type
		//for example btc =  BTC
		//we also appened _TEST for testnet coins
		String symbol = this.type.toUpperCase(Locale.US);
		
		if(!chain.equals("main")) {
			symbol = symbol + "_TEST";
		}
		
		return symbol;
	}
	

	@Override
	public String getShortTitle() {
		//just use symbol for short title
		return this.getSymbol();
	}

	
	/**
	 * @return the longTitle
	 */
	public String getName() {
		return name;
	}

	
	public String getLogoUrl() {
		return this.logoUrl;
	}
	

	/**
	 * gets which type of coin this is for sending to api/database, eg "btc"
	 * @return a String used to identify a coin
	 */
	public String getType() {
		return this.type;
	}


	/**
	 * gets which chain this coin is on eg testnet3 vs main
	 * @return the chain this coin is using as a String
	 */
	public String getChain() {
		return this.chain;
	}

	/**
	 * gets the scheme used for this coin, this is how the app knows which coin to use when 
	 * a user clicks on a supported bitcoin uri link
	 * @return the scheme used in uri
	 */
	public String getScheme() {
		return scheme;
	}

	
	@Override
	public int getScale() {
		return this.scale;
	}

	
	public int getBlockTime() {
		return this.blockTime;
	}
	
	
	/**
	 * is the server able to give proper information about this coin?
	 * meaning is the api enabled, is the blockchain up to date, etc
	 * @return true if the coin is fully enabled, false if there may be coin specific functionality issues
	 */
	public boolean isEnabled() {
		return this.enabled;
	}
	
	
	/**
	 * gets the hard coded resource id for the logo for this coin
	 * @return 
	 */
	public int getLogoResId() {
		Integer resId = logoMap.get(this.type);
		if(resId == null) {
			return 0;
		}
		
		return resId; 
	}

	
	/**
	 * @return the pubKeyHashPrefix
	 */
	public byte getPubKeyHashPrefix() {
		return this.pubKeyHashPrefix;
	}

	/**
	 * @return the scriptHashPrefix
	 */
	public byte getScriptHashPrefix() {
		return this.scriptHashPrefix;
	}

	/**
	 * @return the uncompressedPrivKeyPrefix
	 */
	public byte getPrivKeyPrefix() {
		return this.privKeyPrefix;
	}

	/**
	 * @return the numRecommendedConfirmations
	 */
	public int getNumRecommendedConfirmations() {
		return this.confirmationsNeeded;
	}

	public byte[] getAcceptableAddressCodes() {
		return new byte[] {getPubKeyHashPrefix(), getScriptHashPrefix()};
	}

	/**
	 * @return the secondsPerAverageBlockSolve
	 */
	public int getSecondsPerAverageBlockSolve() {
		return this.blockTime;
	}

	
	@Override
	public String getFormattedAmount(BigDecimal amount) {
		
		BigDecimal coins = ZiftrUtils.formatToNDecimalPlaces(this.getScale(), amount);
		
		coins = coins.stripTrailingZeros();
		if(coins.scale() < 2 || coins.compareTo(BigDecimal.ZERO) == 0) {
			//make sure and always show 2 decimal places (even trailing zeros) like the QT wallets do
			coins = coins.setScale(2);
			
			//note: there is a bug when stripping trailing zeroes from a BigDecimal with value of 0 eg 0.0000
			//http://bugs.java.com/bugdatabase/view_bug.do?bug_id=6480539
			//so we also test if coins has a value of zero and explicitly set the scale
		}
		
		return coins.toPlainString();
	}
	
	@Override
	public String getFormattedAmount(BigInteger atmoicUnits) {
		
		BigDecimal toFormatAsDecimal = this.getAmount(atmoicUnits);
		BigDecimal coins = ZiftrUtils.formatToNDecimalPlaces(this.getScale(), toFormatAsDecimal);
		
		return this.getFormattedAmount(coins);
	}

	/**
	 * This is a placeholder for a spot to determine if a given address
	 * is valid.
	 * 
	 * @param address - The address to verify the validity of.
	 * @return as above
	 * 
	 * TODO use the checksum to make sure addresses are valid
	 */
	public boolean addressIsValid(String address) {
		if (address == null || address.isEmpty()) {
			return false;
		}

		try {
			byte[] decodedBytes = Base58.decode(address);
			for (byte b : this.getAcceptableAddressCodes()) {
				if (decodedBytes[0] == b) {
					return true;
				}
			}
			return false;
		} catch(ZWAddressFormatException afe) {
			return false;
		}
	}


	@Override
	public BigDecimal getAmount(BigInteger atomicUnits) {
		return new BigDecimal(atomicUnits, this.getScale());
	}

	
	@Override
	public BigInteger getAtomicUnits(BigDecimal amount) {
		int precision = -1*this.getScale();
		
		//note, this constructor is weird to me, but RTFM, it's for making small numbers and makes the scale negative
		BigDecimal multiplier = new BigDecimal(BigInteger.ONE, precision); 
		return amount.multiply(multiplier).toBigInteger();
	}
	
	
	
	/**
	 * <p>Given a textual message, returns a byte buffer formatted as follows:</p>
	 *
	 * <tt><p>[24] "Bitcoin Signed Message:\n" [message.length as a varint] message</p></tt>
	 * 
	 */
	public byte[] formatMessageForSigning(String message) {
		try {
			//build the magic value for messaging from the coin title
			String magicValue;
			if(this.name.contains(" Test")) {
				//server adds "Test" or "Testnet" to the coin title, strip that for magic message
				magicValue = this.name.substring(0, this.name.indexOf(" Test")) + " Signed Message:\n";
			}
			else {
				magicValue = this.name + " Signed Message:\n";
			}
			
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] magicBytes = magicValue.getBytes(Charsets.UTF_8);
			bos.write(magicBytes.length);
			bos.write(magicBytes);
			byte[] messageBytes = message.getBytes(Charsets.UTF_8);
			ZWVarInt size = new ZWVarInt(messageBytes.length);
			bos.write(size.encode());
			bos.write(messageBytes);
			return bos.toByteArray();
		} 
		catch (IOException e) {
			ZLog.log("Exception trying to format message for signing", e);
		}
		
		return new byte[0];
	}

	
	
	
	
}