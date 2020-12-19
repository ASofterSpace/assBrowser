/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;

import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.ArrayList;
import java.util.List;


public class ConsoleCtrl {

	private List<String> history = new ArrayList<>();

	private Database database;


	public ConsoleCtrl(Database database) {
		this.database = database;
	}

	public void addPath(String path) {
		if (history.size() > 0) {
			if (path.equals(history.get(history.size() - 1))) {
				return;
			}
		}
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
			command = command.substring(3).trim() + "/";
			command = StrUtils.replaceAll(command, "\\", "/");

			// if we have ~/blubb...
			if (command.startsWith("~")) {
				// then we have /Desktop/blubb
				previousPath = PathCtrl.DESKTOP;
				command = command.substring(2);
			}
			command = StrUtils.replaceAll(command, "\\", "/");
			command = StrUtils.replaceAll(command, "//", "/");

			// if we have C:/, D:/, stuff like that...
			if (command.length() > 2) {
				if ((command.charAt(1) == ':') && (command.charAt(2) == '/')) {
					// use an absolute path!
					previousPath = "";
				}
			}

			while (command.startsWith("./")) {
				command = command.substring(2);
			}

			while (command.equals("..") || command.startsWith("../")) {
				if (command.equals("..")) {
					command = "";
				} else {
					command = command.substring(3);
				}
				String previousPreviousPath = previousPath;
				previousPath = PathCtrl.oneUp(previousPath);
				if (previousPath.length() < 2) {
					if (PathCtrl.startsWithDesktopPath(previousPreviousPath)) {
						previousPath = PathCtrl.oneUp(database.getDesktopLocation());
					}
				}
			}
			return previousPath + "/" + command;
		}

		history.add("ERROR: Command '" + command + "' not understood!");
		return previousPath;
	}

}
