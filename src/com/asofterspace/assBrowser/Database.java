/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.Record;


public class Database {

	private final static String DESKTOP_LOCATION = "desktopLocation";

	private final static String PORT = "port";

	private final static String BROWSER_PATH = "browserPath";

	private final static String FFMPEG_PATH = "ffmpegPath";

	private JsonFile dbFile;

	private JSON root;

	private Integer port;

	private String desktopLocation;

	private String browserPath;

	private String ffmpegPath;


	public Database() {

		this.dbFile = new JsonFile("config/database.json");
		this.dbFile.createParentDirectory();
		try {
			this.root = dbFile.getAllContents();
		} catch (JsonParseException e) {
			System.err.println("Oh no!");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		this.desktopLocation = root.getString(DESKTOP_LOCATION);

		this.port = root.getInteger(PORT);

		this.browserPath = root.getString(BROWSER_PATH);

		this.ffmpegPath = root.getString(FFMPEG_PATH);
	}

	public Record getRoot() {
		return root;
	}

	public void save() {

		root.makeObject();

		root.set(PORT, port);

		root.set(DESKTOP_LOCATION, desktopLocation);

		root.set(BROWSER_PATH, browserPath);

		root.set(FFMPEG_PATH, ffmpegPath);

		dbFile.setAllContents(root);
		dbFile.save();
	}

	public int getPort() {
		if (port == null) {
			return 3013;
		}
		return port;
	}

	public String getDesktopLocation() {
		return desktopLocation;
	}

	public String getBrowserPath() {
		return browserPath;
	}

	public String getFfmpegPath() {
		return ffmpegPath;
	}

}
