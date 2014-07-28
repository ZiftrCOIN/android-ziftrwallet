package com.ziftr.android.onewallet.dialog;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.dialog.handlers.OWNewCurrencyDialogHandler;
import com.ziftr.android.onewallet.fragment.accounts.OWNewCurrencyListAdapter;
import com.ziftr.android.onewallet.fragment.accounts.OWNewCurrencyListItem;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.ZLog;

/**
 * Dialogs where the app requests to get a new currency to add.
 */
public class OWNewCurrencyDialog extends OWDialogFragment {

	/** We only want to show the coins that the user doesn't already have a wallet for. */
	private List<OWCoin.Type> coinsToShowInDialog;

	/** The key to save the text in the box. */
	private static final String SELECTED_CURRENCY_KEY = "selected_key";

	/** To keep track of which type user has selected. */
	private OWCoin.Type currSelectedCoinType;

	/**
	 * Sets up a new currency dialog with all
	 * of the currencies in {@link OWCoin.Type}.
	 */
	public OWNewCurrencyDialog() {
		Bundle b = new Bundle();
		for (OWCoin.Type type : OWCoin.Type.values()) {
			b.putBoolean(type.toString(), true);
		}
		this.initializeFromBundle(b);
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we must make sure that it is able to handle accepting 
	 * and cancelling from passphrase dialogs.
	 * 
	 * This method throws an exception if neither the newly attached activity
	 * nor the target fragment are instances of {@link OWNewCurrencyDialogHandler}.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.validateHandler(activity, OWNewCurrencyDialogHandler.class);
	}

	/**
	 * The bundle should have booleans put into it, one for each {@link OWCoin.Type}.
	 * The boolean should describe whether or not to include the currency
	 * in the dialog.
	 */
	@Override
	public void setArguments(Bundle args) {
		this.initializeFromBundle(args);
		super.setArguments(args);
	}

	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);

		this.setDialogView(this.getActivity().getLayoutInflater().inflate(
				R.layout.dialog_get_new_currency, null));
		builder.setView(this.getDialogView());

		if (savedInstanceState != null) {
			if (savedInstanceState.getString(SELECTED_CURRENCY_KEY) != null) {
				this.currSelectedCoinType = OWCoin.Type.valueOf(
						savedInstanceState.getString(SELECTED_CURRENCY_KEY));
			}
		}

		this.initialzeGridView();

		return builder.create();
	}

	/**
	 * Handle clicks on this dialog. 
	 */
	@Override
	public void onClick(DialogInterface dialog, int which) {
		// Might be null if user doesn't hit anything.
		if (this.currSelectedCoinType != null) {
			OWNewCurrencyDialogHandler handler = getHandler();
			if (which == DialogInterface.BUTTON_POSITIVE) {
				handler.handleNewCurrencyPositive(this.getTargetRequestCode(), 
						this.currSelectedCoinType);
			} else if (which == DialogInterface.BUTTON_NEGATIVE) {
				handler.handleNegative(this.getTargetRequestCode());
			} else {
				ZLog.log("These dialogs shouldn't have neutral buttons.");
			}
		}
	}

	/**
	 * @return
	 */
	private OWNewCurrencyDialogHandler getHandler() {
		return ((OWNewCurrencyDialogHandler) this.getActivity());
	}

	/**
	 * When we save the instance, in addition to doing everything that
	 * all dialogs must do, we also have to store the current entered 
	 * text in the 
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save all of the important strings in the dialog
		if (this.currSelectedCoinType != null) {
			outState.putString(SELECTED_CURRENCY_KEY, 
					this.currSelectedCoinType.toString());
		}
	}

	/**
	 * Sets the adapter and the onclick for the grid view.  
	 */
	private void initialzeGridView() {
		GridView grid = (GridView)
				this.getDialogView().findViewById(R.id.addNewCurrencyGridView);
		grid.setAdapter(new OWNewCurrencyListAdapter(this.getActivity(), 
				R.layout._coin_with_title, this.getItemsFromCoinTypes()));

		grid.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				OWNewCurrencyListItem newItem = (OWNewCurrencyListItem) 
						parent.getItemAtPosition(position);
				currSelectedCoinType = newItem.getCoinId();
			}
		});
	}

	/**
	 * We initialize the list of coins to show in the dialog and then 
	 * for each OWCoin.Type.___.toString() that has a boolean value
	 * of true in the bundle we add it to the list.
	 *  
	 * @param args - The bundle with the booleans put into it. The keys are 
	 * the toString()s of the different OWCoin.Type possible values.
	 */
	private void initializeFromBundle(Bundle args) {
		this.coinsToShowInDialog = new ArrayList<OWCoin.Type>();
		if (args != null) {
			for (OWCoin.Type type : OWCoin.Type.values()) {
				if (args.getBoolean(type.toString())) {
					this.coinsToShowInDialog.add(type);
				}
			}
		}
	}

	/**
	 * Makes the dataset that should be included in the gridview
	 * by using the list of coins that should be shown.
	 * 
	 * @return as above
	 */
	private List<OWNewCurrencyListItem> getItemsFromCoinTypes() {
		List<OWNewCurrencyListItem> list = new ArrayList<OWNewCurrencyListItem>();
		for (OWCoin.Type type : this.coinsToShowInDialog) {
			list.add(new OWNewCurrencyListItem(type));
		}
		return list;
	}

}