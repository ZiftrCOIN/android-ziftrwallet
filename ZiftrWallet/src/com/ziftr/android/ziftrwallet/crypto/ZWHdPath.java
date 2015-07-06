package com.ziftr.android.ziftrwallet.crypto;

import java.util.ArrayList;
import java.util.List;

import com.ziftr.android.ziftrwallet.util.CryptoUtils;

/**
 * An object that stores directions for navigating down a BIP32 HD wallet tree.
 * The path is relative to another key ("m" for private key, or "M" for public key), 
 * which is usually (but not necessarily) the master key. 
 * 
 * These objects can have one of three states:
 *   Relative to pub only:
 *     - M/1/2
 *     - relativeToPrv is null
 *   Relative to prv only:
 *     - m/1'/2
 *     - relativeToPub is null
 *   Relative to pub which is relative to prv:
 *     - [m/1'/2]/3/4
 *     - both relativeToPrv and relativeToPub are null
 */
public class ZWHdPath {

	private List<ZWHdChildNumber> relativeToPrv;
	private List<ZWHdChildNumber> relativeToPub;

	public ZWHdPath(ZWHdPath path) {
		if (path.getRelativeToPrv() != null)
			this.relativeToPrv = new ArrayList<ZWHdChildNumber>(path.relativeToPrv);
		if (path.getRelativeToPub() != null)
			this.relativeToPub = new ArrayList<ZWHdChildNumber>(path.relativeToPub);
		this.checkValidity();
	}

	public ZWHdPath(String path) {

		if (CryptoUtils.isPrivPath(path)) {
			// private path, starts with m
			path = path.replaceFirst("m", "");
			this.relativeToPrv = this.split(path);
			this.relativeToPub = null;
		} else if (CryptoUtils.isPubDerivedFromPrvPath(path)) {
			// public path derived from private, starts with [m
			String main = path.substring(2);
			// -1 makes the empty string at the end included, if a path like [m/1/2]  
			String[] parts = main.split("]", -1);
			CryptoUtils.checkHd(parts.length == 2);
			this.relativeToPrv = this.split(parts[0]);
			this.relativeToPub = this.split(parts[1]);
		} else if (CryptoUtils.isPubDerivedFromPubPath(path)) {
			// public path, starts with M
			path = path.replaceFirst("M", "");
			this.relativeToPrv = null;
			this.relativeToPub = this.split(path);
		} else {
			throw new ZWHdWalletException(path + " is not a valid path!");
		}

		this.checkValidity();
	}

	private void checkValidity() {
		CryptoUtils.checkHd(this.relativeToPrv != null || this.relativeToPub != null);
		CryptoUtils.checkHd(this.resolvesToPrivateKey() != this.resolvesToPublicKey());
		CryptoUtils.checkHd(this.derivedFromPrivateKey() != this.derivedFromPublicKey());
		CryptoUtils.checkHd(!(this.derivedFromPublicKey() && this.resolvesToPrivateKey()));
		if (this.resolvesToPublicKey()) {
			for (ZWHdChildNumber pubChild : this.relativeToPub) {
				CryptoUtils.checkHd(!pubChild.isHardened());
			}
		}
	}

	public boolean derivedFromPrivateKey() {
		return this.relativeToPrv != null;
	}

	public boolean derivedFromPublicKey() {
		return !this.derivedFromPrivateKey();
	}

	public boolean resolvesToPrivateKey() {
		return !this.resolvesToPublicKey() && this.derivedFromPrivateKey();
	}

	public boolean resolvesToPublicKey() {
		return this.relativeToPub != null;
	}

	private List<ZWHdChildNumber> split(String s) {
		List<ZWHdChildNumber> l = new ArrayList<ZWHdChildNumber>();
		if (!s.isEmpty()) {
			if (s.startsWith("/")) {
				s = s.substring(1);
			}
			for (String prvPart : s.split("/", -1)) {
				l.add(new ZWHdChildNumber(prvPart));
			}
		}
		return l;
	}

