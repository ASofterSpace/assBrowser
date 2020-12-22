/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.web;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.console.ConsoleResult;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.utils.TextEncoding;
import com.asofterspace.toolbox.virtualEmployees.SideBarCtrl;
import com.asofterspace.toolbox.virtualEmployees.SideBarEntry;
import com.asofterspace.toolbox.web.WebRequestFormData;
import com.asofterspace.toolbox.web.WebRequestFormDataBlock;
import com.asofterspace.toolbox.web.WebServer;
import com.asofterspace.toolbox.web.WebServerAnswer;
import com.asofterspace.toolbox.web.WebServerAnswerBasedOnFile;
import com.asofterspace.toolbox.web.WebServerAnswerInHtml;
import com.asofterspace.toolbox.web.WebServerAnswerInJson;
import com.asofterspace.toolbox.web.WebServerRequestHandler;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ServerRequestHandler extends WebServerRequestHandler {

	private Database database;

	private Directory serverDir;

	private ConsoleCtrl consoleCtrl;


	public ServerRequestHandler(WebServer server, Socket request, Directory webRoot, Directory serverDir,
		Database database, ConsoleCtrl consoleCtrl) {

		super(server, request, webRoot);

		this.database = database;

		this.serverDir = serverDir;

		this.consoleCtrl = consoleCtrl;
	}

	@Override
	protected void handlePost(String fileLocation) throws IOException {

		WebServerAnswer answer = new WebServerAnswerInJson("{\"success\": true}");

		if (fileLocation.equals("/uploadFile")) {
			WebRequestFormData formData = receiveFormDataContent(StandardCharsets.ISO_8859_1);
			WebRequestFormDataBlock fileBlock = formData.getByName("file");
			String fileContent = fileBlock.getContent();
			WebRequestFormDataBlock fileNameBlock = formData.getByName("filename");
			String fileNameContent = fileNameBlock.getContent().trim();
			WebRequestFormDataBlock pathBlock = formData.getByName("path");
			String pathContent = pathBlock.getContent().trim();
			String path = PathCtrl.ensurePathIsSafe(pathContent);
			path = resolvePath(path);
			Directory parentDir = new Directory(path);
			TextFile uploadedFile = new TextFile(parentDir, fileNameContent);
			uploadedFile.setEncoding(TextEncoding.ISO_LATIN_1);
			uploadedFile.saveContent(fileContent);
			System.out.println("b0: " + (int) fileContent.charAt(0) + " b1: " + (int) fileContent.charAt(1));
			Map<String, String> arguments = new HashMap<>();
			arguments.put("path", pathContent);
			answer = generateAnswerToMainGetRequest(arguments, "The file has been uploaded!");
			respond(200, answer);
			return;
		}

		String jsonData = receiveJsonContent();

		if (jsonData == null) {
			respond(400);
			return;
		}

		WebServerAnswer sideBarAnswer = SideBarCtrl.handlePost(fileLocation, jsonData);
		if (sideBarAnswer != null) {
			respond(200, sideBarAnswer);
			return;
		}

		JSON json;
		try {
			json = new JSON(jsonData);
		} catch (JsonParseException e) {
			respond(400);
			return;
		}

		switch (fileLocation) {

			case "/openFolderInOS":
				if (json.getString("path") == null) {
					respond(403);
				}
				String localPath = resolvePath(json.getString("path"));
				GuiUtils.openFolder(localPath);
				break;

			default:
				respond(404);
				return;
		}

		respond(200, answer);
	}

	@Override
	protected WebServerAnswer answerGet(String location, Map<String, String> arguments) {

		if (!location.startsWith("/")) {
			location = "/" + location;
		}

/*
		if ("/task".equals(location)) {
			String id = arguments.get("id");
			if (id != null) {
				Task task = taskCtrl.getTaskById(id);
				if (task != null) {
					JSON response = new JSON(Record.emptyObject());
					response.set("success", true);
					response.set("title", task.getTitle());
					return new WebServerAnswerInJson(response);
				}
			}
		}
*/

		if (location.startsWith("/index.htm") || location.startsWith("/index") ||
			location.equals("/") || location.startsWith("/?")) {

			return generateAnswerToMainGetRequest(arguments, null);
		}

		return null;
	}

	private WebServerAnswer generateAnswerToMainGetRequest(Map<String, String> arguments, String message) {

		TextFile indexBaseFile = new TextFile(webRoot, "index.htm");
		String indexContent = indexBaseFile.getContent();

		indexContent = StrUtils.replaceAll(indexContent, "[[SIDEBAR]]",
			SideBarCtrl.getSidebarHtmlStr(SideBarEntry.BROWSER));

		String path = arguments.get("path");
		String fileName = arguments.get("file");
		String consoleValue = "";

		path = PathCtrl.ensurePathIsSafe(path);

		// interpret console commands - in case we do a cd, the path has to be changed here - not earlier,
		// not later
		if (arguments.get("console") != null) {
			ConsoleResult consoleResult = consoleCtrl.interpretCommand(arguments.get("console"), path);
			String newPath = consoleResult.getPath();
			consoleValue = consoleResult.getCommand();

			newPath = PathCtrl.ensurePathIsSafe(newPath);

			if (!newPath.equals(path)) {
				fileName = null;
				path = newPath;
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[CONSOLE_VALUE]]", consoleValue);

		// if path starts with the local path of the Desktop, replace it with /Desktop/
		String pathCompare = path + "/";
		String desktopLocation = database.getDesktopLocation();
		desktopLocation = PathCtrl.ensurePathIsSafe(desktopLocation) + "/";
		if (pathCompare.startsWith(desktopLocation)) {
			pathCompare = PathCtrl.DESKTOP + "/" + pathCompare.substring(desktopLocation.length());
			path = PathCtrl.ensurePathIsSafe(pathCompare);
		}

		consoleCtrl.addPath(path);

		String localPath = resolvePath(path);

		Directory folder = new Directory(localPath);

		if ("download".equals(arguments.get("action"))) {
			return new WebServerAnswerBasedOnFile(new File(folder, fileName));
		}

		StringBuilder folderContent = new StringBuilder();

		boolean recursively = false;

		List<Directory> childFolders = folder.getAllDirectories(recursively);
		List<File> childFiles = folder.getAllFiles(recursively);

		SimpleFile vstpuFile = new SimpleFile(folder, "VSTPU.stpu");
		if (vstpuFile.exists()) {
			vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
			List<String> entries = vstpuFile.getContents();

			Map<String, Directory> directories = new HashMap<>();
			for (Directory childFolder : childFolders) {
				directories.put(childFolder.getLocalDirname().toLowerCase(), childFolder);
			}

			Map<String, File> files = new HashMap<>();
			for (File file : childFiles) {
				files.put(file.getLocalFilename().toLowerCase(), file);
			}

			for (String entry : entries) {
				Directory curDir = directories.get(entry.toLowerCase());
				if (curDir != null) {
					addFolderToHtml(folderContent, curDir, path);
				} else {
					File curFile = files.get(entry.toLowerCase() + ".stpu");
					if (curFile != null) {
						addFileToHtml(folderContent, entry, curFile, path);
					} else {
						addTextToHtml(folderContent, entry);
					}
				}
			}
		} else {
			for (Directory childFolder : childFolders) {
				addFolderToHtml(folderContent, childFolder, path);
			}

			if (childFolders.size() > 0) {
				addTextToHtml(folderContent, "");
			}

			for (File childFile : childFiles) {
				addFileToHtml(folderContent, childFile.getLocalFilename(), childFile, path);
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[FOLDER_CONTENT]]", folderContent.toString());

		indexContent = StrUtils.replaceAll(indexContent, "[[CONSOLE]]", consoleCtrl.getHtmlStr());

		JSON jsonData = new JSON(Record.emptyObject());
		jsonData.set("path", path);
		jsonData.set("file", fileName);
		indexContent = StrUtils.replaceAll(indexContent, "[[DATA]]", jsonData.toString());

		indexContent = StrUtils.replaceAll(indexContent, "[[PATH]]", path);


		// buttonBar
		StringBuilder buttonHtml = new StringBuilder();

		buttonHtml.append("<a href=\"/?path=" + path);
		if (fileName != null) {
			buttonHtml.append("&file=" + fileName);
		}
		buttonHtml.append("&console=cd ..\" class='button'>");
		buttonHtml.append("One Folder Up");
		buttonHtml.append("</a>");

		if (fileName != null) {
			buttonHtml.append(getDownloadButtonHtml(path, fileName, ""));
		}

		buttonHtml.append("<span class='button' onclick='browser.openUploadModal()'>");
		buttonHtml.append("Upload a File");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.openFolderInOS()'>");
		buttonHtml.append("Open Folder in OS");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.expandConsole()' id='expandConsoleBtn'>");
		buttonHtml.append("Expand Console");
		buttonHtml.append("</span>");

		indexContent = StrUtils.replaceAll(indexContent, "[[BUTTONS]]", buttonHtml.toString());


		if (message == null) {
			message = "";
		}
		if (!message.equals("")) {
			message = "alert(\"" + message + "\")";
		}
		indexContent = StrUtils.replaceAll(indexContent, "[[EXTRA_MESSAGE]]", message);


		String fileHtmlStr = "";

		if (fileName != null) {
			File genericFile = new File(folder, fileName);
			if (!genericFile.exists()) {
				fileHtmlStr = "The file '" + fileName + "' does not exist!";
			} else {
				String lowCaseFileName = fileName.toLowerCase();
				if (lowCaseFileName.endsWith(".stpu") || lowCaseFileName.endsWith(".txt") ||
					lowCaseFileName.endsWith(".ini")) {
					TextFile file = new TextFile(folder, fileName);
					file.setEncoding(TextEncoding.ISO_LATIN_1);
					fileHtmlStr = file.getContent();
					fileHtmlStr = HTML.escapeHTMLstr(fileHtmlStr);
					fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "&#10;", "<br>");
					fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, " ", "&nbsp;");
				} else if (lowCaseFileName.endsWith(".json")) {
					TextFile file = new TextFile(folder, fileName);
					fileHtmlStr = file.getContent();
					fileHtmlStr = HTML.escapeHTMLstr(fileHtmlStr);
					fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "&#10;", "<br>");
					fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, " ", "&nbsp;");
				} else if (lowCaseFileName.endsWith(".jpg") || lowCaseFileName.endsWith(".jpeg") ||
					lowCaseFileName.endsWith(".png")) {
					fileHtmlStr = "<img src=\"/?path=" + path +
						"&file=" + fileName +
						"&action=download\" style='max-width:99%; max-height:99%;' />";
				} else {
					fileHtmlStr = "No preview for '" + fileName + "' available.<br><br>" +
								  getDownloadButtonHtml(path, fileName, "padding: 4pt 9pt;");
				}
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[FILE_CONTENT]]", fileHtmlStr);

		return new WebServerAnswerInHtml(indexContent);
	}

	private void addFolderToHtml(StringBuilder folderContent, Directory childFolder, String path) {
		folderContent.append("<a href=\"/?path=" + path + "/");
		folderContent.append(childFolder.getLocalDirname() + "\">");
		folderContent.append("<div class='line'>");
		folderContent.append(HTML.escapeHTMLstr(childFolder.getLocalDirname()));
		folderContent.append("</div>");
		folderContent.append("</a>");
	}

	private void addFileToHtml(StringBuilder folderContent, String filename, File childFile, String path) {
		folderContent.append("<a href=\"/?path=" + path);
		folderContent.append("&file=" + childFile.getLocalFilename() + "\">");
		folderContent.append("<div class='line'>");
		folderContent.append(HTML.escapeHTMLstr(filename));
		folderContent.append("</div>");
		folderContent.append("</a>");
	}

	private void addTextToHtml(StringBuilder folderContent, String text) {
		folderContent.append("<div>");
		if ("".equals(text)) {
			folderContent.append("&nbsp;");
		} else {
			folderContent.append(HTML.escapeHTMLstr(text));
		}
		folderContent.append("</div>");
	}

	@Override
	protected File getFileFromLocation(String location, String[] arguments) {

		File sideBarImageFile = SideBarCtrl.getSideBarImageFile(location);
		if (sideBarImageFile != null) {
			return sideBarImageFile;
		}

		String locEquiv = getWhitelistedLocationEquivalent(location);

		// if no root is specified, then we are just not serving any files at all
		// and if no location equivalent is found on the whitelist, we are not serving this request
		if ((webRoot != null) && (locEquiv != null)) {

			// serves images and text files directly from the server dir, rather than the deployed dir
			if (locEquiv.toLowerCase().endsWith(".jpg") || locEquiv.toLowerCase().endsWith(".pdf") ||
				locEquiv.toLowerCase().endsWith(".png") || locEquiv.toLowerCase().endsWith(".stp") ||
				locEquiv.toLowerCase().endsWith(".txt") || locEquiv.toLowerCase().endsWith(".stpu") ||
				locEquiv.toLowerCase().endsWith(".json")) {

				File result = new File(serverDir, locEquiv);
				if (result.exists()) {
					return result;
				}
			}

			// actually get the file
			return webRoot.getFile(locEquiv);
		}

		// if the file was not found on the whitelist, do not return it
		// - even if it exists on the server!
		return null;
	}

	private String resolvePath(String path) {

		if (PathCtrl.startsWithDesktopPath(path)) {
			String desktopLocation = database.getDesktopLocation();
			desktopLocation = PathCtrl.oneUp(desktopLocation);
			return StrUtils.replaceAll(
				StrUtils.replaceAll(desktopLocation + "/" + path, "\\", "/"),
				"//", "/");
		}

		return path;
	}

	private String getDownloadButtonHtml(String path, String fileName, String style) {
		return "<a href=\"/?path=" + path +
			   "&file=" + fileName +
			   "&action=download\" target='_blank' " +
			   "class='button'" +
			   "style='" + style + "'>" +
			   "Download Current File" +
			   "</a>";
	}

}
