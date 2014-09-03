package com.ziftr.android.onewallet.crypto;

/**
 * <p>Exception to provide the following to {@link EncrypterDecrypterOpenSSL}:</p>
 * <ul>
 * <li>Provision of encryption / decryption exception</li>
 * </ul>
 * <p>This base exception acts as a general failure mode not attributable to a specific cause (other than
 * that reported in the exception message). Since this is in English, it may not be worth reporting directly
 * to the user other than as part of a "general failure to parse" response.</p>
 */
public class OWKeyCrypterException extends RuntimeException {
    private static final long serialVersionUID = -4441989608332681377L;

    public OWKeyCrypterException(String s) {
        super(s);
    }

    public OWKeyCrypterException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
