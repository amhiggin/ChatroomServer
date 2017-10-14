package main.java;

/*
 * Enum for possible valid requests from a client.
 */

public enum Request {
	HELO_TEXT("HELO text\n"), JOIN_CHATROOM("JOIN_CHATROOM"), CHAT("CHAT"), LEAVE_CHATROOM(
			"LEAVE_CHATROOM"), KILL_SERVICE("KILL_SERVICE\n"), DISCONNECT("DISCONNECT");

	private String value;

	Request(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
