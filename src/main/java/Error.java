package main.java;

/*
 * Enum for error responses to the client 
 * e.g.
 * 		ERROR_CODE: [integer]
		ERROR_DESCRIPTION: [string describing error]
 */

public enum Error {
	InvalidRequest(0, "Invalid request provided");

	private int errorCode;
	private String description;

	Error(final int errorCode, final String errorDescription) {
		this.errorCode = errorCode;
		this.description = errorDescription;
	}

	public int getValue() {
		return this.errorCode;
	}

	public String getDescription() {
		return this.description;
	}
}
