package de.tudarmstadt.informatik.hostage.protocol;

public interface Protocol {

	public static enum TALK_FIRST {
		SERVER, CLIENT
	};

	int getPort();

	TALK_FIRST whoTalksFirst();

	String processMessage(String message);

	boolean isClosed();

}
