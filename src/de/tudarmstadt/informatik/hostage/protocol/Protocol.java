package de.tudarmstadt.informatik.hostage.protocol;

import java.util.List;

public interface Protocol<T> {

	public static enum TALK_FIRST {
		SERVER, CLIENT
	};

	int getPort();

	TALK_FIRST whoTalksFirst();

	List<T> processMessage(T message);

	boolean isClosed();

	boolean isSecure();

	Class<T> getType();

}
