package com.ziftr.android.ziftrwallet;

import java.util.List;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.ziftr.android.ziftrwallet.util.OWCoin;
import com.ziftr.android.ziftrwallet.util.ZLog;

public class ziftrwalletWidget extends AppWidgetProvider{
	
	private static final String WIDGET_SEND = "widgetSendButton";
	private static final String WIDGET_RECEIVE = "widgetReceiveButton";
	private static final String WIDGET_CURR = "widgetCurrencyButton";
	
	//TODO replace this with a preference
	private static OWCoin selectedCurr;
	
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds){
		for (int i=0; i<appWidgetIds.length; i++){
			Intent intent = new Intent(context, ziftrwalletWidget.class);
			intent.setAction(WIDGET_SEND);
			PendingIntent pendingIntent_send = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			views.setOnClickPendingIntent(R.id.widget_send, pendingIntent_send);
			intent.setAction(WIDGET_RECEIVE);
			PendingIntent pendingIntent_receive = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_receive, pendingIntent_receive);
			intent.setAction(WIDGET_CURR);
			PendingIntent pendingIntent_curr = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			views.setOnClickPendingIntent(R.id.widget_change_coin, pendingIntent_curr);
			changeCurrency(views);
            appWidgetManager.updateAppWidget(appWidgetIds[i], views);
		}
	}
	
	@Override
	public void onReceive(Context context, Intent intent){
		super.onReceive(context, intent);
		if (WIDGET_SEND.equals(intent.getAction())){
			ZLog.log("ABAaRRA");
		} else if (WIDGET_RECEIVE.equals(intent.getAction())){
			ZLog.log("ABAaRRA2");
		} else if (WIDGET_CURR.equals(intent.getAction())){
	        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
	        changeCurrency(views);
	        ComponentName cn = new ComponentName(context, ziftrwalletWidget.class);
	        AppWidgetManager.getInstance(context).updateAppWidget(cn, views);
		}
	}
	
	
	private void changeCurrency(RemoteViews views){
		List<OWCoin> coins = OWWalletManager.getInstance().getAllSetupWalletTypes();
		if (coins.size() >0) {
			int next = (coins.indexOf(selectedCurr) + 1) % coins.size();
			this.selectedCurr = coins.get(next);
			views.setImageViewResource(R.id.widget_select_coin, selectedCurr.getLogoResId());
			views.setTextViewText(R.id.widget_coin, selectedCurr.getLongTitle());
		} else {
			views.setTextViewText(R.id.widget_coin, "No Wallets");
		}
	}
}
