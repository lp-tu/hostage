package de.tudarmstadt.informatik.hostage.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.tudarmstadt.informatik.hostage.R;

public class ListViewAdapter extends BaseAdapter {

	private LayoutInflater inflater;
	private ArrayList<HashMap<String, String>> data;

	public ListViewAdapter(LayoutInflater inflater,
			ArrayList<HashMap<String, String>> data) {
		this.inflater = inflater;
		this.data = data;
	}

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public Object getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			v = inflater.inflate(R.layout.list_view_protocols_row, null);
		}

		ImageView light = (ImageView) v.findViewById(R.id.imageViewLight);
		TextView protocol = (TextView) v.findViewById(R.id.textViewProtocol);
		TextView connections = (TextView) v
				.findViewById(R.id.textViewConnectionsValue);

		HashMap<String, String> d = new HashMap<String, String>();
		d = data.get(position);

		light.setImageResource(Integer.valueOf(d.get("light")).intValue());
		protocol.setText(d.get("protocol"));
		connections.setText(d.get("connections"));

		return v;
	}
}
