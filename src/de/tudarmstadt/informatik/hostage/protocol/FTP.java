package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class FTP implements Protocol<String> {

	private static enum STATE {
		NONE, OPEN, CLOSED, USER, LOGGED_IN
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 21;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

	@Override
	public List<String> processMessage(String message) {
		List<String> response = new ArrayList<String>();
		switch (state) {
		case NONE:
			if (message == null) {
				state = STATE.OPEN;
				response.add("220 Service ready for new user.");
			} else {
				state = STATE.CLOSED;
				response.add("421 Service not available, closing control connection.");
			}
			break;
		case OPEN:
			if (Pattern.matches("^QUIT\\s?", message)) {
				state = STATE.CLOSED;
				return null;
			} else if (Pattern.matches("^USER (\\w)+$", message)) {
				state = STATE.USER;
				response.add("331 User name ok, need password.");
			} else if (message != null && Pattern.matches("^USER\\s?", message)) {
				response.add("530 Not logged in.");
			} else if (message != null && Pattern.matches("^USER.*", message)) {
				response.add("501 Syntax error in parameters or arguments");
			} else {
				response.add("332 Need account for login.");
			}
			break;
		case USER:
			if (Pattern.matches("^PASS (\\S)+$", message)) {
				state = STATE.LOGGED_IN;
				response.add("230 User logged in.");
			} else if (Pattern.matches("^PASS.*", message)) {
				state = STATE.OPEN;
				response.add("501 Syntax error in parameters or arguments");
			} else {
				state = STATE.CLOSED;
				response.add("221 Service closing control connection.");
			}
			break;
		case LOGGED_IN:
			if (Pattern.matches("^QUIT\\s?", message)) {
				state = STATE.CLOSED;
				response.add("221 Service closing control connection.");
			} else if (message != null) {
				response.add("502 Command not implemented.");
			} else {
				state = STATE.CLOSED;
				response.add("221 Service closing control connection.");
			}
			break;
		default:
			state = STATE.CLOSED;
			response.add("421 Service not available, closing control connection.");
		}
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
		return "FTP";
	}

}
