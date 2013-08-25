package de.tudarmstadt.informatik.hostage.system;

import java.io.FileDescriptor;

import android.net.LocalServerSocket;
import android.net.LocalSocket;

public class PrivilegedPort implements Runnable {

	private final static String NAME = "hostage";

	private int port;

	public PrivilegedPort(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		Process p;
		try {
			String command = String.format("/data/local/p %d", port);
			p = Runtime.getRuntime().exec(new String[] { "su", "-c", command });
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public FileDescriptor bindAndGetFD() {
		FileDescriptor fd = null;
		try {
			new Thread(this).start();
			LocalServerSocket lss = new LocalServerSocket(NAME);
			LocalSocket ls = lss.accept();
			while (ls.getInputStream().read() != -1)
				;
			FileDescriptor[] fdarr;
			fdarr = ls.getAncillaryFileDescriptors();
			if (fdarr != null) {
				fd = fdarr[0];
			}
			ls.close();
			lss.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fd;
	}

}
