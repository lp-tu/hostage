package de.tudarmstadt.informatik.hostage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import de.tudarmstadt.informatik.hostage.logging.Logger;
import de.tudarmstadt.informatik.hostage.logging.Record;
import de.tudarmstadt.informatik.hostage.logging.Record.TYPE;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.Protocol.TALK_FIRST;

public class HoneyHandler implements Runnable {

	private static final int TIMEOUT = 30 * 1000;

	private Protocol protocol;
	private Socket client;
	private Thread thread;

	private HoneyService service;
	private HoneyListener listener;
	private Logger log;

	public HoneyHandler(HoneyService service, HoneyListener listener,
			Protocol protocol, Socket client) {
		this.service = service;
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
		BufferedReader in;
		BufferedWriter out;
		try {
			in = new BufferedReader(new InputStreamReader(
					client.getInputStream()));
			out = new BufferedWriter(new OutputStreamWriter(
					client.getOutputStream()));
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

	private void talkToClient(BufferedReader in, BufferedWriter out)
			throws IOException {
		String inputLine;
		String outputLine;

		if (protocol.whoTalksFirst() == TALK_FIRST.SERVER) {
			outputLine = protocol.processMessage(null);
			out.write(outputLine + "\n");
			out.flush();
			log.write(createRecord(TYPE.SEND, outputLine));
		}

		while (!thread.isInterrupted() && (inputLine = in.readLine()) != null) {
			log.write(createRecord(TYPE.RECEIVE, inputLine));
			outputLine = protocol.processMessage(inputLine);
			if (outputLine != null) {
				out.write(outputLine + "\n");
				out.flush();
				log.write(createRecord(TYPE.SEND, outputLine));
			}
			if (protocol.isClosed())
				break;
		}
	}

	private Record createRecord(TYPE type, String packet) {
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
