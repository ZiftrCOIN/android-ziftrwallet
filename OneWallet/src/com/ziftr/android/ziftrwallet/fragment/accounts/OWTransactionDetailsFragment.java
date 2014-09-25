package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.crypto.OWTransaction;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWConverter;
import com.ziftr.android.ziftrwallet.util.OWEditState;
import com.ziftr.android.ziftrwallet.util.OWFiat;
import com.ziftr.android.ziftrwallet.util.TempPreferencesUtil;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWTransactionDetailsFragment extends OWWalletUserFragment 
implements OWEditableTextBoxController.EditHandler<OWTransaction> {

	private static final String TX_ITEM_HASH_KEY = "txItemHash";

	private static final String IS_EDITING_KEY = "isEditing";

	private View rootView;
	private ImageView editLabelButton;
	private EditText labelEditText;
	private TextView amount;
	private TextView amountLabel;
	private TextView timeLabel;
	private TextView currency;
	private TextView confirmationFee;
	private TextView currencyType;
	private TextView time;
	private TextView addressTextView;
	private TextView status;
	private TextView timeLeft;
	private ProgressBar progressBar;
	private ImageView coinLogo;

	private OWTransaction txItem;

	private OWEditState curEditState;

	private boolean isEditing;

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);

		// Resize view when keyboard pops up instead of just default pan so user 
		// can see field more clearly
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		this.hideWalletHeader();

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(IS_EDITING_KEY)) {
				this.isEditing = savedInstanceState.getBoolean(IS_EDITING_KEY);
				if (this.isEditing) {
					this.curEditState = OWEditState.loadFromBundle(savedInstanceState);
				}
			}
			if (savedInstanceState.containsKey(TX_ITEM_HASH_KEY)) {
				this.txItem = getWalletManager().readTransactionByHash(this.getSelectedCoin(), savedInstanceState.getString(TX_ITEM_HASH_KEY));
			}
		}

		this.editLabelButton = (ImageView) rootView.findViewById(R.id.edit_txn_note);
		this.labelEditText = (EditText) rootView.findViewById(R.id.txn_note);
		this.amount = (TextView) rootView.findViewById(R.id.amount);
		this.amountLabel = (TextView) rootView.findViewById(R.id.amountLabel);
		this.timeLabel = (TextView) rootView.findViewById(R.id.date_label);
		this.currency = (TextView) rootView.findViewById(R.id.currencyValue);
		this.confirmationFee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);
		this.currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		this.time = (TextView) rootView.findViewById(R.id.date);
		this.addressTextView = (TextView) rootView.findViewById(R.id.routing_address);
		this.status = (TextView) rootView.findViewById(R.id.status);
		this.timeLeft = (TextView) rootView.findViewById(R.id.time_left);
		this.progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
		this.coinLogo = (ImageView) rootView.findViewById(R.id.coin_logo);
		this.initFields(savedInstanceState);

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_EDITING_KEY, this.isEditing);
		if (this.isEditing) {
			OWEditState.saveIntoBundle(curEditState, outState);
		}
		outState.putString(TX_ITEM_HASH_KEY, this.txItem.getSha256Hash().toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("TRANSACTION", false, false);
	}

	/**
	 * Get the arguments passed from the activity and set the text fields 
	 * on the view
	 */
	public void initFields(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(IS_EDITING_KEY)) {
				this.isEditing = savedInstanceState.getBoolean(IS_EDITING_KEY);
			}
			if (savedInstanceState.containsKey(TX_ITEM_HASH_KEY)) {
				this.txItem = getWalletManager().readTransactionByHash(
						this.getSelectedCoin(), savedInstanceState.getString(TX_ITEM_HASH_KEY));
				ZLog.log("Charmander: " + txItem);
			}
		}

		Drawable coinImage = this.getResources().getDrawable(
				this.getSelectedCoin().getLogoResId());
		this.coinLogo.setImageDrawable(coinImage);

		OWFiat fiat = TempPreferencesUtil.getSelectedFiat();
		this.populateAmount();
		this.populateCurrency();
		this.confirmationFee.setText(txItem.getCoinId().getDefaultFeePerKb());
		this.currencyType.setText(fiat.getName());

		Date date = new Date(this.txItem.getTxTime() * 1000);
		this.time.setText(ZiftrUtils.formatterNoTimeZone.format(date));

		this.populateAddress();

		this.populatePendingInformation();

		OWEditableTextBoxController<OWTransaction> controller = new OWEditableTextBoxController<OWTransaction>(
				this, labelEditText, editLabelButton, this.txItem.getTxNote(), txItem);
		editLabelButton.setOnClickListener(controller);
		this.labelEditText.addTextChangedListener(controller);
	}

	private void populateAmount() {
		BigInteger baseAmount = this.txItem.getTxAmount();
		BigDecimal amountValue = ZiftrUtils.bigIntToBigDec(txItem.getCoinId(), baseAmount); 
		amount.setText(OWCoin.formatCoinAmount(txItem.getCoinId(), amountValue).toPlainString());
		if (this.txItem.getTxAmount().compareTo(BigInteger.ZERO) < 0) {
			// This means the tx is sent (relative to user)
			this.amountLabel.setText("Amount Sent");
			this.timeLabel.setText("Sent");
		} else {
			// This means the tx is received (relative to user)
			this.amountLabel.setText("Amount Received");
			this.timeLabel.setText("Received");
		}

		if (txItem.isPending()) {
			this.amount.setTextColor(getResources().getColor(R.color.Crimson));
			this.amountLabel.append(" (pending)");
		}
	}

	private void populateCurrency() {
		OWFiat fiat = TempPreferencesUtil.getSelectedFiat();
		
		BigInteger fiatAmt = OWConverter.convert(txItem.getTxAmount(), 
				txItem.getCoinId(), fiat);
		String formattedfiatAmt = OWFiat.formatFiatAmount(fiat, 
				ZiftrUtils.bigIntToBigDec(txItem.getCoinId(), fiatAmt), false);

		currency.setText(formattedfiatAmt);

		TextView currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		currencyType.setText(fiat.getName());

		TextView time = (TextView) rootView.findViewById(R.id.date);
		Date date = new Date(this.txItem.getTxTime() * 1000);
		time.setText(ZiftrUtils.formatterNoTimeZone.format(date));
	}

	private void populateAddress() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < txItem.getDisplayAddresses().size(); i++) {
			String a = txItem.getDisplayAddresses().get(i);
			sb.append(a);
			if (i != txItem.getDisplayAddresses().size()-1) {
				sb.append("\n");
			}
		}
		ZLog.log(sb.toString());
		addressTextView.setText(sb.toString());

	}

	private void populatePendingInformation() {
		int totalConfirmations = txItem.getCoinId().getNumRecommendedConfirmations();
		int confirmed = txItem.getNumConfirmations();

		this.status.setText("Confirmed (" + confirmed + " of " + totalConfirmations + ")");

		int estimatedTime = txItem.getCoinId().getSecondsPerAverageBlockSolve()*(totalConfirmations-confirmed);
		this.timeLeft.setText(formatEstimatedTime(estimatedTime));

		this.progressBar.setMax(totalConfirmations);
		this.progressBar.setProgress(confirmed);
	}

	public void setTxItem(OWTransaction txItem) {
		this.txItem = txItem;
	}

	public String formatEstimatedTime(int seconds) {
		StringBuilder sb = new StringBuilder();
		if (seconds > 3600) {
			int hours = seconds / 3600;
			seconds = seconds % 3600;
			sb.append(hours + " hours, ");
		}
		int minutes = seconds/60;
		seconds = seconds % 60;
		sb.append (minutes + " minutes");
		if (seconds != 0) {
			sb.append(seconds + ", seconds");
		}
		return sb.toString();
	}

	//////////////////////////////////////////////////////
	////////// Methods for being an EditHandler //////////
	//////////////////////////////////////////////////////

	@Override
	public void onEditStart(OWEditState state, OWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}
		this.curEditState = state;
		this.isEditing = true;
	}
	
	@Override
	public void onEdit(OWEditState state, OWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}
		this.curEditState = state;
		ZLog.log("pika");
		// TODO Auto-generated method stub
		txItem.setTxNote(state.text);
		getWalletManager().updateTransactionNote(txItem);
	}

	@Override
	public void onEditEnd(OWTransaction t) {
		if (t != this.txItem) {
			ZLog.log("Should have gotten the same object, something went wrong. ");
			return;
		}

		// Now that we are done editing 
		this.isEditing = false;
	}

	@Override
	public boolean isInEditingMode() {
		return this.isEditing;
	}

	@Override
	public boolean isEditing(OWTransaction t) {
		return this.isEditing && t == this.txItem; 
	}
	
	/**
	 * @return the curEditState
	 */
	@Override
	public OWEditState getCurEditState() {
		return this.curEditState;
	}

}
