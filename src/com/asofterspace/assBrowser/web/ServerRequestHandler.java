/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.web;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.console.ConsoleResult;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.coders.UrlDecoder;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class ServerRequestHandler extends WebServerRequestHandler {

	private Database database;

	private Directory serverDir;

	private ConsoleCtrl consoleCtrl;

	private List<String> IMAGE_EXTENSIONS;

	private String videoDirPathStr = null;

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

		IMAGE_EXTENSIONS = new ArrayList<>();
		IMAGE_EXTENSIONS.add("jpg");
		IMAGE_EXTENSIONS.add("png");
		IMAGE_EXTENSIONS.add("gif");
		IMAGE_EXTENSIONS.add("bmp");
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
			File vidfile = new File(viddir, UrlDecoder.decode(arguments.get("path")));
			return new WebServerAnswerBasedOnFile(vidfile);
		}

		if (location.startsWith("/funtubePreview")) {
			Directory viddir = new Directory(videoDirPathStr);
			File prevfile = new File(viddir, UrlDecoder.decode(arguments.get("path")) + ".jpg");
			if (!prevfile.exists()) {
				// generate the preview file
				File vidfile = new File(viddir, UrlDecoder.decode(arguments.get("path")));

				generatePreviewFile(vidfile, prevfile);
			}
			return new WebServerAnswerBasedOnFile(prevfile);
		}

		if (location.startsWith("/funtube")) {
			return generateAnswerToFunTubeRequest(arguments);
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

		// if a link argument exists, it overrides path and file - it may contain just a path, or a path followed
		// by a filename without the .stpu (so link=/foo/bar may link to the folder bar inside the folder foo,
		// or may link to the file bar.stpu inside the folder foo)
		String link = arguments.get("link");

		String consoleValue = "";

		path = PathCtrl.ensurePathIsSafe(path);

		// interpret console commands - in case we do a cd, the path has to be changed here - not earlier,
		// not later
		if (arguments.get("console") != null) {
			ConsoleResult consoleResult = consoleCtrl.interpretCommand(arguments.get("console"), path);
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
			String localLink = resolvePath(link);
			Directory localLinkDir = new Directory(localLink);
			if (localLinkDir.exists()) {
				path = link;
				fileName = null;
			} else {
				link = StrUtils.replaceAll(link, "\\", "/");
				int pos = link.lastIndexOf("/");
				path = link.substring(0, pos);
				fileName = link.substring(pos + 1) + ".stpu";
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

			if (entries == null) {
				addTextToHtml(folderContent, "! Unable to load VSTPU.stpu !");
			} else {

				for (String entry : entries) {
					Directory curDir = directories.get(entry.toLowerCase());
					if (curDir != null) {
						addFolderToHtml(folderContent, curDir, path);
					} else {
						File curFile = null;
						if (entry.toLowerCase().endsWith(".sll")) {
							curFile = files.get(entry.toLowerCase());
						} else {
							curFile = files.get(entry.toLowerCase() + ".stpu");
						}
						if (curFile != null) {
							addFileToHtml(folderContent, entry, curFile, path);
						} else {
							boolean foundImage = false;
							for (String imageExt : IMAGE_EXTENSIONS) {
								File imageFile = new File(folder, entry + "_1." + imageExt);
								if (imageFile.exists()) {
									foundImage = true;
									break;
								}
							}
							if (foundImage) {
								addFileToHtml(folderContent, entry, new File(folder, entry + ".stpu"), path);
							} else {
								addTextToHtml(folderContent, entry);
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
		buttonHtml.append("..");
		buttonHtml.append("</a>");

		if (fileName != null) {
			buttonHtml.append(getDownloadButtonHtml(path, fileName, ""));
		}

		buttonHtml.append("<span class='button' onclick='browser.openUploadModal()'>");
		buttonHtml.append("Upload a File");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.openFolderInOS()'>");
		buttonHtml.append("Open in OS");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.openTileView()'>");
		buttonHtml.append("Tile");
		buttonHtml.append("</span>");

		buttonHtml.append("<span class='button' onclick='browser.openComicView()'>");
		buttonHtml.append("Comic");
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
		String imagesStr = "";

		if (fileName != null) {
			File genericFile = new File(folder, fileName);
			String lowCaseFileName = fileName.toLowerCase();
			if (lowCaseFileName.endsWith(".stpu") || lowCaseFileName.endsWith(".sll") ||
				lowCaseFileName.endsWith(".txt") || lowCaseFileName.endsWith(".ini")) {

				if (genericFile.exists()) {
					TextFile file = new TextFile(folder, fileName);
					file.setEncoding(TextEncoding.ISO_LATIN_1);
					fileHtmlStr = file.getContent();
				} else {
					fileHtmlStr = genericFile.getLocalFilenameWithoutType() + "\n\n";
				}

				if (fileHtmlStr == null) {
					fileHtmlStr = "! Unable to load file: " + genericFile.getAbsoluteFilename() + " !";
				}

				// follow link automatically
				if (fileHtmlStr.startsWith("%[") && fileHtmlStr.contains("]")) {
					String newLink = fileHtmlStr.substring(2, fileHtmlStr.indexOf("]"));
					Map<String, String> newArgs = new HashMap<>();
					newArgs.put("link", newLink);
					return generateAnswerToMainGetRequest(newArgs, message);
				}

				fileHtmlStr = prepareStrForDisplayInHtml(fileHtmlStr);
				// replace %[...] with internal links
				StringBuilder newFileHtml = new StringBuilder();
				int pos = fileHtmlStr.indexOf("%[");
				int start = 0;
				while (pos >= 0) {
					int end = fileHtmlStr.indexOf("]", pos);
					if (end >= 0) {
						newFileHtml.append(fileHtmlStr.substring(start, pos));
						String linkStr = fileHtmlStr.substring(pos + 2, end);
						newFileHtml.append("<a href=\"/?link=" + linkStr + "\">%[");
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
					String imgUrl = "/?path=" + path + "&file=" + baseName + "_" +
						imgNum + "." + imgExtFound + "&action=download";
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
					overallBuilder.append("<span class='button' onclick='browser.closeView()' style='display:none; margin-top:5pt; margin-bottom:5pt;' ");
					overallBuilder.append("id='closeComicViewBtn'>Close View</span>");
					overallBuilder.append(imagesStrBuilder);
					overallBuilder.append("</div>");

					overallBuilder.append("<div id='tileStripsContainer' style='display:none; overflow-y:scroll;'>");
					overallBuilder.append("<span class='button' onclick='browser.closeView()' style='display:block; margin-top:5pt; margin-bottom:5pt;' ");
					overallBuilder.append("id='closeTileViewBtn'>Close View</span>");
					overallBuilder.append("<div>");

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
			} else if (lowCaseFileName.endsWith(".jpg") || lowCaseFileName.endsWith(".jpeg") ||
				lowCaseFileName.endsWith(".png") || lowCaseFileName.endsWith(".gif") ||
				lowCaseFileName.endsWith(".bmp")) {
				String imgUrl = "/?path=" + path + "&file=" + fileName + "&action=download";
				fileHtmlStr = "<a target=\"_blank\" href=\"" + imgUrl + "\" style='max-width:99%; max-height:99%;' />";
				fileHtmlStr += "<img src=\"" + imgUrl + "\" style='max-width:100%; max-height:100%;' />";
				fileHtmlStr += "</a>";
			} else {
				fileHtmlStr = "No preview for '" + fileName + "' available.<br><br>" +
							  getDownloadButtonHtml(path, fileName, "padding: 4pt 9pt;");
			}
		}

		indexContent = StrUtils.replaceAll(indexContent, "[[FILE_CONTENT]]", fileHtmlStr);

		indexContent = StrUtils.replaceAll(indexContent, "[[IMAGES]]", imagesStr);

		return new WebServerAnswerInHtml(indexContent);
	}

	private WebServerAnswer generateAnswerToFunTubeRequest(Map<String, String> arguments) {

		TextFile indexBaseFile = new TextFile(webRoot, "funtube.htm");
		String html = indexBaseFile.getContent();

		html = StrUtils.replaceAll(html, "[[SIDEBAR]]",
			SideBarCtrl.getSidebarHtmlStr(SideBarEntryForTool.FUNTUBE));

		String videoPath = UrlDecoder.decode(arguments.get("path"));
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

		html = StrUtils.replaceAll(html, "[[VIDEO_PATH]]", UrlEncoder.encode(videoPath));

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
		folderContent.append("<a href=\"/?path=" + path + "/");
		folderContent.append(childFolder.getLocalDirname() + "\">");
		folderContent.append("<div class='line folder'>");
		folderContent.append(HTML.escapeHTMLstrNbsp(childFolder.getLocalDirname()));
		folderContent.append("</div>");
		folderContent.append("</a>");
	}

	private void addFileToHtml(StringBuilder folderContent, String filename, File childFile, String path) {
		String entryOrLink = "entry";
		TextFile txtFile = new TextFile(childFile);
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
		folderContent.append("<a href=\"/?path=" + path);
		folderContent.append("&file=" + childFile.getLocalFilename() + "\">");
		folderContent.append("<div class='line " + entryOrLink + "'>");
		folderContent.append(HTML.escapeHTMLstrNbsp(filename));
		folderContent.append("</div>");
		folderContent.append("</a>");
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

}
