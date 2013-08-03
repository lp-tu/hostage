package de.tudarmstadt.informatik.hostage.protocol;

public final class ECHO implements Protocol {

	@Override
	public int getPort() {
		return 8007;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public String processMessage(String message) {
		return message;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public String toString() {
		return "ECHO";
	}

}
