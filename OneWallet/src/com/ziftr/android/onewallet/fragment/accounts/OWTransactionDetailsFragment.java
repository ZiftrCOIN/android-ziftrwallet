package com.ziftr.android.onewallet.fragment.accounts;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;
import com.ziftr.android.onewallet.crypto.OWTransaction;
import com.ziftr.android.onewallet.util.OWCoin;
import com.ziftr.android.onewallet.util.OWConverter;
import com.ziftr.android.onewallet.util.OWFiat;
import com.ziftr.android.onewallet.util.OWUtils;

public class OWTransactionDetailsFragment extends OWWalletUserFragment implements OnClickListener {

	private static final String TX_ITEM_HASH_KEY = "txItemHash";

	private static final String IS_EDITING_KEY = "isEditing";

	private View rootView;
	private ImageView editLabelButton;
	private EditText labelEditText;
	
	private OWTransaction txItem;

	private boolean isEditing;

	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, Bundle savedInstanceState) {

		//resize view when keyboard pops up instead of just default pan so user can see field more clearly
		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

		this.hideWalletHeader();

		this.rootView = inflater.inflate(R.layout.accounts_transaction_details, container, false);
		if (savedInstanceState != null){
				if (savedInstanceState.containsKey(IS_EDITING_KEY)){
					this.isEditing = savedInstanceState.getBoolean(IS_EDITING_KEY);
				}
				if (savedInstanceState.containsKey(TX_ITEM_HASH_KEY)){
					this.txItem = getWalletManager().readTransactionByHash(this.getCurSelectedCoinType(), savedInstanceState.getString(TX_ITEM_HASH_KEY));
				}
		}

		this.initFields();
		return this.rootView;
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		outState.putBoolean(IS_EDITING_KEY, this.isEditing);
		outState.putString(TX_ITEM_HASH_KEY, this.txItem.getSha256Hash().toString());
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onResume() {
		super.onResume();
		this.getOWMainActivity().changeActionBar("TRANSACTION", false, false);
	}

	/**
	 * Get the arguments passed from the activity and set the text fields on the view
	 * 
	 * TODO the string constants should be public static final fields in this class. 
	 *  
	 */
	@SuppressLint("NewApi")
	public void initFields() {
		// TODO 
		TextView amount = (TextView) rootView.findViewById(R.id.amount);
		BigInteger baseAmount = this.txItem.getTxAmount();
		BigDecimal amountValue = OWUtils.bigIntToBigDec(txItem.getCoinId(), baseAmount); 
		amount.setText(OWCoin.formatCoinAmount(txItem.getCoinId(), amountValue).toPlainString());
		TextView amountLabel = (TextView) rootView.findViewById(R.id.amountLabel);
		TextView timeLabel = (TextView) rootView.findViewById(R.id.date_label);
		if (this.txItem.getTxAmount().compareTo(BigInteger.ZERO) < 0) {
			// This means the tx is sent (relative to user)
			amountLabel.setText("Amount Sent");
			timeLabel.setText("Sent");
		} else {
			// This means the tx is received (relative to user)
			amountLabel.setText("Amount Received");
			timeLabel.setText("Received");
		}
		if (txItem.isPending()) {
			amount.setTextColor(getResources().getColor(R.color.Crimson));
			amountLabel.append(" (pending)");
		}
		TextView currency = (TextView) rootView.findViewById(R.id.currencyValue);
		BigInteger fiatAmt = OWConverter.convert(txItem.getTxAmount(), 
				txItem.getCoinId(), txItem.getFiatType());
		BigDecimal formattedfiatAmt = OWFiat.formatFiatAmount(
				txItem.getFiatType(), OWUtils.bigIntToBigDec(txItem.getCoinId(), fiatAmt));
		currency.setText(formattedfiatAmt.toPlainString());
		
		TextView confirmationFee = (TextView) rootView.findViewById(R.id.confirmation_fee_amount);
		confirmationFee.setText(txItem.getCoinId().getDefaultFeePerKb());
		
		TextView currencyType = (TextView) rootView.findViewById(R.id.currencyType);
		currencyType.setText(this.txItem.getFiatType().getName());

		TextView time = (TextView) rootView.findViewById(R.id.date);
		Date date = new Date(this.txItem.getTxTime() * 1000);
		time.setText(OWUtils.formatterNoTimeZone.format(date));
		
		TextView routingAddress = (TextView) rootView.findViewById(R.id.routing_address);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < txItem.getDisplayAddresses().size(); i++) {
			String a = txItem.getDisplayAddresses().get(i);
			sb.append(a);
			if (i != txItem.getDisplayAddresses().size()-1) {
				sb.append("\n");
			}
		}
		routingAddress.setText(sb.toString());
		
		this.editLabelButton = (ImageView) rootView.findViewById(R.id.edit_txn_note);
		this.labelEditText = (EditText) rootView.findViewById(R.id.txn_note);
		this.labelEditText.setText(txItem.getTxNote());
		toggleEditNote(!isEditing);
		editLabelButton.setOnClickListener(this);
		
		int totalConfirmations = txItem.getCoinId().getNumRecommendedConfirmations();
		int confirmed = txItem.getNumConfirmations();
		
		TextView status = (TextView) rootView.findViewById(R.id.status);
		status.setText("Confirmed (" + confirmed + " of " + totalConfirmations + ")");
		
		TextView timeLeft = (TextView) rootView.findViewById(R.id.time_left);
		int estimatedTime = txItem.getCoinId().getSecondsPerAverageBlockSolve()*(totalConfirmations-confirmed);
		timeLeft.setText(formatEstimatedTime(estimatedTime));
		
		ProgressBar progressBar = (ProgressBar) rootView.findViewById(R.id.progress_bar);
		progressBar.setMax(totalConfirmations);
		progressBar.setProgress(confirmed);
	}
	
	public void setTxItem(OWTransaction txItem){
		this.txItem = txItem;
	}
	/**
	 * 
	 * @param txNote - EditText for note view
	 * @param editLabelButton - ImageView for edit button
	 * @param toggleOff - boolean to determine how to toggle
	 */
	public void toggleEditNote(boolean toggleOff){
		if (!toggleOff) {
			this.labelEditText.setBackgroundResource(android.R.drawable.editbox_background_normal);
			this.labelEditText.setFocusable(true);
			this.labelEditText.setFocusableInTouchMode(true);
			this.labelEditText.requestFocus();
			editLabelButton.setImageResource(R.drawable.close_enabled);
			isEditing = true;
		} else {
			this.labelEditText.setFocusable(false);
			this.labelEditText.setFocusableInTouchMode(false);
			this.labelEditText.setBackgroundColor(getResources().getColor(android.R.color.transparent));
			this.labelEditText.setPadding(0, 0, 0, 0);
			editLabelButton.setImageResource(R.drawable.edit_enabled);
			isEditing = false;
			txItem.setTxNote(this.labelEditText.getText().toString());
			getWalletManager().updateTransactionNote(txItem);
		}
	}
	
	public String formatEstimatedTime(int seconds){
		StringBuilder sb = new StringBuilder();
		if (seconds > 3600){
			int hours = seconds / 3600;
			seconds = seconds % 3600;
			sb.append(hours + " hours, ");
		}
		int minutes = seconds/60;
		seconds = seconds % 60;
		sb.append (minutes + " minutes, ");
		if (seconds != 0){
			sb.append(seconds + " seconds");
		}
		return sb.toString();
	}

	@Override
	public void onClick(View v) {
		if (v == this.editLabelButton) {
			toggleEditNote(isEditing);
		}
	}
	
}
