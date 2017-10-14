package main.java;

/*
 * Enum for error responses to the client 
 */

public enum Error {
	InvalidRequest(0, "Invalid request provided");

	private int value;
	private String description;

	Error(final int value, final String description) {
		this.value = value;
		this.description = description;
	}

	public int getValue() {
		return this.value;
	}

	public String getDescription() {
		return this.description;
	}
}
