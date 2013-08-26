package de.tudarmstadt.informatik.hostage.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.ByteArray;

public class ByteArrayReaderWriter implements ReaderWriter<ByteArray> {

	private BufferedInputStream in;
	private BufferedOutputStream out;

	public ByteArrayReaderWriter(InputStream in, OutputStream out) {
		this.in = new BufferedInputStream(in);
		this.out = new BufferedOutputStream(out);
	}

	@Override
	public ByteArray read() throws IOException {
		int availableBytes;
		while ((availableBytes = in.available()) <= 0)
			;
		byte[] buffer = new byte[availableBytes];
		in.read(buffer);
		return new ByteArray(buffer);
	}

	@Override
	public void write(List<ByteArray> message) throws IOException {
		for (ByteArray m : message) {
			out.write(m.get());
		}
	}

}
