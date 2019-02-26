package com.googlecode.opennars.parser;

/**
 * An input exception from a NARS parser.
 * @author jgeldart
 *
 */
public class InvalidInputException extends Exception {

	/**
	 * Autogenerated serial code
	 */
	private static final long serialVersionUID = 325617403436238787L;

	public InvalidInputException() {
		super("Unknown input error");
	}

	public InvalidInputException(String message) {
		super(message);
	}

	public InvalidInputException(Throwable cause) {
		super(cause);
	}

	public InvalidInputException(String message, Throwable cause) {
		super(message, cause);
	}

}
