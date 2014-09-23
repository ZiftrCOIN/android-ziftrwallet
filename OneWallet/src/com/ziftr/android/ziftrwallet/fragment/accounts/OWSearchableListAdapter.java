package com.ziftr.android.ziftrwallet.fragment.accounts;

import java.util.ArrayList;
import java.util.List;

import com.ziftr.android.ziftrwallet.util.ZLog;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;

public abstract class OWSearchableListAdapter<T extends OWSearchableListItem> extends BaseAdapter implements Filterable {
	
	private Context context;
	private LayoutInflater inflater;
	
	/** This is the list actually used for display purposes. */
	private List<T> workingList;
	
	/** This is the list of all items which can be filtered and added to the working list. */
	private List<T> fullList;

	private CharSequence filterConstraints = "";
	
	public OWSearchableListAdapter(Context ctx, int resource, List<T> txList) {
		//super(ctx, resource, txList);
		super();
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
		
		getFilter().filter(filterConstraints);
		
		ZLog.log("Full list size: ", String.valueOf(fullList.size()));
		ZLog.log("Working list size: ", String.valueOf(workingList.size()));
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				// Seems a little hacky that we don't use the filter results at all
				List<T> reversedSearchResult = new ArrayList<T>();

				filterConstraints = constraint;
				ZLog.log("Filtering - full list size: " + fullList.size());
				
				for (int i = (fullList.size() - 1); i >= 0; i--) {
					T item = fullList.get(i);
					T nextItem = reversedSearchResult.size() <= 0 ? null : 
						reversedSearchResult.get(reversedSearchResult.size() - 1);
					if (item.matches(constraint, nextItem)) {
						reversedSearchResult.add(item);
					}
				}

				List<T> searchResults = new ArrayList<T>();
				for (int i = (reversedSearchResult.size() - 1); i >= 0; i--) {
					searchResults.add(reversedSearchResult.get(i));
				}
				
				FilterResults fr = new FilterResults();
				fr.count = searchResults.size();
				fr.values = searchResults;
				ZLog.log("Search results: " + searchResults.size());
				return fr;
			}

			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint,
					FilterResults results) {
				ZLog.log("Publishing filter results.");
				ZLog.log("Filter constraint: -", constraint, "-");
				ZLog.log("Before filter size: " + workingList.size());
				workingList.clear();
				workingList.addAll((List<T>) results.values); 
				ZLog.log("After filter size: ", String.valueOf(workingList.size()));
				notifyDataSetChanged();
			}

		};
		return filter;
	}

	@Override
	public int getCount() {
		int count = this.workingList.size();
		ZLog.log("Adapter count: ", String.valueOf(count));
		return count;
	}

	@Override
	public T getItem(int position) {
		return this.workingList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}


}