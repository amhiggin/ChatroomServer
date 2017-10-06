package main.java;

public enum Request {
	Join("Join"), Chat("Chat"), Leave("Leave");

	private String value;

	Request(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
