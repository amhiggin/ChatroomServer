package main.java;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionObject implements Comparable<ClientConnectionObject> {
	private volatile Socket socket;
	private volatile PrintWriter socketOutputStream;
	private volatile BufferedInputStream socketInputStream;
	private int id;

	public ClientConnectionObject(Socket socket, PrintWriter socketOutputStream,
			BufferedInputStream bufferedInputStream, int id) {
		this.setSocket(socket);
		this.setSocketOutputStream(socketOutputStream);
		this.setSocketInputStream(bufferedInputStream);
		this.id = id;
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

	@Override
	public int compareTo(ClientConnectionObject connectionObject) {
		if (this.getId() < connectionObject.getId()) {
			return -1;
		} else if (this.getId() > connectionObject.getId()) {
			return 1;
		}
		return 0;
	}

	private int getId() {
		return this.id;
	}
}
