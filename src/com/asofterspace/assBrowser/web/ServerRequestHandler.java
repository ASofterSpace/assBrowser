/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.web;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.console.ConsoleResult;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.coders.UrlEncoder;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.HTML;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.Record;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.utils.TextEncoding;
import com.asofterspace.toolbox.Utils;
import com.asofterspace.toolbox.virtualEmployees.SideBarCtrl;
import com.asofterspace.toolbox.virtualEmployees.SideBarEntry;
import com.asofterspace.toolbox.virtualEmployees.SideBarEntryForTool;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class ServerRequestHandler extends WebServerRequestHandler {

	private Database database;

	private Directory serverDir;

	private ConsoleCtrl consoleCtrl;

	private static final String[] IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "gif", "bmp", "webp"};

	private String videoDirPathStr = null;

	private boolean accessFilesLocally = true;

	private Random rand = new Random();

	// the link to the video that is played next (the first entry on the list of other videos)
	private String nextVidLink = null;


	public ServerRequestHandler(WebServer server, Socket request, Directory webRoot, Directory serverDir,
		Database database, ConsoleCtrl consoleCtrl) {

		super(server, request, webRoot);

		this.database = database;

		this.videoDirPathStr = database.getVideoDirPathStr();

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
			path = PathCtrl.resolvePath(path);
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

			case "/openFileInOS":
				if (json.getString("path") == null) {
					respond(403);
				}
				List<String> arguments = new ArrayList<>();
				String fileToOpenPath = json.getString("path");
				int dotIndex = fileToOpenPath.lastIndexOf(".");
				if (dotIndex >= 0) {
					Map<String, String> programs = database.getProgramsToOpenFiles();
					String program = programs.get(fileToOpenPath.substring(dotIndex).toLowerCase());
					if (program != null) {
						IoUtils.executeAsync(program, fileToOpenPath);
						break;
					}
				}
				IoUtils.executeAsync(fileToOpenPath);
				break;

			case "/openFolderInOS":
				if (json.getString("path") == null) {
					respond(403);
				}
				String localPath = PathCtrl.resolvePath(json.getString("path"));
				GuiUtils.openFolder(localPath);
				break;

			case "/saveEntry":
				String path = json.getString("path");
				String fileName = json.getString("file");
				String content = json.getString("content");
				path = PathCtrl.ensurePathIsSafe(path);
				path = PathCtrl.browserizePath(path);
				localPath = PathCtrl.resolvePath(path);
				Directory folder = new Directory(localPath);
				TextFile entryFile = new TextFile(folder, fileName);
				entryFile.setEncoding(TextEncoding.ISO_LATIN_1);
				// remove conditional break dashes
				content = StrUtils.replaceAll(content, "­", "");
				entryFile.saveContent(content);
				Record rec = Record.emptyObject();
				rec.setString("path", path);
				rec.setString("file", fileName);
				answer = new WebServerAnswerInJson(rec);
				break;

			case "/saveFolder":
				path = json.getString("path");
				localPath = PathCtrl.resolvePath(path);
				folder = new Directory(localPath);
				SimpleFile vstpuFile = new SimpleFile(folder, "VSTPU.stpu");
				vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
				content = json.getString("content");
				// remove conditional break dashes
				content = StrUtils.replaceAll(content, "­", "");
				vstpuFile.saveContent(content);
				rec = Record.emptyObject();
				rec.setString("path", path);
				answer = new WebServerAnswerInJson(rec);
				break;

			case "/doRename":
				path = json.getString("path");
				fileName = json.getString("file");
				String newName = json.getString("newName").trim();
				path = PathCtrl.ensurePathIsSafe(path);
				path = PathCtrl.browserizePath(path);
				localPath = PathCtrl.resolvePath(path);
				folder = new Directory(localPath);
				entryFile = new TextFile(folder, fileName);
				rec = Record.emptyObject();
				rec.setString("path", path);
				rec.setString("file", fileName);
				rec.setString("newName", newName);
				if (fileName.equals(newName)) {
					rec.setString("error", "The new name is identical to the previous one!");
				} else {
					TextFile newEntryFile = new TextFile(folder, newName);
					if (newEntryFile.exists()) {
						rec.setString("error", "A file with the name '" + newName + "' already exists!");
					} else {
						if (!entryFile.exists() && !fileName.endsWith(".stpu")) {
							rec.setString("error", "Source file with the name '" + newName + "' does not exist!");
						} else {
							if (entryFile.exists()) {
								try {
									Path newPath = newEntryFile.getJavaPath();
									Files.move(entryFile.getJavaPath(), newPath, StandardCopyOption.REPLACE_EXISTING);
								} catch (IOException e) {
									rec.setString("error", "Encountered an IOException: " + e);
								}
							}
							if (fileName.endsWith(".stpu") && newName.endsWith(".stpu")) {
								String fileBaseName = fileName.substring(0, fileName.length() - 5);
								String newBaseName = newName.substring(0, newName.length() - 5);
								int i = 1;
								while (true) {
									boolean didCopyAFile = false;
									for (String curImgExt : IMAGE_EXTENSIONS) {
										didCopyAFile = tryCopy(folder, fileBaseName, newBaseName, i, "." + curImgExt);
										if (didCopyAFile) {
											break;
										}
									}
									if (!didCopyAFile) {
										break;
									}
									i++;
								}

								vstpuFile = new SimpleFile(folder, "VSTPU.stpu");
								vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
								content = "\n" + vstpuFile.getContent() + "\n";
								content = StrUtils.replaceAll(content, fileBaseName, newBaseName);
								content = content.substring(1, content.length() - 1);
								vstpuFile.saveContent(content);
							}
						}
					}
				}
				answer = new WebServerAnswerInJson(rec);
				break;

			case "/findCrossReferences":
				path = json.getString("path");
				String searchForText = json.getString("text");
				localPath = PathCtrl.resolvePath(path);
				folder = new Directory(localPath);
				StringBuilder answerText = new StringBuilder();

				Directory parentFolder = folder.getParentDirectory();
				boolean recursively = true;
				List<File> files = parentFolder.getAllFilesEndingWith(".stpu", recursively);
				for (File file : files) {
					if ("VSTPU.stpu".equals(file.getLocalFilename())) {
						continue;
					}
					vstpuFile = new SimpleFile(file);
					vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
					content = vstpuFile.getContent();
					int pos = content.indexOf(searchForText);
					while (pos >= 0) {
						int begin = content.lastIndexOf("\n\n", pos);
						if (begin < 0) {
							begin = content.lastIndexOf('\n', pos);
						}
						if (begin < 0) {
							begin = 0;
						}

						int end = content.indexOf("\n\n", pos);
						if (end < 0) {
							end = content.indexOf('\n', pos);
						}
						if (end < 0) {
							end = pos + searchForText.length();
						}
						if (end < begin) {
							end = begin;
						}
						String copyEntryPart = content.substring(begin, end).trim();
						copyEntryPart = StrUtils.replaceAll(copyEntryPart, "\n", "<br>");
						answerText.append(copyEntryPart + ":<br>");

						String linkStr = PathCtrl.browserizePath(file.getCanonicalFilename());
						if (linkStr.endsWith(".stpu")) {
							linkStr = linkStr.substring(0, linkStr.length() - 5);
						}
						String encodedLinkStr = linkStr;
						encodedLinkStr = HTML.unescapeHTMLstr(encodedLinkStr);
						encodedLinkStr = UrlEncoder.encode(encodedLinkStr);
						answerText.append("<a href=\"/?link=" + encodedLinkStr + "\" target=\"_blank\">%[");
						answerText.append(StrUtils.replaceAll(linkStr, "/", "\\"));
						answerText.append("]</a>");
						answerText.append("<br>");
						answerText.append("<br>");
						pos = content.indexOf(searchForText, pos + 1);
					}
				}

				rec = Record.emptyObject();
				rec.setString("text", answerText.toString());
				answer = new WebServerAnswerInJson(rec);
				break;

			default:
				respond(404);
				return;
		}

		respond(200, answer);
	}

	private boolean tryCopy(Directory folder, String fileBaseName, String newBaseName, int i, String fileEnding) {
		try {
			File oldFile = new File(folder, fileBaseName + "_" + i + fileEnding);
			File newFile = new File(folder, newBaseName + "_" + i + fileEnding);
			if (oldFile.exists()) {
				Files.move(oldFile.getJavaPath(), newFile.getJavaPath(), StandardCopyOption.REPLACE_EXISTING);
				return true;
			}
		} catch (IOException e) {
		}
		return false;
	}

	@Override
	protected WebServerAnswer answerGet(String location, Map<String, String> arguments) {

		if (!location.startsWith("/")) {
			location = "/" + location;
		}

		if (location.equals("/exit")) {
			System.exit(0);
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

		if (location.startsWith("/funtubeVideo")) {
			Directory viddir = new Directory(videoDirPathStr);
			File vidfile = new File(viddir, arguments.get("path"));
			return new WebServerAnswerBasedOnFile(vidfile);
		}

		if (location.startsWith("/funtubePreview")) {
			Directory viddir = new Directory(videoDirPathStr);
			File prevfile = new File(viddir, arguments.get("path") + ".jpg");
			if (!prevfile.exists()) {
				// generate the preview file
				File vidfile = new File(viddir, arguments.get("path"));

				generatePreviewFile(vidfile, prevfile);
			}
			return new WebServerAnswerBasedOnFile(prevfile);
		}

		if (location.startsWith("/funtube")) {
			return generateAnswerToFunTubeRequest(arguments);
		}

		if (location.startsWith("/getEntry")) {
			String path = arguments.get("path");
			String fileName = arguments.get("file");
			path = PathCtrl.ensurePathIsSafe(path);
			path = PathCtrl.browserizePath(path);
			String localPath = PathCtrl.resolvePath(path);
			Directory folder = new Directory(localPath);
			File genericFile = new File(folder, fileName);
			String fileHtmlStr = loadEntryAsStr(genericFile);
			if ("false".equals(arguments.get("editingMode"))) {
				fileHtmlStr = prepareEntryForDisplayInHtml(fileHtmlStr, folder, fileName);
			}
			Record rec = Record.emptyObject();
			rec.setString("path", path);
			rec.setString("file", fileName);
			rec.setString("entry", fileHtmlStr);
			return new WebServerAnswerInJson(rec);
		}

		if (location.startsWith("/getFolder")) {
			String path = arguments.get("path");
			String fileName = arguments.get("file");
			String localPath = PathCtrl.resolvePath(path);
			Directory folder = new Directory(localPath);

			String folderContentStr = null;

			if ("false".equals(arguments.get("editingMode"))) {
				boolean quickView = !"false".equals(arguments.get("quickView"));
				folderContentStr = getFolderContentHtml(folder, path, fileName, quickView);
			} else {
				SimpleFile vstpuFile = new SimpleFile(folder, "VSTPU.stpu");
				if (vstpuFile.exists()) {
					vstpuFile.setEncoding(TextEncoding.ISO_LATIN_1);
					folderContentStr = vstpuFile.getContent();
				} else {
					folderContentStr = null;
				}
			}
			Record rec = Record.emptyObject();
			rec.setString("path", path);
			rec.setString("content", folderContentStr);
			return new WebServerAnswerInJson(rec);
		}

		return null;
	}

	// synchronized, such that ffmpeg is not called twice at the same time, which actually does lead to it getting
	// stuck and then it can no longer be called at all
	private synchronized void generatePreviewFile(File vidfile, File prevfile) {
		String ffmpegPath = database.getFfmpegPath();
		String ffmpegInvocation = ffmpegPath;
		ffmpegInvocation += " -ss 1 -i \"";
		ffmpegInvocation += vidfile.getAbsoluteFilename();
		ffmpegInvocation += "\" -vframes 1 -f image2 \"";
		ffmpegInvocation += prevfile.getAbsoluteFilename();
		ffmpegInvocation += "\"";
		IoUtils.execute(ffmpegInvocation);
	}

	private WebServerAnswer generateAnswerToMainGetRequest(Map<String, String> arguments, String message) {

		TextFile indexBaseFile = new TextFile(webRoot, "index.htm");
		String indexContent = indexBaseFile.getContent();

		indexContent = StrUtils.replaceAll(indexContent, "[[SIDEBAR]]",
			SideBarCtrl.getSidebarHtmlStr(SideBarEntryForTool.BROWSER));

		String path = arguments.get("path");
		String fileName = arguments.get("file");
		path = PathCtrl.ensurePathIsSafe(path);

		// if a link argument exists, it overrides path and file - it may contain just a path, or a path followed
		// by a filename without the .stpu (so link=/foo/bar may link to the folder bar inside the folder foo,
		// or may link to the file bar.stpu inside the folder foo)
		String link = arguments.get("link");
		String consoleValue = "";

		// interpret console commands - in case we do a cd, the path has to be changed here - not earlier,
		// not later
		if (arguments.get("console") != null) {
			boolean fromOutside = true;
			ConsoleResult consoleResult = consoleCtrl.interpretCommand(arguments.get("console"), path, fromOutside);
			String newPath = consoleResult.getPath();
			consoleValue = consoleResult.getCommand();

			newPath = PathCtrl.ensurePathIsSafe(newPath);

			// if the path has changed, that overrides the link, which overrides the path and the file
			// (we override link, not just path, because an SLL file interpreted by the console could
			// have redirected to a file, rather than to a path!
			// as a side effect, we can write cd file in the console to actually open the file, which is
			// fine ^^)
			if (!newPath.equals(path)) {
				link = newPath;
			}
		}

		if (link != null) {
			String localLink = PathCtrl.resolvePath(link);
			Directory localLinkDir = new Directory(localLink);
			// if a directory exists with the specified name, go for that immediately
			if (localLinkDir.exists()) {
				path = link;
				fileName = null;
			} else {
				// otherwise, go one up and check for a file with the name that the link ends in
				link = StrUtils.replaceAll(link, "\\", "/");
				int pos = link.lastIndexOf("/");
				if (pos >= 0) {
					path = link.substring(0, pos);
					Directory pathDir = new Directory(path);
					// if a file exists with that exact name, go for it!
					File notStpuFile = new File(pathDir, link.substring(pos + 1));
					if (notStpuFile.exists()) {
						fileName = link.substring(pos + 1);
					} else {
						// otherwise, assume an stpu file is meant
						fileName = link.substring(pos + 1) + ".stpu";
					}
				} else {
					path = link;
					fileName = null;
				}
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[CONSOLE_VALUE]]", consoleValue);

		path = PathCtrl.browserizePath(path);

		consoleCtrl.addPath(path);

		String localPath = PathCtrl.resolvePath(path);

		Directory folder = new Directory(localPath);

		if ("download".equals(arguments.get("action"))) {
			return new WebServerAnswerBasedOnFile(new File(folder, fileName));
		}

		boolean quickView = true;
		String folderContentStr = getFolderContentHtml(folder, path, fileName, quickView);
		indexContent = StrUtils.replaceAll(indexContent, "[[FOLDER_CONTENT]]", folderContentStr);

		indexContent = StrUtils.replaceAll(indexContent, "[[CONSOLE]]", consoleCtrl.getHtmlStr());

		JSON jsonData = new JSON(Record.emptyObject());
		jsonData.set("path", path);
		jsonData.set("file", fileName);
		indexContent = StrUtils.replaceAll(indexContent, "[[DATA]]", jsonData.toString());

		indexContent = StrUtils.replaceAll(indexContent, "[[PATH]]", path);


		// buttonBar
		StringBuilder buttonHtml = new StringBuilder();

		buttonHtml.append("<span id='save-folder-btn' class='button' onclick='browser.saveFolder()' style='display:none;position:absolute;left:5%;bottom:0;'>");
		buttonHtml.append("Save");
		buttonHtml.append("</span>");

		buttonHtml.append("<span id='edit-folder-btn' class='button' onclick='browser.toggleEditFolder()' style='position:absolute;left:10%;bottom:0;'>");
		buttonHtml.append("Edit");
		buttonHtml.append("</span>");

		buttonHtml.append("<a href=\"/?path=" + path);
		if (fileName != null) {
			buttonHtml.append("&file=" + fileName);
		}
		buttonHtml.append("&console=cd ..\" class='button'>");
		buttonHtml.append("..");
		buttonHtml.append("</a>");

		if (fileName != null) {
			buttonHtml.append("<span id='save-btn' class='button' onclick='browser.saveEntry()' style='display:none;'>");
			buttonHtml.append("Save");
			buttonHtml.append("</span>");

			buttonHtml.append("<span id='edit-btn' class='button' onclick='browser.toggleEditEntry()'>");
			buttonHtml.append("Edit");
			buttonHtml.append("</span>");
		}

		buttonHtml.append("<span class='button' onclick='browser.openTileView()'>");
		buttonHtml.append("&#x25A6; Tile");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.openComicView()'>");
		buttonHtml.append("&#x25AF; Comic");
		buttonHtml.append("</span>");

		if (fileName != null) {

			buttonHtml.append("<span class='button' onclick='browser.toggleTextActions()' id='text-actions-btn'>");
			buttonHtml.append("Text Actions...");
			buttonHtml.append("</span>");

			buttonHtml.append("<div id='text-actions-container' style='display:none;'>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='toolbox.utils.StrUtils.applyQuoteStyleToLog(\"fileContentTextarea\"); browser.closeMoreActions();'>");
			buttonHtml.append("Quote Selection");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='toolbox.utils.StrUtils.applyEmailStyleToLog(\"fileContentTextarea\"); browser.closeMoreActions();'>");
			buttonHtml.append("Apply Email Un-styling to Selection");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='toolbox.utils.StrUtils.applyRemoveNewlineStyleToLog(\"fileContentTextarea\"); browser.closeMoreActions();'>");
			buttonHtml.append("Remove \\n from Selection");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='toolbox.utils.StrUtils.applySpaceToBulletPoint(\"fileContentTextarea\"); browser.closeMoreActions();'>");
			buttonHtml.append("\"&nbsp;&nbsp;&nbsp;&nbsp;\" to \"* \" in Selection");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='browser.showSearchReplaceModal(); browser.closeMoreActions();'>");
			buttonHtml.append("Search + Replace");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button' onclick='browser.extractTLDR()'>");
			buttonHtml.append("Extract Summary to clipboard");
			buttonHtml.append("</span>");
			buttonHtml.append("<textarea id='clipboardHelper' style='display:none'></textarea>");
			buttonHtml.append("<br>");

			buttonHtml.append("</div>");
		}

		buttonHtml.append("<span class='button' onclick='browser.toggleMoreActions()' id='more-actions-btn'>");
		buttonHtml.append("Other Actions...");
		buttonHtml.append("</span>");

		buttonHtml.append("<div id='more-actions-container' style='display:none;'>");

		if (fileName != null) {
			buttonHtml.append(getFileButtonsHtml(path, fileName, ""));
			buttonHtml.append("<br>");

			buttonHtml.append("<a class='button' id='exportButtonA' target='_blank'>");
			buttonHtml.append("Print View / Export to PDF");
			buttonHtml.append("</a>\n");
			buttonHtml.append("<script>\n");
			buttonHtml.append("window.setTimeout(function() {\n");
			buttonHtml.append("  var exportButtonA = document.getElementById('exportButtonA');\n");
			buttonHtml.append("  exportButtonA.href = window.location.href + '&export=true';\n");
			buttonHtml.append("}, 1000);\n");
			buttonHtml.append("</script>\n");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button editBtnDisabled' onclick='browser.findCrossReferencesSelectedText()'>");
			buttonHtml.append("Find cross-references to selected text (edit mode only)");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");

			buttonHtml.append("<span class='button' onclick='browser.findCrossReferencesEntry()'>");
			buttonHtml.append("Find cross-references to this entry");
			buttonHtml.append("</span>");
			buttonHtml.append("<br>");
		}

		buttonHtml.append("<span class='button' onclick='browser.openUploadModal()'>");
		buttonHtml.append("Upload a File");
		buttonHtml.append("</span>");
		buttonHtml.append("<br>");

		buttonHtml.append("<span class='button' onclick='browser.copyLinkToThis()'>");
		buttonHtml.append("Copy Link to This");
		buttonHtml.append("</span>");
		buttonHtml.append("<br>");

		buttonHtml.append("<span class='button' onclick='browser.openFolderInOS()'>");
		buttonHtml.append("Open in OS");
		buttonHtml.append("</span>");
		buttonHtml.append("<br>");

		buttonHtml.append("<span class='button' onclick='browser.expandConsole()' id='expandConsoleBtn'>");
		buttonHtml.append("Expand Console");
		buttonHtml.append("</span>");

		buttonHtml.append("</div>");

		indexContent = StrUtils.replaceAll(indexContent, "[[BUTTONS]]", buttonHtml.toString());


		if (message == null) {
			message = "";
		}
		if (!message.equals("")) {
			message = "alert(\"" + message + "\")";
		}
		indexContent = StrUtils.replaceAll(indexContent, "[[EXTRA_MESSAGE]]", message);


		String fileHtmlStr = "";
		String imagesStr = "";

		if (fileName != null) {
			File genericFile = new File(folder, fileName);
			String lowCaseFileName = fileName.toLowerCase();
			if (lowCaseFileName.endsWith(".stpu") || lowCaseFileName.endsWith(".sll") ||
				lowCaseFileName.endsWith(".txt") || lowCaseFileName.endsWith(".ini") ||
				lowCaseFileName.endsWith(".srt")) {

				fileHtmlStr = loadEntryAsStr(genericFile);

				// follow link automatically
				if (fileHtmlStr.startsWith("%[") && fileHtmlStr.contains("]")) {
					String newLink = fileHtmlStr.substring(2, fileHtmlStr.indexOf("]"));
					if (newLink.startsWith("se:")) {
						// actually, this is no link but a request to execute something - so do so,
						// before continuing to just show the content as usual!
						IoUtils.executeAsync(newLink.substring(3).trim());
					} else {
						Map<String, String> newArgs = new HashMap<>();
						newArgs.put("link", newLink);
						return generateAnswerToMainGetRequest(newArgs, message);
					}
				}

				fileHtmlStr = prepareEntryForDisplayInHtml(fileHtmlStr, folder, fileName);

				final int TILE_COLUMN_AMOUNT = 4;
				List<StringBuilder> imagesColStrBuilders = new ArrayList<>();
				for (int i = 0; i < TILE_COLUMN_AMOUNT; i++) {
					imagesColStrBuilders.add(new StringBuilder());
				}

				StringBuilder imagesStrBuilder = new StringBuilder();
				String baseName = fileName.substring(0, fileName.lastIndexOf("."));
				int imgNum = 1;
				while (true) {
					String imgExtFound = null;
					for (String curImgExt : IMAGE_EXTENSIONS) {
						File imgFile = new File(folder, baseName + "_" + imgNum + "." + curImgExt);
						if (imgFile.exists()) {
							imgExtFound = curImgExt;
						}
					}
					if (imgExtFound == null) {
						break;
					}
					String imgUrl = getFileAccessUrl(path, baseName + "_" + imgNum + "." + imgExtFound);
					imagesStrBuilder.append("<a target=\"_blank\" href=\"" + imgUrl + "\">");
					imagesStrBuilder.append("<img src=\"" + imgUrl + "\">");
					imagesStrBuilder.append("</a>");
					imagesColStrBuilders.get((imgNum - 1) % 4).append("<a target=\"_blank\" href=\"" + imgUrl + "\">");
					imagesColStrBuilders.get((imgNum - 1) % 4).append("<img src=\"" + imgUrl + "\">");
					imagesColStrBuilders.get((imgNum - 1) % 4).append("</a>");
					imgNum++;
				}
				if (imgNum > 1) {
					StringBuilder overallBuilder = new StringBuilder();
					overallBuilder.append("<div id='imageStrip' class='imageStrip'>");
					overallBuilder.append("<span style='position:fixed;top:2pt;right:15pt;background:rgba(64,0,128,0.8);border-radius: 6pt;padding: 0pt 3pt;'>");
					overallBuilder.append("<span class='button' onclick='browser.closeView()' style='display:none; margin-top:5pt; margin-bottom:5pt;' ");
					overallBuilder.append("id='closeComicViewBtn'>Close View</span>");
					overallBuilder.append("</span>");
					overallBuilder.append(imagesStrBuilder);
					overallBuilder.append("</div>");

					overallBuilder.append("<div id='tileStripsContainer' style='display:none; overflow-y:scroll;'>");
					overallBuilder.append("<div style='position:fixed;top:0;left:0;right:15pt;background:rgb(64,0,128);'>");
					overallBuilder.append("<span class='button' onclick='browser.closeView()' style='display:block; margin-top:5pt; margin-bottom:5pt;' ");
					overallBuilder.append("id='closeTileViewBtn'>Close View</span>");
					overallBuilder.append("</div>");
					overallBuilder.append("<div style='padding-top:25pt;'>");

					for (int i = 0; i < TILE_COLUMN_AMOUNT; i++) {
						overallBuilder.append("<div class='imageStrip imageStripColumn'>");
						overallBuilder.append(imagesColStrBuilders.get(i));
						overallBuilder.append("</div>");
					}

					overallBuilder.append("</div>");
					overallBuilder.append("</div>");

					imagesStr = overallBuilder.toString();
				}

			// only now check if the file even exists - as we allow for STPU files which do not exist,
			// e.g. if they only have pictures but no contents
			} else if (!genericFile.exists()) {
				fileHtmlStr = "The file '" + fileName + "' does not exist!";

			} else if (lowCaseFileName.endsWith(".json") || lowCaseFileName.endsWith(".java") ||
				lowCaseFileName.endsWith(".bat") || lowCaseFileName.endsWith(".sh") ||
				lowCaseFileName.endsWith(".md")) {
				TextFile file = new TextFile(folder, fileName);
				fileHtmlStr = file.getContent();
				fileHtmlStr = prepareStrForDisplayInHtml(fileHtmlStr);
			} else {
				boolean isImageFile = false;
				for (String curImgExt : IMAGE_EXTENSIONS) {
					if (lowCaseFileName.endsWith("." + curImgExt)) {
						isImageFile = true;
						break;
					}
				}
				if (isImageFile) {
					String imgUrl = getFileAccessUrl(path, fileName);
					fileHtmlStr = "<a target=\"_blank\" href=\"" + imgUrl + "\" style='max-width:99%; max-height:99%;' />";
					fileHtmlStr += "<img src=\"" + imgUrl + "\" style='max-width:100%; max-height:100%;' />";
					fileHtmlStr += "</a>";
				} else {
					fileHtmlStr = "<div style='line-height: 2.5;text-align: center;' id='fileButtonContainer'>" +
								  "No preview for '" + fileName + "' available.<br><br>" +
								  getFileButtonsHtml(path, fileName, "padding: 4pt 9pt;") +
								  "</div>";
				}
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[FILE_CONTENT]]", fileHtmlStr);

		indexContent = StrUtils.replaceAll(indexContent, "[[IMAGES]]", imagesStr);

		if ("true".equals(arguments.get("export"))) {
			indexContent += "\n"+
				"<script>\n" +
				"window.setTimeout(function() {\n" +
				"  var el = document.getElementById('imageStrip');\n" +
				"  if (el) { el.style.display = 'none'; }\n" +
				"  document.getElementById('consoleContainer').style.display = 'none';\n" +
				"  document.getElementById('folderContainer').style.display = 'none';\n" +
				"  var buttonBars = document.getElementsByClassName('buttonBar');\n" +
				"  for (var i = 0; i < buttonBars.length; i++) {\n" +
				"    buttonBars[i].style.display = 'none';\n" +
				"  }\n" +
				"  var sidebarItems = document.getElementsByClassName('sidebar');\n" +
				"  for (var i = 0; i < sidebarItems.length; i++) {\n" +
				"    sidebarItems[i].style.display = 'none';\n" +
				"  }\n" +
				"  var aItems = document.getElementsByTagName('a');\n" +
				"  for (var i = 0; i < aItems.length; i++) {\n" +
				"    aItems[i].style.color = '#555';\n" +
				"  }\n" +
				"  document.getElementById('fileContentContainer').style.height = 'unset';\n" +
				"  document.getElementById('fileContentContainer').style.color = '#000';\n" +
				"  document.getElementById('mainContent').style.height = 'unset';\n" +
				"  document.getElementById('mainContent').style.overflow = 'unset';\n" +
				"  document.getElementsByTagName('body')[0].style.padding = '0';\n" +
				"  document.getElementsByTagName('body')[0].style.background = '#FFF';\n" +
				"  document.getElementsByTagName('body')[0].style.overflowY = 'scroll';\n" +
				"  \n" +
				"  window.browser.exportView = true;\n" +
				"  window.browser.onResize();\n" +
				"}, 500);\n" +
				"</script>";
		}

		return new WebServerAnswerInHtml(indexContent);
	}

	private WebServerAnswer generateAnswerToFunTubeRequest(Map<String, String> arguments) {

		TextFile indexBaseFile = new TextFile(webRoot, "funtube.htm");
		String html = indexBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[SIDEBAR]]",
			SideBarCtrl.getSidebarHtmlStr(SideBarEntryForTool.FUNTUBE));

		String videoPath = arguments.get("path");
		String title = videoPathToTitle(videoPath);

		Directory viddir = new Directory(videoDirPathStr);
		boolean recursively = true;

		if (videoPath != null) {
			if (isAudio(videoPath)) {
				html = StrUtils.replaceAll(html, "[[VIDEO_TYPE]]", "audio/" + getLowEnding(videoPath));
				html = StrUtils.replaceAll(html, "[[VIDEO_TAG]]", "audio");
			} else {
				html = StrUtils.replaceAll(html, "[[VIDEO_TYPE]]", "video/" + getLowEnding(videoPath));
				html = StrUtils.replaceAll(html, "[[VIDEO_TAG]]", "video");
			}
			html = StrUtils.replaceAll(html, "[[VIDEO_STYLE]]", "");
			html = StrUtils.replaceAll(html, "[[VIDEO_CATEGORIES]]", "");
		} else {
			html = StrUtils.replaceAll(html, "[[VIDEO_STYLE]]", "display: none;");
			title = "Categories";

			List<Record> categories = database.getFuntubeCategories();
			StringBuilder catStr = new StringBuilder();
			catStr.append("<div style='position: fixed; top: 35pt; left: 15pt; bottom: 15pt; overflow-y: scroll; width: 55%;'>");
			for (Record category : categories) {
				catStr.append("<div class='category_title'>" + category.getString("name") + "</div>");
				List<String> folders = category.getArrayAsStringList("folders");

				for (String folder : folders) {

					Directory categoryDir = new Directory(viddir, folder);
					List<File> videoFilesInCategory = categoryDir.getAllFiles(recursively);

					String aVideoPath = getFunTubeVidPath(videoFilesInCategory);

					catStr.append("<div class='category'>" +
						"<a href=\"funtube?path=" + UrlEncoder.encode(aVideoPath) + "\">" + folder + "</a>" +
						"</div>");
				}
			}
			catStr.append("</div>");

			html = StrUtils.replaceAll(html, "[[VIDEO_CATEGORIES]]", catStr.toString());
		}

		html = StrUtils.replaceAll(html, "[[TITLE]]", title);

		if (accessFilesLocally) {
			File vidfile = new File(viddir, videoPath);
			html = StrUtils.replaceAll(html, "[[VIDEO_PATH]]", "file:///" +
				StrUtils.replaceAll(vidfile.getCanonicalFilename(), "\\", "/"));
		} else {
			html = StrUtils.replaceAll(html, "[[VIDEO_PATH]]", "funtubeVideo?path=" + UrlEncoder.encode(videoPath));
		}

		StringBuilder otherVideos = new StringBuilder();

		int proposalAmount = 20;
		int id = 1;

		nextVidLink = null;

		if (videoPath != null) {
			if (videoPath.contains("/")) {
				String otherVideosBySameCreatorPath = videoPath.substring(0, videoPath.indexOf("/"));
				Directory otherVideosBySameCreatorDir = new Directory(viddir, otherVideosBySameCreatorPath);
				List<File> otherVideoFiles = otherVideosBySameCreatorDir.getAllFiles(recursively);
				for (int i = 0; i < 9; i++) {
					if (otherVideoFiles.size() < 1) {
						break;
					}
					otherVideos.append(getFunTubeVidLink(otherVideoFiles, id++));
					proposalAmount--;
				}
			}
		}

		List<File> otherVideoFiles = viddir.getAllFiles(recursively);
		for (int i = 0; i < proposalAmount; i++) {
			if (otherVideoFiles.size() < 1) {
				break;
			}
			otherVideos.append(getFunTubeVidLink(otherVideoFiles, id++));
		}

		html = StrUtils.replaceAll(html, "[[OTHER_VIDEOS]]", otherVideos.toString());

		nextVidLink = "funtube?path=" + UrlEncoder.encode(nextVidLink);
		html = StrUtils.replaceAll(html, "[[NEXT_VID_LINK]]", nextVidLink);

		return new WebServerAnswerInHtml(html);
	}

	private String videoPathToTitle(String videoPath) {
		if (videoPath == null) {
			return "";
		}
		String title = videoPath;
		if (title.contains(".")) {
			title = title.substring(0, title.lastIndexOf("."));
		}
		title = StrUtils.replaceAll(title, "/", " - ");
		title = StrUtils.replaceAll(title, "&", "&amp;");
		return title;
	}

	private String getFunTubeVidPath(List<File> videoFiles) {

		File videoFile = null;

		while (videoFiles.size() > 0) {
			int index = rand.nextInt(videoFiles.size());
			File curVideoFile = videoFiles.get(index);

			// do not display the same video twice in the recommendations
			videoFiles.remove(index);

			// only display actual videos or audio files
			String lowName = curVideoFile.getFilename().toLowerCase();
			if (lowName.endsWith(".mp4") || lowName.endsWith(".mpg") ||
				lowName.endsWith(".wmv") || lowName.endsWith(".avi") ||
				lowName.endsWith(".mov") || lowName.endsWith(".flv") ||
				lowName.endsWith(".mp3") || lowName.endsWith(".webm")) {
				videoFile = curVideoFile;
				break;
			}
		}
		if (videoFile == null) {
			return "";
		}

		String videoPath = videoFile.getCanonicalFilename();
		videoPath = StrUtils.replaceAll(videoPath, "\\", "/");
		if (videoPath.startsWith(videoDirPathStr)) {
			videoPath = videoPath.substring(videoDirPathStr.length());
		}

		return videoPath;
	}

	private String getFunTubeVidLink(List<File> videoFiles, int id) {

		String videoPath = getFunTubeVidPath(videoFiles);

		if (nextVidLink == null) {
			nextVidLink = videoPath;
		}

		return "<a href=\"funtube?path=" + UrlEncoder.encode(videoPath) + "\">" +
				"<div class='funtube_img_container'>" +
				"<span class='vertical_center_helper'></span>" +
				"<img id='funtube_img_" + id + "' />" +
				"</div>" +
				"<script>" +
				"window.setTimeout(function() {" +
				"  document.getElementById('funtube_img_" + id + "').src = \"funtubePreview?path=" + UrlEncoder.encode(videoPath) + "\";" +
				"}, " + id*100 + ");" +
				"</script>" +
				"<div class='funtube_vid_title'>" +
				videoPathToTitle(videoPath) +
				"</div>" +
				"</a>";
	}

	private String prepareStrForDisplayInHtml(String fileHtmlStr) {
		fileHtmlStr = HTML.escapeHTMLstr(fileHtmlStr);
		fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "&#10;", "<br>");
		fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "<br> ", "<br>&nbsp;");
		fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "  ", "&nbsp;&nbsp;");
		fileHtmlStr = StrUtils.replaceAll(fileHtmlStr, "&nbsp; ", "&nbsp;&nbsp;");
		return fileHtmlStr;
	}

	private void addFolderToHtml(StringBuilder folderContent, Directory childFolder, String path) {
		String mainStr = childFolder.getLocalDirname();
		String displayStr = mainStr;
		if ("".equals(path)) {
			// display a disk
			displayStr = "Disk " + displayStr;
			List<File> files = childFolder.getAllFilesStartingAndEndingWith(
				"hdd_", ".txt", false);
			if (files.size() > 0) {
				displayStr += " :: " + files.get(0).getLocalFilenameWithoutType();
			}
		} else {
			// display a regular folder
			path += "/";
		}

		folderContent.append("<a href=\"/?path=");
		folderContent.append(path);
		folderContent.append(mainStr);
		folderContent.append("\">");
		folderContent.append("<div class='line folder'>");
		folderContent.append(HTML.escapeHTMLstrNbsp(displayStr));
		folderContent.append("</div>");
		folderContent.append("</a>");
	}

	private void addFileToHtml(StringBuilder folderContent, String filename, File childFile, String path,
		boolean tryToLoad, String compareToFileName) {

		String entryOrLink = "entry";
		if (tryToLoad) {
			TextFile txtFile = new TextFile(childFile);

			// only load the file if it is small - if it is huge, it is VERY VERY likely not to be a link anyway...
			long contentLen = txtFile.getContentLength();
			if (contentLen < 255) {
				boolean complainIfMissing = false;
				String txt = txtFile.getContent(complainIfMissing);
				if ((txt != null) && txt.startsWith("%[")) {
					boolean haveLink = false;
					int endind = txt.indexOf("]\r\n");
					if (endind >= 0) {
						haveLink = endind < txt.indexOf("\r\n");
					} else {
						endind = txt.indexOf("]\n");
						if (endind >= 0) {
							haveLink = endind < txt.indexOf("\n");
						}
					}
					if (haveLink) {
						// we have not just a file, but the file is a link!
						entryOrLink = "link";
					}
				}
			}
		}
		folderContent.append("<div class='a line ");
		if (filename.toLowerCase().equals(compareToFileName)) {
			folderContent.append("opened ");
		}
		folderContent.append(entryOrLink + "' ");
		String link = "/?path=" + funkCode(path) + "&file=" + funkCode(childFile.getLocalFilename());
		link = StrUtils.replaceAll(link, "'", "\\'");
		folderContent.append("onclick=\"browser.navigateTo('" + link + "')\">");
		folderContent.append(HTML.escapeHTMLstrNbsp(filename));
		folderContent.append("</div>");
	}

	private void addTextToHtml(StringBuilder folderContent, String text) {
		folderContent.append("<div class='line text'>");
		if ("".equals(text)) {
			folderContent.append("&nbsp;");
		} else {
			folderContent.append(HTML.escapeHTMLstrNbsp(text));
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

	private String getFileButtonsHtml(String path, String fileName, String style) {
		return "<span class='button' onclick='browser.openFileInOS(\"" + path + "/" + fileName + "\")' " +
			   "style='" + style + "'>" +
			   "Open Current File" +
			   "</span>" +
			   "<br>" +

			   "<a href=\"" + getFileAccessUrl(path, fileName) +
			   "\" target='_blank' " +
			   "class='button'" +
			   "style='" + style + "'>" +
			   "Download Current File" +
			   "</a>" +
			   "<br>" +

			   "<span class='button' onclick='browser.showRenameModal();' " +
			   "style='" + style + "'>" +
			   "Rename Current File" +
			   "</span>" +
			   "<br>" +

			   "<span class='button' onclick='browser.showDeleteModal();' " +
			   "style='" + style + "'>" +
			   "Delete Current File" +
			   "</span>";
	}

	private boolean isAudio(String path) {
		String ending = getLowEnding(path);
		return ending.equals("mp3");
	}

	private String getLowEnding(String path) {
		if (path.contains(".")) {
			path = path.substring(path.lastIndexOf(".") + 1);
		}
		return path.toLowerCase();
	}

	private String getFileAccessUrl(String path, String fileName) {
		if (accessFilesLocally) {
			// works only if the browser is jury-rigged to accept localhost connecting to local files
			String fullPath = path + "/" + fileName;
			if (fullPath.startsWith("/Desktop/") || path.startsWith("\\Desktop\\")) {
				fullPath = PathCtrl.getOneUpDesktopLocation() + "/" + fullPath;
			}
			return "file:///" + StrUtils.replaceAll(StrUtils.replaceAll(
				fullPath,
				"\\", "/"), "//", "/");
		}

		// works in any browser, but is more effort - linking through this server:
		return "/?path=" + path + "&file=" + fileName + "&action=download";
	}

	private String getFolderContentHtml(Directory folder, String path, String compareToFileName, boolean quickView) {

		if (quickView) {
			String inMemoryFolderContent = database.getInMemoryFolderContent(path);
			if (inMemoryFolderContent != null) {
				return inMemoryFolderContent;
			}
		}

		if (compareToFileName == null) {
			compareToFileName = "";
		}
		if (compareToFileName.endsWith(".stpu")) {
			compareToFileName = compareToFileName.substring(0, compareToFileName.length() - 5);
		}
		compareToFileName = compareToFileName.toLowerCase();

		StringBuilder folderContent = new StringBuilder();

		if ("".equals(folder.getDirname())) {

			for (char letter = 'A'; letter < 'Z'; letter++) {
				Directory disk = new Directory(letter + ":");
				if (disk.exists()) {
					addFolderToHtml(folderContent, disk, "");
				}
			}

		} else {

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

				if (entries == null) {
					addTextToHtml(folderContent, "! Unable to load VSTPU.stpu !");
				} else {

					for (String entry : entries) {
						if ("".equals(entry)) {
							addTextToHtml(folderContent, entry);
						} else {
							Directory curDir = directories.get(entry.toLowerCase());
							if (curDir != null) {
								addFolderToHtml(folderContent, curDir, path);
							} else {
								File curFile = null;
								String ext = "";
								if (!entry.toLowerCase().endsWith(".sll")) {
									ext = ".stpu";
								}
								curFile = files.get(entry.toLowerCase() + ext);
								if (curFile != null) {
									addFileToHtml(folderContent, entry, curFile, path, !quickView, compareToFileName);
								} else {
									addFileToHtml(folderContent, entry, new File(folder, entry + ext), path, false, compareToFileName);
								}
							}
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
					addFileToHtml(folderContent, childFile.getLocalFilename(), childFile, path, !quickView, compareToFileName);
				}
			}
		}

		String result = folderContent.toString();

		if (!quickView) {
			database.setInMemoryFolderContent(path, result);
		}

		return result;
	}

	private String loadEntryAsStr(File genericFile) {

		String fileHtmlStr = null;

		if (genericFile.exists()) {
			TextFile file = new TextFile(genericFile);
			file.setEncoding(TextEncoding.ISO_LATIN_1);
			fileHtmlStr = file.getContent();
			fileHtmlStr = DateUtils.convertDateTimeStampsDEtoEN(fileHtmlStr);
		} else {
			fileHtmlStr = genericFile.getLocalFilenameWithoutType() + "\n\n";
		}

		if (fileHtmlStr == null) {
			fileHtmlStr = "! Unable to load file: " + genericFile.getAbsoluteFilename() + " !";
		}

		return fileHtmlStr;
	}

	private String prepareEntryForDisplayInHtml(String fileHtmlStr, Directory folder, String fileName) {

		fileHtmlStr = prepareStrForDisplayInHtml(fileHtmlStr);

		String lowFileName = fileName.toLowerCase();

		// for sll and similar files, just show the plain content
		if (lowFileName.endsWith(".sll") || lowFileName.endsWith(".ini") ||
			lowFileName.endsWith(".srt") ||
			// if the entry is practically similar to an sll file, as in just opening an
			// external program, then also just show the plain content
			fileHtmlStr.startsWith("%[se:")) {
			return fileHtmlStr;
		}

		// set first line as title
		int firstLinefeed = fileHtmlStr.indexOf("<br>");
		if (firstLinefeed < 0) {
			firstLinefeed = fileHtmlStr.length();
		}
		fileHtmlStr = "<h1>" + fileHtmlStr.substring(0, firstLinefeed) + "</h1>" +
			fileHtmlStr.substring(firstLinefeed);

		// iterate over the lines and specially highlight headlines
		String[] contentStrs = fileHtmlStr.split("<br>");
		int emptyLinesSoFar = 0;
		for (int i = 0; i < contentStrs.length - 1; i++) {
			String line = contentStrs[i];
			if ("".equals(line)) {
				emptyLinesSoFar++;
			} else {
				final String SPOILERLINESTR = "%SPOILERLINE%";
				boolean applySpoilerLine = false;
				if (line.startsWith(SPOILERLINESTR)) {
					line = line.substring(SPOILERLINESTR.length()).trim();
					applySpoilerLine = true;
				}

				boolean addSummaryText = false;
				if (line.startsWith("Summary: ")) {
					line = line.substring(9);
					addSummaryText = true;
				}
				if (line.startsWith("TL;DR: ")) {
					line = line.substring(7);
					addSummaryText = true;
				}

				if (addSummaryText) {
					line = "<span class='headSection'>Summary:</span> " + line;
				} else {
					line = HTML.prettifyLine(line);

					if ((emptyLinesSoFar > 0) && line.endsWith(":") && !line.endsWith("&quot;:") &&
						!line.startsWith("see ") && !line.equals("see:") &&
						!line.contains("picture ") && !line.contains("pictures up to ")) {
						line = "<span class='headSection'>" + line + "</span>";
					} else {
						// add folder links for folders that are just plainly on a line - so just "folder" not "folder > file"
						if (!line.contains(" &gt; ")) {
							if (line.length() > 3) {
								if ((line.charAt(1) == ':') && (line.charAt(2) == '\\')) {
									line = "<span class='a' onclick=\"browser.openFolderInOS('" + StrUtils.replaceAll(line, "\\", "\\\\") + "')\">" +
										line + "</span>";
								}
							}
						}
					}
				}

				if ((emptyLinesSoFar > 1) && ("".equals(contentStrs[i+1])) &&
					!line.startsWith("| ") &&
					!line.contains("picture ") && !line.contains("pictures up to ")) {
					// if there are two empty lines following, do not apply <h2>!
					if ((i+2 >= contentStrs.length) || (!"".equals(contentStrs[i+2]))) {
						line = "<h2>" + line + "</h2>";
					}
				}

				if (applySpoilerLine) {
					line = "<span onclick='browser.unspoil(" + i + ");' id='spoiler_" + i + "' class='spoiled'>" + line + "</span>";
				}

				contentStrs[i] = line;
				emptyLinesSoFar = 0;
			}
		}

		// blockquotes should be formatted suchly
		StringBuilder contentStrBui = new StringBuilder();
		String sep = "";
		boolean inQuote = false;
		for (String line : contentStrs) {
			if (line.startsWith("| ")) {
				contentStrBui.append(sep);
				if (!inQuote) {
					contentStrBui.append("<div class='quote'>");
				}
				contentStrBui.append(line.substring(2));
				inQuote = true;
			} else {
				if (inQuote) {
					contentStrBui.append("</div>");
				} else {
					// the </div> eats one separator, so we only put if it we did not put </div>
					contentStrBui.append(sep);
				}
				contentStrBui.append(line);
				inQuote = false;
			}
			// newlines should be shown as such, so we use <br> instead of \n
			sep = "<br>";
		}
		if (inQuote) {
			contentStrBui.append("</div>");
		}
		contentStrBui.append("<br>");
		fileHtmlStr = contentStrBui.toString();

		// replace %[...] with internal links
		StringBuilder newFileHtml = new StringBuilder();
		int pos = fileHtmlStr.indexOf("%[");
		int start = 0;
		while (pos >= 0) {
			int end = fileHtmlStr.indexOf("]", pos);
			if (end >= 0) {
				newFileHtml.append(fileHtmlStr.substring(start, pos));
				String linkStr = fileHtmlStr.substring(pos + 2, end);
				String encodedLinkStr = linkStr;
				encodedLinkStr = HTML.unescapeHTMLstr(encodedLinkStr);
				encodedLinkStr = UrlEncoder.encode(encodedLinkStr);
				newFileHtml.append("<a href=\"/?link=" + encodedLinkStr + "\">%[");
				newFileHtml.append(linkStr);
				newFileHtml.append("]</a>");
				start = end + 1;
				pos = fileHtmlStr.indexOf("%[", end);
			} else {
				break;
			}
		}
		newFileHtml.append(fileHtmlStr.substring(start));
		fileHtmlStr = newFileHtml.toString();

		// replace http:// and https:// with external links
		fileHtmlStr = prepareExternalLinks(fileHtmlStr, "http://");
		fileHtmlStr = prepareExternalLinks(fileHtmlStr, "https://");
		fileHtmlStr = prepareExternalLinks(fileHtmlStr, "file://");

		// replace C:\... > with OS links - so "folder > file", not just "folder"
		newFileHtml = new StringBuilder();
		start = 0;
		fileHtmlStr += "<br>";
		int nextLine = fileHtmlStr.indexOf("<br>", start);
		while (nextLine >= 0) {
			int begin = fileHtmlStr.indexOf(":", start);
			int end = fileHtmlStr.indexOf(" &gt; ", start);
			if ((begin == start + 1) && (end >= start) && (end < nextLine)) {
				String linkStr = fileHtmlStr.substring(start, end);
				newFileHtml.append("<span class='a' onclick=\"browser.openFolderInOS('" + StrUtils.replaceAll(linkStr, "\\", "\\\\") + "')\">");
				newFileHtml.append(linkStr);
				newFileHtml.append("</span>");
				start = end;
			} else {
				if (nextLine >= start) {
					newFileHtml.append(fileHtmlStr.substring(start, nextLine + 4));
					start = nextLine + 4;
					nextLine = fileHtmlStr.indexOf("<br>", start);
				} else {
					break;
				}
			}
		}
		newFileHtml.append(fileHtmlStr.substring(start));
		fileHtmlStr = newFileHtml.toString().substring(0, newFileHtml.length() - 4);

		fileHtmlStr = addPicturesToEntryHtml(fileHtmlStr, folder, fileName);

		return fileHtmlStr;
	}

	private static String addPicturesToEntryHtml(String contentStr, Directory folder, String fileName) {

		StringBuilder multipleStrBuilder = new StringBuilder();
		multipleStrBuilder.append("<div class='warning_outer'>");
		multipleStrBuilder.append("<div class='warning'>");
		multipleStrBuilder.append("<div class='line'>");
		multipleStrBuilder.append("<b>Warning!</b> Some pictures of this entry exist with multiple extensions!<br>");
		multipleStrBuilder.append("They are:");
		multipleStrBuilder.append("</div>");

		boolean foundMultiples = false;

		List<String> pictureFileNames = new ArrayList<>();
		List<File> pictureFiles = new ArrayList<>();
		List<Long> pictureFileSizes = new ArrayList<>();
		String baseFileName = fileName;
		int index = baseFileName.lastIndexOf(".");
		if (index >= 0) {
			baseFileName = baseFileName.substring(0, index);
		}
		int curImgNum = 1;
		while (true) {
			boolean foundOne = false;
			for (String curImgExt : IMAGE_EXTENSIONS) {
				String curName = baseFileName + "_" + curImgNum + "." + curImgExt;
				File imageFile = new File(folder, curName);
				if (imageFile.exists()) {
					if (foundOne) {
						foundMultiples = true;
						multipleStrBuilder.append("<div class='line'>");
						multipleStrBuilder.append("Image number " + curImgNum + " has multiple extensions!");
						multipleStrBuilder.append("</div>");
					} else {
						pictureFileNames.add(UrlEncoder.encode(curName));
						pictureFiles.add(imageFile);
						pictureFileSizes.add(imageFile.getSize());
						foundOne = true;
					}
				}
			}
			if (!foundOne) {
				break;
			}
			curImgNum++;
		}

		// if there are no pictures, then there is nothing to do...
		if (pictureFileNames.size() < 1) {
			return contentStr;
		}

		if (foundMultiples) {
			multipleStrBuilder.append("</div>");
			multipleStrBuilder.append("</div>");
			contentStr = multipleStrBuilder.toString() + contentStr;
		}

		// check if any two pictures seem to be the same / duplicates, actually
		StringBuilder duplicateStrBuilder = new StringBuilder();
		duplicateStrBuilder.append("<div class='warning_outer'>");
		duplicateStrBuilder.append("<div class='warning'>");
		duplicateStrBuilder.append("<div class='line'>");
		duplicateStrBuilder.append("<b>Warning!</b> Several pictures of this entry seem to have the same size, ");
		duplicateStrBuilder.append("indicating they might be the same file!<br>");
		duplicateStrBuilder.append("They are:");
		duplicateStrBuilder.append("</div>");

		boolean foundDuplicates = false;

		for (int i = 0; i < pictureFiles.size(); i++) {
			long iLen = pictureFileSizes.get(i);
			for (int j = i+1; j < pictureFiles.size(); j++) {
				long jLen = pictureFileSizes.get(j);
				if (iLen == jLen) {
					foundDuplicates = true;
					duplicateStrBuilder.append("<div class='line'>");
					duplicateStrBuilder.append(pictureFiles.get(i).getCanonicalFilename() +
						" and " + pictureFiles.get(j).getCanonicalFilename());
					duplicateStrBuilder.append("</div>");
				}
			}
		}

		if (foundDuplicates) {
			duplicateStrBuilder.append("</div>");
			duplicateStrBuilder.append("</div>");
			contentStr = duplicateStrBuilder.toString() + contentStr;
		}


		Set<Integer> picturesAdded = new HashSet<>();
		String picLinkBase = "file:///" + UrlEncoder.encode(folder.getCanonicalDirname()) + "/";

		for (int i = pictureFileNames.size() - 1; i >= 0; i--) {

			String beforeThisRound = contentStr;

			// transform just the text picture 3 into a link to the third picture
			String replaceWith = "<a href='" + picLinkBase + pictureFileNames.get(i) + "' " +
				"target='_blank'>" +
				"picture " + (i+1) +
				"</a>";

			// replace all not preceded by >
			contentStr = StrUtils.replaceAllIfNotInsideTag(
				contentStr,
				"picture " + (i+1),
				replaceWith
			);

			// replace all preceded by exactly <br>
			contentStr = StrUtils.replaceAll(
				contentStr,
				"<br>picture " + (i+1),
				"<br>" + replaceWith
			);

			contentStr = StrUtils.addAfterLinesContaining(
				contentStr,
				replaceWith,
				picToHtml(picLinkBase, pictureFileNames.get(i)),
				"<br>"
			);

			// transform just the text pictures up to 3 into a link to the third picture,
			// which will later be augmented by pictures 1 and 2 (if they have not been added already)
			replaceWith = "<a href='" + picLinkBase + pictureFileNames.get(i) + "' " +
				"target='_blank'>" +
				"pictures up to " + (i+1) +
				"</a>";

			// replace all not preceded by >
			contentStr = StrUtils.replaceAllIfNotInsideTag(
				contentStr,
				"pictures up to " + (i+1),
				replaceWith
			);

			// replace all preceded by exactly <br>
			contentStr = StrUtils.replaceAll(
				contentStr,
				"<br>pictures up to " + (i+1),
				"<br>" + replaceWith
			);

			contentStr = StrUtils.addAfterLinesContaining(
				contentStr,
				replaceWith,
				"[[LOGPIC-UP-TO-" + i + "]]" +
				picToHtml(picLinkBase, pictureFileNames.get(i)),
				"<br>"
			);

			if (!contentStr.equals(beforeThisRound)) {
				picturesAdded.add(i);
			}
		}

		// second round to add the other pictures when going "up to" a certain one
		for (int i = pictureFileNames.size() - 1; i >= 0; i--) {
			String token = "[[LOGPIC-UP-TO-" + i + "]]";
			if (contentStr.contains(token)) {
				Integer nextPic = i - 1;
				if ((nextPic < 0) || picturesAdded.contains(nextPic)) {
					contentStr = StrUtils.replaceAll(contentStr, token, "");
				} else {
					contentStr = StrUtils.replaceAll(contentStr, token,
						"[[LOGPIC-UP-TO-" + nextPic + "]]" +
						picToHtml(picLinkBase, pictureFileNames.get(nextPic))
					);
					picturesAdded.add(nextPic);
				}
			}
		}

		/* actually, skip the third round and only add pictures when necessary
		// third round to add all remaining pictures in the very end
		int highestPicShown = -1;
		for (Integer picNum : picturesAdded) {
			if (picNum > highestPicShown) {
				highestPicShown = picNum;
			}
		}

		for (int i = highestPicShown + 1; i < pictureFileNames.size(); i++) {
			contentStr += picToHtml(picLinkBase, pictureFileNames.get(i));
		}
		*/

		return contentStr;
	}

	private static String picToHtml(String picLinkBase, String picFileName) {
		return "<a href='" + picLinkBase + picFileName + "' " +
				"target='_blank'>" +
				"<img src='" + picLinkBase + picFileName + "' style='padding-top: 4pt; max-width: 100%;' /></a><br>";
	}

	private static String prepareExternalLinks(String fileHtmlStr, String urlStart) {
		StringBuilder newFileHtml = new StringBuilder();
		int pos = fileHtmlStr.indexOf(urlStart);
		int start = 0;
		while (pos >= 0) {
			int end = fileHtmlStr.length();
			int end1 = fileHtmlStr.indexOf(" ", pos);
			int end2 = fileHtmlStr.indexOf("\t", pos);
			int end3 = fileHtmlStr.indexOf("<br>", pos);
			if (end1 >= start) {
				end = Math.min(end1, end);
			}
			if (end2 >= start) {
				end = Math.min(end2, end);
			}
			if (end3 >= start) {
				end = Math.min(end3, end);
			}
			newFileHtml.append(fileHtmlStr.substring(start, pos));
			String linkStr = fileHtmlStr.substring(pos, end);
			newFileHtml.append("<a href=\"" + linkStr + "\" target='_blank'>");
			newFileHtml.append(linkStr);
			newFileHtml.append("</a>");
			start = end;
			pos = fileHtmlStr.indexOf(urlStart, end);
		}
		newFileHtml.append(fileHtmlStr.substring(start));
		return newFileHtml.toString();
	}

	private static String funkCode(String str) {
		str = UrlEncoder.encode(str);
		str = StrUtils.replaceAll(str, "%C2%82", "%E2%80%9A");
		str = StrUtils.replaceAll(str, "%C2%84", "%E2%80%9E");
		str = StrUtils.replaceAll(str, "%C2%91", "%E2%80%98");
		str = StrUtils.replaceAll(str, "%C2%92", "%E2%80%99");
		str = StrUtils.replaceAll(str, "%C2%93", "%E2%80%9C");
		str = StrUtils.replaceAll(str, "%C2%94", "%E2%80%9D");
		return str;
	}

}