	private void appendPathSegment(StringBuilder sb, List<ZWHdChildNumber> segment) {
		for (ZWHdChildNumber child : segment) {
			sb.append("/");
			sb.append(child.toString());
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.resolvesToPublicKey()) {
			if (this.derivedFromPrivateKey()) {
				sb.append("[m");
				this.appendPathSegment(sb, this.relativeToPrv);
				sb.append("]");
			} else {
				sb.append("M");
			}
			this.appendPathSegment(sb, this.relativeToPub);
		} else if (this.resolvesToPrivateKey()) {
			sb.append("m");
			this.appendPathSegment(sb, this.relativeToPrv);
		} else {
			throw new ZWHdWalletException("Illegal path -- should not happen");
		}

		return sb.toString();
	}

	/**
	 * @return the relativeToPrv
	 */
	public List<ZWHdChildNumber> getRelativeToPrv() {
		return relativeToPrv;
	}

	/**
	 * @return the relativeToPub
	 */
	public List<ZWHdChildNumber> getRelativeToPub() {
		return relativeToPub;
	}

	// Call with null to shallow clone
	public ZWHdPath slash(ZWHdChildNumber child) {
		ZWHdPath clone = new ZWHdPath(this);
		if (child != null) {
			if (this.resolvesToPublicKey()) {
				CryptoUtils.checkHd(!child.isHardened());
				clone.relativeToPub.add(child);
			} else {
				clone.relativeToPrv.add(child);
			}
		}
		return clone;
	}

	/**
	 * This is for altering a path that resolves to a private key to resolve to a public key.
	 * This can be done in two ways,
	 * 
	 *  - Adding "[]"s around the path. Ex: m/1/2 -> [m/1/2]
	 *  - Replacing m with M. Ex: m/1/2 -> M/1/2
	 *  
	 * The latter case will throw if the resulting path is invalid. 
	 * For example, attempting this will throw: 
	 *   
	 *   m/44'/0'/0' -> M/44'/0'/0'
	 */
	public ZWHdPath toPublicPath(boolean maintainRelation) {
		if (this.resolvesToPublicKey()) {
			throw new ZWHdWalletException("This path already describes the resolution to a public key. ");
		}

		if (maintainRelation) {
			// Adds []s
			ZWHdPath path = new ZWHdPath(this);
			path.relativeToPub = new ArrayList<ZWHdChildNumber>();
			return path;
		} else {
			// Replaces m->M, may throw
			return new ZWHdPath(this.toString().replace("m", "M"));
		}
	}

	/**
	 * Ex:
	 *  - [m/44'/0'/0']/0/1 -> m/44'/0'/0'/0/1
	 *  - M/0/1 -> m/0/1 
	 */
	public ZWHdPath toPrivatePath() {
		if (this.resolvesToPrivateKey()) {
			throw new ZWHdWalletException("This path already describes the resolution to a private key. ");
		}

		ZWHdPath p = new ZWHdPath(this);
		if (p.relativeToPrv == null)
			p.relativeToPrv = new ArrayList<ZWHdChildNumber>();
		p.relativeToPrv.addAll(p.relativeToPub);
		p.relativeToPub = null;
		return p;
	}

