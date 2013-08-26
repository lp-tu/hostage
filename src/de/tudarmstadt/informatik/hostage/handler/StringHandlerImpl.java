package de.tudarmstadt.informatik.hostage.handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import de.tudarmstadt.informatik.hostage.HoneyListener;
import de.tudarmstadt.informatik.hostage.HoneyService;
import de.tudarmstadt.informatik.hostage.io.ReaderWriter;
import de.tudarmstadt.informatik.hostage.io.StringReaderWriter;
import de.tudarmstadt.informatik.hostage.logging.Record.TYPE;
import de.tudarmstadt.informatik.hostage.protocol.Protocol;
import de.tudarmstadt.informatik.hostage.protocol.Protocol.TALK_FIRST;

public class StringHandlerImpl extends AbstractHandler {

	public StringHandlerImpl(HoneyService service, HoneyListener listener,
			Protocol protocol, Socket client) {
		super(service, listener, protocol, client);
	}

	@Override
	protected void talkToClient(InputStream in, OutputStream out)
			throws IOException {
		ReaderWriter<String> stream = new StringReaderWriter(in, out);

		String inputLine;
		List<String> outputLine;

		if (protocol.whoTalksFirst() == TALK_FIRST.SERVER) {
			outputLine = protocol.processMessage(null);
			stream.write(outputLine);
			for (String s : outputLine) {
				log.write(createRecord(TYPE.SEND, s));
			}
		}

		while (!thread.isInterrupted() && (inputLine = stream.read()) != null) {
			log.write(createRecord(TYPE.RECEIVE, inputLine));
			outputLine = protocol.processMessage(inputLine);
			if (outputLine != null) {
				stream.write(outputLine);
				for (String s : outputLine) {
					log.write(createRecord(TYPE.SEND, s));
				}
			}
			if (protocol.isClosed()) {
				break;
			}
		}
	}

}
