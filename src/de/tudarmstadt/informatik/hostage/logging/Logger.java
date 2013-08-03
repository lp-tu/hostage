package de.tudarmstadt.informatik.hostage.logging;

public interface Logger {

	void write(Record record);

	void close();

}
