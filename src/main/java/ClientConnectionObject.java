package main.java;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionObject {
	private Socket socket;
	private PrintWriter socketOutputStream;
	private BufferedReader socketInputStream;

	public ClientConnectionObject(Socket socket, PrintWriter socketOutputStream, BufferedReader socketInputStream) {
		this.setSocket(socket);
		this.setSocketOutputStream(socketOutputStream);
		this.setSocketInputStream(socketInputStream);
	}

	private void setSocketInputStream(BufferedReader socketInputStream) {
		this.socketInputStream = socketInputStream;
	}

	public BufferedReader getSocketInputStream() {
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
