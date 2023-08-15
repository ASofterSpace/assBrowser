/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.paths;

import com.asofterspace.assBrowser.Database;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.SimpleFile;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.StrUtils;


public class PathCtrl {

	public final static String DESKTOP = "\\Desktop\\";
	public final static String DESKTOP_FORWARD = StrUtils.replaceAll(DESKTOP, "\\", "/");
	public final static String VSTPU_STPU = "VSTPU.stpu";

	private static String desktopLocation = null;

	private static String oneUpDesktopLocation = null;

	private static Database database = null;


	public static void setDatabase(Database arg) {
		database = arg;
	}

	public static String removeTrailingSlash(String path) {
		while (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		while (path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

	public static boolean startsWithDesktopPath(String path) {

		path = StrUtils.replaceAll(StrUtils.replaceAll(path + "/", "\\", "/"), "//", "/");
		return path.startsWith(DESKTOP_FORWARD);
	}

	public static String ensurePathIsSafe(String path) {

		if (path == null) {
			path = DESKTOP;
		}

		path = StrUtils.replaceAll(StrUtils.replaceAll(path + "/", "\\", "/"), "//", "/");

		path = removeTrailingSlash(path);

		return path;
	}

	public static String oneUp(String path) {

		path = removeTrailingSlash(path);

		int lastSlash = path.lastIndexOf("/");
		int lastBackSlash = path.lastIndexOf("\\");

		if (lastSlash > lastBackSlash) {
			path = path.substring(0, lastSlash);
		} else {
			if (lastBackSlash >= 0) {
				path = path.substring(0, lastBackSlash);
			} else {
				path = "/";
			}
		}

		return path;
	}

	public static String getDesktopLocation() {
		if (desktopLocation == null) {
			desktopLocation = PathCtrl.ensurePathIsSafe(database.getDesktopLocation()) + "/";
		}
		return desktopLocation;
	}

	public static String getOneUpDesktopLocation() {
		if (oneUpDesktopLocation == null) {
			oneUpDesktopLocation = PathCtrl.oneUp(getDesktopLocation());
		}
		return oneUpDesktopLocation;
	}

	/**
	 * Takes a path and turns it into the form used in the operating system.
	 * So rplaces \Desktop\foo with C:\blubb\Desktop\foo
	 */
	public static String resolvePath(String path) {

		if (startsWithDesktopPath(path)) {
			return StrUtils.replaceAll(
				StrUtils.replaceAll(getOneUpDesktopLocation() + "/" + path, "\\", "/"),
				"//", "/");
		}

		return path;
	}

	/**
	 * Takes a path and turns it into the form used within the assBrowser.
	 * So replaces C:\blubb\Desktop\foo with \Desktop\foo
	 */
	public static String browserizePath(String path) {
		String pathCompare = StrUtils.replaceAll(path, "\\", "/") + "/";
		String desktopLocation = getDesktopLocation();
		if (pathCompare.startsWith(desktopLocation)) {
			pathCompare = DESKTOP + "/" + pathCompare.substring(desktopLocation.length());
			path = ensurePathIsSafe(pathCompare);
		}
		return path;
	}

	public static SimpleFile getVSTPUfile(Directory folder) {
		SimpleFile vstpuFile = new SimpleFile(folder, VSTPU_STPU);
		vstpuFile.setISOorUTFreadAndUTFwriteEncoding(true);
		return vstpuFile;
	}

	public static TextFile getEntryFile(File file) {
		TextFile entryFile = new TextFile(file);
		entryFile.setISOorUTFreadAndUTFwriteEncoding(true);
		return entryFile;
	}

	public static SimpleFile getSimpleEntryFile(File file) {
		SimpleFile entryFile = new SimpleFile(file);
		entryFile.setISOorUTFreadAndUTFwriteEncoding(true);
		return entryFile;
	}

}
