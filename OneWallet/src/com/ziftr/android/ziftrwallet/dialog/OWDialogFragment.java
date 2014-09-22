package com.ziftr.android.ziftrwallet.dialog;


import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.ziftr.android.ziftrwallet.OWMainFragmentActivity;
import com.ziftr.android.ziftrwallet.R;
import com.ziftr.android.ziftrwallet.util.ZLog;


public abstract class OWDialogFragment extends DialogFragment implements View.OnClickListener {

	/** The title of the dialog. */
	protected String title;
	/** The message of the dialog. */
	protected String message;
	/** The positive button's text. */
	protected String positiveButtonText;
	/** The neutral button's text. */
	protected String neutralButtonText;
	/** The negative button's text. */
	protected String negativeButtonText;

	/** The key to save the Dialog's title text in the bundle. */
	private static final String TITLE_KEY = "title";
	/** The key to save the Dialog's message text in the bundle. */
	private static final String MESSAGE_KEY = "message";
	/** The key to save the positive button text in the bundle. */
	private static final String POSITIVE_BUTTON_TEXT_KEY = "positive";
	/** The key to save the neutral button text in the bundle. */
	private static final String NEUTRAL_BUTTON_TEXT_KEY = "neutral";
	/** The key to save the negative button text in the bundle. */
	private static final String NEGATIVE_BUTTON_TEXT_KEY = "negative";

	/** The view for this dialog. */
	private View dialogView;

	public static final String REQUEST_CODE_KEY = "request_code";

	/**
	 * Set's up a basic dialog with all of its content.
	 * 
	 * @param title - The Dialog's title.
	 * @param message - The Dialog's message/instructions to user.
	 * @param posButtonText - The 
	 * @param negButtonText
	 */
	public void setupDialog(String title, String message, 
			String posButtonText, String neuButtonText, String negButtonText) {
		this.title = title;
		this.message = message;
		this.positiveButtonText = posButtonText;
		this.neutralButtonText = neuButtonText;
		this.negativeButtonText = negButtonText;
	}

	/**
	 * Creates and returns the dialog to show the user.
	 * Sets all the basic text fields that all dialogs have 
	 * to be what this diaglog was set up with. 
	 */
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder builder = this.createBuilder(savedInstanceState);
		return builder.create();
	}
	


	/**
	 * Makes a builder for a basic dialog with all of the common
	 * dialog elements (title, message, etc).
	 * 
	 * @param savedInstanceState - The saved instance state to build from.
	 * @return The new builder
	 */
	public AlertDialog.Builder createBuilder(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			if (title == null) {
				title = savedInstanceState.getString(TITLE_KEY);
			}
			if (message == null) {
				message = savedInstanceState.getString(MESSAGE_KEY);
			}
			if (positiveButtonText == null) {
				positiveButtonText = savedInstanceState.getString(
						POSITIVE_BUTTON_TEXT_KEY);
			}
			if (neutralButtonText == null) {
				neutralButtonText = savedInstanceState.getString(
						NEUTRAL_BUTTON_TEXT_KEY);
			}
			if (negativeButtonText == null) {
				negativeButtonText = savedInstanceState.getString(
						NEGATIVE_BUTTON_TEXT_KEY);
			}
		}
		
		AlertDialog.Builder builder = 
				new AlertDialog.Builder(this.getActivity());

		return builder;
	}
	/**
	 * Method to set the title and message text fields for the dialogs.
	 */
	public void initDialogFields() {
		View view = this.getDialogView();
		//setTitle
		TextView titlefield = (TextView) view.findViewById(R.id.dialog_title);
		titlefield.setText(this.title);
		//setMessage
		if (this.message != null) {
			TextView messagefield = (TextView) view.findViewById(R.id.dialog_message);
			messagefield.setText(this.message);
		}

	}	


	/**
	 * When we save the instance, we put the four basic text fields
	 * in the bundle.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		// Save all of the important strings in the button
		outState.putString(TITLE_KEY, this.title);
		outState.putString(MESSAGE_KEY, this.message);
		outState.putString(POSITIVE_BUTTON_TEXT_KEY, this.positiveButtonText);
		outState.putString(NEUTRAL_BUTTON_TEXT_KEY, this.neutralButtonText);
		outState.putString(NEGATIVE_BUTTON_TEXT_KEY, this.negativeButtonText);
		super.onSaveInstanceState(outState);
	}

	/**
	 * @return the dialogView
	 */
	public View getDialogView() {
		return dialogView;
	}

	/**
	 * @param dialogView the dialogView to set for this dialog
	 */
	public void setDialogView(View dialogView) {
		this.dialogView = dialogView;
	}

	/**
	 * If this is a dialog that is specific to a particular fragment, then we
	 * return this.getTargetFragment();
	 * Otherwise, this is a dialog that is not specific to a particular fragment and
	 * is handled in the activity. In this case we should return this.getActivity();
	 * 
	 * @return
	 */
	protected abstract Object getHandler();

	/**
	 * OWDialogs must have their own respective handlers. When
	 * the activity is attached, this method should be called in
	 * order to verify that either the target fragment or the 
	 * activity is a handler of the type specified (by the c param).
	 * 
	 * @param c - The handler class for this type of dialog.
	 */
	protected void validateHandler(Class<?> c) {
		if (!(c.isAssignableFrom(this.getHandler().getClass()))) {
			throw new ClassCastException(this.getHandler().getClass().toString() + 
					" must implement " + c.toString() + 
					" to be able to use this kind of dialog.");
		}
	}

	/**
	 * Sets a specified EditText to have the string setVal in it's 
	 * writable field. 
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the EditText to set the value in.
	 * @param setVal - The value to set it to.
	 * @return as above.
	 */
	protected void setStringInEditText(int id, String setVal) {
		EditText textBox = (EditText) this.getDialogView().findViewById(id);
		textBox.setText(setVal);
	}

	/**
	 * Given a resource id, this method finds the correpsonding
	 * view and then gets the text from from that view, returning 
	 * the value in a string.
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the view to find.
	 * @return as above
	 */
	protected String getStringFromEditText(int id) {
		EditText textBox = (EditText) this.getDialogView().findViewById(id);
		return textBox.getText().toString();
	}

	/**
	 * Given a resource id, this method finds the corresponding
	 * view and then gets the text from from that view, returning 
	 * the value in bytes.
	 * Will throw a null pointer if no such view with id.
	 * Will thow a class cast if view is not an EditText.
	 * 
	 * @param id - The id of the view to find.
	 * @return as above
	 */
	protected byte[] getBytesFromEditText(int id) {
		return this.getStringFromEditText(id).getBytes();
	}

	@Override
	public void onStart() {
		//ignore this if showing dialog in welcome activity
		if (this.getActivity().getClass().getSimpleName().equals("OWMainFragmentActivity"))
			((OWMainFragmentActivity) this.getActivity()).setShowingDialog(true);
		super.onStart();
	}

	@Override
	public void onDetach() {
		//ignore this if showing dialog in welcome activity
		if (this.getActivity().getClass().getSimpleName().equals("OWMainFragmentActivity"))
			((OWMainFragmentActivity) this.getActivity()).setShowingDialog(false);
		super.onDetach();
	}
	
	/**
	 * @return the requestCode
	 */
	public int getRequestCode() {
		Bundle args = this.getArguments();
		if (args != null && args.getInt(REQUEST_CODE_KEY, -1) != -1) {
			return args.getInt(REQUEST_CODE_KEY);	
		}
		ZLog.log("No request code set, that's a problem!");
		return 0;
	}

}