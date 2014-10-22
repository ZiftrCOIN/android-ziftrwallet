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
	private Filter filter;

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

	public void refreshWorkingList(boolean applySort) {
		this.workingList.clear();
		if (applySort) {
			this.applySortToFullList();
		}
		this.workingList.addAll(this.fullList);
		getFilter().filter(filterConstraints);
	}
	
	public abstract void applySortToFullList();
	
	@Override
	public Filter getFilter() {
		if (this.filter == null) {
			this.filter = new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					// Seems a little hacky that we don't use the filter results at all
					List<T> reversedSearchResult = new ArrayList<T>();

					filterConstraints = constraint;

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
					return fr;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					workingList.clear();
					workingList.addAll((List<T>) results.values); 
					notifyDataSetChanged();
				}

			};
		}
		return this.filter;
	}

	@Override
	public int getCount() {
		return this.workingList.size();
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