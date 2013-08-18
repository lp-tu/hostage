package de.tudarmstadt.informatik.hostage.protocol;

import java.util.List;

public interface Protocol {

	public static enum TALK_FIRST {
		SERVER, CLIENT
	};

	int getPort();

	TALK_FIRST whoTalksFirst();

	List<byte[]> processMessage(byte[] message);

	boolean isClosed();

}
