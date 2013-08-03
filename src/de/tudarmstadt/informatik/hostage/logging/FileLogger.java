package de.tudarmstadt.informatik.hostage.logging;

import java.io.FileOutputStream;

import android.content.Context;

public class FileLogger implements Logger {

	private FileOutputStream log = null;

	public FileLogger(Context context) {
		try {
			log = context.openFileOutput("hostage.log", Context.MODE_APPEND);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void write(Record record) {
		if (log != null) {
			try {
				log.write((record.toString() + "\n").getBytes());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void close() {
		if (log != null) {
			try {
				log.flush();
				log.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
