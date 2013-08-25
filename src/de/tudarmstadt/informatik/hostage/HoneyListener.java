package de.tudarmstadt.informatik.hostage;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import de.tudarmstadt.informatik.hostage.net.MyServerSocketFactory;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;

public class HoneyListener implements Runnable {

	private ArrayList<HoneyHandler> handlers = new ArrayList<HoneyHandler>();

	public int getHandlerCount() {
		return handlers.size();
	}

	private Protocol protocol;
	private ServerSocket server;
	private Thread thread;

	private HoneyService service;

	private boolean running = false;

	public boolean isRunning() {
		return running;
	}

	public HoneyListener(HoneyService service, Protocol protocol) {
		this.service = service;
		this.protocol = protocol;
	}

	@Override
	public void run() {
		while (!thread.isInterrupted()) {
			addHandler();
		}
		for (HoneyHandler handler : handlers) {
			handler.kill();
		}
	}

	public void start() {
		try {
			server = new MyServerSocketFactory().createServerSocket(protocol
					.getPort());
			(this.thread = new Thread(this)).start();
			running = true;
			service.notifyUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			thread.interrupt();
			server.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		running = false;
		service.notifyUI();
	}

	public String getProtocolName() {
		return protocol.toString();
	}

	public void refreshHandlers() {
		for (HoneyHandler handler : handlers) {
			if (handler.isTerminated()) {
				handlers.remove(handler);
			}
		}
		service.notifyUI();
	}

	private void addHandler() {
		try {
			Socket client = server.accept();
			handlers.add(new HoneyHandler(service, this, protocol.getClass()
					.newInstance(), client));
			service.notifyUI();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
