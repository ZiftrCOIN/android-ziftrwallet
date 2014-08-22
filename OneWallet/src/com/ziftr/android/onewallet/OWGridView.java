package com.ziftr.android.onewallet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.GridView;

public class OWGridView extends GridView{

	public OWGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public OWGridView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public OWGridView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Hack to let ScrollView have GridView child
	 */
	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// HACK! TAKE THAT ANDROID!
		// Calculate entire height by providing a very large height hint.
		// View.MEASURED_SIZE_MASK represents the largest height possible.
		int expandSpec = MeasureSpec.makeMeasureSpec(MEASURED_SIZE_MASK,
				MeasureSpec.AT_MOST);
		super.onMeasure(widthMeasureSpec, expandSpec);

		ViewGroup.LayoutParams params = getLayoutParams();
		params.height = getMeasuredHeight() + 100;
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}
}
