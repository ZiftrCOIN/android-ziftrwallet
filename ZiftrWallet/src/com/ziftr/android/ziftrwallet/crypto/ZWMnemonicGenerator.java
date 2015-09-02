package com.ziftr.android.ziftrwallet.crypto;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;

import org.spongycastle.crypto.digests.SHA512Digest;
import org.spongycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWApplication;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZWCryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ZWMnemonicGenerator {

public static synchronized String[] createHdWalletMnemonic(byte[] entropy) {
		
		int entropySize = Byte.SIZE * entropy.length; //bits in each byte * number of bytes
		
		if(entropySize < 128 || entropySize % 32 != 0) {
			//TODO error, must be at least 128 bits of entropy and must be a multiple of 32
		}
		
		byte[] sha256hash = ZWCryptoUtils.Sha256Hash(entropy);
		int checksumSize = entropySize / 32;
		
		boolean[] checksumBits = ZiftrUtils.bytesToBits(sha256hash, true);
		boolean[] mnemonicBits = ZiftrUtils.bytesToBits(entropy, true);
		
		
		mnemonicBits = Arrays.copyOf(mnemonicBits, entropySize + checksumSize);
		
		//append the first bits of the checksum to the end of the original entropy, per bip-39
		for(int x = 0; x < checksumSize; x++) {
			mnemonicBits[entropySize + x] = checksumBits[x];
		}
		
		
		//create an array of indexes into the word list
		int[] mnemonicWordIndexes = new int[(entropySize + checksumSize) / 11]; 
		
		for(int x = 0; x < mnemonicWordIndexes.length; x++) {
			int startBitIndex = x * 11;
			int endBitIndex = startBitIndex + 11;
			
			boolean[] wordIndexBits = Arrays.copyOfRange(mnemonicBits, startBitIndex, endBitIndex);
			mnemonicWordIndexes[x] = ZiftrUtils.bitsToInt(wordIndexBits, true);
		}
		
		ArrayList<String> wordList = getWordList();
		
		String[] mnemonicSentence = new String[mnemonicWordIndexes.length];
		for(int x = 0; x < mnemonicSentence.length; x++) {
			mnemonicSentence[x] = wordList.get(mnemonicWordIndexes[x]);
		}
		
		//note: make sure not to log this on release
		//ZLog.log("Generated mnemonic sentence: ", Arrays.toString(mnemonicSentence));
		
		return mnemonicSentence;
	}
	
	
	
	public static boolean passesChecksum(String mnemonic) {
		
		//first normalize the string per bip39
		String mnemonicString = Normalizer.normalize(mnemonic, Normalizer.Form.NFKD);
		
		//now break the sentence up into an array of Strings
		String[] mnemonicSentence = mnemonicString.split(" ");
		return passesChecksum(mnemonicSentence);
	}
	
	public static boolean passesChecksum(String[] mnemonic) {
		
		//reverse lookup on the word list to get integers for each word
		
		boolean[] mnemonicBits = new boolean[mnemonic.length * 11];
		int bitIndex = 0; 
		
		ArrayList<String> wordList = getWordList();
		
		for(String word : mnemonic) {
			int wordIndex = wordList.indexOf(word);
			if(wordIndex < 0) {
				return false; //using a word not even in the word list, so doesn't pass checksum
			}
			
			boolean[] bits = ZiftrUtils.integerToBits(wordIndex, true);
			
			//it's most significant bit first, but we want the least significant 11 bits of the integer, 
			//ordered by the most significant of those 11 bits
			for(int x = bits.length - 11; x < bits.length; x++) {
				//we only use the first 11 bits of the integer per BIP39
				mnemonicBits[bitIndex++] = bits[x];
			}
		}
		
		//per BIP39
		//checksumSize = entropySize /32
		//mnemonicBits.length = checksumSize + entropySize
		//entropySize = mnemonicBits - checksumSize;
		//checksumSize = (mnemonicBits - checksumSize) / 32
		//checksumSize * 32 = mnemonicBits - checksumSize
		//checksumSize = mnemonicBits.length / 33
		
		int checksumSize = mnemonicBits.length / 33;
		boolean[] checksumBits = Arrays.copyOfRange(mnemonicBits, mnemonicBits.length-checksumSize, mnemonicBits.length);
		
		boolean[] originalMnemonicBits = Arrays.copyOfRange(mnemonicBits, 0, mnemonicBits.length-checksumSize);
		
		byte[] originalEntroy = ZiftrUtils.bitsToBytes(originalMnemonicBits, true);
		byte[] sha256hash = ZWCryptoUtils.Sha256Hash(originalEntroy);
		
		boolean[] checksumHashBits = ZiftrUtils.bytesToBits(sha256hash, true);
		
		//only the first few bits of the hash (depending on checksum size) are used for checksum
		boolean[] originalChecksumBits = Arrays.copyOfRange(checksumHashBits, 0, checksumSize); 
		
		boolean passedChecksum = Arrays.equals(checksumBits, originalChecksumBits);
		return passedChecksum;
	}
	
	
	public static ArrayList<String> getWordList() {
		
		ArrayList<String> wordList = new ArrayList<String>(2048);
		
		try {
			InputStream wordlistInputStream = ZWApplication.getApplication().getResources().openRawResource(R.raw.english);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(wordlistInputStream));
			String word = reader.readLine();
			while(word != null) {
				wordList.add(word.trim());
				word = reader.readLine();
			}
			
			reader.close();
		}
		catch(Exception e) {
			ZLog.log("Exception loading bip39 word list: ", e);
		}
		
		
		return wordList;
	}
	
	
	public static byte[] generateHdSeed(String[] mnemonicSentence, String seedPassword) {
		
		if(seedPassword == null) {
			seedPassword = "";
		}
		else {
			//we need to make sure the password is ok for this particular version of Android
			//see: http://android-developers.blogspot.com/2013/12/changes-to-secretkeyfactory-api-in.html
			//best solution is to just not allow users to use non-ascii characters in their password for 4.3 and earlier versions
			//this way there's no issues of possible compatibility with other bip 39 hd wallet implementations
			
			//note: using spongycastle now so we don't need to worry about this
			/**
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
				for(char c : seedPassword.toCharArray()) {
					if((c & 0x7F) != c) {
						return null;
					}
				}
			}
			**/
		}
		
		
		String mnemonicString = Joiner.on(" ").join(mnemonicSentence);
		mnemonicString = Normalizer.normalize(mnemonicString, Normalizer.Form.NFKD);
		
		String salt = "mnemonic" + seedPassword;
		salt = Normalizer.normalize(salt, Normalizer.Form.NFKD);
		

		int keySize = 512;
		int iterations = 2048;
		
		try {
			byte[] mnemonicBytes = mnemonicString.getBytes("UTF-8");
			byte[] saltBytes = salt.getBytes("UTF-8");
			
			
			PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA512Digest());
			generator.init(mnemonicBytes, saltBytes, iterations);
			
			KeyParameter keyParams = (KeyParameter) generator.generateDerivedParameters(keySize);
			byte[] derivedKeyBytes = keyParams.getKey();
			
			return derivedKeyBytes;
		} 
		catch (Exception e) {
			ZLog.log("Excpetion generating HD Seed: ", e);
		}
		
		
		return null;
	}
	
	
}
