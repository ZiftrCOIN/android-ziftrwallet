package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public abstract class OWSearchableListAdapter<T extends OWSearchableListItem> extends ArrayAdapter<T> implements Filterable {
	private Context context;
	private LayoutInflater inflater;
	private List<T> workingList;
	private List<T> fullList;
	
	public OWSearchableListAdapter(Context ctx, List<T> txList) {
		super(ctx, 0, txList);
		this.setInflater(LayoutInflater.from(ctx));
		this.setContext(ctx);
		this.workingList = txList;
		this.fullList = new ArrayList<T>();
		this.fullList.addAll(this.workingList);
	}
	
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

	public List<T> getWorkingList() {
		return this.workingList;
	}
	
	public List<T> getFullList() {
		return this.fullList;
	}
	
	public void refreshWorkingList() {
		this.workingList.clear();
		this.workingList.addAll(this.fullList);
	}
	
	@SuppressLint("DefaultLocale")
	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// Seems a little hacky, but we don't use the filter results at all
				workingList.clear();
				
				for (T item : fullList) {
					if (item.matches(constraint)) {
						workingList.add(item);
					}
				}
				
				return null;
			}

			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				notifyDataSetChanged();
			}

		};
		return filter;
	}
	
}