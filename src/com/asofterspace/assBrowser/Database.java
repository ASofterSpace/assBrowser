/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser;

import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.utils.Record;


public class Database {

	private final static String DESKTOP_LOCATION = "desktopLocation";

	private final static String PORT = "port";

	private JsonFile dbFile;

	private JSON root;

	private Integer port;

	private String desktopLocation;


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

		this.port = root.getInteger(PORT);

		this.desktopLocation = root.getString(DESKTOP_LOCATION);
	}

	public Record getRoot() {
		return root;
	}

	public void save() {

		root.makeObject();

		root.set(PORT, port);

		root.set(DESKTOP_LOCATION, desktopLocation);

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

}
