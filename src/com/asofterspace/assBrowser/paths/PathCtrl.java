/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.paths;

import com.asofterspace.toolbox.utils.StrUtils;


public class PathCtrl {

	public final static String DESKTOP = "\\Desktop\\";


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
		String desktop = StrUtils.replaceAll(StrUtils.replaceAll(DESKTOP + "/", "\\", "/"), "//", "/");

		return path.startsWith(desktop);
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

}
