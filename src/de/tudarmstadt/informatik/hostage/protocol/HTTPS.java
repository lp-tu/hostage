package de.tudarmstadt.informatik.hostage.protocol;

import java.util.regex.Pattern;

public final class HTTPS implements Protocol {

	private static enum STATE {
		NONE, OPEN, CLOSED
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 8443;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

	@Override
	public String processMessage(String message) {
		switch (state) {
		case NONE:
			state = STATE.OPEN;
			return "Connection established.";
		case OPEN:
			if (Pattern.matches("^QUIT\\s?", message)) {
				state = STATE.CLOSED;
				return null;
			} else {
				return message;
			}
		default:
			return "Connection closed.";
		}
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public boolean isSecure() {
		return true;
	}

	@Override
	public String toString() {
		return "HTTPS";
	}

}
