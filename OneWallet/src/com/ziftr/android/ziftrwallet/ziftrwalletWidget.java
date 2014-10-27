package com.ziftr.android.ziftrwallet;

import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.OWPreferencesUtils;

public class ziftrwalletWidget extends AppWidgetProvider{
	
	public static final String WIDGET_SEND = "widgetSendButton";
	public static final String WIDGET_RECEIVE = "widgetReceiveButton";
	public static final String WIDGET_CURR = "widgetCurrencyButton";
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		for (int i=0; i<appWidgetIds.length; i++){
			RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

			Intent intent_send = new Intent(context, OWMainFragmentActivity.class);
			intent_send.putExtra(WIDGET_SEND, true);
			PendingIntent pendingIntent_send = PendingIntent.getActivity(context, 0, intent_send, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_send, pendingIntent_send);

			Intent intent_receive = new Intent(context, OWMainFragmentActivity.class);
			intent_receive.putExtra(WIDGET_RECEIVE, true);
			PendingIntent pendingIntent_receive = PendingIntent.getActivity(context, 0, intent_receive, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_receive, pendingIntent_receive);

			Intent intent = new Intent(context, ziftrwalletWidget.class);
			intent.setAction(WIDGET_CURR);
			PendingIntent pendingIntent_curr = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_change_coin, pendingIntent_curr);
			
			if (OWPreferencesUtils.getWidgetCoin(context) == null){
				OWPreferencesUtils.setWidgetCoin(context, OWWalletManager.getInstance().getAllSetupWalletTypes().get(0).toString());
			}
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
	        AppWidgetManager.getInstance(context).updateAppWidget(cn, views);
		}
	}
	
	
	private void changeCurrency(Context context, RemoteViews views){
		List<OWCoin> coins = OWWalletManager.getInstance().getAllSetupWalletTypes();
		if (coins.size() >0) {
			int next = (coins.indexOf(OWCoin.valueOf(OWPreferencesUtils.getWidgetCoin(context))) + 1) % coins.size();
			OWPreferencesUtils.setWidgetCoin(context, coins.get(next).toString());
			OWCoin selectedCurr = coins.get(next);
			views.setViewVisibility(R.id.widget_select_coin, View.VISIBLE);
			views.setImageViewResource(R.id.widget_select_coin, selectedCurr.getLogoResId());
			views.setTextViewText(R.id.widget_coin, selectedCurr.getLongTitle());
		} else {
			views.setViewVisibility(R.id.widget_select_coin, View.INVISIBLE);
			views.setTextViewText(R.id.widget_coin, "No Wallets");
		}
	}
	
	
}
