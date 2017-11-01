package main.java;

/*
 * Enum for possible valid requests from a client.
 */

public enum ClientRequest {
	HELO("HELO"), JOIN_CHATROOM("JOIN_CHATROOM"), CHAT("CHAT"), LEAVE_CHATROOM("LEAVE_CHATROOM"), DISCONNECT(
			"DISCONNECT"), KILL_SERVICE("KILL SERVICE\n");

	private String value;

	ClientRequest(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
