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


// TODO: This class is quite a mess by now. Once users are migrated away from Java serialization for the wallets,
// refactor this to have better internal layout and a more consistent API.

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
	 * The encrypted private key information.
	 */
	protected ZWEncryptedData encryptedPrivateKey;

	/**
	 * Instance of the KeyCrypter interface to use for encrypting and decrypting the key.
	 * Used to be transient, but we are not serializing so no need for this.
	 */
	protected ZWKeyCrypter keyCrypter;

	/**
	 * The public key associated with this private key. Call checkPub() to make sure it is correct,
	 * although know that this is a computationally expensive operation.
	 */
	protected ZWPublicKey pub;
	
	/**
	 * Generates an entirely new keypair. Point compression is used so the resulting public key will be 33 bytes
	 * (32 for the co-ordinate and 1 byte to represent the y bit).
	 */
	public ZWPrivateKey() {
		ECKeyPairGenerator generator = new ECKeyPairGenerator();
		ECKeyGenerationParameters keygenParams = new ECKeyGenerationParameters(ZWCurveParameters.CURVE, ZWCurveParameters.getSecureRandom());
		generator.init(keygenParams);
		AsymmetricCipherKeyPair keypair = generator.generateKeyPair();
		ECPrivateKeyParameters privParams = (ECPrivateKeyParameters) keypair.getPrivate();
		
		// TODO check if this (above) actually calculates the public key anyway, if it does
		// may as well store it.
		// Alternatively, generate a private key by hardcoding the order of the curve and choosing
		// a random integer between 1 and the order.

		this.priv = privParams.getD();
	}

	public ZWPrivateKey(BigInteger privKey, boolean compressed) {
		if (privKey == null || privKey.equals(BigInteger.ZERO))
			throw new IllegalArgumentException("ZWPrivateKey requires a non-zero private key");
		this.priv = privKey;
	}

	/**
	 * Creates an ECKey given either the private key only, the public key only, or both. If only the private key 
	 * is supplied, the public key will be calculated from it (this is slow). If both are supplied, it's assumed
	 * the public key already correctly matches the public key. If only the public key is supplied, this ECKey cannot
	 * be used for signing.
	 */
	public ZWPrivateKey(BigInteger privKey, ZWPublicKey pubKey) {
		if (privKey == null || privKey.equals(BigInteger.ZERO))
			throw new IllegalArgumentException("ZWPrivateKey requires a non-zero private key");
		this.priv = privKey;
		this.initPub(pubKey);
	}

	/** Creates an ECKey given the private key only.  The public key is calculated from it (this is slow) */
	public ZWPrivateKey(BigInteger privKey) {
		this(privKey, (ZWPublicKey)null);
	}

	/**
	 * Create a new ECKey with an encrypted private key, a public key and a KeyCrypter.
	 *
	 * @param encryptedPrivateKey The private key, encrypted,
	 * @param pubKey The keys public key
	 * @param keyCrypter The KeyCrypter that will be used, with an AES key, to encrypt and decrypt the private key
	 */
	public ZWPrivateKey(ZWEncryptedData encryptedPrivateKey, ZWPublicKey pubKey, ZWKeyCrypter keyCrypter) {
		this.initPub(pubKey);
		this.keyCrypter = keyCrypter;
		this.encryptedPrivateKey = encryptedPrivateKey;
	}
	
	protected void initPub(ZWPublicKey pubKey) {
		this.pub = null;
		if (pubKey != null) {
			this.pub = new ZWPublicKey(pubKey.getPubKeyBytes());
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean hasValidPrivateKey() {
		return this.priv != null && this.priv.compareTo(BigInteger.ZERO) > 0;
	}

	public boolean hasCalculatedPublicKey() {
		return this.pub != null;
	}

	public void ensureHasPublicKey() {
		if (this.hasCalculatedPublicKey()) {
			return;
		} else if (this.isEncrypted()) {
			throw new ZWInvalidPublicKeyException("Encrypted priv needs pub but was not constructed with it. "); 
		} else {
			this.pub = this.calculatePublicKey(true);
		}
	}

	public void checkPubValidity() throws ZWInvalidPublicKeyException {
		boolean valid = false;
		if (this.pub != null) {
			boolean compressed = this.pub != null ? this.pub.isCompressed() : false;
			ZWPublicKey shouldBe = this.calculatePublicKey(compressed);
			valid = shouldBe.equals(this.pub); 
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
		if (this.encryptedPrivateKey != null) {
			this.encryptedPrivateKey.clear();
		}
		this.keyCrypter = null;
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

		// The private key bytes to use for signing.
		BigInteger privateKeyForSigning;

		if (isEncrypted()) {
			// The private key needs decrypting before use.
			if (this.keyCrypter == null) {
				throw new ZWKeyCrypterException("There is no keyCrypter to decrypt the private key for signing.");
			}

			privateKeyForSigning = new BigInteger(1, this.keyCrypter.decryptToBytes(encryptedPrivateKey));

			// Check encryption was correct. TODO maybe enable this check? Seems unnecessary. 
			// if (!Arrays.equals(pub, publicKeyFromPrivate(privateKeyForSigning, isCompressed())))
			//	throw new ZWKeyCrypterException("Could not decrypt bytes");
		} else {
			// No decryption of private key required.
			if (priv == null) {
				throw new ZWKeyCrypterException("This ECKey does not have the private key necessary for signing.");
			} else {
				privateKeyForSigning = priv;
			}
		}

		ECDSASigner signer = new ECDSASigner();
		ECPrivateKeyParameters privKey = new ECPrivateKeyParameters(privateKeyForSigning, ZWCurveParameters.CURVE);
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
			throw new IllegalStateException("This ECKey does not have the private key necessary for signing.");
		}
		this.ensureHasPublicKey();
		byte[] data = coin.formatMessageForSigning(message);
		ZWSha256Hash hash = ZWSha256Hash.createDouble(data);
		ZWECDSASignature sig = sign(hash);
		// Now we have to work backwards to figure out the recId needed to recover the signature.
		// TODO is this necessary for what we are doing?
		int recId = -1;
		for (int i = 0; i < 4; i++) {
			ZWPublicKey k = ZWPublicKey.recoverFromSignature(i, sig, hash, this.pub.isCompressed());
			if (this.pub.equals(k)) {
				recId = i;
				break;
			}
		}
		if (recId == -1) {
			ZLog.log("Could not construct a recoverable key. This should never happen.");
			return null;
		}
		int headerByte = recId + 27 + (this.pub.isCompressed() ? 4 : 0);
		byte[] sigData = new byte[65];  // 1 header + 32 bytes for R + 32 bytes for S
		sigData[0] = (byte)headerByte;
		System.arraycopy(ZiftrUtils.bigIntegerToBytes(sig.r, 32), 0, sigData, 1, 32);
		System.arraycopy(ZiftrUtils.bigIntegerToBytes(sig.s, 32), 0, sigData, 33, 32);
		return new String(Base64.encode(sigData), Charsets.UTF_8);
	}

	/**
	 * @return the pub
	 */
	public ZWPublicKey getPub() {
		return pub;
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
		if (this.pub.isCompressed()) {
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
	public ZWPrivateKey encrypt(ZWKeyCrypter keyCrypter) throws ZWKeyCrypterException {
		// Will throw an error if cannot decrypt using the key crypter field
		ZWPrivateKey decrypted = this;
		if (this.isEncrypted()) {
			decrypted = this.decrypt();
		}

		if (keyCrypter == null) {
			throw new ZWKeyCrypterException("Cannot encrypt this key using a null keyCrypter");
		}

		final byte[] privKeyBytes = decrypted.getPrivKeyBytes();

		if (privKeyBytes == null) {
			throw new ZWKeyCrypterException("Private key is not available");
		}

		ZWEncryptedData encryptedPrivateKey = keyCrypter.encrypt(ZiftrUtils.bytesToHexString(privKeyBytes));
		ZWPrivateKey result = new ZWPrivateKey(encryptedPrivateKey, this.pub, keyCrypter);
		return result;
	}

	public ZWPrivateKey encrypt() throws ZWKeyCrypterException {
		return this.encrypt(getKeyCrypter());
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
	public ZWPrivateKey decrypt() throws ZWKeyCrypterException {
		// Check that the keyCrypter matches the one used to encrypt the keys, if set.
		if (!this.isEncrypted()) {
			//throw new ZWKeyCrypterException("This ZWPrivateKey is not encrypted.");
			return this;
		}
		
		if (this.keyCrypter == null) {
			throw new ZWKeyCrypterException("There is no key crypter set to decrypt.");
		} else if (this.encryptedPrivateKey == null) {
			throw new ZWKeyCrypterException("There is no encrypted key to decrypt .");
		}
		
		byte[] unencryptedPrivateKey = this.keyCrypter.decryptToBytes(this.encryptedPrivateKey); 
		ZWPrivateKey key = new ZWPrivateKey(new BigInteger(1, unencryptedPrivateKey));
		return key;

		//		These extra checks are too slow, and essentially never fail		
		//		this.ensureHasPublicKey();
		//		if (!this.pub.equals(key.pub))
		//			throw new ZWKeyCrypterException("Provided key crypter is wrong");
	}

	/**
	 * Check that it is possible to decrypt the key with the keyCrypter and that the original key is returned.
	 *
	 * Because it is a critical failure if the private keys cannot be decrypted successfully (resulting of loss of all bitcoins controlled
	 * by the private key) you can use this method to check when you *encrypt* a wallet that it can definitely be decrypted successfully.
	 * See {link Wallet#encrypt(KeyCrypter keyCrypter, KeyParameter aesKey)} for example usage.
	 *
	 * @return true if the encrypted key can be decrypted back to the original key successfully.
	 */
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
	}

	/**
	 * Indicates whether the private key is encrypted (true) or not (false).
	 * A private key is deemed to be encrypted when there is both a KeyCrypter and the encryptedPrivateKey is non-zero.
	 */
	public boolean isEncrypted() {
		return encryptedPrivateKey != null && 
				encryptedPrivateKey.getEncryptedData() != null &&  !encryptedPrivateKey.getEncryptedData().isEmpty();
	}

	/**
	 * @return The encryptedPrivateKey (containing the encrypted private key bytes and initialisation vector) for this ECKey,
	 *         or null if the ECKey is not encrypted.
	 */
	@Nullable
	public ZWEncryptedData getEncryptedPrivateKey() {
		if (encryptedPrivateKey == null) {
			return null;
		} else {
			return encryptedPrivateKey.clone();
		}
	}

	/**
	 * You need this to decrypt the ECKey.
	 * 
	 * @return The SecretKey that was used to encrypt to encrypt this ECKey. 
	 */
	public ZWKeyCrypter getKeyCrypter() {
		return this.keyCrypter;
	}

	public void setKeyCrypter(ZWKeyCrypter keyCrypter) {
		this.keyCrypter = keyCrypter;
		if (this.keyCrypter != null) {
			ZWPrivateKey encrypted = this.encrypt();
			this.encryptedPrivateKey = encrypted.getEncryptedPrivateKey();
		}
	}

	@Override
	public String toString() {
		return "prv: " + ZiftrUtils.bytesToHexString(ZiftrUtils.bigIntegerToBytes(this.priv, 32));
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