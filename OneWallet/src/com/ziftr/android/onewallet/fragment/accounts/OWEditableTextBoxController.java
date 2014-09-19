package com.ziftr.android.onewallet.fragment.accounts;

import android.content.res.Resources;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.ziftr.android.onewallet.R;

public class OWEditableTextBoxController<T> implements OnClickListener {

	public interface EditHandler<T> {
		public void onEditStart(T t);
		public void onEditEnd(String newText, T t);
		public boolean isInEditingMode();
		public boolean isEditing(T t);
		public Resources getResources();
	}

	private EditHandler<T> handler;

	private TextView textView;
	private ImageView imgView;
	
	private T returnData;

	public OWEditableTextBoxController(EditHandler<T> handler, TextView textView, 
			ImageView imgView, String initialText, T returnData) {
		this.handler = handler;
		this.textView = textView;
		this.imgView = imgView;
		this.textView.setText(initialText);
		
		this.returnData = returnData;

		// This is necessary because on screen rotates we need
		// to initialze correctly
		this.toggleEditNote(!handler.isEditing(this.returnData));
	}

	/**
	 * 
	 * @param txNote - EditText for note view
	 * @param imgView - ImageView for edit button
	 * @param toggleOff - boolean to determine how to toggle
	 */
	public void toggleEditNote(boolean toggleOff) {
		if (toggleOff) {
			this.textView.setFocusable(false);
			this.textView.setFocusableInTouchMode(false);
			this.textView.setBackgroundColor(handler.getResources().getColor(R.color.transparent));
			this.textView.setPadding(0, 0, 0, 0);
			imgView.setImageResource(R.drawable.edit_enabled);
			
			handler.onEditEnd(this.textView.getText().toString(), this.returnData);
		} else {
			this.textView.setBackgroundResource(android.R.drawable.editbox_background_normal);
			this.textView.setFocusable(true);
			this.textView.setFocusableInTouchMode(true);
			this.textView.requestFocus();
			imgView.setImageResource(R.drawable.close_enabled);
			
			handler.onEditStart(this.returnData);
		}
	}

	@Override
	public void onClick(View v) {
		if (v == this.imgView) {
			// If the handler is already editing then we 
			if (!handler.isInEditingMode() || handler.isEditing(this.returnData)) {
				toggleEditNote(handler.isEditing(this.returnData));
			}
		}
	}

}