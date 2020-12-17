/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;

import java.util.ArrayList;
import java.util.List;


public class ConsoleCtrl {

	List<String> history = new ArrayList<>();


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

}
