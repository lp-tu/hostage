package de.tudarmstadt.informatik.hostage.system;

import java.io.FileDescriptor;
import java.io.IOException;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

public class NativeBridge implements Runnable {

	private final static NativeBridge INSTANCE = new NativeBridge();

	public NativeBridge getInstance() {
		return INSTANCE;
	}

	private final static LocalSocket SOCK = new LocalSocket();

	private NativeBridge() {
		try {
			SOCK.bind(new LocalSocketAddress("hostage"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		try {
			Process p = Runtime.getRuntime().exec(
					new String[] { "/data/local/p", "21" });
			p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static FileDescriptor getFD(int port) {
		new Thread(INSTANCE).start();
		try {
			FileDescriptor[] fds = SOCK.getAncillaryFileDescriptors();
			return fds[0];
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
