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
import com.asofterspace.toolbox.utils.StrUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Database {

	private final static String DESKTOP_LOCATION = "desktopLocation";

	private final static String PORT = "port";

	private static final String CYBER_SNAIL_PORT = "cyberSnailPort";

	private final static String BROWSER_PATH = "browserPath";

	private final static String NIRCMD_PATH = "nircmdPath";

	private final static String BATTERY_STATE_SCRIPT_PATH = "batteryStateScriptPath";

	private final static String FFMPEG_PATH = "ffmpegPath";

	private final static String FUNTUBE_CATEGORIES = "funtubeCategories";

	private final static String VIDEO_DIR = "videoDir";

	private final static String PROGRAMS_TO_OPEN_FILES = "programsToOpenFiles";

	private final static String HOME_DIR = "homeDir";

	private final static String DISPLAY_GUI = "displayGUI";

	private final static String DISPLAY_SIDEBAR = "displaySidebar";

	private final static String SET_AUDIO_VOLUME_TO_SILENCE_AT_STARTUP = "setAudioVolumetoSilenceAtStartup";

	private final static String STYLE = "style";

	private JsonFile dbFile;

	private JSON root;

	private Integer port;

	private Integer cyberSnailPort;

	private String desktopLocation;

	private String browserPath;

	private String nircmdPath;

	private String batteryStateScriptPath;

	private String ffmpegPath;

	private List<Record> funtubeCategories;

	private String videoDirPath;

	private Map<String, String> programsToOpenFiles = new HashMap<>();

	private Map<String, String> inMemoryFolderContent = new ConcurrentHashMap<>();

	private String homeDirPath;
	private Directory homeDir = null;

	private boolean displayGUI = true;

	private boolean displaySidebar = true;

	private boolean setAudioVolumetoSilenceAtStartup = true;

	private String style;


	public Database() {

		this.dbFile = new JsonFile("config/database.json");
		this.dbFile.createParentDirectory();
		try {
			this.root = dbFile.getAllContents();
		} catch (JsonParseException e) {
			System.err.println("Oh no, the file " + this.dbFile.getCanonicalFilename() + " could not be loaded!");
			e.printStackTrace(System.err);
			System.exit(1);
		}

		this.desktopLocation = root.getString(DESKTOP_LOCATION);

		this.port = root.getInteger(PORT);

		this.cyberSnailPort = root.getInteger(CYBER_SNAIL_PORT);

		this.browserPath = root.getString(BROWSER_PATH);

		this.nircmdPath = root.getString(NIRCMD_PATH);

		this.batteryStateScriptPath = root.getString(BATTERY_STATE_SCRIPT_PATH);

		this.ffmpegPath = root.getString(FFMPEG_PATH);

		this.funtubeCategories = root.getArray(FUNTUBE_CATEGORIES);

		this.videoDirPath = root.getString(VIDEO_DIR);

		this.programsToOpenFiles = root.getStringMap(PROGRAMS_TO_OPEN_FILES);

		this.homeDirPath = root.getString(HOME_DIR);

		this.displayGUI = root.getBoolean(DISPLAY_GUI, true);

		this.displaySidebar = root.getBoolean(DISPLAY_SIDEBAR, true);

		this.setAudioVolumetoSilenceAtStartup = root.getBoolean(SET_AUDIO_VOLUME_TO_SILENCE_AT_STARTUP, false);

		this.style = root.getString(STYLE, "turquoise");
	}

	public Record getRoot() {
		return root;
	}

	public void save() {

		root.makeObject();

		root.set(PORT, port);

		root.set(CYBER_SNAIL_PORT, cyberSnailPort);

		root.set(DESKTOP_LOCATION, desktopLocation);

		root.set(BROWSER_PATH, browserPath);

		root.set(NIRCMD_PATH, nircmdPath);

		root.set(BATTERY_STATE_SCRIPT_PATH, batteryStateScriptPath);

		root.set(FFMPEG_PATH, ffmpegPath);

		root.set(FUNTUBE_CATEGORIES, funtubeCategories);

		root.set(VIDEO_DIR, videoDirPath);

		root.set(PROGRAMS_TO_OPEN_FILES, programsToOpenFiles);

		root.set(HOME_DIR, homeDirPath);

		root.set(DISPLAY_GUI, displayGUI);

		root.set(DISPLAY_SIDEBAR, displaySidebar);

		root.set(SET_AUDIO_VOLUME_TO_SILENCE_AT_STARTUP, setAudioVolumetoSilenceAtStartup);

		root.set(STYLE, style);

		dbFile.setAllContents(root);
		dbFile.save();
	}

	public int getPort() {
		if (port == null) {
			return 3013;
		}
		return port;
	}

	public Integer getCyberSnailPort() {
		return cyberSnailPort;
	}

	public String getDesktopLocation() {
		return desktopLocation;
	}

	public String getBrowserPath() {
		return browserPath;
	}

	public String getNircmdPath() {
		return nircmdPath;
	}

	public String getBatteryStateScriptPath() {
		return batteryStateScriptPath;
	}

	public String getFfmpegPath() {
		return ffmpegPath;
	}

	public List<Record> getFuntubeCategories() {
		return funtubeCategories;
	}

	public String getVideoDirPathStr() {
		if (videoDirPath.contains("\\")) {
			videoDirPath = StrUtils.replaceAll(videoDirPath, "\\", "/");
		}
		if (!videoDirPath.endsWith("/")) {
			videoDirPath = videoDirPath + "/";
		}
		return videoDirPath;
	}

	public String setInMemoryFolderContent(String path, String content) {
		// for quick in memory folder content, remove the opened-highlighting as this will be shown
		// when opening a different entry as well!
		content = StrUtils.replaceAll(content, "<div class='a line opened ", "<div class='a line ");
		return inMemoryFolderContent.put(path, content);
	}

	public String getInMemoryFolderContent(String path) {
		return inMemoryFolderContent.get(path);
	}

	public Map<String, String> getProgramsToOpenFiles() {
		return programsToOpenFiles;
	}

	public String getHomeDirPathStr() {
		return homeDirPath;
	}

	public Directory getHomeDir() {
		if (homeDir == null) {
			homeDir = new Directory(homeDirPath);
		}
		return homeDir;
	}

	public boolean getDisplayGUI() {
		return displayGUI;
	}

	public boolean getDisplaySidebar() {
		return displaySidebar;
	}

	public boolean getSetAudioVolumetoSilenceAtStartup() {
		return setAudioVolumetoSilenceAtStartup;
	}

	public String getStyle() {
		return style;
	}

}
