/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.console;

import com.asofterspace.assBrowser.AssBrowser;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.gui.GUI;
import com.asofterspace.assBrowser.gui.ShutdownLaterGUI;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.MathUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.awt.Desktop;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


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
		if (commandLow.equals("cd..")) {
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

			if (command.length() > 2) {
				// if we have C:/, D:/, stuff like that on Windows...
				if (((command.charAt(1) == ':') && (command.charAt(2) == '/')) ||
					// ... or /something on Linux ...
					(command.charAt(0) == '/')) {
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

			// actually check if something starts with command rather than is command in full
			// (as we add a slash in the very beginning, let's remove the trailing slash)
			String commandStart = command;
			while (commandStart.endsWith("/")) {
				commandStart = commandStart.substring(0, commandStart.length() - 1);
			}
			if (commandStart.length() > 0) {
				ConsoleResult res = redirectToPathIfMatchesFileOrFolder(previousPath, commandStart, result);
				if (res != null) {
					return res;
				}
			}

			// even if nothing is found that starts with the command - just hardcoded go in there
			result.setPath(previousPath + "/" + command);
			return result;
		}


		// switch to disk overview
		if (("/".equals(commandLow)) || ("\\".equals(commandLow))) {
			result.setPath("/");
			return result;
		}


		/*
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
		*/


		// math org

		if (commandLow.startsWith("mo:")) {
			result.setCommand("mo: " + computeMathOrg(command.substring(3)));
			if (history.size() > 0) {
				history.set(history.size() - 1, origHistoryLine + " > " + result.getCommand());
			}
			return result;
		}


		// move file

		if (commandLow.startsWith("mv:")) {
			command = command.substring(3).trim();
			Directory homeDir = database.getHomeDir();
			String startStr = "new";
			boolean recursively = false;
			List<File> existingFiles = homeDir.getAllFilesStartingWith(startStr, recursively);
			if (existingFiles.size() < 1) {
				// if no file exists in the home directory, check if there is a single file in the Downloads folder instead...
				Directory downloadsDir = new Directory(homeDir, "Downloads");
				existingFiles = downloadsDir.getAllFiles(recursively);
				if (existingFiles.size() < 1) {
					GuiUtils.complain("No file called new.* exists in " + homeDir.getAbsoluteDirname() + " and none in Downloads - no idea have to move / rename!");
				} else {
					if (existingFiles.size() > 1) {
						GuiUtils.complain("Several files exist in " + homeDir.getAbsoluteDirname() + "/Downloads - no idea which one to choose!");
					} else {
						existingFiles.get(0).rename("../" + command);
					}
				}
			} else {
				if (existingFiles.size() > 1) {
					GuiUtils.complain("Several files called new.* exist in " + homeDir.getAbsoluteDirname() + " - no idea which one to choose!");
				} else {
					existingFiles.get(0).rename(command);
				}
			}
			return result;
		}


		// shell execute

		if (commandLow.startsWith("se:")) {
			command = command.substring(3).trim();
			command = replaceHomeDirStr(command);
			Directory cmdDir = new Directory(command);
			if (cmdDir.exists()) {
				GuiUtils.openFolder(command);
			} else {
				IoUtils.executeAsync(command);
			}
			return result;
		}


		// alert

		if (commandLow.startsWith("alert(") && commandLow.endsWith(")")) {
			command = command.substring(6);
			command = command.substring(0, command.length() - 1);
			command = command.trim();
			if (command.startsWith("\"") || command.startsWith("'")) {
				command = command.substring(1);
			}
			if (command.endsWith("\"") || command.endsWith("'")) {
				command = command.substring(0, command.length() - 1);
			}
			GuiUtils.notify(command);
			return result;
		}


		// shutdown / reboot / timer

		String commandLowNoSpace = StrUtils.replaceAll(commandLow, " ", "");
		commandLowNoSpace = StrUtils.replaceAll(commandLowNoSpace, "\t", "");
		if (commandLowNoSpace.equals("shutdown")) {
			shutdownNow();
			return result;
		}
		if (commandLowNoSpace.equals("reboot")) {
			rebootNow();
			return result;
		}
		if (commandLowNoSpace.equals("shutlater") || commandLowNoSpace.equals("shutdownlater") ||
			commandLowNoSpace.equals("timer")) {
			if (gui != null) {
				ShutdownLaterGUI shutdownLaterGUI = new ShutdownLaterGUI(gui, this);
				if (commandLowNoSpace.equals("timer")) {
					shutdownLaterGUI.show("alert(\"Time is up!\")");
				} else {
					shutdownLaterGUI.show("shutdown");
				}
			} else {
				GuiUtils.complain("Cannot create window as GUI is null!");
			}
			return result;
		}


		// close

		if (commandLowNoSpace.equals("close") || commandLowNoSpace.equals("exit") || commandLowNoSpace.equals("quit")) {
			if (gui != null) {
				gui.close();
			}
			System.exit(0);
			return result;
		}


		// min
		if (commandLowNoSpace.equals("min")) {
			if (gui != null) {
				gui.minimize();
			}
			return result;
		}


		// help

		if (commandLowNoSpace.equals("help")) {
			String helpMessage = AssBrowser.PROGRAM_TITLE + " Help:\n" +
				"\n" +
				"help .. shows this help\n" +
				"about .. shows an about message\n" +
				"close / exit / quit .. exits the program\n" +
				"min .. minimizes the GUI to a 1px high bar at the top\n" +
				"shutdown .. shuts down the computer\n" +
				"reboot .. reboots the computer\n" +
				"shutdownlater / shutlater .. shuts down the computer after some time\n" +
				"timer .. opens a generic timer GUI\n" +
				"alert([xyz]) .. shows an alert message popup\n" +
				"cd [xyz] .. navigates into a certain directory\n" +
				"se: [xyz] .. executes [xyz] as OS shell command\n" +
				"mo: [xyz] .. call the MathOrg for the mathematical formula [xyz]\n" +
				"mv: [xyz] .. move a file called " + database.getHomeDirPathStr() + "/new.* to " + database.getHomeDirPathStr() + "/[xyz]\n" +
				"grep / sed / find .. shows syntax of common commandline calls\n" +
				"\n" +
				"Block commands in entries:\n" +
				"_, **, ` .. italic style, bold style, code style\n" +
				"%SPOILER% .. spoiler block\n" +
				"%WORDCOUNT% .. count words in block\n" +
				"picture X, pictures up to X .. inline pictures\n";
			GuiUtils.notify(helpMessage);
			return result;
		}


		// commandline syntax reminders

		String syntaxReminder = applyCommandlineSyntaxReminder(commandLow);
		if (syntaxReminder != null) {
			result.setCommand(syntaxReminder);
			if (history.size() > 0) {
				history.set(history.size() - 1, origHistoryLine + " > " + result.getCommand());
			}
			return result;
		}


		if (commandLowNoSpace.equals("about")) {
			String aboutMessage = "This is the " + AssBrowser.PROGRAM_TITLE + ".\n" +
				"Version: " + AssBrowser.VERSION_NUMBER + " (" + AssBrowser.VERSION_DATE + ")\n" +
				"Brought to you by: A Softer Space";
			GuiUtils.notify(aboutMessage);
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
		File sllFileToRunWithArgs = null;
		String sllFileArgs = null;
		for (File file : sllFiles) {
			String sllFileName = file.getLocalFilename().toLowerCase();
			if (sllFileName.endsWith("%.sll")) {
				if (matchWith.startsWith(sllFileName.substring(0, sllFileName.length() - 5) + " ")) {
					sllFileToRunWithArgs = file;
					sllFileArgs = command.substring(sllFileName.length() - 4);
				}
			} else {
				if (matchWith.equals(sllFileName)) {
					return runSllFile(file, result, null);
				}
			}
		}
		if (sllFileToRunWithArgs != null) {
			return runSllFile(sllFileToRunWithArgs, result, sllFileArgs);
		}


		// only if called from the outside (not e.g. from an sll file)...
		if (calledFromOutside) {
			ConsoleResult res = redirectToPathIfMatchesFileOrFolder(previousPath, replaceHomeDirStr(commandLow), result);
			if (res != null) {
				return res;
			}
		}


		// if this is the name of a folder, open it...
		command = replaceHomeDirStr(command);
		Directory openDir = new Directory(command);
		if (openDir.exists()) {
			GuiUtils.openFolder(command);
			return result;
		}


		history.add("ERROR: Command '" + command + "' not understood!");
		return result;
	}

	private static ConsoleResult redirectToPathIfMatchesFileOrFolder(String previousPath, String command, ConsoleResult result) {

		String commandLow = command.trim().toLowerCase();

		// ... interpret as beginning of the name of an article in the currently opened directory
		Directory osDir = new Directory(PathCtrl.resolvePath(previousPath));

		// if a vstpu file exists, use that one to find priority among potential entries to be opened
		SimpleFile vstpuFile = PathCtrl.getVSTPUfile(osDir);
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
			boolean recursively = false;
			for (File file : osDir.getAllFiles(recursively)) {
				String locName = file.getLocalFilename();
				if (locName.toLowerCase().startsWith(commandLow)) {
					// found one! actually open the file...
					result.setPath(previousPath + "/" + locName);
					return result;
				}
			}
		}

		// return null if nothing is matched
		return null;
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

		String result = MathUtils.calculateMathStr(cur);

		prevMathOrgResult = result;

		// automatically copy computation result to clipboard
		GuiUtils.copyToClipboard(result);

		return command.trim() + " = " + result;
	}

	private ConsoleResult runSllFile(File sllFile, ConsoleResult result, String sllFileArgs) {

		TextFile sllSimpleFile = PathCtrl.getEntryFile(sllFile);
		String content = sllSimpleFile.getContent();

		content = StrUtils.replaceAll(content, "%args%", sllFileArgs);

		if (content.contains("%")) {
			SimpleFile envVarsFile = PathCtrl.getSimpleEntryFile(new File(sllFile.getParentDirectory(), "Umgebungsvariablen.stpu"));
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

	public static void shutdownNow() {
		if (AssBrowser.BACKUP_RUN_FILE.exists()) {
			GuiUtils.notify("Backup run in progress, shutdown aborted.\n" +
				"(" + AssBrowser.BACKUP_RUN_FILE.getCanonicalFilename() + " exists.)");
			return;
		}

		if ("\\".equals(System.lineSeparator())) {
			// for Windows
			IoUtils.executeAsync("shutdown -s -t 1");
		} else {
			// for Linux
			IoUtils.executeAsync("/shutdown.sh");
		}
	}

	public static void rebootNow() {
		if (AssBrowser.BACKUP_RUN_FILE.exists()) {
			GuiUtils.notify("Backup run in progress, reboot aborted.\n" +
				"(" + AssBrowser.BACKUP_RUN_FILE.getCanonicalFilename() + " exists.)");
			return;
		}

		if ("\\".equals(System.lineSeparator())) {
			// for Windows
			IoUtils.executeAsync("shutdown -r -t 1");
		} else {
			// for Linux
			IoUtils.executeAsync("/reboot.sh");
		}
	}

	public String applyCommandlineSyntaxReminder(String commandLow) {
		if (commandLow == null) {
			return null;
		}

		Map<String, String> commandlineSyntaxReminders = database.getCommandlineSyntaxReminders();
		if (commandlineSyntaxReminders == null) {
			return null;
		}

		return commandlineSyntaxReminders.get(commandLow.trim());
	}

	private String replaceHomeDirStr(String commandStr) {
		if (commandStr.contains(" ~")) {
			commandStr = StrUtils.replaceAll(commandStr, " ~", " " + database.getHomeDir().getCanonicalDirname());
		}
		return commandStr;
	}

}
