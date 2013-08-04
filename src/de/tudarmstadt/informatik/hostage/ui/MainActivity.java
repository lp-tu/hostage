package de.tudarmstadt.informatik.hostage.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ToggleButton;
import android.widget.ViewAnimator;
import de.tudarmstadt.informatik.hostage.HoneyListener;
import de.tudarmstadt.informatik.hostage.HoneyService;
import de.tudarmstadt.informatik.hostage.HoneyService.LocalBinder;
import de.tudarmstadt.informatik.hostage.R;

public class MainActivity extends Activity {

	public static final String BROADCAST = "de.tudarmstadt.informatik.hostage.BROADCAST";

	public static final int LIGHT_GREY = 0x01;
	public static final int LIGHT_GREEN = 0x02;
	public static final int LIGHT_RED = 0x03;
	public static final int LIGHT_YELLOW = 0x04;

	private HoneyService mService;

	private ViewAnimator viewAnimator;
	private GestureDetector gestureDetector;

	private Animation animFlipInLR;
	private Animation animFlipOutLR;
	private Animation animFlipInRL;
	private Animation animFlipOutRL;

	private ListView listView;
	private ListViewAdapter adapter;

	private boolean running;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initViewAnimator();
		initListView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onStart() {
		super.onStart();
		registerReceiver();
		if (isServiceRunning()) {
			bindService(getServiceIntent(), mConnection, BIND_ABOVE_CLIENT);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (isServiceRunning()) {
			unbindService(mConnection);
		}
		unregisterReceiver();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	public void buttonOnOffClick(View view) {
		if (((ToggleButton) view).isChecked()) {
			startAndBind();
		} else {
			stopAndUnbind();
			running = false;
		}
	}

	private void startAndBind() {
		startService(getServiceIntent());
		bindService(getServiceIntent(), mConnection, BIND_ABOVE_CLIENT);
	}

	private void stopAndUnbind() {
		mService.stopListeners();
		unbindService(mConnection);
		stopService(getServiceIntent());
	}

	private boolean isParanoid() {
		return ((CheckBox) findViewById(R.id.checkBoxParanoid)).isChecked();
	}

	private void initViewAnimator() {
		viewAnimator = (ViewAnimator) findViewById(R.id.viewAnimator);
		gestureDetector = new GestureDetector(this, simpleOnGestureListener);

		animFlipInLR = AnimationUtils.loadAnimation(this,
				R.anim.in_left_to_right);
		animFlipOutLR = AnimationUtils.loadAnimation(this,
				R.anim.out_left_to_right);
		animFlipInRL = AnimationUtils.loadAnimation(this,
				R.anim.in_right_to_left);
		animFlipOutRL = AnimationUtils.loadAnimation(this,
				R.anim.out_right_to_left);
	}

	private void initListView() {
		ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
		for (String protocol : getResources().getStringArray(R.array.protocols)) {
			HashMap<String, String> d = new HashMap<String, String>();
			d.put("light", String.valueOf(R.drawable.light_grey));
			d.put("protocol", protocol);
			d.put("connections", "-");
			data.add(d);
		}
		listView = (ListView) findViewById(R.id.listViewProtocols);
		adapter = new ListViewAdapter(getLayoutInflater(), data);
		listView.setAdapter(adapter);
		listView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return gestureDetector.onTouchEvent(event);
			}

		});
		listView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String protocolName = ((HashMap<String, String>) adapter
						.getItem(position)).get("protocol");
				if (isServiceRunning()) {
					mService.toggleListener(protocolName);
				}
			}

		});
	}

	private void swipeRightToLeft() {
		if (viewAnimator.getDisplayedChild() == 0) {
			viewAnimator.setInAnimation(animFlipInRL);
			viewAnimator.setOutAnimation(animFlipOutRL);
			viewAnimator.setDisplayedChild(1);
		}
	}

	private void swipeLeftToRight() {
		if (viewAnimator.getDisplayedChild() == 1) {
			viewAnimator.setInAnimation(animFlipInLR);
			viewAnimator.setOutAnimation(animFlipOutLR);
			viewAnimator.setDisplayedChild(0);
		}
	}

	SimpleOnGestureListener simpleOnGestureListener = new SimpleOnGestureListener() {
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			float sensitvity = 50;
			if ((e1.getX() - e2.getX()) > sensitvity) {
				swipeRightToLeft();
			} else if ((e2.getX() - e1.getX()) > sensitvity) {
				swipeLeftToRight();
			}

			return true;
		}
	};

	private boolean isServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (service.service.getClassName().equals(
					HoneyService.class.getName())) {
				return true;
			}
		}
		return false;
	}

	private Intent getServiceIntent() {
		return new Intent(this, HoneyService.class);
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = ((LocalBinder) service).getService();
			if (!running) {
				if (isParanoid()) {
					mService.startListeners();
				} else {
					mService.startListener("SMB");
				}
			}
			running = true;
			updateUI();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}

	};

	private void registerReceiver() {
		LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,
				new IntentFilter(BROADCAST));
	}

	private void unregisterReceiver() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			updateUI();
		}

	};

	private void updateUI() {
		boolean activeListeners = false;
		boolean activeHandlers = false;

		for (HoneyListener listener : mService.getListeners()) {
			if (listener.isRunning()) {
				activeListeners = true;
				if (listener.getHandlerCount() == 0) {
					updateProtocolLight(LIGHT_GREEN, listener.getProtocolName());
				} else {
					activeHandlers = true;
					updateProtocolLight(LIGHT_RED, listener.getProtocolName());
				}
				updateProtocolConnections(listener.getHandlerCount(),
						listener.getProtocolName());
			} else {
				updateProtocolLight(LIGHT_GREY, listener.getProtocolName());
			}
		}

		if (activeListeners) {
			if (activeHandlers) {
				updateStatusLight(LIGHT_RED);
			} else {
				updateStatusLight(LIGHT_GREEN);
			}
			((ToggleButton) findViewById(R.id.toggleButtonOnOff))
					.setChecked(true);
			findViewById(R.id.checkBoxParanoid).setEnabled(false);
		} else {
			updateStatusLight(LIGHT_GREY);
			((ToggleButton) findViewById(R.id.toggleButtonOnOff))
					.setChecked(false);
			findViewById(R.id.checkBoxParanoid).setEnabled(true);
		}
	}

	private void updateStatusLight(int light) {
		switch (light) {
		case LIGHT_GREY:
			((ImageView) findViewById(R.id.imageViewLight))
					.setImageResource(R.drawable.light_grey_large);
			break;
		case LIGHT_GREEN:
			((ImageView) findViewById(R.id.imageViewLight))
					.setImageResource(R.drawable.light_green_large);
			break;
		case LIGHT_RED:
			((ImageView) findViewById(R.id.imageViewLight))
					.setImageResource(R.drawable.light_red_large);
			break;
		case LIGHT_YELLOW:
			((ImageView) findViewById(R.id.imageViewLight))
					.setImageResource(R.drawable.light_yellow_large);
			break;
		}
	}

	private void updateProtocolLight(int light, String protocolName) {
		for (int i = 0; i < adapter.getCount(); ++i) {
			HashMap<String, String> d = ((HashMap<String, String>) adapter
					.getItem(i));
			if (d.get("protocol").equals(protocolName)) {
				switch (light) {
				case LIGHT_GREY:
					d.put("light", String.valueOf(R.drawable.light_grey));
					d.put("connections", "-");
					break;
				case LIGHT_GREEN:
					d.put("light", String.valueOf(R.drawable.light_green));
					d.put("connections", "0");
					break;
				case LIGHT_RED:
					d.put("light", String.valueOf(R.drawable.light_red));
					break;
				case LIGHT_YELLOW:
					d.put("light", String.valueOf(R.drawable.light_yellow));
					d.put("connections", "0");
					break;
				}
			}
		}
		adapter.notifyDataSetChanged();
	}

	private void updateProtocolConnections(int connections, String protocolName) {
		for (int i = 0; i < adapter.getCount(); ++i) {
			HashMap<String, String> d = ((HashMap<String, String>) adapter
					.getItem(i));
			if (d.get("protocol").equals(protocolName)) {
				d.put("connections", String.valueOf(connections));
			}
		}
		adapter.notifyDataSetChanged();
	}

}
