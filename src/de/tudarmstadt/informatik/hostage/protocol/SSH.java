package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

public final class SSH implements Protocol<String> {

	private static enum STATE {
		NONE, OPEN, CLOSED
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 22;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public List<String> processMessage(String message) {
		ArrayList<String> response = new ArrayList<String>();
		response.add("Not implemented yet!");
		return response;
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}

	@Override
	public String toString() {
		return "SSH";
	}

}
