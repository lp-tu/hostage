package de.tudarmstadt.informatik.hostage;

import java.util.ArrayList;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import de.tudarmstadt.informatik.hostage.logging.FileLogger;
import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.ui.MainActivity;

public class HoneyService extends Service {

	private ArrayList<HoneyListener> listeners = new ArrayList<HoneyListener>();

	public List<HoneyListener> getListeners() {
		return listeners;
	}

	private Logger log;

	public Logger getLog() {
		return log;
	}

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public HoneyService getService() {
			return HoneyService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		log = new FileLogger(getApplicationContext());
		createNotification();
		for (Protocol protocol : getProtocolArray()) {
			listeners.add(new HoneyListener(this, protocol));
		}
	}

	@Override
	public void onDestroy() {
		cancelNotification();
		log.close();
		super.onDestroy();
	}

	private void createNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText("Honeypot up and running!");
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		stackBuilder.addParentStack(MainActivity.class);
		stackBuilder.addNextIntent(new Intent(this, MainActivity.class));
		PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
				PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.notify(1, builder.build());
	}

	private void cancelNotification() {
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mNotificationManager.cancel(1);
	}

	private ArrayList<Protocol> getProtocolArray() {
		String[] protocols = getResources().getStringArray(R.array.protocols);
		String packageName = Protocol.class.getPackage().getName();
		ArrayList<Protocol> protocolArray = new ArrayList<Protocol>();

		for (String protocol : protocols) {
			try {
				protocolArray.add((Protocol) Class.forName(
						String.format("%s.%s", packageName, protocol))
						.newInstance());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return protocolArray;
	}

	public void notifyUI() {
		Intent intent = new Intent(MainActivity.BROADCAST);
		LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
	}

	public void startListeners() {
		for (HoneyListener listener : listeners) {
			listener.start();
		}
	}

	public void stopListeners() {
		for (HoneyListener listener : listeners) {
			listener.stop();
		}
	}

	public void startListener(String protocolName) {
		for (HoneyListener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName)) {
				listener.start();
			}
		}
	}

	public void stopListener(String protocolName) {
		for (HoneyListener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName)) {
				listener.stop();
			}
		}
	}

	public void toggleListener(String protocolName) {
		for (HoneyListener listener : listeners) {
			if (listener.getProtocolName().equals(protocolName)) {
				if (listener.isRunning()) {
					stopListener(protocolName);
				} else {
					startListener(protocolName);
				}
			}
		}
	}

}
