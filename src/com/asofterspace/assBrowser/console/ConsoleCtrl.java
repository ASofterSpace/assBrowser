/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;

import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.gui.GUI;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.utils.TextEncoding;

import java.util.ArrayList;
import java.util.List;


public class ConsoleCtrl {

	private List<String> history = new ArrayList<>();

	private Database database;

	private String prevMathOrgResult = "0";

	private GUI gui = null;


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
	public ConsoleResult interpretCommand(String command, String previousPath, boolean calledFromOutside) {

		ConsoleResult result = new ConsoleResult("", previousPath);

		String origHistoryLine = "";
		if (history.size() > 0) {
			origHistoryLine = history.get(history.size() - 1);
			history.set(history.size() - 1, origHistoryLine + " > " + command);
		}

		command = command.trim();
		while (command.endsWith(";")) {
			command = command.substring(0, command.length() - 1).trim();
		}
		String commandLow = command.toLowerCase();


		// POSIX: cd (change directory)
		if (commandLow.equals("..")) {
			command = "cd ..";
			commandLow = command;
		}
		if (commandLow.equals("...")) {
			command = "cd ../..";
			commandLow = command;
		}
		if (commandLow.startsWith(PathCtrl.DESKTOP.toLowerCase())) {
			command = "cd " + command;
			commandLow = "cd " + commandLow;
		}
		if (commandLow.equals("cd") || commandLow.equals("cd ~")) {
			result.setPath(PathCtrl.DESKTOP);
			return result;
		}
		if (("cd /".equals(commandLow)) || ("cd \\".equals(commandLow))) {
			result.setPath("");
			return result;
		}
		if (commandLow.startsWith("cd ")) {
			command = command.substring(3).trim() + "/";
			commandLow = commandLow.substring(3).trim() + "/";
			command = StrUtils.replaceAll(command, "\\", "/");
			commandLow = StrUtils.replaceAll(commandLow, "\\", "/");

			// if we have ~/blubb...
			if (command.startsWith("~")) {
				// then we have /Desktop/blubb
				previousPath = PathCtrl.DESKTOP;
				command = command.substring(2);
			}
			// if we have \Desktop\blubb...
			if (commandLow.startsWith(StrUtils.replaceAll(PathCtrl.DESKTOP, "\\", "/").toLowerCase())) {
				// then we have /Desktop/blubb
				previousPath = PathCtrl.DESKTOP;
				command = command.substring(PathCtrl.DESKTOP.length());
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
			result.setPath(previousPath + "/" + command);
			return result;
		}


		// switch to disk overview
		if (("/".equals(commandLow)) || ("\\".equals(commandLow))) {
			result.setPath("");
			return result;
		}


		// switch to different disk
		if ((commandLow.length() == 1) ||
			((commandLow.length() == 2) && (commandLow.charAt(1) == ':')) ||
			((commandLow.length() == 3) && (commandLow.charAt(1) == ':') && (commandLow.charAt(2) == '/')) ||
			((commandLow.length() == 3) && (commandLow.charAt(1) == ':') && (commandLow.charAt(2) == '\\'))) {
			result.setPath(commandLow.toUpperCase().charAt(0) + ":/");
			if (commandLow.startsWith("c")) {
				result.setPath("C:/home/");
			}
			return result;
		}


		// math org

		if (commandLow.startsWith("mo:")) {
			result.setCommand("mo: " + computeMathOrg(command.substring(3)));
			if (history.size() > 0) {
				history.set(history.size() - 1, origHistoryLine + " > " + result.getCommand());
			}
			return result;
		}


		// shell execute

		if (commandLow.startsWith("se:")) {
			IoUtils.executeAsync(command.substring(3).trim());
			return result;
		}


		// shutdown or reboot

		if (commandLow.equals("shutdown")) {
			// for Windows
			IoUtils.executeAsync("shutdown -s -t 1");
			// for Linux
			IoUtils.executeAsync("sudo poweroff");
			return result;
		}
		if (commandLow.equals("reboot")) {
			// for Windows
			IoUtils.executeAsync("shutdown -r -t 1");
			// for Linux
			IoUtils.executeAsync("sudo reboot");
			return result;
		}


		// close

		if (commandLow.equals("close") || commandLow.equals("exit") || commandLow.equals("quit")) {
			if (gui != null) {
				gui.close();
			}
			System.exit(0);
			return result;
		}


		// sll files

		String desktopLocation = database.getDesktopLocation();
		Directory desktopDir = new Directory(desktopLocation);
		Directory systemDir = new Directory(desktopDir, "system");
		boolean recursively = false;
		List<File> sllFiles = systemDir.getAllFilesEndingWith(".sll", recursively);
		String matchWith = commandLow;
		while (matchWith.length() < 3) {
			matchWith += '_';
		}
		matchWith += ".sll";
		for (File file : sllFiles) {
			if (matchWith.equals(file.getLocalFilename().toLowerCase())) {
				result = runSllFile(file, result);
				return result;
			}
		}


		// only if called from the outside (not e.g. from an sll file)...
		if (calledFromOutside) {
			// ... interpret as beginning of the name of an article in the currently opened directory
			Directory osDir = new Directory(PathCtrl.resolvePath(previousPath));

			// if a vstpu file exists, use that one to find priority among potential entries to be opened
			SimpleFile vstpuFile = new SimpleFile(osDir, "VSTPU.stpu");
			vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
			if (vstpuFile.exists()) {
				List<String> vstpuLines = vstpuFile.getContents();
				for (String vstpuLine : vstpuLines) {
					if (vstpuLine.toLowerCase().startsWith(commandLow)) {
						// found one! actually open the file...
						result.setPath(previousPath + "/" + vstpuLine);
						return result;
					}
				}
			} else {
				recursively = false;
				for (File file : osDir.getAllFiles(recursively)) {
					String locName = file.getLocalFilename();
					if (locName.toLowerCase().startsWith(commandLow)) {
						// found one! actually open the file...
						result.setPath(previousPath + "/" + locName);
						return result;
					}
				}
			}
		}


		history.add("ERROR: Command '" + command + "' not understood!");
		return result;
	}

	/**
	 * Enter stuff like 4 + 5 or 6 * 6 = 2
	 * Returns stuff like 4 + 5 = 9 or 6 * 6 = 36
	 */
	private String computeMathOrg(String command) {

		if (command.contains("=")) {
			command = command.substring(0, command.indexOf("="));
		}

		String cur = command.toLowerCase();

		cur = StrUtils.replaceAll(cur, "ans", "(" + prevMathOrgResult + ")");

		cur = StrUtils.replaceAll(cur, "pi", "(3,141592653589793238)");

		cur = StrUtils.replaceAll(cur, ".", ",");
		cur = StrUtils.replaceAll(cur, "•", "*");
		cur = StrUtils.replaceAll(cur, "·", "*");
		cur = StrUtils.replaceAll(cur, "x", "*");
		cur = StrUtils.replaceAll(cur, "×", "*");
		cur = StrUtils.replaceAll(cur, "-/", "\\");
		cur = StrUtils.replaceAll(cur, "/", ":");

		String result = computeMathOrgCalculate(cur);
		result = StrUtils.replaceAll(result, ",", ".");

		prevMathOrgResult = result;

		return command.trim() + " = " + result;
	}

	/**
	 * Actually perform the mathorg calculation by first checking if there are brackets present, and if so,
	 * recursively removing them by calculating inner parts, and if not, by splitting the input on math ops
	 * and applying one after the other
	 * (the input is called vars as this was directly translated from datacomx source code)
	 */
	private String computeMathOrgCalculate(String vars) {

		if (vars.contains("(")) {

			int lastStartPos = -1;
			for (int i = 0; i < vars.length(); i++) {
				char c = vars.charAt(i);
				switch (c) {
					case '(':
						lastStartPos = i;
						break;
					case ')':
						if (lastStartPos >= 0) {
							return computeMathOrgCalculate(
								vars.substring(0, lastStartPos) +
								computeMathOrgCalculate(vars.substring(lastStartPos + 1, i)) +
								vars.substring(i + 1)
							);
						}
						return "ERROR: Encountered unmatched ')'!";
				}
			}
			return "ERROR: Encountered unmatched '('!";
		}

		if (vars.contains(")")) {
			return "ERROR: Encountered unmatched ')'!";
		}

		vars = StrUtils.replaceAll(vars, " ", "");
		vars = StrUtils.replaceAll(vars, "\t", "");
		vars = StrUtils.replaceAll(vars, "/", ":");
		vars = StrUtils.replaceAll(vars, "-", "+-");
		vars = StrUtils.replaceAll(vars, "*+-", "*-");
		vars = StrUtils.replaceAll(vars, ":+-", ":-");
		while (vars.contains("++")) {
			vars = StrUtils.replaceAll(vars, "++", "+");
		}
		while (vars.startsWith("+")) {
			if (vars.length() > 1) {
				vars = vars.substring(2);
			} else {
				return "ERROR: No input!";
			}
		}

		// ensure the first term starts with a plus
		vars = "+" + vars;

		// we split the string that is operated on into terms, with each term starting with a math operation
		// followed by a number (where the number itself may contain the char '\' to indicate root, so e.g.
		// a term can be +1 (add one) or :4 (divide by four) or -3/9 (minus the third root of nine))
		List<String> terms = new ArrayList<>();
		String currentTerm = "";
		for (int i = 0; i < vars.length(); i++) {
			char c = vars.charAt(i);
			switch (c) {
				case '*':
				case ':':
				case '+':
				case '^':
					terms.add(currentTerm);
					currentTerm = "" + c;
					break;
				default:
					currentTerm += c;
					break;
			}
		}
		terms.add(currentTerm);

		// iterate over all terms and resolve potentially existing roots
		for (int i = 0; i < terms.size(); i++) {
			String curTerm = terms.get(i);
			int pos = curTerm.indexOf("\\");
			// \ cannot be the first letter, as that is the math op
			if (pos > 0) {
				// if it is the second letter, we have a square root
				Double whichRoot = 2.0;
				if (pos > 1) {
					// if it is the third letter or later, we have a special root
					String whichRootStr = curTerm.substring(2, pos);
					whichRoot = StrUtils.strToDouble(whichRootStr);
					if (whichRoot == null) {
						return "ERROR: '" + whichRootStr + "'-th root cannot be parsed!";
					}
				}
				String underRootStr = curTerm.substring(pos + 1);
				Double underRoot = StrUtils.strToDouble(underRootStr);
				if (underRoot == null) {
					return "ERROR: Term under root '" + underRootStr + "' cannot be parsed!";
				}
				terms.set(i, "" + curTerm.charAt(0) + Math.pow(underRoot, 1 / whichRoot));
			}
		}

		// perform the actual computation of the terms
		double result = 0.0;

		for (String term : terms) {

			// a term that is just an op without a number does not do anything
			if (term.length() < 2) {
				continue;
			}

			char ops = term.charAt(0);

			Double termNum = StrUtils.strToDouble(term.substring(1));
			if (termNum == null) {
				return "ERROR: The term '" + term.substring(1) + "' could not be parsed!";
			}

			switch (ops) {
				case '+':
					result += termNum;
					break;
				case '*':
					result *= termNum;
					break;
				case ':':
					result /= termNum;
					break;
				case '^':
					result = Math.pow(result, termNum);
					break;
			}
		}

		return StrUtils.doubleToStr(result);
	}

	private ConsoleResult runSllFile(File sllFile, ConsoleResult result) {

		SimpleFile sllSimpleFile = new SimpleFile(sllFile);

		String content = sllSimpleFile.getContent();

		if (content.contains("%")) {
			SimpleFile envVarsFile = new SimpleFile(sllFile.getParentDirectory(), "Umgebungsvariablen.stpu");
			envVarsFile.setEncoding(TextEncoding.ISO_LATIN_1);
			List<String> envVars = envVarsFile.getContents();
			for (int i = 0; i < envVars.size(); i++) {
				if (envVars.get(i).endsWith("%")) {
					content = StrUtils.replaceAll(content, "%" + envVars.get(i),
						envVars.get(i+1));
					i++;
				}
			}
		}

		String originalLineEndStr = StrUtils.detectLineEndStr(content);
		String[] lines = content.split(originalLineEndStr);

		for (String line : lines) {
			if (line.trim().equals("")) {
				continue;
			}

			result = interpretCommand(line, result.getPath(), false);
		}

		return result;
	}

	public void setGUI(GUI gui) {
		this.gui = gui;
	}

}
