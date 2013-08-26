package de.tudarmstadt.informatik.hostage.protocol;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;

import de.tudarmstadt.informatik.hostage.wrapper.ByteArray;

public final class SMB implements Protocol<ByteArray> {

	private static enum STATE {
		NONE, CONNECTED, AUTHENTICATED, LISTING, DISCONNECTED, CLOSED
	}

	private STATE state = STATE.NONE;

	@Override
	public int getPort() {
		return 445;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public List<ByteArray> processMessage(ByteArray message) {
		byte[] primitiveByteArray = message.get();
		SmbPacket packet = new SmbPacket(primitiveByteArray);
		byte smbCommand = packet.getSmbCommand();
		List<ByteArray> response = new ArrayList<ByteArray>();
		switch (state) {
		case NONE:
			if (smbCommand == 0x72) {
				state = STATE.CONNECTED;
				response.add(new ByteArray(packet.getNego()));
			} else {
				state = STATE.DISCONNECTED;
				response.add(new ByteArray(packet.getTreeDisc()));
			}
			break;
		case CONNECTED:
			if (smbCommand == 0x73) {
				response.add(new ByteArray(packet.getSessSetup()));
			} else if (smbCommand == 0x75) {
				state = STATE.AUTHENTICATED;
				response.add(new ByteArray(packet.getTreeCon()));
			} else {
				state = STATE.DISCONNECTED;
				response.add(new ByteArray(packet.getTreeDisc()));
			}
			break;
		case AUTHENTICATED:
			if (smbCommand == (byte) 0xa2) {
				state = STATE.LISTING;
				response.add(new ByteArray(packet.getNTCreate()));
			} else if (smbCommand == 0x2b) {
				response.add(new ByteArray(packet.getEcho()));
			} else if (smbCommand == 0x32) {
				response.add(new ByteArray(packet.getTrans2()));
			} else if (smbCommand == 0x04) {
				response.add(new ByteArray(packet.getClose()));
			} else if (smbCommand == 0x71) {
				state = STATE.CLOSED;
				response.add(new ByteArray(packet.getTreeDisc()));
			} else {
				state = STATE.DISCONNECTED;
				response.add(new ByteArray(packet.getTreeDisc()));
			}
			break;
		case LISTING:
			if (smbCommand == 0x25) {
				response.add(new ByteArray(packet.getTrans()));
			} else if (smbCommand == 0x04) {
				response.add(new ByteArray(packet.getClose()));
			} else if (smbCommand == 0x71) {
				state = STATE.CLOSED;
				response.add(new ByteArray(packet.getTreeDisc()));
			} else if (smbCommand == 0x72) {
				state = STATE.CONNECTED;
				response.add(new ByteArray(packet.getNego()));
			} else {
				state = STATE.DISCONNECTED;
				response.add(new ByteArray(packet.getTreeDisc()));
			}
			break;
		case DISCONNECTED:
			state = STATE.CLOSED;
			response.add(new ByteArray(packet.getTreeDisc()));
			break;
		default:
			state = STATE.CLOSED;
			response.add(new ByteArray(packet.getTreeDisc()));
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
		return "SMB";
	}

	public byte[] concat(byte[]... bytes) {
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

	public byte[] getTimeInBytes() {
		long time = System.currentTimeMillis();
		Calendar calend = Calendar.getInstance();
		calend.setTimeZone(TimeZone.getTimeZone("UTC"));
		calend.set(1601, 0, 01, 00, 00, 00);
		time -= calend.getTimeInMillis();
		time *= 10000;

		byte[] b = new byte[8];
		byte[] b2 = ByteBuffer.allocate(8).putLong(time).array();

		for (int i = 0, j = 7; i < 8 && j > -1; i++, j--) {
			b[i] = (byte) (b2[j] & 0xff);
		}

		return b;
	}

	public byte[] randomBytes(int size) {
		byte[] bytes = new byte[size];
		Random rdm = new Random();
		rdm.nextBytes(bytes);
		return bytes;
	}

	public String charToString(char[] chars) {
		char[] newChars = new char[chars.length];
		for (int i = 0, j = 0; i < chars.length && j < newChars.length; i++) {
			if (isLetter(chars[i])) {
				newChars[j] = chars[i];
				j++;
			}
		}
		return new String(newChars);
	}

	public byte[] charToByte(char[] chars) {
		byte[] bytes = new byte[chars.length];
		for (int i = 0; i < chars.length; i++)
			bytes[i] = (byte) chars[i];
		return bytes;
	}

	public char[] byteToChar(byte[] bytes) {
		char[] chars = new char[bytes.length];
		for (int i = 0; i < bytes.length; i++)
			chars[i] = (char) bytes[i];
		return chars;
	}

	private boolean isLetter(char c) {
		return (c >= 32 && c <= 127);
	}

	private class SmbPacket {
		private byte[] msg = null;
		private final byte[] serverGUID = randomBytes(16);
		private boolean authenticateNext = false;

		private byte[] serverComp = new byte[4];
		private byte[] smbCommand = new byte[1];
		private byte[] ntStat = new byte[4];
		private byte[] smbFlags = new byte[1];
		private byte[] smbFlags2 = new byte[2];
		private byte[] processIDHigh = new byte[2];
		private byte[] signature = new byte[8];
		private byte[] reserved = new byte[2];
		private byte[] treeID = new byte[2];
		private byte[] processID = new byte[2];
		private byte[] userID = new byte[2];
		private byte[] multiplexID = new byte[2];

		public SmbPacket(byte[] message) {
			this.msg = message;
			serverComp = new byte[] { message[4], message[5], message[6],
					message[7] };
			smbCommand = new byte[] { message[8] };
			ntStat = new byte[] { message[9], message[10], message[11],
					message[12] };
			smbFlags = new byte[] { (byte) (message[13] | 0x80) }; // | 0x80 for
																	// mark
																	// response
																	// bit
			smbFlags2 = new byte[] { message[14], message[15] };
			processIDHigh = new byte[] { message[16], message[17] };
			signature = new byte[] { message[18], message[19], message[20],
					message[21], message[22], message[23], message[24],
					message[25] };
			reserved = new byte[] { message[26], message[27] };
			treeID = new byte[] { message[28], message[29] };
			processID = new byte[] { message[30], message[31] };
			userID = new byte[] { message[32], message[33] };
			multiplexID = new byte[] { message[34], message[35] };
		}

		private byte[] getNetbios(byte[] response) {
			byte[] netbios = { 0x00 };
			byte[] buf = ByteBuffer.allocate(4).putInt(response.length).array(); // allocate(4)
																					// because
																					// int
																					// is
																					// 4
																					// bytes
																					// long
			byte[] netbiosLength = { buf[1], buf[2], buf[3] }; // only bytes 1-3
																// needed, byte
																// 0 is not
																// needed
			return concat(netbios, netbiosLength);
		}

		private byte[] getHeader() {
			byte[] header = new byte[0];
			return concat(header, serverComp, smbCommand, ntStat, smbFlags,
					smbFlags2, processIDHigh, signature, reserved, treeID,
					processID, userID, multiplexID);
		}

		public byte[] getNego() {
			byte[] wordCount = { 0x11 };
			byte[] dialect = evaluateDialect();
			byte[] secMode = { 0x03 };
			byte[] maxMpxC = { 0x32, 0x00 };
			byte[] maxVcs = { 0x01, 0x00 };
			byte[] maxBufSize = { 0x04, 0x11, 0x00, 0x00 };
			byte[] maxRawBuf = { 0x00, 0x00, 0x01, 0x00 };
			byte[] sessionKey = { 0x00, 0x00, 0x00, 0x00 };
			byte[] capabilities = { (byte) 0xfc, (byte) 0xe3, 0x01, (byte) 0x80 };
			byte[] sysTime = getTimeInBytes();
			byte[] timeZone = { (byte) 0x88, (byte) 0xff }; // FIXME correct
															// time zone
			byte[] keyLength = { 0x00 };
			byte[] byteCount = { 0x3a, 0x00 };
			byte[] guid = serverGUID;
			byte[] secBlob = { 0x60, 0x28, 0x06, 0x06 };
			byte[] oid = { 0x2b, 0x06, 0x01, 0x05, 0x05, 0x02 };
			byte[] protectNeg = { (byte) 0xa0, 0x1e };
			byte[] negToken = { 0x30, 0x1c, (byte) 0xa0, 0x1a, 0x30, 0x18 };
			byte[] mechType = { 0x06, 0x0a, 0x2b, 0x06, 0x01, 0x04, 0x01,
					(byte) 0x82, 0x37, 0x02, 0x02, 0x1e };
			byte[] mechType2 = { 0x06, 0x0a, 0x2b, 0x06, 0x01, 0x04, 0x01,
					(byte) 0x82, 0x37, 0x02, 0x02, 0x0a };

			byte[] response = concat(getHeader(), wordCount, dialect, secMode,
					maxMpxC, maxVcs, maxBufSize, maxRawBuf, sessionKey,
					capabilities, sysTime, timeZone, keyLength, byteCount,
					guid, secBlob, oid, protectNeg, negToken, mechType,
					mechType2);
			return concat(getNetbios(response), response);
		}

		private byte[] evaluateDialect() {
			byte[] dialectMsg = java.util.Arrays.copyOfRange(msg, 39,
					msg.length);
			short dialectNumber = 0;
			for (int i = 0, start = 0; i < dialectMsg.length; i++) {
				if (dialectMsg[i] == 0x00) {
					if (new String(java.util.Arrays.copyOfRange(dialectMsg,
							start, i)).contains("NT LM 0.12")) {
						return new byte[] { (byte) dialectNumber,
								(byte) (dialectNumber >> 8) };
					}
					start = i + 1;
					dialectNumber++;
				}
			}
			return new byte[] { 0x00, 0x00 };
		}

		public byte[] getSessSetup() {
			if (authenticateNext)
				return getSetupAuth();
			else {
				authenticateNext = true;
				return getSetupChal();
			}
		}

		private byte[] getSetupChal() {
			byte[] wordCount = { 0x04 };
			byte[] andXCommand = { (byte) 0xff };
			byte[] reserved = { 0x00 };
			byte[] andXOffset = { 0x60, 0x01 };
			byte[] action = { 0x00, 0x00 };
			byte[] secBlobLength = { (byte) 0xc7, 0x00 };
			byte[] byteCount = { 0x35, 0x01 };
			byte[] secBlob = { (byte) 0xa1, (byte) 0x81, (byte) 0xc4 };
			byte[] negToken = { 0x30, (byte) 0x81, (byte) 0xc1, (byte) 0xa0,
					0x03, 0x0a, 0x01 };
			byte[] negResult = { 0x01 };
			byte[] negToken2 = { (byte) 0xa1, 0x0c, 0x06, 0x0a };
			byte[] supportedMech = { 0x2b, 0x06, 0x01, 0x04, 0x01, (byte) 0x82,
					0x37, 0x02, 0x02, 0x0a };
			byte[] negToken3 = { (byte) 0xa2, (byte) 0x81, (byte) 0xab, 0x04,
					(byte) 0x81, (byte) 0xa8 };
			byte[] respToken = { 0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50,
					0x00, 0x02, 0x00, 0x00, 0x00, 0x10, 0x00, 0x10, 0x00, 0x38,
					0x00, 0x00, 0x00, 0x15, (byte) 0x82, (byte) 0x8a, 0x62 };
			byte[] challenge = randomBytes(8);
			byte[] respToken2 = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00, 0x60, 0x00, 0x60, 0x00, 0x48, 0x00, 0x00, 0x00, 0x06,
					0x01, (byte) 0xb0, 0x1d, 0x00, 0x00, 0x00, 0x0f, 0x42,
					0x00, 0x55, 0x00, 0x53, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x45,
					0x00, 0x53, 0x00, 0x53, 0x00, 0x02, 0x00, 0x10, 0x00, 0x42,
					0x00, 0x55, 0x00, 0x53, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x45,
					0x00, 0x53, 0x00, 0x53, 0x00, 0x01, 0x00, 0x10, 0x00, 0x42,
					0x00, 0x55, 0x00, 0x53, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x45,
					0x00, 0x53, 0x00, 0x53, 0x00, 0x04, 0x00, 0x10, 0x00, 0x42,
					0x00, 0x55, 0x00, 0x53, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x45,
					0x00, 0x53, 0x00, 0x53, 0x00, 0x03, 0x00, 0x10, 0x00, 0x42,
					0x00, 0x55, 0x00, 0x53, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x45,
					0x00, 0x53, 0x00, 0x53, 0x00, 0x07, 0x00, 0x08, 0x00 };
			byte[] timeStamp = getTimeInBytes();
			byte[] respToken3 = { 0x00, 0x00, 0x00, 0x00 };
			byte[] nativOS = { 0x57, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x64, 0x00,
					0x6f, 0x00, 0x77, 0x00, 0x73, 0x00, 0x20, 0x00, 0x37, 0x00,
					0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x66, 0x00,
					0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x69, 0x00, 0x6f, 0x00,
					0x6e, 0x00, 0x61, 0x00, 0x6c, 0x00, 0x20, 0x00, 0x37, 0x00,
					0x36, 0x00, 0x30, 0x00, 0x30, 0x00, 0x00, 0x00 }; // Windows
																		// 7
																		// Professional
																		// 7600
			byte[] nativLanMngr = { 0x57, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x64,
					0x00, 0x6f, 0x00, 0x77, 0x00, 0x73, 0x00, 0x20, 0x00, 0x37,
					0x00, 0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x66,
					0x00, 0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x69, 0x00, 0x6f,
					0x00, 0x6e, 0x00, 0x61, 0x00, 0x6c, 0x00, 0x20, 0x00, 0x36,
					0x00, 0x2e, 0x00, 0x31, 0x00, 0x00, 0x00 }; // Windows 7
																// Professional
																// 6.1

			ntStat = new byte[] { 0x16, 0x00, 0x00, (byte) 0xc0 };
			userID = new byte[] { 0x00, 0x08 };

			byte[] response = concat(getHeader(), wordCount, andXCommand,
					reserved, andXOffset, action, secBlobLength, byteCount,
					secBlob, negToken, negResult, negToken2, supportedMech,
					negToken3, respToken, challenge, respToken2, timeStamp,
					respToken3, nativOS, nativLanMngr);
			return concat(getNetbios(response), response);
		}

		private byte[] getSetupAuth() {
			byte[] wordCount = { 0x04 };
			byte[] andXCommand = { (byte) 0xff };
			byte[] reserved = { 0x00 };
			byte[] andXOffset = { (byte) 0xa2, 0x00 };
			byte[] action = { 0x01, 0x00 };
			byte[] secBlobLength = { 0x09, 0x00 };
			byte[] byteCount = { (byte) 0x77, 0x00 };
			byte[] secBlob = { (byte) 0xa1, 0x07, 0x30, 0x05, (byte) 0xa0,
					0x03, 0x0a, 0x01, 0x00 };
			byte[] nativOS = { 0x57, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x64, 0x00,
					0x6f, 0x00, 0x77, 0x00, 0x73, 0x00, 0x20, 0x00, 0x37, 0x00,
					0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x66, 0x00,
					0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x69, 0x00, 0x6f, 0x00,
					0x6e, 0x00, 0x61, 0x00, 0x6c, 0x00, 0x20, 0x00, 0x37, 0x00,
					0x36, 0x00, 0x30, 0x00, 0x30, 0x00, 0x00, 0x00 }; // Windows
																		// 7
																		// Professional
																		// 7600
			byte[] nativLanMngr = { 0x57, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x64,
					0x00, 0x6f, 0x00, 0x77, 0x00, 0x73, 0x00, 0x20, 0x00, 0x37,
					0x00, 0x20, 0x00, 0x50, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x66,
					0x00, 0x65, 0x00, 0x73, 0x00, 0x73, 0x00, 0x69, 0x00, 0x6f,
					0x00, 0x6e, 0x00, 0x61, 0x00, 0x6c, 0x00, 0x20, 0x00, 0x36,
					0x00, 0x2e, 0x00, 0x31, 0x00, 0x00, 0x00 }; // Windows 7
																// Professional
																// 6.1

			byte[] response = concat(getHeader(), wordCount, andXCommand,
					reserved, andXOffset, action, secBlobLength, byteCount,
					secBlob, nativOS, nativLanMngr);
			return concat(getNetbios(response), response);
		}

		public byte[] getTreeCon() {
			String str = toString();
			byte[] wordCount = { 0x00 };
			byte[] andXCommand = { 0x00, 0x00 };
			byte[] response = null;

			if (str.contains("IPC$") || str.contains("DOCS")) {
				wordCount = new byte[] { 0x07 };
				andXCommand = new byte[] { (byte) 0xff };
				byte[] reserved = { 0x00 };
				byte[] andXOffset = { 0x38, 0x00 };
				byte[] optionalSupport = { 0x01, 0x00 };
				byte[] maxShareAccess = { (byte) 0xff, (byte) 0xff, 0x1f, 0x00 };
				byte[] guestMaxShareAccess = { (byte) 0xff, (byte) 0xff, 0x1f,
						0x00 };
				byte[] byteCount = { 0x07, 0x00 };
				byte[] service = { 0x49, 0x50, 0x43, 0x00 };
				byte[] extraParameters = { 0x00, 0x00, 0x00 };

				treeID = new byte[] { 0x00, 0x08 };

				response = concat(getHeader(), wordCount, andXCommand,
						reserved, andXOffset, optionalSupport, maxShareAccess,
						guestMaxShareAccess, byteCount, service,
						extraParameters);
			} else if (str.contains("C$") || str.contains("ADMIN$")) {
				ntStat = new byte[] { 0x22, 0x00, 0x00, (byte) 0xc0 };
				response = concat(getHeader(), wordCount, andXCommand);
			} else {
				ntStat = new byte[] { (byte) 0xcc, 0x00, 0x00, (byte) 0xc0 };
				response = concat(getHeader(), wordCount, andXCommand);
			}

			return concat(getNetbios(response), response);
		}

		public byte[] getNTCreate() {
			byte[] wordCount = { 0x22 };
			byte[] andXCommand = { (byte) 0xff };
			byte[] reserved = { 0x00 };
			byte[] andXOffset = { 0x67, 0x00 };
			byte[] oplockLevel = { 0x00 };
			byte[] fid = { (byte) 0x00, 0x40 };
			byte[] createAction = { 0x01, 0x00, 0x00, 0x00 };
			byte[] created = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			byte[] lastAccess = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00 };
			byte[] lastWrite = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			byte[] change = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			byte[] fileAttributes = { (byte) 0x80, 0x00, 0x00, 0x00 };
			byte[] allocationSize = { 0x00, 0x10, 0x00, 0x00, 0x00, 0x00, 0x00,
					0x00 };
			byte[] endOfFile = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
			byte[] fileType = { 0x02, 0x00 };
			byte[] ipcState = { (byte) 0xff, 0x05 };
			byte[] isDirectory = { 0x00 };
			byte[] byteCount = { 0x00, 0x00 };

			byte[] response = concat(getHeader(), wordCount, andXCommand,
					reserved, andXOffset, oplockLevel, fid, createAction,
					created, lastAccess, lastWrite, change, fileAttributes,
					allocationSize, endOfFile, fileType, ipcState, isDirectory,
					byteCount);
			return concat(getNetbios(response), response);
		}

		public byte[] getTrans() {
			byte[] transSub = getTransSub();
			byte[] response = null;
			if (transSub[0] == 0x00 && transSub[1] == 0x0b) {
				byte[] wordCount = { 0x0a };
				byte[] totalParamCount = { 0x00, 0x00 };
				byte[] totalDataCount = { 0x44, 0x00 };
				byte[] reserved = { 0x00, 0x00 };
				byte[] paramCount = { 0x00, 0x00 };
				byte[] paramOffset = { 0x38, 0x00 };
				byte[] paramDisplace = { 0x00, 0x00 };
				byte[] dataCount = { 0x44, 0x00 };
				byte[] dataOffset = { 0x38, 0x00 };
				byte[] dataDisplace = { 0x00, 0x00 };
				byte[] setupCount = { 0x00 };
				byte[] reserved2 = { 0x00 };
				byte[] byteCount = { 0x45, 0x00 };
				byte[] padding = { 0x00 };

				byte[] dcerpc = { 0x05, 0x00, 0x0c, 0x03, 0x10, 0x00, 0x00,
						0x00, 0x44, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
						(byte) 0xb8, 0x10, (byte) 0xb8, 0x10, 0x4a, 0x41, 0x00,
						0x00, 0x0d, 0x00, 0x5c, 0x50, 0x49, 0x50, 0x45, 0x5c,
						0x73, 0x72, 0x76, 0x73, 0x76, 0x63, 0x00, 0x00, 0x01,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x04, 0x5d,
						(byte) 0x88, (byte) 0x8a, (byte) 0xeb, 0x1c,
						(byte) 0xc9, 0x11, (byte) 0x9f, (byte) 0xe8, 0x08,
						0x00, 0x2b, 0x10, 0x48, 0x60, 0x02, 0x00, 0x00, 0x00 };

				response = concat(getHeader(), wordCount, totalParamCount,
						totalDataCount, reserved, paramCount, paramOffset,
						paramDisplace, dataCount, dataOffset, dataDisplace,
						setupCount, reserved2, byteCount, padding, dcerpc);

			} else if (transSub[0] == 0x00 && transSub[1] == 0x00) {
				byte[] wordCount = { 0x0a };
				byte[] totalParamCount = { 0x00, 0x00 };
				byte[] totalDataCount = { 0x54, 0x01 };
				byte[] reserved = { 0x00, 0x00 };
				byte[] paramCount = { 0x00, 0x00 };
				byte[] paramOffset = { 0x38, 0x00 };
				byte[] paramDisplace = { 0x00, 0x00 };
				byte[] dataCount = { 0x54, 0x01 };
				byte[] dataOffset = { 0x38, 0x00 };
				byte[] dataDisplace = { 0x00, 0x00 };
				byte[] setupCount = { 0x00 };
				byte[] reserved2 = { 0x00 };
				byte[] byteCount = { 0x55, 0x01 };
				byte[] padding = { 0x00 };

				byte[] dcerpc = { 0x05, 0x00, 0x02, 0x03, 0x10, 0x00, 0x00,
						0x00, 0x54, 0x01, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
						0x3c, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
				byte[] serverService = { 0x01, 0x00, 0x00, 0x00, 0x01, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x04, 0x00, 0x00,
						0x00, 0x04, 0x00, 0x02, 0x00, 0x04, 0x00, 0x00, 0x00,
						0x08, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, (byte) 0x80,
						0x0c, 0x00, 0x02, 0x00, 0x10, 0x00, 0x02, 0x00, 0x00,
						0x00, 0x00, (byte) 0x80, 0x14, 0x00, 0x02, 0x00, 0x18,
						0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x1c, 0x00,
						0x02, 0x00, 0x20, 0x00, 0x02, 0x00, 0x03, 0x00, 0x00,
						(byte) 0x80, 0x24, 0x00, 0x02, 0x00, 0x07, 0x00, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00,
						0x41, 0x00, 0x44, 0x00, 0x4d, 0x00, 0x49, 0x00, 0x4e,
						0x00, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0d, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0d, 0x00, 0x00,
						0x00, 0x52, 0x00, 0x65, 0x00, 0x6d, 0x00, 0x6f, 0x00,
						0x74, 0x00, 0x65, 0x00, 0x20, 0x00, 0x41, 0x00, 0x64,
						0x00, 0x6d, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
						0x00, 0x03, 0x00, 0x00, 0x00, 0x43, 0x00, 0x24, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0x44, 0x00,
						0x65, 0x00, 0x66, 0x00, 0x61, 0x00, 0x75, 0x00, 0x6c,
						0x00, 0x74, 0x00, 0x20, 0x00, 0x73, 0x00, 0x68, 0x00,
						0x61, 0x00, 0x72, 0x00, 0x65, 0x00, 0x00, 0x00, 0x05,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x05, 0x00,
						0x00, 0x00, 0x64, 0x00, 0x6f, 0x00, 0x63, 0x00, 0x73,
						0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00, 0x00,
						0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x49, 0x00, 0x50,
						0x00, 0x43, 0x00, 0x24, 0x00, 0x00, 0x00, 0x00, 0x00,
						0x0b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0b,
						0x00, 0x00, 0x00, 0x52, 0x00, 0x65, 0x00, 0x6d, 0x00,
						0x6f, 0x00, 0x74, 0x00, 0x65, 0x00, 0x20, 0x00, 0x49,
						0x00, 0x50, 0x00, 0x43, 0x00, 0x00, 0x00 };
				byte[] totalEntries = { 0x00, 0x00, 0x04, 0x00, 0x00, 0x00 };
				byte[] referentID = { 0x28, 0x00, 0x02, 0x00 };
				byte[] resumeHandle = { 0x00, 0x00, 0x00, 0x00 };
				byte[] windowsError = { 0x00, 0x00, 0x00, 0x00 };

				response = concat(getHeader(), wordCount, totalParamCount,
						totalDataCount, reserved, paramCount, paramOffset,
						paramDisplace, dataCount, dataOffset, dataDisplace,
						setupCount, reserved2, byteCount, padding, dcerpc,
						serverService, totalEntries, referentID, resumeHandle,
						windowsError);

			}

			return concat(getNetbios(response), response);
		}

		public byte[] getClose() {
			byte[] wordCount = { 0x00 };
			byte[] byteCount = { 0x00, 0x00 };

			smbCommand = new byte[] { 0x04 };

			byte[] response = concat(getHeader(), wordCount, byteCount);

			return concat(getNetbios(response), response);
		}

		public byte[] getTreeDisc() {
			byte[] wordCount = { 0x00 };
			byte[] byteCount = { 0x00, 0x00 };

			smbCommand[0] = 0x71;

			byte[] response = concat(getHeader(), wordCount, byteCount);

			return concat(getNetbios(response), response);
		}

		public byte[] getEcho() {
			byte[] wordCount = { 0x01 };
			byte[] echoSeq = { 0x01, 0x00 };
			byte[] byteCount = { 0x10, 0x00 };
			byte[] echoData = { (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
					(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
					(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
					(byte) 0xf0, (byte) 0xf0, (byte) 0xf0, (byte) 0xf0,
					(byte) 0xf0 };
			byte[] response = concat(getHeader(), wordCount, echoSeq,
					byteCount, echoData);
			return concat(getNetbios(response), response);

		}

		public byte[] getTrans2() {
			byte[] response = null;
			byte[] wordCount = { 0x00 };
			byte[] andXCommand = { 0x00, 0x00 };
			ntStat = new byte[] { 0x22, 0x00, 0x00, (byte) 0xc0 };
			response = concat(getHeader(), wordCount, andXCommand);
			return concat(getNetbios(response), response);
		}

		private byte[] getTransSub() {
			byte[] transSub = new byte[2];
			if (smbCommand[0] == 0x32)
				transSub = new byte[] { msg[66], msg[65] };
			else if (smbCommand[0] == 0x25)
				transSub = new byte[] { 0x00, msg[90] };
			else
				transSub = new byte[] { 0x00, 0x00 };
			return transSub;
		}

		public String toString() {
			return new String(msg);
		}

		public byte getSmbCommand() {
			return smbCommand[0];
		}
	}
}
