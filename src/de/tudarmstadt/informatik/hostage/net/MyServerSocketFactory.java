package de.tudarmstadt.informatik.hostage.net;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketImpl;

import javax.net.ServerSocketFactory;

import de.tudarmstadt.informatik.hostage.system.PrivilegedPort;

public class MyServerSocketFactory extends ServerSocketFactory {

	@Override
	public ServerSocket createServerSocket(int port) throws IOException {
		FileDescriptor fd = new PrivilegedPort(port).bindAndGetFD();

		ServerSocket socket = new ServerSocket();
		try {
			SocketImpl impl = getImpl(socket);
			injectFD(fd, impl);
			injectImpl(impl, socket);
			setBound(socket);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return socket;
	}

	private SocketImpl getImpl(ServerSocket socket) throws Exception {
		Field implField = socket.getClass().getDeclaredField("impl");
		implField.setAccessible(true);
		return (SocketImpl) implField.get(socket);
	}

	private void injectFD(FileDescriptor fd, SocketImpl impl) throws Exception {
		Class<?> plainServerSocketImplClazz = impl.getClass();
		Class<?> plainSocketImplClazz = plainServerSocketImplClazz
				.getSuperclass();
		Class<?> socketImplClazz = plainSocketImplClazz.getSuperclass();
		Field fdField = socketImplClazz.getDeclaredField("fd");
		fdField.setAccessible(true);
		fdField.set(impl, fd);
	}

	private void injectImpl(SocketImpl impl, ServerSocket socket)
			throws Exception {
		Field implField = socket.getClass().getDeclaredField("impl");
		implField.setAccessible(true);
		implField.set(socket, impl);
	}

	private void setBound(ServerSocket socket) throws Exception {
		Field boundField = socket.getClass().getDeclaredField("isBound");
		boundField.setAccessible(true);
		boundField.set(socket, true);
	}

	@Override
	public ServerSocket createServerSocket(int port, int backlog)
			throws IOException {
		return createServerSocket(port);
	}

	@Override
	public ServerSocket createServerSocket(int port, int backlog,
			InetAddress iAddress) throws IOException {
		return createServerSocket(port);
	}

}
