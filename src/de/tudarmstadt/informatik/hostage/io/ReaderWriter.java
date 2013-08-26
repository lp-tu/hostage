package de.tudarmstadt.informatik.hostage.io;

import java.io.IOException;
import java.util.List;

public interface ReaderWriter<T> {

	T read() throws IOException;

	void write(List<T> outputLine) throws IOException;

}
