package de.tudarmstadt.informatik.hostage.logging;

import java.io.Serializable;
import java.net.InetAddress;

public class Record implements Serializable {

	private static final long serialVersionUID = 1L;

	public static enum TYPE {
		SEND, RECEIVE
	};

	private TYPE type;
	private long timestamp;
	private InetAddress localIP;
	private int localPort;
	private InetAddress remoteIP;
	private int remotePort;
	private String packet;

	public TYPE getType() {
		return type;
	}

	public void setType(TYPE type) {
		this.type = type;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public InetAddress getLocalIP() {
		return localIP;
	}

	public void setLocalIP(InetAddress localIP) {
		this.localIP = localIP;
	}

	public int getLocalPort() {
		return localPort;
	}

	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	}

	public InetAddress getRemoteIP() {
		return remoteIP;
	}

	public void setRemoteIP(InetAddress remoteIP) {
		this.remoteIP = remoteIP;
	}

	public int getRemotePort() {
		return remotePort;
	}

	public void setRemotePort(int remotePort) {
		this.remotePort = remotePort;
	}

	public String getPacket() {
		return packet;
	}

	public void setPacket(String packet) {
		this.packet = packet;
	}

	@Override
	public String toString() {
		return String.format("%s [%d,%s:%d,%s:%d,%s]",
				((type == TYPE.SEND) ? "SEND" : "RECEIVE"), timestamp,
				localIP.getHostAddress(), localPort, remoteIP.getHostAddress(),
				remotePort, packet);
	}
	
}