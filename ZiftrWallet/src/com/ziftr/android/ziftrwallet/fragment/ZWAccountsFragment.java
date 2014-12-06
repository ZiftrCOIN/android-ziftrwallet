package com.ziftr.android.ziftrwallet.fragment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.ZWPreferencesUtils;
import com.ziftr.android.ziftrwallet.ZWWalletManager;
import com.ziftr.android.ziftrwallet.crypto.ZWCoin;
import com.ziftr.android.ziftrwallet.crypto.ZWConverter;
import com.ziftr.android.ziftrwallet.crypto.ZWFiat;
import com.ziftr.android.ziftrwallet.sqlite.ZWSQLiteOpenHelper;

/**
 * The ZWMainActivity starts this fragment. This fragment is 
 * associated with list view of user wallets and a bar at the bottom of the list
 * view which opens a dialog to add rows to the list view.  
 */
public class ZWAccountsFragment extends ZWFragment {

	/** The view container for this fragment. */
	private View rootView;

	/** The context for this fragment. */
	private FragmentActivity mContext;

	/** The list view which shows a link to open each of the wallets/currency types. */
	private ListView currencyListView;
	/** The data set that the currencyListView uses. */
	private List<ZWCurrencyListItem> userWallets;

	/** The wallet manager. This fragment works with */
	private ZWWalletManager walletManager;

	private ArrayAdapter<ZWCurrencyListItem> walletAdapter;

	private View footer;
	
	private TextView totalBalance;
	private ZWFiat fiatType;

	/** 
	 * Placeholder for later, doesn't do anything other than 
	 * what parent method does right now.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

	}

	@Override
	public void onSaveInstanceState(Bundle outState) { 
		super.onSaveInstanceState(outState);
	}


	@Override
	public void onResume() {
		super.onResume();
		this.getZWMainActivity().changeActionBar("ziftrWALLET", true, false, true);
	}

	/**
	 * To create the view for this fragment, we start with the 
	 * basic fragment_home view which just has a few buttons that
	 * open up different coin-type wallets.
	 */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		this.rootView = inflater.inflate(R.layout.section_accounts_layout, container, false);

		this.walletManager = this.getZWMainActivity().getWalletManager();

		//dropshadow under listview
		this.footer = this.getActivity().getLayoutInflater().inflate(R.layout.dropshadowlayout, null);

		this.totalBalance = (TextView) this.rootView.findViewById(R.id.user_total_balance);
		
		// Initialize the list of user wallets that they can open
		this.initializeCurrencyListView();
		
		//show Add Currency Message if user has no wallets
		this.toggleAddCurrencyMessage();
		
		this.fiatType = ZWPreferencesUtils.getFiatCurrency();

		this.calculateTotal();
		// Return the view which was inflated
		return rootView;
	}

	/**
	 * Whenever this is fragment is attached to an activity 
	 * we save a reference to the activity.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		this.mContext = (FragmentActivity) activity;
	}

	/**
	 * A messy method which is just used to not have huge if else
	 * blocks elsewhere. Gets the item that we will add to the currency
	 * list when the user adds a new currency.
	 * 
	 * @param id - The type of coin to get a new {@link ZWCurrencyListItem} for. 
	 * @return as above
	 */
	private ZWCurrencyListItem getItemForCoinType(ZWCoin id) {
		int resId = R.layout.accounts_currency_list_single_item;
		BigDecimal amount = id.getAmount(this.walletManager.getWalletBalance(id, ZWSQLiteOpenHelper.BalanceType.AVAILABLE));
		String balance = id.getFormattedAmount(amount);
		ZWFiat fiat = ZWPreferencesUtils.getFiatCurrency();
		String fiatBalance = ZWConverter.convert(amount, id, fiat).setScale(2, RoundingMode.HALF_DOWN).toPlainString();
		String fiatVal = ZWConverter.convert(BigDecimal.ONE, id, fiat).toString();
		return new ZWCurrencyListItem(id, fiatVal, balance, fiatBalance, resId);
	}


	/**
	 * Notifies the list view adapter that the data set has changed
	 * to initialize a redraw. 
	 * 
	 * May save this for later to refresh when deleting a wallet from the list
	 * of wallets on this accounts page. 
	 */
	public void refreshListOfUserWallets() {
		this.walletAdapter.notifyDataSetChanged();
		if (this.userWallets.size() == 0){
			this.currencyListView.removeFooterView(this.footer);
		}
	}

	/**
	 * Sets up the data set, the adapter, and the onclick for 
	 * the list view of currencies that the user currently has a 
	 * wallet for.
	 */
	private void initializeCurrencyListView() {
		// Get the values from the manager and initialize the list view from them
		this.userWallets = new ArrayList<ZWCurrencyListItem>();
		for (ZWCoin type : this.walletManager.readAllActivatedTypes()) {
			this.userWallets.add(this.getItemForCoinType(type));
		}
		this.currencyListView = (ListView) 
		this.rootView.findViewById(R.id.listOfUserWallets);

		// The dropshadow at the bottom
		if (this.userWallets.size() > 0){
			this.currencyListView.addFooterView(this.footer, null ,false);
		}
		this.currencyListView.setFooterDividersEnabled(false);

		this.walletAdapter = new ZWCurrencyListAdapter(this.mContext, this.userWallets);
		this.currencyListView.setAdapter(this.walletAdapter);

		this.currencyListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, 
					int position, long id) {
				ZWCurrencyListItem item = (ZWCurrencyListItem) 
						parent.getItemAtPosition(position);

				getZWMainActivity().openWalletView(item.getCoinId());

			}
		});

		//Long click to deactivate wallet
		this.currencyListView.setOnItemLongClickListener(new OnItemLongClickListener(){

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				ZWCurrencyListItem item = (ZWCurrencyListItem) parent.getItemAtPosition(position);
				Bundle b = new Bundle();
				b.putString(ZWCoin.TYPE_KEY, item.getCoinId().getShortTitle());
				b.putInt("ITEM_LOCATION", position);
				getZWMainActivity().alertConfirmation(ZWRequestCodes.DEACTIVATE_WALLET, "Are you sure you want to "
						+ "deactivate this wallet? This will not actually delete any of your stored data, and you can reactivate it at any time.", 
						ZWTags.DEACTIVATE_WALLET, b);
				return true;
			}

		});

	}

	public void removeFromView(int pos){
		this.userWallets.remove(pos);
		this.refreshListOfUserWallets();
		this.toggleAddCurrencyMessage();
	}
	
	public void toggleAddCurrencyMessage(){
		if (this.userWallets.size() <=0){
			this.rootView.findViewById(R.id.add_currency_message).setVisibility(View.VISIBLE);
			this.rootView.findViewById(R.id.total_label).setVisibility(View.GONE);
			this.totalBalance.setVisibility(View.GONE);
		} else {
			this.rootView.findViewById(R.id.add_currency_message).setVisibility(View.GONE);
			this.rootView.findViewById(R.id.total_label).setVisibility(View.VISIBLE);
			this.totalBalance.setVisibility(View.VISIBLE);
		}

	}
	
	private void calculateTotal(){
		BigDecimal total = BigDecimal.ZERO;
		for (int i=0; i< this.walletAdapter.getCount(); i++){
			ZWCurrencyListItem wallet = this.walletAdapter.getItem(i);
			BigDecimal amount = new BigDecimal(wallet.getWalletTotalFiatEquiv());
			total = total.add(amount);
		}
		this.totalBalance.setText(this.fiatType.getSymbol() + total.toPlainString());
	}
	
}

