package com.ziftr.android.ziftrwallet.tests;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWHdChildNumber;
import com.ziftr.android.ziftrwallet.crypto.ZWHdWalletException;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.util.CryptoUtils;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class HdWalletTests extends TestCase {

	public static final byte[] MASTER_SEED = ZiftrUtils.hexStringToBytes("000102030405060708090a0b0c0d0e0f");
	public static final String X_PUB_M = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8";
	public static final String X_PRV_m = "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi";

	public HdWalletTests() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	private void testHdChildNumber(String cn, boolean hardened, boolean expectToThrow) {
		boolean threw = false;
		ZWHdChildNumber c = null;
		try {
			c = new ZWHdChildNumber(cn);
			assertEquals(cn, c.toString());
			assertTrue(hardened == c.isHardened());
		} catch (ZWHdWalletException e) {
			threw = true;
		}

		if (expectToThrow != threw) {
			throw new RuntimeException("Test failed: " + cn + " " + hardened + " " + expectToThrow);
		}
	}

	@Test
	public void testHdChildNumbers() {
		// 2147483647 (2^31-1) should succeed as a child number, but
		// 2147483648 (2^31) should fail because it is out of range

		testHdChildNumber("0'", true, false);
		testHdChildNumber("1'", true, false);
		testHdChildNumber("2147483647'", true, false);

		testHdChildNumber("0", false, false);
		testHdChildNumber("1", false, false);
		testHdChildNumber("2147483647", false, false);

		testHdChildNumber("-1", false, true);
		testHdChildNumber("-1'", true, true);
		testHdChildNumber("2147483648'", true, true);
	}

	@Test
	public void testPubSerialization() throws ZWAddressFormatException { 
		ZWExtendedPublicKey xpubkey = new ZWExtendedPublicKey(X_PUB_M);
		assertEquals(X_PUB_M, xpubkey.xpub(false));
	}

	@Test
	public void testPrvSerialization() throws ZWAddressFormatException { 
		ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey(X_PRV_m);
		assertEquals(X_PRV_m, xprvkey.xprv(false));
	}

	@Test
	public void testSeed() throws ZWAddressFormatException {
		ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey(MASTER_SEED);
		assertEquals(X_PRV_m, xprvkey.xprv(false));
		assertFalse(xprvkey.hasCalculatedPublicKey());
	}

	@Test
	public void testExtPubFromExtPrv() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey(X_PRV_m);
		assertEquals(X_PRV_m, m.xprv(false));
		ZWExtendedPublicKey M = (ZWExtendedPublicKey) m.getPub();
		assertEquals(X_PUB_M, M.xpub(false));
	}

	@Test
	public void testPathUtils() throws ZWAddressFormatException {
		List<ZWHdChildNumber> expect = new ArrayList<ZWHdChildNumber>();
		expect.add(new ZWHdChildNumber(0, true));
		expect.add(new ZWHdChildNumber(1, false));
		expect.add(new ZWHdChildNumber(2, true));
		expect.add(new ZWHdChildNumber(2, false));
		expect.add(new ZWHdChildNumber(1000000000, false));

		String createdPath = CryptoUtils.createPath(expect, false);
		String expectedPath = "m/0'/1/2'/2/1000000000";
		assertEquals(createdPath, expectedPath);

		List<ZWHdChildNumber> parsed = CryptoUtils.parsePath(expectedPath);
		assertTrue(expect.size() == parsed.size());
		for (int i = 0; i < expect.size(); i++) {
			ZWHdChildNumber c1 = parsed.get(i);
			ZWHdChildNumber c2 = expect.get(i);
			assertTrue(c1.equals(c2));
		}
	}

	@Test(timeout=5000)
	public void testPrvDerivation() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey(X_PRV_m);
		ZWExtendedPrivateKey m_0h_1_2h_2_1000000000 = m.deriveChild("m/0'/1/2'/2/1000000000");
		String xprv = "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76";
		assertEquals(xprv, m_0h_1_2h_2_1000000000.xprv(false));
	}

	@Test(timeout=5000)
	public void testPubDerivation() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey(X_PRV_m);
		ZWExtendedPrivateKey m_child = m.deriveChild("m/0'/1/2'");
		ZWExtendedPrivateKey m_child_child = m_child.deriveChild("m/2/1000000000");

		ZWExtendedPublicKey M_child_child_1 = (ZWExtendedPublicKey) m_child_child.calculatePublicKey(true); 
		ZWExtendedPublicKey M_child = (ZWExtendedPublicKey) m_child.calculatePublicKey(true);
		ZWExtendedPublicKey M_child_child_2 = M_child.deriveChild("M/2/1000000000");

		assertEquals(M_child_child_1, M_child_child_2);
	}

	@Test
	public void testPubDerivationThrows() throws ZWAddressFormatException {
		ZWExtendedPublicKey M = new ZWExtendedPublicKey(X_PUB_M);
		boolean threw = false;
		try {
			M.deriveChild(new ZWHdChildNumber(0, true));
		} catch(ZWHdWalletException hdwe) {
			threw = true;
		}

		if (!threw) throw new RuntimeException("Public keys should not be able to derive hardened keys!");

		threw = false;
		try {
			M.deriveChild("M/0'");
		} catch(ZWHdWalletException hdwe) {
			threw = true;
		}

		if (!threw) throw new RuntimeException("Public keys should not be able to derive hardened keys!");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

}
