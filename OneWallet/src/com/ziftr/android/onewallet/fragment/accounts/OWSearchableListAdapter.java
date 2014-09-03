package com.ziftr.android.onewallet.fragment.accounts;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Filterable;

public abstract class OWSearchableListAdapter extends ArrayAdapter<OWWalletTransaction> implements Filterable {
	private Context context;
	private LayoutInflater inflater;
	private List<OWWalletTransaction> workingTxList;
	
	public OWSearchableListAdapter(Context ctx, List<OWWalletTransaction> txList) {
		super(ctx, 0, txList);
		this.setInflater(LayoutInflater.from(ctx));
		this.setContext(ctx);
		this.workingTxList = txList;
	}
	
	public abstract void refreshWorkingList();
	
	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	public LayoutInflater getInflater() {
		return inflater;
	}

	public void setInflater(LayoutInflater inflater) {
		this.inflater = inflater;
	}

	public List<OWWalletTransaction> getWorkingTxList() {
		return workingTxList;
	}
	
}