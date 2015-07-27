/* Copyright ( C ) ZiftrCOIN LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium, including being compiled as part of a binary package is strictly prohibited
 *
 * ZiftrWALLET is a trademark of Ziftr, LLC
 */


package com.ziftr.android.ziftrwallet.crypto;

import java.math.BigInteger;

import javax.annotation.Nullable;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.signers.ECDSASigner;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Base64;

import com.google.common.base.Charsets;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;


/**
 * <p>Represents an elliptic curve public point, usable for digital signatures but not encryption.
 * You can check signatures but not create them.</p>
 *
 * <p>Also provides access to *coin-Qt compatible text message signing, as accessible via the UI or JSON-RPC.
 * This is slightly different to signing raw bytes - if you want to sign your own data and it won't be exposed as
 * text to people, you don't want to use this. If in doubt, ask on the bitcoin-dev mailing list.</p>
 *
 * <p>The ECDSA algorithm supports <i>key recovery</i> in which a signature plus a couple of discriminator bits can
 * be reversed to find the public key used to calculate it. This can be convenient when you have a message and a
 * signature and want to find out who signed it, rather than requiring the user to provide the expected identity.</p>
 */
public class ZWPrivateKey {

	/**
	 * The private key is an integer between 1 and a really large integer that depends on the 
	 * Bitcoin Curve, secp256k1. 
	 */
	protected BigInteger priv;

	/**
	 * The public key associated with this private key. Call checkPub() to make sure it is correct,
	 * although know that this is a computationally expensive operation.
	 * The key may not be calculated, as doing so is CPU intensive.
	 *   getPub() will get it and calculate it if it has not already been calculated.
	 *   getRawPub() will get what is stored in this field, without trying to calculate it if it does not already have it.
	 */
	protected ZWPublicKey pub;

	protected void init(BigInteger privKey, ZWPublicKey pubKey) {
		if (privKey == null && pubKey != null) {
			throw new IllegalArgumentException("Cannot request a new private key with a pre-calculated public key");
		}

		if (privKey == null) {
			ECKeyPairGenerator generator = new ECKeyPairGenerator();
			ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ZWCurveParameters.CURVE, ZiftrUtils.createTrulySecureRandom());
			generator.init(keygenParams);
			AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
			ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();

			// TODO check if this (above) actually calculates the public key anyway, if it does
			// may as well store it.
			// Alternatively, generate a private key by hardcoding the order of the curve and choosing
			// a random integer between 1 and the order.

			this.priv = privParams.getD();
		} else {
			if (privKey.equals(BigInteger.ZERO))
				throw new IllegalArgumentException("ZWPrivateKey requires a non-zero private key");
			this.priv = privKey;
		}

