/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.web;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.virtualEmployees.SideBarCtrl;
import com.asofterspace.toolbox.virtualEmployees.SideBarEntry;
import com.asofterspace.toolbox.web.WebServer;
import com.asofterspace.toolbox.web.WebServerAnswer;
import com.asofterspace.toolbox.web.WebServerAnswerInHtml;
import com.asofterspace.toolbox.web.WebServerAnswerInJson;
import com.asofterspace.toolbox.web.WebServerRequestHandler;

import java.io.IOException;
import java.net.Socket;
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

		String jsonData = receiveJsonContent();

		if (jsonData == null) {
			respond(400);
			return;
		}

		JSON json;
		try {
			json = new JSON(jsonData);
		} catch (JsonParseException e) {
			respond(400);
			return;
		}

		WebServerAnswer answer = new WebServerAnswerInJson("{\"success\": true}");

		try {

			switch (fileLocation) {

				case "/todo":
					answer = new WebServerAnswerInJson(new JSON("{\"success\": maybe}"));
					break;
/*
				case "/addSingleTask":

					Task addedOrEditedTask = addOrEditSingleTask(json);
					if (addedOrEditedTask == null) {
						return;
					}
					answer = new WebServerAnswerInJson(new JSON("{\"success\": true}"));
					break;
*/

				default:
					respond(404);
					return;
			}

		} catch (JsonParseException e) {
			respond(403);
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

			TextFile indexBaseFile = new TextFile(webRoot, "index.htm");
			String indexContent = indexBaseFile.getContent();

			indexContent = StrUtils.replaceAll(indexContent, "[[SIDEBAR]]",
				SideBarCtrl.getSidebarHtmlStr(SideBarEntry.BROWSER));

			String path = arguments.get("path");

			path = PathCtrl.ensurePathIsSafe(path);

			// interpret console commands - in case we do a cd, the path has to be changed here - not earlier,
			// not later
			if (arguments.get("console") != null) {
				path = consoleCtrl.interpretCommand(arguments.get("console"), path);
			}

			path = PathCtrl.ensurePathIsSafe(path);

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

			StringBuilder folderContent = new StringBuilder();

			Directory folder = new Directory(localPath);

			boolean recursively = false;

			List<Directory> childFolders = folder.getAllDirectories(recursively);

			for (Directory childFolder : childFolders) {
				folderContent.append("<a href='/?path=" + path + "/" + childFolder.getLocalDirname() + "'>");
				folderContent.append("<div class='line'>");
				folderContent.append(childFolder.getLocalDirname());
				folderContent.append("</div>");
				folderContent.append("</a>");
			}

			if (childFolders.size() > 0) {
				folderContent.append("<div>");
				folderContent.append("&nbsp;");
				folderContent.append("</div>");
			}

			List<File> childFiles = folder.getAllFiles(recursively);

			for (File childFile : childFiles) {
				folderContent.append("<div class='line'>");
				folderContent.append(childFile.getLocalFilename());
				folderContent.append("</div>");
			}

			indexContent = StrUtils.replaceAll(indexContent, "[[FOLDER_CONTENT]]", folderContent.toString());

			indexContent = StrUtils.replaceAll(indexContent, "[[CONSOLE]]", consoleCtrl.getHtmlStr());

			indexContent = StrUtils.replaceAll(indexContent, "[[DATA]]", "{\"path\": \"" +
				JSON.escapeJSONstr(path) + "\"}");

			return new WebServerAnswerInHtml(indexContent);
		}

		return null;
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

}
