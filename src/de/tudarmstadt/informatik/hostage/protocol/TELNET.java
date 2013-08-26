package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.informatik.hostage.wrapper.ByteArray;

public final class TELNET implements Protocol<ByteArray> {

	private static enum STATE {
		NONE, OPEN, CLOSED, LOGIN, AUTHENTICATE, LOGGED_IN
	};

	private STATE state = STATE.NONE;

	private byte[] usr;
	private byte[] cmd;

	@Override
	public int getPort() {
		return 23;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public List<ByteArray> processMessage(ByteArray message) {
		List<ByteArray> response = new ArrayList<ByteArray>();
		switch (state) {
		case NONE:
			response.add(new ByteArray(cmdRequests));
			response.add(new ByteArray(getCmdResponses(message.get())));
			state = STATE.OPEN;
			break;
		case OPEN:
			response.add(new ByteArray("Debian GNU/Linux 7.0\r\n".getBytes()));
			response.add(new ByteArray("raspberrypi login: ".getBytes()));
			state = STATE.LOGIN;
			break;
		case LOGIN:
			usr = java.util.Arrays.copyOfRange(message.get(), 0,
					message.size() - 2);
			response.add(new ByteArray("Password: ".getBytes()));
			state = STATE.AUTHENTICATE;
			break;
		case AUTHENTICATE:
			response.add(new ByteArray(
					"Last Login: \r\nLinux raspberrypi 3.6.11+\r\n".getBytes()));
			response.add(new ByteArray(concatenate(a, usr, b, usr, c)));
			state = STATE.LOGGED_IN;
			break;
		case LOGGED_IN:
			cmd = java.util.Arrays.copyOfRange(message.get(), 0,
					message.size() - 2);
			if (new String(cmd).contains("exit")) {
				response.add(new ByteArray("logout\r\n".getBytes()));
				state = STATE.CLOSED;
			} else {
				String bash = "-bash: " + new String(cmd)
						+ ": command not found";
				response.add(new ByteArray(bash.getBytes()));
				response.add(new ByteArray("\r\n".getBytes()));
				response.add(new ByteArray(concatenate(a, usr, b, usr, c)));
			}
			break;
		default:
			response.add(new ByteArray("\r\nlogout\r\n".getBytes()));
			state = STATE.CLOSED;
			break;
		}
		return response;
	}

	@Override
	public boolean isClosed() {
		return (state == STATE.CLOSED);
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public Class<ByteArray> getType() {
		return ByteArray.class;
	}

	@Override
	public String toString() {
		return "TELNET";
	}

	private byte[] getCmdResponses(byte[] request) {
		byte[] resp = new byte[request.length];
		for (int i = 0; i < resp.length - 2; i += 3) { // respond to all
														// requests with no
			resp[i] = (byte) 0xff;
			if (request[i + 1] == (byte) 0xfb)
				resp[i + 1] = (byte) 0xfe;
			else if (request[i + 1] == (byte) 0xfd)
				resp[i + 1] = (byte) 0xfc;
			else
				resp[i + 1] = (byte) 0xfe;
			resp[i + 2] = request[i + 2];
		}
		return resp;
	}

	private final byte[] cmdRequests = { (byte) 0xff, (byte) 0xfd, 0x18,
			(byte) 0xff, (byte) 0xfd, 0x20, (byte) 0xff, (byte) 0xfd, 0x27 };
	private final byte[] a = { 0x1b, 0x5d, 0x30, 0x3b };
	private final byte[] b = { 0x40, 0x72, 0x61, 0x73, 0x70, 0x62, 0x65, 0x72,
			0x72, 0x79, 0x70, 0x69, 0x3a, 0x20, 0x7e, 0x07, 0x1b, 0x5b, 0x30,
			0x31, 0x3b, 0x33, 0x32, 0x6d };
	private final byte[] c = { 0x40, 0x72, 0x61, 0x73, 0x70, 0x62, 0x65, 0x72,
			0x72, 0x79, 0x70, 0x69, 0x1b, 0x5b, 0x30, 0x30, 0x6d, 0x20, 0x1b,
			0x5b, 0x30, 0x31, 0x3b, 0x33, 0x34, 0x6d, 0x7e, 0x20, 0x24, 0x1b,
			0x5b, 0x30, 0x30, 0x6d, 0x20 };

	public byte[] concatenate(byte[]... bytes) {
		int newSize = 0;
		for (byte[] b : bytes)
			newSize += b.length;
		byte[] dst = new byte[newSize];

		int currentPos = 0;
		int newPos;
		for (byte[] b : bytes) {
			newPos = b.length;
			System.arraycopy(b, 0, dst, currentPos, newPos);
			currentPos += newPos;
		}
		return dst;
	}

}
