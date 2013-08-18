package de.tudarmstadt.informatik.hostage.protocol;

public final class TELNET implements Protocol {

	private static enum STATE {
		NONE, OPEN, CLOSED
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 8023;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.SERVER;
	}

	@Override
	public String processMessage(String message) {
		message.getBytes();
		return null;
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public String toString() {
		return "TELNET";
	}

}
