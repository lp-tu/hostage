package de.tudarmstadt.informatik.hostage.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import de.tudarmstadt.informatik.hostage.HoneyListener;
import de.tudarmstadt.informatik.hostage.HoneyService;
import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.Record.TYPE;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;

public abstract class AbstractHandler implements Runnable {

	protected static final int TIMEOUT = 30 * 1000;

	protected Protocol protocol;
	private Socket client;
	protected Thread thread;

	private HoneyListener listener;
	protected Logger log;

	public AbstractHandler(HoneyService service, HoneyListener listener,
			Protocol protocol, Socket client) {
		this.listener = listener;
		this.log = service.getLog();
		this.protocol = protocol;
		this.client = client;
		this.thread = new Thread(this);
		setSoTimeout(client);
		thread.start();
	}

	private void setSoTimeout(Socket client) {
		try {
			client.setSoTimeout(TIMEOUT);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		InputStream in;
		OutputStream out;
		try {
			in = client.getInputStream();
			out = client.getOutputStream();
			talkToClient(in, out);
		} catch (Exception e) {
			e.printStackTrace();
		}
		kill();
	}

	public void kill() {
		thread.interrupt();
		try {
			client.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		listener.refreshHandlers();
	}

	public boolean isTerminated() {
		return thread.isInterrupted();
	}

	abstract protected void talkToClient(InputStream in, OutputStream out)
			throws IOException;

	protected Record createRecord(TYPE type, String packet) {
		Record record = new Record();
		record.setType(type);
		record.setTimestamp(System.currentTimeMillis());
		record.setLocalIP(client.getLocalAddress());
		record.setLocalPort(protocol.getPort());
		record.setRemoteIP(client.getInetAddress());
		record.setRemotePort(client.getPort());
		record.setPacket(packet);
		return record;
	}

}
