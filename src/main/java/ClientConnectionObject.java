package main.java;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionObject {
	private volatile Socket socket;
	private volatile PrintWriter socketOutputStream;
	private volatile BufferedReader socketInputStream;

	public ClientConnectionObject(Socket socket, PrintWriter socketOutputStream, BufferedReader bufferedInputStream) {
		this.setSocket(socket);
		this.setSocketOutputStream(socketOutputStream);
		this.setSocketInputStream(bufferedInputStream);
	}

	private void setSocketInputStream(BufferedReader socketInputStream) {
		this.socketInputStream = socketInputStream;
	}

	public BufferedReader getSocketInputStreamReader() {
		return this.socketInputStream;
	}

	public PrintWriter getSocketOutputStreamWriter() {
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
