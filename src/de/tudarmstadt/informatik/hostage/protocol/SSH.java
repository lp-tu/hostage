package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

public final class SSH implements Protocol {

	private static enum STATE {
		NONE, OPEN, CLOSED
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 8022;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public List<byte[]> processMessage(byte[] message) {
		List<byte[]> response = new ArrayList<byte[]>();
		return response;
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public String toString() {
		return "SSH";
	}

}
