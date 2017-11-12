package main.java;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionObject {
	private volatile Socket socket;
	private volatile PrintWriter socketOutputStream;
	private volatile BufferedInputStream socketInputStream;

	public ClientConnectionObject(Socket socket, PrintWriter socketOutputStream,
			BufferedInputStream bufferedInputStream) {
		this.setSocket(socket);
		this.setSocketOutputStream(socketOutputStream);
		this.setSocketInputStream(bufferedInputStream);
	}

	private void setSocketInputStream(BufferedInputStream socketInputStream) {
		this.socketInputStream = socketInputStream;
	}

	public BufferedInputStream getSocketInputStream() {
		return this.socketInputStream;
	}

	public PrintWriter getSocketOutputStream() {
		return socketOutputStream;
	}

	public void setSocketOutputStream(PrintWriter printWriter) {
		this.socketOutputStream = printWriter;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
