package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
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
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.ZLog;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class OWTransactionDetailsFragment extends OWWalletUserFragment 
implements OWEditableTextBoxController.EditHandler<OWTransaction>, OnClickListener {

	public static final String TX_ITEM_HASH_KEY = "txItemHash";

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
	private ImageView sendToAddress;

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

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);

		this.txItem = getWalletManager().readTransactionByHash(this.getSelectedCoin(), this.getArguments().getString(TX_ITEM_HASH_KEY));

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
		this.sendToAddress = (ImageView) rootView.findViewById(R.id.sendTo);
		this.initFields(savedInstanceState);

		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean(IS_EDITING_KEY, this.isEditing);
		if (this.isEditing) {
			OWEditState.saveIntoBundle(curEditState, outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("TRANSACTION", false, false, false);
	}

	/**
	 * Get the arguments passed from the activity and set the text fields 
	 * on the view
	 */
	public void initFields(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(IS_EDITING_KEY)) {
				this.isEditing = savedInstanceState.getBoolean(IS_EDITING_KEY);
				this.curEditState = OWEditState.loadFromBundle(savedInstanceState);

			}
		}
		Drawable coinImage = this.getResources().getDrawable(
				this.getSelectedCoin().getLogoResId());
		this.coinLogo.setImageDrawable(coinImage);

		OWFiat fiat = OWPreferencesUtils.getFiatCurrency(getActivity());
		this.populateAmount();
		this.populateCurrency();
		this.confirmationFee.setText(txItem.getCoinId().getDefaultFeePerKb());
		this.currencyType.setText(fiat.getName());

		Date date = new Date(this.txItem.getTxTime() * 1000);
		this.time.setText(ZiftrUtils.formatterNoTimeZone.format(date));

		this.populateAddress();

		this.populatePendingInformation();
		this.sendToAddress.setOnClickListener(this);
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
		OWFiat fiat = OWPreferencesUtils.getFiatCurrency(getActivity());
		
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
			ZLog.log("ABCEEEEE" + a);
			sb.append(a);
			if (i != txItem.getDisplayAddresses().size()-1) {
				sb.append("\n");
				ZLog.log("ABCDEF"+i);
				ZLog.log("ABCGHE" + txItem.getDisplayAddresses().size());
			}
		}
		addressTextView.setText(sb.toString());

	}

	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	private void populatePendingInformation() {
		int totalConfirmations = txItem.getCoinId().getNumRecommendedConfirmations();
		long confirmed = txItem.getNumConfirmations();
		if (((int)confirmed) >= totalConfirmations){
			this.status.setText("Confirmed");
		} else {
			this.status.setText("Confirmed (" + confirmed + " of " + totalConfirmations + ")");
		}
		
		//TO prevent estimated time and Confirmed text views from overlapping,
		//we only show estimated time if the screen width > 3 * width of the confirmed textview
		int screenWidth;
		if (android.os.Build.VERSION.SDK_INT < 13){
			screenWidth = getOWMainActivity().getWindowManager().getDefaultDisplay().getWidth();
		} else {
			Point size = new Point();
			getOWMainActivity().getWindowManager().getDefaultDisplay().getSize(size);
			screenWidth = size.x; 
		}
		this.status.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
		
		if (confirmed < totalConfirmations && screenWidth > this.status.getMeasuredWidth() * 3){
			long estimatedTime = txItem.getCoinId().getSecondsPerAverageBlockSolve()*(totalConfirmations-confirmed);
			this.timeLeft.setText("Estimated Time: " + formatEstimatedTime(estimatedTime));
		}
		//in theory a really old network, or a network with fast enough blocks, could have more blocks than an int can hold
		//however even in this case it's needed confirmations would still be a smaller number
		//this just checks it so that if we're for some reason checking the progress of a really old transactions 
		//it won't have usses due to converting a long to an int
		int progress;
		if(confirmed <= totalConfirmations) {
			progress = (int) confirmed;
		}
		else {
			progress = totalConfirmations;
		}
		this.progressBar.setMax(totalConfirmations);
		this.progressBar.setProgress(progress);
	}

	public void setTxItem(OWTransaction txItem) {
		this.txItem = txItem;
	}

	public String formatEstimatedTime(long estimatedTime) {
		StringBuilder sb = new StringBuilder();
		if (estimatedTime > 3600) {
			long hours = estimatedTime / 3600;
			estimatedTime = estimatedTime % 3600;
			sb.append(hours + " hours, ");
		}
		long minutes = estimatedTime/60;
		estimatedTime = estimatedTime % 60;
		sb.append (minutes + " minutes");
		if (estimatedTime != 0) {
			sb.append(estimatedTime + ", seconds");
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

	@Override
	public void onClick(View v) {
		if (v==this.sendToAddress){
			getOWMainActivity().openSendCoinsView(this.addressTextView.getText().toString());
		}
	}

}