		this.pub = pubKey;
	}

	/**
	 * Only use this if the constructor in the subclass needs to explicitly specify 
	 * that no super constructor needs to be used.  
	 */
	protected ZWPrivateKey(boolean doInit) {
		if (doInit) {
			this.init(null, null);
		}
	}

	/**
	 * Generates an entirely new keypair. Point compression is used so the resulting public key will be 33 bytes
	 * (32 for the co-ordinate and 1 byte to represent the y bit).
	 */
	public ZWPrivateKey() {
		this.init(null, null);
	}

	/**
	 * Creates a key given either the private key only, the public key only, or both. If only the private key 
	 * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
	 * the public key already correctly matches the public key. If only the public key is supplied, this key cannot
	 * be used for signing.
	 */
	public ZWPrivateKey(BigInteger privKey, ZWPublicKey pubKey) {
		this.init(privKey, pubKey);
	}

	/** Creates a key given the private key only.  The public key is calculated from it (this is slow) */
	public ZWPrivateKey(BigInteger privKey) {
		this.init(privKey, null);
	}

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean hasValidPrivateKey() {
		return this.priv != null && this.priv.compareTo(BigInteger.ZERO) > 0;
	}

	public boolean hasCalculatedPublicKey() {
		return this.getRawPub() != null;
	}

	public void checkPubValidity() throws ZWInvalidPublicKeyException {
		boolean valid = false;
		if (this.getRawPub() != null) {
			ZWPublicKey shouldBe = this.calculatePublicKey(this.getRawPub().isCompressed());
			valid = shouldBe.equals(this.getRawPub());
		}

		if (!valid) {
			throw new ZWInvalidPublicKeyException("The public key given with this private key is not the correct public key.");
		}
	}

	/**
	 * CPU costly operation!
	 */
	public ZWPublicKey calculatePublicKey(boolean compressed) {
		ECPoint point = ZWCurveParameters.CURVE.getG().multiply(this.priv);
		return new ZWPublicKey(point.getEncoded(compressed));
	}

	public void clearPriv() {
		this.priv = null;
	}

	public void clear() {
		this.clearPriv();
		this.pub = null;
	}

	/**
	 * converts the string to a {@link ZWSha256Hash} then signs it and returns the R and S components as BigIntegers. 
	 * In the Bitcoin protocol, they are usually encoded using DER format, so you may want ECKey.ECDSASignature#encodeToDER()
	 * instead. However sometimes the independent components can be useful, for instance, if you're doing to do further
	 * EC math using the R and S components.
	 *
	 * @param hashString a string representation of a hash to be signed
	 * @throws ZWKeyCrypterException if this ECKey doesn't have a private part.
	 */
	public ZWECDSASignature sign(String hashString) throws ZWKeyCrypterException {
		ZWSha256Hash hash = new ZWSha256Hash(hashString);
		return this.sign(hash);
	}

	/**
	 * Signs the given hash and returns the R and S components as BigIntegers. In the Bitcoin protocol, they are
	 * usually encoded using DER format, so you want ECKey.ECDSASignature#encodeToDER()
	 * instead. However sometimes the independent components can be useful, for instance, if you're doing to do further
	 * EC math using the R and S components.
	 *
	 * @param input a {@link ZWSha256Hash} to be signed
	 * @throws ZWKeyCrypterException if this ECKey doesn't have a private part.
	 */
	public ZWECDSASignature sign(ZWSha256Hash input) throws ZWKeyCrypterException {

		// No decryption of private key required.
		if (this.priv == null) {
			throw new ZWKeyCrypterException("This Private Key does not have the private key necessary for signing.");
		}

		ECDSASigner signer = new ECDSASigner();
		ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(this.priv, ZWCurveParameters.CURVE);
		signer.init(true, privKey);
		BigInteger[] components = signer.generateSignature(input.getBytes());
		final ZWECDSASignature signature = new ZWECDSASignature(components[0], components[1]);
		signature.ensureCanonical();
		return signature;
	}

	/**
	 * Signs a text message using the standard Bitcoin messaging signing format and returns the signature as a base64
	 * encoded string.
	 *
	 * @throws IllegalStateException if this ECKey does not have the private part.
	 * @throws ZWKeyCrypterException if this ECKey is encrypted and no AESKey is provided or it does not decrypt the ECKey.
	 */
	public String signMessage(ZWCoin coin, String message) throws ZWKeyCrypterException {
		if (priv == null) {
			throw new IllegalStateException("This key does not have the private key necessary for signing.");
		}
		byte[] data = coin.formatMessageForSigning(message);
		ZWSha256Hash hash = ZWSha256Hash.createDouble(data);
		ZWECDSASignature sig = sign(hash);
		// Now we have to work backwards to figure out the recId needed to recover the signature.
		// TODO is this necessary for what we are doing?
		int recId = -1;
		for (int i = 0; i < 4; i++) {
			ZWPublicKey k = ZWPublicKey.recoverFromSignature(i, sig, hash, this.getPub().isCompressed());
			if (this.getPub().equals(k)) {
				recId = i;
				break;
			}
		}
		if (recId == -1) {
			ZLog.log("Could not construct a recoverable key. This should never happen.");
			return null;
		}
		int headerByte = recId + 27 + (this.getPub().isCompressed() ? 4 : 0);
		byte[] sigData = new byte[65];  // 1 header + 32 bytes for R + 32 bytes for S
		sigData[0] = (byte)headerByte;
		System.arraycopy(ZiftrUtils.bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32);
		System.arraycopy(ZiftrUtils.bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32);
		return new String(Base64.encode(sigData), Charsets.UTF_8);
	}

	/**
	 * @return the pub
	 */
	public ZWPublicKey getRawPub() {
		return this.pub;
	}

	public ZWPublicKey getPub() {
		if (!this.hasCalculatedPublicKey()) {
			this.pub = this.calculatePublicKey(true);
		}
		return this.getRawPub();
	}

	/**
	 * Returns a 32 byte array containing the private key, or null if the key is encrypted or public only
	 */
	@Nullable
	public byte[] getPrivKeyBytes() {
		return ZiftrUtils.bigIntegerToBytes(priv, 32);
	}

	/**
	 * Returns a 32 byte array containing the private key, or null if the key is encrypted or public only
	 * Adds an extra 0x01 on the end if this is a compressed key
	 */
	@Nullable
	public byte[] getPrivKeyBytesForAddressEncoding() {
		byte[] privBytes = ZiftrUtils.bigIntegerToBytes(priv, 32);
		if (!this.hasCalculatedPublicKey() || this.getPub().isCompressed()) {
			byte[] privBytesForAddressEncoding = new byte[33];
			System.arraycopy(privBytes, 0, privBytesForAddressEncoding, 0, 32);
			privBytesForAddressEncoding[privBytesForAddressEncoding.length - 1] = (byte) 0x01; 
			return privBytesForAddressEncoding;
		} else {
			return privBytes;
		}
	}

	/**
	 * Create an encrypted private key with the keyCrypter and the AES key supplied.
	 * This method returns a new encrypted key and leaves the original unchanged.
	 * To be secure you need to clear the original, unencrypted private key bytes.
	 *
	 * @param keyCrypter The keyCrypter that specifies exactly how the encrypted bytes are created.
	 * @param aesKey The KeyParameter with the AES encryption key (usually constructed with keyCrypter#deriveKey and cached as it is slow to create).
	 * @return encryptedKey
	 */
	public ZWEncryptedData encrypt(ZWKeyCrypter keyCrypter) throws ZWKeyCrypterException {
		if (keyCrypter == null) {
			throw new ZWKeyCrypterException("Cannot encrypt this key using a null keyCrypter");
		}

		ZWEncryptedData encryptedPrivateKey = keyCrypter.encrypt(this.getPrivHex());
		return encryptedPrivateKey;
	}

	/**
	 * Create a decrypted private key with the keyCrypter and AES key supplied. Note that if the aesKey is wrong, this
	 * has some chance of throwing KeyCrypterException due to the corrupted padding that will result, but it can also
	 * just yield a garbage key.
	 *
	 * @param keyCrypter The keyCrypter that specifies exactly how the decrypted bytes are created.
	 * @param aesKey The KeyParameter with the AES encryption key (usually constructed with keyCrypter#deriveKey and cached).
	 * @return unencryptedKey
	 */
	public static ZWPrivateKey decrypt(ZWEncryptedData data, ZWKeyCrypter crypter) throws ZWKeyCrypterException {
		if (data == null) {
			throw new ZWKeyCrypterException("There is no encrypted key to decrypt .");
		}

		byte[] unencryptedPrivateKey = crypter.decryptToBytes(data);
		ZWPrivateKey key = new ZWPrivateKey(new BigInteger(1, unencryptedPrivateKey));
		return key;
	}

	/*
	 * Check that it is possible to decrypt the key with the keyCrypter and that the original key is returned.
	 *
	 * Because it is a critical failure if the private keys cannot be decrypted successfully (resulting of loss of all bitcoins controlled
	 * by the private key) you can use this method to check when you *encrypt* a wallet that it can definitely be decrypted successfully.
	 * See {link Wallet#encrypt(KeyCrypter keyCrypter, KeyParameter aesKey)} for example usage.
	 *
	 * @return true if the encrypted key can be decrypted back to the original key successfully.
	 *
	public static boolean encryptionIsReversible(ZWPrivateKey originalKey, ZWPrivateKey encryptedKey) {
		String genericErrorText = "The check that encryption could be reversed failed for key " + originalKey.toString() + ". ";
		try {
			ZWPrivateKey rebornUnencryptedKey = encryptedKey.decrypt();
			if (rebornUnencryptedKey == null) {
				ZLog.log(genericErrorText + "The test decrypted key was missing.");
				return false;
			}

			byte[] originalPrivateKeyBytes = originalKey.getPrivKeyBytes();
			if (originalPrivateKeyBytes != null) {
				if (rebornUnencryptedKey.getPrivKeyBytes() == null) {
					ZLog.log(genericErrorText + "The test decrypted key was missing.");
					return false;
				} else {
					if (originalPrivateKeyBytes.length != rebornUnencryptedKey.getPrivKeyBytes().length) {
						ZLog.log(genericErrorText + "The test decrypted private key was a different length to the original.");
						return false;
					} else {
						for (int i = 0; i < originalPrivateKeyBytes.length; i++) {
							if (originalPrivateKeyBytes[i] != rebornUnencryptedKey.getPrivKeyBytes()[i]) {
								ZLog.log(genericErrorText + "Byte " + i + " of the private key did not match the original.");
								return false;
							}
						}
					}
				}
			}
		} catch (ZWKeyCrypterException kce) {
			ZLog.log(kce.getMessage());
			return false;
		}

		// Key can successfully be decrypted.
		return true;
	}*/
	
	public String getPrivHex() {
		final byte[] privKeyBytes = this.getPrivKeyBytes();
		if (privKeyBytes == null) {
			throw new ZWKeyCrypterException("Private key is not available");
		}
		return ZiftrUtils.bytesToHexString(privKeyBytes);
	}

	@Override
	public String toString() {
		return getPrivHex();
	}

	//	/**
	//	 * Construct an ECKey from an ASN.1 encoded private key. These are produced by OpenSSL and stored by the Bitcoin
	//	 * reference implementation in its wallet. Note that this is slow because it requires an EC point multiply.
	//	 */
	//	public static ZWPrivateKey fromASN1(byte[] asn1privkey) {
	//		return new ZWPrivateKey(extractPrivateKeyFromASN1(asn1privkey));
	//	}
	//	private static BigInteger extractPrivateKeyFromASN1(byte[] asn1privkey) {
	//		// To understand this code, see the definition of the ASN.1 format for EC private keys in the OpenSSL source
	//		// code in ec_asn1.c:
	//		//
	//		// ASN1_SEQUENCE(EC_PRIVATEKEY) = {
	//		//   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
	//		//   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
	//		//   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
	//		//   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
	//		// } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
	//		//
	//		try {
	//			ASN1InputStream decoder = new ASN1InputStream(asn1privkey);
	//			DLSequence seq = (DLSequence) decoder.readObject();
	//			checkArgument(seq.size() == 4, "Input does not appear to be an ASN.1 OpenSSL EC private key");
	//			checkArgument(((ASN1Integer) seq.getObjectAt(0)).getValue().equals(BigInteger.ONE),
	//					"Input is of wrong version");
	//			Object obj = seq.getObjectAt(1);
	//			byte[] bits = ((ASN1OctetString) obj).getOctets();
	//			decoder.close();
	//			return new BigInteger(1, bits);
	//		} catch (IOException e) {
	//			throw new RuntimeException(e);  // Cannot happen, reading from memory stream.
	//		}
	//	}
	//	/**
	//	 * Output this ECKey as an ASN.1 encoded private key, as understood by OpenSSL or used by the BitCoin reference
	//	 * implementation in its wallet storage format.
	//	 * 
	//	 * TODO if we use this, change the version bytes
	//	 */
	//	public byte[] toASN1() {
	//		try {
	//			ByteArrayOutputStream baos = new ByteArrayOutputStream(400);
	//
	//			// ASN1_SEQUENCE(EC_PRIVATEKEY) = {
	//			//   ASN1_SIMPLE(EC_PRIVATEKEY, version, LONG),
	//			//   ASN1_SIMPLE(EC_PRIVATEKEY, privateKey, ASN1_OCTET_STRING),
	//			//   ASN1_EXP_OPT(EC_PRIVATEKEY, parameters, ECPKPARAMETERS, 0),
	//			//   ASN1_EXP_OPT(EC_PRIVATEKEY, publicKey, ASN1_BIT_STRING, 1)
	//			// } ASN1_SEQUENCE_END(EC_PRIVATEKEY)
	//			DERSequenceGenerator seq = new DERSequenceGenerator(baos);
	//			seq.addObject(new ASN1Integer(1)); // version
	//			seq.addObject(new DEROctetString(priv.toByteArray()));
	//			seq.addObject(new DERTaggedObject(0, SECNamedCurves.getByName("secp256k1").toASN1Primitive()));
	//			seq.addObject(new DERTaggedObject(1, new DERBitString(getPubKey())));
	//			seq.close();
	//			return baos.toByteArray();
	//		} catch (IOException e) {
	//			throw new RuntimeException(e);  // Cannot happen, writing to memory stream
	//		}
	//	}

}