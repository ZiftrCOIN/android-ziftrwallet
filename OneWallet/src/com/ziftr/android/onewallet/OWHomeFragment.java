package com.ziftr.android.onewallet;

import java.util.Arrays;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import com.ziftr.android.onewallet.util.ZLog;
import com.ziftr.android.onewallet.util.ZiftrUtils;

/**
 * The OWMainActivity starts this fragment. This fragment is 
 * associated with a few buttons where the user can choose what
 * kind of wallet they want to open.
 */
public class OWHomeFragment extends Fragment implements OWPassphraseConsumer {

	/** The view container for this fragment. */
	protected View rootView;

	/** The most recently clicked button. */
	private View clickedButton;

	/** The key for getting the hash from the preferences. */
	private final String PASSPHRASE_KEY = "ow_passphrase_key_1";

	/** 
	 * Placeholder for later, doesn't do anything other than 
	 * what parent method does right now.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.fragment_home, container, false);

		// For the bitcoinWalletButton
		View bitcoinWalletButton = rootView.findViewById(
				R.id.buttonBitcoinWallet);
		// Add what kind of fragment this button should create as tag
		bitcoinWalletButton.setTag(OWBitcoinWalletFragment.class); 
		// Set the listener for the clicks
		bitcoinWalletButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				OWHomeFragment.this.clickedButton = v;

				final SharedPreferences prefs = 
						getActivity().getSharedPreferences(PASSPHRASE_KEY, 
								Context.MODE_PRIVATE);
				byte[] storedHash = getStoredPassphraseHash(prefs);

				OWPassphraseConsumer passphraseConsumer;
				String message;
				if (storedHash == null) {
					passphraseConsumer = new OWPassphraseConsumer() {
						@Override
						public void consumePassphraseHash(byte[] inputHash) {
							Editor editor = prefs.edit();
							editor.putString(
									OWHomeFragment.this.PASSPHRASE_KEY, 
									ZiftrUtils.binaryToHexString(inputHash));
							editor.commit();
							OWHomeFragment.this.startFragmentFromButtonTag();
						}
					};
					message = "Please input your passphrase. This will "
							+ "be used for securing all your wallets.";
				} else {
					passphraseConsumer = OWHomeFragment.this;
					message = "Please input your passphrase";
				}

				OWHomeFragment.getPassphraseFromUser(
						OWHomeFragment.this.getActivity(), message,
						passphraseConsumer, null);

			}
		});

		// Return the view which was inflated
		return rootView;
	}

	@Override
	public void consumePassphraseHash(byte[] inputHash) {
		// This is called after the user hits ok in the passphrase dialog
		// only if it isn't the first time.
		SharedPreferences prefs = getActivity().getSharedPreferences(
				PASSPHRASE_KEY, Context.MODE_PRIVATE);
		byte[] storedHash = this.getStoredPassphraseHash(prefs);
		if (storedHash != null && inputHash != null) {
			if (Arrays.equals(storedHash, inputHash)) {
				this.startFragmentFromButtonTag();
			} else {
				OWHomeFragment.alertUser(getActivity(), 
						"Passphrases don't match\n"
								+ Arrays.toString(inputHash) + "\n" + 
								Arrays.toString(storedHash));
			}
		} else {
			ZLog.log("Error, something was null that shouldn't have been.");
		}
	}

	private void startFragmentFromButtonTag() {
		try {
			@SuppressWarnings("unchecked") 
			Class<? extends Fragment> newFragmentClass = (
					Class<? extends Fragment>) this.clickedButton.getTag();

			Fragment fragment = newFragmentClass.newInstance();
			String tagString = newFragmentClass.getSimpleName();

			getFragmentManager().beginTransaction().replace(
					R.id.mainActivityContainer, fragment, tagString
					).addToBackStack(null).commit();
		} catch(Exception e) {
			ZLog.log("Exceptiong trying to load wallet fragment. Was a "
					+ "button not properly setup? ", e);
		}
	}

	/**
	 * @param prefs
	 * @return
	 */
	private byte[] getStoredPassphraseHash(SharedPreferences prefs) {
		String storedPassphrase = prefs.getString(
				this.PASSPHRASE_KEY, null);
	
		byte[] storedHash = (storedPassphrase == null) ?
				null : ZiftrUtils.hexStringToBinary(storedPassphrase);
		return storedHash;
	}

	public static void getPassphraseFromUser(
			Context context,
			String message,
			final OWPassphraseConsumer onConfirm, 
			final Runnable onCancel) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);

		alert.setTitle("OneWallet");
		alert.setMessage(message);

		// Set an EditText view to get user input 
		final EditText input = new EditText(context);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (onConfirm != null) {
					byte[] passphraseBytes = 
							input.getText().toString().getBytes();
					onConfirm.consumePassphraseHash(
							ZiftrUtils.Sha256Hash(passphraseBytes));
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (onCancel != null) {
					onCancel.run();
				}
			}
		});

		alert.show();
	}

	public static void alertUser(
			Context context, String message) {
		AlertDialog.Builder alert = new AlertDialog.Builder(context);

		alert.setTitle("OneWallet");
		alert.setMessage(message);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Do nothing
			}
		});

		alert.show();
	}

}
