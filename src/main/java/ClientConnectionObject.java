package main.java;

import java.io.PrintWriter;
import java.net.Socket;

public class ClientConnectionObject {
	private Socket socket;
	private PrintWriter printWriter;

	public ClientConnectionObject(Socket socket, PrintWriter printWriter) {
		this.setSocket(socket);
		this.setPrintWriter(printWriter);
	}

	public PrintWriter getPrintWriter() {
		return printWriter;
	}

	public void setPrintWriter(PrintWriter printWriter) {
		this.printWriter = printWriter;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
