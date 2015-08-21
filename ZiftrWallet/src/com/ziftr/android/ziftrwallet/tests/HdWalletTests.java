package com.ziftr.android.ziftrwallet.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPrivateKey;
import com.ziftr.android.ziftrwallet.crypto.ZWExtendedPublicKey;
import com.ziftr.android.ziftrwallet.crypto.ZWHdChildNumber;
import com.ziftr.android.ziftrwallet.crypto.ZWHdPath;
import com.ziftr.android.ziftrwallet.crypto.ZWHdWalletException;
import com.ziftr.android.ziftrwallet.exceptions.ZWAddressFormatException;
import com.ziftr.android.ziftrwallet.sqlite.ZWWalletManager;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class HdWalletTests extends TestCase {

	public static final byte[] MASTER_SEED = ZiftrUtils.hexStringToBytes("000102030405060708090a0b0c0d0e0f");
	public static final String MASTER_X_PUB = "xpub661MyMwAqRbcFtXgS5sYJABqqG9YLmC4Q1Rdap9gSE8NqtwybGhePY2gZ29ESFjqJoCu1Rupje8YtGqsefD265TMg7usUDFdp6W1EGMcet8";
	public static final String MASTER_X_PRV = "xprv9s21ZrQH143K3QTDL4LXw2F7HEK3wJUD2nW2nRk4stbPy6cq3jPPqjiChkVvvNKmPGJxWUtg6LnF5kejMRNNU3TGtRBeJgk33yuGBxrMPHi";

	public HdWalletTests() {
		super();
	}

	protected void setUp() throws Exception {
		super.setUp();
	}


	private void _testHdChildNumber(String cn, boolean hardened, boolean expectToThrow) {
		boolean threw = false;
		try {
			ZWHdChildNumber c = new ZWHdChildNumber(cn);
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

		_testHdChildNumber("0'", true, false);
		_testHdChildNumber("1'", true, false);
		_testHdChildNumber("2147483647'", true, false);

		_testHdChildNumber("0", false, false);
		_testHdChildNumber("1", false, false);
		_testHdChildNumber("2147483647", false, false);

		_testHdChildNumber("-1", false, true);
		_testHdChildNumber("-1'", true, true);
		_testHdChildNumber("2147483648'", true, true);
	}

	private void _testValidPath(String path, boolean derivedFromPrv, boolean resolvesToPrv) {
		ZWHdPath p = new ZWHdPath(path);
		assertEquals(p.derivedFromPrivateKey(), derivedFromPrv);
		assertEquals(p.derivedFromPublicKey(), !derivedFromPrv);
		assertEquals(p.resolvesToPrivateKey(), resolvesToPrv);
		assertEquals(p.resolvesToPublicKey(), !resolvesToPrv);
		assertEquals(p.toString(), path);
	}

	@Test
	public void testPathsValid() {
		_testValidPath("m", true, true);
		_testValidPath("m/1", true, true);
		_testValidPath("m/1'", true, true);
		_testValidPath("m/2147483647'/0", true, true);
		_testValidPath("m/0'/1/0'/1", true, true);
		_testValidPath("[m/0'/1/0'/1]", true, false);
		_testValidPath("[m/0']/1", true, false);
		_testValidPath("[m]", true, false);
		_testValidPath("M/1/2", false, false);
		_testValidPath("M/1/2147483647", false, false);
	}

	private void _testInvalidPath(String path) {
		boolean threw = false;
		try {
			new ZWHdPath(path);
		} catch (ZWHdWalletException e) {
			threw = true;
		}

		if (!threw) {
			throw new RuntimeException("Test failed: " + path);
		}
	}

	@Test
	public void testPathsInvalid() {
		_testInvalidPath("m'");
		_testInvalidPath("m/1''");
		_testInvalidPath("m/-1");
		_testInvalidPath("m/-1'");
		_testInvalidPath("m/1'/");
		_testInvalidPath("m/1/");
		_testInvalidPath("m/2147483648");
		_testInvalidPath("m/2147483648'");
		_testInvalidPath("M/0'/1/0'/1");
		_testInvalidPath("[m]/0'/1/0'/1]");
		_testInvalidPath("[m/0']/1'");
		_testInvalidPath("[[m]]/1/2");
		_testInvalidPath("[[m]/1]/2");
		_testInvalidPath("M/1/2147483648");
		_testInvalidPath("/1");
		_testInvalidPath("[/1]");
	}

	@Test
	public void testPathDerivations() {
		String pathStr = "m/44'/0'/1'";
		ZWHdPath path = new ZWHdPath(pathStr);
		assertEquals(pathStr, path.toString());
		assertEquals(pathStr + "/1", path.slash(new ZWHdChildNumber("1")).toString());
		assertEquals(pathStr + "/1'", path.slash(new ZWHdChildNumber("1'")).toString());
		assertEquals(pathStr + "/1/2", path.slash(new ZWHdChildNumber("1")).slash(new ZWHdChildNumber("2")).toString());

		assertEquals("[" + pathStr + "]", path.toPublicPath(true).toString());
		assertEquals("[" + pathStr + "]/1", path.toPublicPath(true).slash(new ZWHdChildNumber("1")).toString());
		assertEquals("[" + pathStr + "]/1/2", path.toPublicPath(true).slash(new ZWHdChildNumber("1")).slash(new ZWHdChildNumber("2")).toString());

		pathStr = "m/0/1";
		path = new ZWHdPath(pathStr);
		assertEquals(pathStr, path.toString());
		assertEquals("M/0/1", path.toPublicPath(false).toString());
		assertEquals("M/0/1/2", path.toPublicPath(false).slash(new ZWHdChildNumber("2")).toString());

		assertEquals(pathStr, path.toPublicPath(false).toPrivatePath().toString());
		assertEquals(pathStr, path.toPublicPath(true).toPrivatePath().toString());
	}

	@Test
	public void testBip44Paths() {
		ZWHdPath path = new ZWHdPath("m/44'/0'/1'").toPublicPath(true).slash(new ZWHdChildNumber("2")).slash(new ZWHdChildNumber("3"));
		assertTrue(path.isBip44Path());
		assertEquals(path.getBip44Purpose(), 44);
		assertEquals(path.getBip44CoinType(), 0);
		assertEquals(path.getBip44Account(), 1);
		assertEquals(path.getBip44Change(), 2);
		assertEquals(path.getBip44AddressIndex(), 3);
	}

	@Test
	public void testPathRebase() {
		ZWHdPath path = new ZWHdPath("m/44'/0'/1'").toPublicPath(true).slash(new ZWHdChildNumber("2")).slash(new ZWHdChildNumber("3"));
		assertTrue(path.isBip44Path());
		assertEquals("[m/44'/0'/1']/2/3", path.toString());

		assertEquals("M/2/3", path.rebaseRelativity(new ZWHdPath("[m/44'/0'/1']"), false).toString());
		assertEquals("M/3", path.rebaseRelativity(new ZWHdPath("[m/44'/0'/1']/2"), false).toString());
		assertEquals("[m/0'/1']/2/3", path.rebaseRelativity(new ZWHdPath("m/44'"), false).toString());
		
		path = new ZWHdPath("M/2/3");
		assertEquals("M/2/3", path.toString());
		assertEquals("[m/44'/0'/1']/2/3", path.rebaseRelativity(new ZWHdPath("[m/44'/0'/1']"), true).toString());
	}

	@Test
	public void testPubSerialization() throws ZWAddressFormatException { 
		ZWExtendedPublicKey xpubkey = new ZWExtendedPublicKey("M", MASTER_X_PUB);
		assertEquals(MASTER_X_PUB, xpubkey.xpub(true));
	}

	@Test
	public void testPrvSerialization() throws ZWAddressFormatException { 
		ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey("m", MASTER_X_PRV);
		assertEquals(MASTER_X_PRV, xprvkey.xprv(true));
	}

	@Test
	public void testSeed() throws ZWAddressFormatException {
		ZWExtendedPrivateKey xprvkey = new ZWExtendedPrivateKey(MASTER_SEED);
		assertEquals(MASTER_X_PRV, xprvkey.xprv(true));
		assertFalse(xprvkey.hasCalculatedPublicKey());
	}

	@Test
	public void testExtPubFromExtPrv() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey("m", MASTER_X_PRV);
		assertEquals(MASTER_X_PRV, m.xprv(true));
		ZWExtendedPublicKey M = (ZWExtendedPublicKey) m.getPub();
		assertEquals(MASTER_X_PUB, M.xpub(true));
	}

	@Test
	public void testPathUtils() throws ZWAddressFormatException {
		List<ZWHdChildNumber> expect = new ArrayList<ZWHdChildNumber>();
		expect.add(new ZWHdChildNumber(0, true));
		expect.add(new ZWHdChildNumber(1, false));
		expect.add(new ZWHdChildNumber(2, true));
		expect.add(new ZWHdChildNumber(2, false));
		expect.add(new ZWHdChildNumber(1000000000, false));

		String expectedPath = "m/0'/1/2'/2/1000000000";
		ZWHdPath createdPath = new ZWHdPath(expectedPath); 
		assertEquals(createdPath.toString(), expectedPath);
		assertNull(createdPath.getRelativeToPub());
		assertNotNull(createdPath.getRelativeToPrv());

		List<ZWHdChildNumber> parsed = createdPath.getRelativeToPrv();
		assertTrue(expect.size() == parsed.size());
		for (int i = 0; i < expect.size(); i++) {
			ZWHdChildNumber c1 = parsed.get(i);
			ZWHdChildNumber c2 = expect.get(i);
			assertTrue(c1.equals(c2));
		}
	}

	@Test(timeout=5000)
	public void testPrvDerivation() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey("m", MASTER_X_PRV);
		ZWExtendedPrivateKey m_0h_1_2h_2_1000000000 = (ZWExtendedPrivateKey) m.deriveChild("m/0'/1/2'/2/1000000000");
		String xprv = "xprvA41z7zogVVwxVSgdKUHDy1SKmdb533PjDz7J6N6mV6uS3ze1ai8FHa8kmHScGpWmj4WggLyQjgPie1rFSruoUihUZREPSL39UNdE3BBDu76";
		assertEquals(xprv, m_0h_1_2h_2_1000000000.xprv(true));
	}

	@Test(timeout=5000)
	public void testPubDerivation() throws ZWAddressFormatException {
		ZWExtendedPrivateKey m = new ZWExtendedPrivateKey("m", MASTER_X_PRV);
		ZWExtendedPrivateKey m_child = (ZWExtendedPrivateKey) m.deriveChild("m/0'/1/2'");
		ZWExtendedPrivateKey m_child_child = (ZWExtendedPrivateKey) m_child.deriveChild("m/2/1000000000");

		ZWExtendedPublicKey M_child_child_1 = (ZWExtendedPublicKey) m_child_child.calculatePublicKey(true); 
		ZWExtendedPublicKey M_child = (ZWExtendedPublicKey) m_child.calculatePublicKey(true);
		ZWExtendedPublicKey M_child_child_2 = M_child.deriveChild("M/2/1000000000");

		assertTrue(Arrays.equals(M_child_child_1.serialize(true), M_child_child_2.serialize(true)));
	}
	
	
	@Test
	public void testMnemonicGeneration() {
		
		byte[] testSeedData = new byte[]{0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f, 0x7f};
		
		String[] mnemonicSentence = ZWWalletManager.createHdWalletMnemonic(testSeedData);
		
		//make sure the mnemonic was properly generated from a known data set
		String mnemonicString = Joiner.on(" ").join(mnemonicSentence);
		assertEquals(mnemonicString, "legal winner thank year wave sausage worth useful legal winner thank yellow");
		
		//make sure we can generate a proper seed from the mnemonic and password according to bip39
		byte[] hdSeed = ZWWalletManager.generateHdSeed(mnemonicSentence, "this is a test");
		
		ZWExtendedPrivateKey rootKey = new ZWExtendedPrivateKey(hdSeed);
		String rootKeyString = rootKey.xprv(true);
		
		assertEquals(rootKeyString, "xprv9s21ZrQH143K3Xgy6M7PUxb8TB8ABQ2EPWytNqKDgsRNra9Y8sjrHswsoL7C5vGw5R89EBhcipM32YsfmTeZ5k9NUeAybM3NbJJoLbwDQh1");
		
		//make sure we're properly using the bip39 root key to created bip44 keys
		String path = "m/44'/0'/0'/0"; //private, bip44, bitcoin, hardened, account 0
		ZWExtendedPrivateKey extendedKey = (ZWExtendedPrivateKey) rootKey.deriveChild(path);
		String extendedKeyString = extendedKey.xprv(true);
		
		assertEquals(extendedKeyString, "xprvA1hCyZyZxgqyC1yaZqo93iUNTWr6V7nBnCrEmVZswNDPiqT6JBWG1S6banhSoWVdv5DvEsTYEX8Dsfh5wWtadFkJMsPMZzA3n86v8M1A84R");
	}
	

	@Test
	public void testPubDerivationThrows() throws ZWAddressFormatException {
		ZWExtendedPublicKey M = new ZWExtendedPublicKey("M", MASTER_X_PUB);
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
