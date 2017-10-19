package main.java;

/*
 * Enum for possible valid requests from a client.
 */

public enum ClientRequest {
	HELO_TEXT("HELO text\n"), JOIN_CHATROOM("JOIN_CHATROOM"), CHAT("CHAT"), LEAVE_CHATROOM(
			"LEAVE_CHATROOM"), DISCONNECT("DISCONNECT");

	private String value;

	ClientRequest(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
