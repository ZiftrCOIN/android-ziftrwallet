package com.ziftr.android.onewallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public abstract class OWSearchableListAdapter<T extends OWSearchableListItem> extends ArrayAdapter<T> implements Filterable {
	
	private Context context;
	private LayoutInflater inflater;
	
	/** This is the list actually used for display purposes. */
	private List<T> workingList;
	
	/** This is the list of all items which can be filtered and added to the working list. */
	private List<T> fullList;

	public OWSearchableListAdapter(Context ctx, int resource, List<T> txList) {
		super(ctx, resource, txList);
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

	public List<T> getFullList() {
		return this.fullList;
	}

	public void refreshWorkingList() {
		this.workingList.clear();
		this.workingList.addAll(this.fullList);
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// Seems a little hacky that we don't use the filter results at all
				List<T> reversedSearchResult = new ArrayList<T>();

				for (int i = (fullList.size() - 1); i >= 0; i--) {
					T item = fullList.get(i);
					T nextItem = reversedSearchResult.size() <= 0 ? null : 
						reversedSearchResult.get(reversedSearchResult.size() - 1);
					if (item.matches(constraint, nextItem)) {
						reversedSearchResult.add(item);
					}
				}

				workingList.clear();
				for (int i = (reversedSearchResult.size() - 1); i >= 0; i--) {
					workingList.add(reversedSearchResult.get(i));
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