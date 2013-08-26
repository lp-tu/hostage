package de.tudarmstadt.informatik.hostage.protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class HTTP implements Protocol<String> {

	@Override
	public int getPort() {
		return 80;
	}

	@Override
	public TALK_FIRST whoTalksFirst() {
		return TALK_FIRST.CLIENT;
	}

	@Override
	public List<String> processMessage(String message) {
		List<String> response = new ArrayList<String>();
		if (Pattern.matches("^HEAD\\s?", message)) {
			response.add(buildHeader());
		} else if (Pattern.matches("^GET\\s?", message)) {
			response.add((buildHeader() + buildPage()));
		} else {
			response.add("HTTP/1.1 501 Not Implemented");
		}
		return response;
	}

	@Override
	public boolean isClosed() {
		return false;
	}

	@Override
	public boolean isSecure() {
		return false;
	}

	@Override
	public Class<String> getType() {
		return String.class;
	}

	@Override
	public String toString() {
		return "HTTP";
	}

	private String buildHeader() {
		StringBuilder builder = new StringBuilder();
		builder.append("HTTP/1.1 200 OK\r\n");
		builder.append("Server: Apache/2.4.6 (Unix) PHP/5.5.2\r\n");
		builder.append("Content-Length: 148\r\n");
		builder.append("Content-Language: en\r\n");
		builder.append("Connection: close\r\n");
		builder.append("Content-Type: text/html\r\n");
		builder.append("\r\n");
		return builder.toString();
	}

	private String buildPage() {
		StringBuilder builder = new StringBuilder();
		builder.append("<html>");
		builder.append("<head>");
		builder.append("<title>hostage</title>");
		builder.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\" />");
		builder.append("</head>");
		builder.append("<body>");
		builder.append("<p>Hello there!</p>");
		builder.append("</body>");
		builder.append("</html>");
		return builder.toString();
	}

}
