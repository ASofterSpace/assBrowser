/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;

import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;

import java.util.ArrayList;
import java.util.List;


public class ConsoleCtrl {

	private List<String> history = new ArrayList<>();

	private Database database;


	public ConsoleCtrl(Database database) {
		this.database = database;
	}

	public void addPath(String path) {
		history.add(path);
	}

	public String getHtmlStr() {

		StringBuilder html = new StringBuilder();

		String sep = "";

		for (String historyLine : history) {
			html.append(sep);
			sep = "<br>";
			html.append(historyLine);
		}

		return html.toString();
	}

	/**
	 * Return the state of the path after executing the command
	 */
	public String interpretCommand(String command, String previousPath) {

		if (history.size() > 0) {
			history.set(history.size() - 1, history.get(history.size() - 1) + " > " + command);
		}

		command = command.trim();
		if (command.equals("cd")) {
			return PathCtrl.DESKTOP;
		}
		if (command.startsWith("cd ")) {
			command = command.substring(3).trim();
			if (command.equals(".")) {
				return previousPath;
			}
			if (command.equals("..")) {
				String result = PathCtrl.oneUp(previousPath);
				if (result.length() < 2) {
					if (PathCtrl.startsWithDesktopPath(previousPath)) {
						result = PathCtrl.oneUp(database.getDesktopLocation());
					}
				}
				return result;
			}
			if (command.equals("~")) {
				return PathCtrl.DESKTOP;
			}
			return previousPath + "/" + command;
		}

		history.add("ERROR: Command '" + command + "' not understood!");
		return previousPath;
	}

}
