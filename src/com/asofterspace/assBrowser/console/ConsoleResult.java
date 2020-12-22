/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;


public class ConsoleResult {

	// the preset command of the console
	// (most of the time, the console is empty by default, but if e.g. mo:1+1 was entered,
	// then the preset console content is now mo: 1+1 = 2)
	private String command;

	// the current path of the console
	private String path;


	public ConsoleResult(String command, String path) {
		this.command = command;
		this.path = path;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

}
