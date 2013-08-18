package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class FTP implements Protocol {

	private static enum STATE {
		NONE, OPEN, CLOSED, USER, LOGGED_IN
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 8021;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

	@Override
	public List<byte[]> processMessage(byte[] message) {
		String request = new String(message);
		List<byte[]> response = new ArrayList<byte[]>();
		switch (state) {
		case NONE:
			if (message == null) {
				state = STATE.OPEN;
				response.add("220 Service ready for new user.".getBytes());
			} else {
				state = STATE.CLOSED;
				response.add("421 Service not available, closing control connection."
						.getBytes());
			}
			break;
		case OPEN:
			if (Pattern.matches("^QUIT\\s?", request)) {
				state = STATE.CLOSED;
				return null;
			} else if (Pattern.matches("^USER (\\w)+$", request)) {
				state = STATE.USER;
				response.add("331 User name ok, need password.".getBytes());
			} else if (message != null && Pattern.matches("^USER\\s?", request)) {
				response.add("530 Not logged in.".getBytes());
			} else if (message != null && Pattern.matches("^USER.*", request)) {
				response.add("501 Syntax error in parameters or arguments"
						.getBytes());
			} else {
				response.add("332 Need account for login.".getBytes());
			}
			break;
		case USER:
			if (Pattern.matches("^PASS (\\S)+$", request)) {
				state = STATE.LOGGED_IN;
				response.add("230 User logged in.".getBytes());
			} else if (Pattern.matches("^PASS.*", request)) {
				state = STATE.OPEN;
				response.add("501 Syntax error in parameters or arguments"
						.getBytes());
			} else {
				state = STATE.CLOSED;
				response.add("221 Service closing control connection."
						.getBytes());
			}
			break;
		case LOGGED_IN:
			if (Pattern.matches("^QUIT\\s?", request)) {
				state = STATE.CLOSED;
			} else if (message != null) {
				response.add("502 Command not implemented.".getBytes());
			} else {
				state = STATE.CLOSED;
				response.add("221 Service closing control connection."
						.getBytes());
			}
			break;
		default:
			state = STATE.CLOSED;
			response.add("421 Service not available, closing control connection."
					.getBytes());
		}
		return response;
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public String toString() {
		return "FTP";
	}

}
