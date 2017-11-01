package main.java;

public class ClientNodeUndefinedException extends Exception {

	public ClientNodeUndefinedException() {
	}

	public ClientNodeUndefinedException(String message) {
		super(message);
	}

	public ClientNodeUndefinedException(Throwable cause) {
		super(cause);
	}

	public ClientNodeUndefinedException(String message, Throwable cause) {
		super(message, cause);
	}

	public ClientNodeUndefinedException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
