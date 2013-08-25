package de.tudarmstadt.informatik.hostage.protocol.transport;

public interface Protocol<T> {

	T processMessage(T message);

}
