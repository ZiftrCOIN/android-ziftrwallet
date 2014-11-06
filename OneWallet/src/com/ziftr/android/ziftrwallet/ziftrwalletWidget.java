package com.ziftr.android.ziftrwallet;

import java.math.BigDecimal;
import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.ziftr.android.ziftrwallet.sqlite.OWSQLiteOpenHelper;
import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;
import com.ziftr.android.ziftrwallet.util.ZiftrUtils;

public class ziftrwalletWidget extends AppWidgetProvider{
	
	public static final String WIDGET_SEND = "widgetSendButton";
	public static final String WIDGET_RECEIVE = "widgetReceiveButton";
	public static final String WIDGET_CURR = "widgetCurrencyButton";
	
	private static final int send_intent_requestcode = 0;
	private static final int receive_intent_requestcode = 1;
	private static final int curr_requestcode = 2;
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
		
		for (int i=0; i<appWidgetIds.length; i++){
			initButtons(context, views);
			changeCurrency(context, views);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
		super.onReceive(context, intent);
		if (WIDGET_CURR.equals(intent.getAction())){
	        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
	        changeCurrency(context, views);
	        ComponentName cn = new ComponentName(context, ziftrwalletWidget.class);
	        initButtons(context, views);
	        AppWidgetManager.getInstance(context).updateAppWidget(cn, views);
		}
	}
	
	private void changeCurrency(Context context, RemoteViews views){
		List<OWCoin> coins = OWWalletManager.getInstance().getAllSetupWalletTypes();
		if (coins.size() >0) {
			views.setViewVisibility(R.id.no_wallets, View.GONE);
			views.setViewVisibility(R.id.widget_select_coin, View.VISIBLE);
			views.setViewVisibility(R.id.widget_coin, View.VISIBLE);
			views.setViewVisibility(R.id.widget_balance, View.VISIBLE);

			if (OWPreferencesUtils.getWidgetCoin(context) == null){
				OWPreferencesUtils.setWidgetCoin(context, OWWalletManager.getInstance().getAllSetupWalletTypes().get(0).toString());
			}
			
			int next = (coins.indexOf(OWCoin.valueOf(OWPreferencesUtils.getWidgetCoin(context))) + 1) % coins.size();
			OWPreferencesUtils.setWidgetCoin(context, coins.get(next).toString());
			OWCoin selectedCurr = coins.get(next);
			views.setViewVisibility(R.id.widget_select_coin, View.VISIBLE);
			views.setImageViewResource(R.id.widget_select_coin, selectedCurr.getLogoResId());
			views.setTextViewText(R.id.widget_coin, selectedCurr.getLongTitle());
			BigDecimal balance = ZiftrUtils.bigIntToBigDec(selectedCurr, OWWalletManager.getInstance().getWalletBalance(selectedCurr, OWSQLiteOpenHelper.BalanceType.AVAILABLE));
			views.setTextViewText(R.id.widget_balance, selectedCurr.getFormattedAmount(balance));
		} else {
			OWPreferencesUtils.setWidgetCoin(context, null);
			views.setViewVisibility(R.id.widget_select_coin, View.INVISIBLE);
			views.setViewVisibility(R.id.widget_coin, View.INVISIBLE);
			views.setViewVisibility(R.id.widget_balance, View.INVISIBLE);
			views.setViewVisibility(R.id.no_wallets, View.VISIBLE);
		}
	}
	
	private void initButtons(Context context, RemoteViews views){

		Intent intent_send = new Intent(context, OWMainFragmentActivity.class);
		intent_send.putExtra(WIDGET_SEND, true);
		intent_send.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		PendingIntent pendingIntent_send = PendingIntent.getActivity(context, send_intent_requestcode, intent_send, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_send, pendingIntent_send);

		Intent intent_receive = new Intent(context, OWMainFragmentActivity.class);
		intent_receive.putExtra(WIDGET_RECEIVE, true);
		intent_receive.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

		PendingIntent pendingIntent_receive = PendingIntent.getActivity(context, receive_intent_requestcode, intent_receive, PendingIntent.FLAG_CANCEL_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_receive, pendingIntent_receive);

		Intent intent = new Intent(context, ziftrwalletWidget.class);
		intent.setAction(WIDGET_CURR);
		PendingIntent pendingIntent_curr = PendingIntent.getBroadcast(context, curr_requestcode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		views.setOnClickPendingIntent(R.id.widget_change_coin, pendingIntent_curr);

	}
	
	
}