	/**
	 * This is useful for altering the relativity of the path.
	 * 
	 * For example, take the path
	 * 
	 *   [m/44'/0'/0']/0/1
	 *   
	 * To derive it's public component from the checkpoint stored in the accounts table,
	 * we need to replace "[m/44'/0'/0']" with just "M".
	 * 
	 * More generally, this can be used whenever partial derivations are known. For example,
	 * if "m" itself is not known, but m/44'/0' is, then this can be used to change the relativity
	 * like so: 
	 * 
	 *   [m/44'/0'/0']/0/1 -> [m/0']/0/1
	 *   
	 * This method can also be used to expand relativity, to reverse the previous operation.
	 * 
	 *   [m/0']/0/1 -> [m/44'/0'/0']/0/1
	 *   
	 * If the relative path passed in is not a valid rebase, a {@link ZWHdWalletException} exception is thrown.
	 */
	public ZWHdPath rebaseRelativity(ZWHdPath rel, boolean expand) {
		rel.checkValidity();
		String currPath = this.toString();
		String relPath = rel.toString();

		String replaceMe = expand ? this.getBaseRelativity() : relPath;
		String withMe = expand ? relPath : (rel.resolvesToPublicKey() ? "M" : "m");

		if (!currPath.contains(replaceMe)) {
			throw new ZWHdWalletException("Path is not a valid rebase: " + currPath + ": " + replaceMe + " -> " + withMe);
		}
		// The constructor itself checks for validity of the result, so that we don't end up 
		// rebasing and doing things like
		//   [m/44'/0'/0']/0/1 -> [[m]/44'/0'/0']/0/1
		// or
		//   m/44'/0'/0' -> [m/44']/0'/0'
		// or
		//   [m/44']/0/0 -> m/44'/0/0 
		return new ZWHdPath(currPath.replace(replaceMe, withMe));
	}

	public String getBaseRelativity() {
		if (this.derivedFromPrivateKey() && !this.derivedFromPublicKey()) {
			return "m";
		} else if (!this.derivedFromPrivateKey() && this.derivedFromPublicKey()) {
			return "M";
		} else {
			throw new ZWHdWalletException("Ambiguous path");
		}
	}

	private void checkBip44Path() {
		CryptoUtils.checkHd(this.derivedFromPrivateKey());

		if (this.resolvesToPrivateKey()) {
			CryptoUtils.checkHd(this.relativeToPrv.size() == 5);
			CryptoUtils.checkHd(!this.relativeToPrv.get(3).isHardened());
			CryptoUtils.checkHd(!this.relativeToPrv.get(4).isHardened());
		} else {
			CryptoUtils.checkHd(this.relativeToPrv.size() == 3);
			CryptoUtils.checkHd(this.relativeToPub.size() == 2);
			CryptoUtils.checkHd(!this.relativeToPub.get(0).isHardened());
			CryptoUtils.checkHd(!this.relativeToPub.get(1).isHardened());
		}

		CryptoUtils.checkHd(this.relativeToPrv.get(0).isHardened());
		CryptoUtils.checkHd(this.relativeToPrv.get(1).isHardened());
		CryptoUtils.checkHd(this.relativeToPrv.get(2).isHardened());
	}

	public boolean isBip44Path() {
		try {
			this.checkBip44Path();
		} catch(ZWHdWalletException hdwe) {
			return false;
		}
		return true;
	}

	public int getBip44Purpose() {
		this.checkBip44Path();
		return this.relativeToPrv.get(0).getIndex();
	}

	public int getBip44CoinType() {
		this.checkBip44Path();
		return this.relativeToPrv.get(1).getIndex();
	}

	public int getBip44Account() {
		this.checkBip44Path();
		return this.relativeToPrv.get(2).getIndex();
	}

	public int getBip44Change() {
		this.checkBip44Path();
		if (this.resolvesToPrivateKey()) {
			return this.relativeToPrv.get(3).getIndex();
		} else {
			return this.relativeToPub.get(0).getIndex();
		}
	}

	public int getBip44AddressIndex() {
		this.checkBip44Path();
		if (this.resolvesToPrivateKey()) {
			return this.relativeToPrv.get(4).getIndex();
		} else {
			return this.relativeToPub.get(1).getIndex();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ZWHdPath other = (ZWHdPath) obj;

		if (relativeToPrv == null) {
			if (other.relativeToPrv != null)
				return false;
		} else if (!relativeToPrv.equals(other.relativeToPrv)) {
			return false;
		}

		if (relativeToPub == null) {
			if (other.relativeToPub != null)
				return false;
		} else if (!relativeToPub.equals(other.relativeToPub)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((relativeToPrv == null) ? 0 : relativeToPrv.hashCode());
		result = prime * result
				+ ((relativeToPub == null) ? 0 : relativeToPub.hashCode());
		return result;
	}

}
