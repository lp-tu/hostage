package de.tudarmstadt.informatik.hostage.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

public class StringReaderWriter implements ReaderWriter<String> {

	private BufferedReader in;
	private BufferedWriter out;

	public StringReaderWriter(InputStream in, OutputStream out) {
		this.in = new BufferedReader(new InputStreamReader(in));
		this.out = new BufferedWriter(new OutputStreamWriter(out));

	}

	@Override
	public String read() throws IOException {
		return in.readLine();
	}

	@Override
	public void write(List<String> message) throws IOException {
		for (String m : message) {
			out.write(m + "\n");
			out.flush();
		}
	}

}
