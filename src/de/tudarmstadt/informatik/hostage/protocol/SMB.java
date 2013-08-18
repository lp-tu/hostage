package de.tudarmstadt.informatik.hostage.protocol;


public final class SMB implements Protocol {

	private static enum STATE {
		NONE, OPEN, CLOSED
	};

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 8445;
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
		return "SMB";
	}

}
