/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.gui.GUI;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.assBrowser.web.Server;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.JSON;
import com.asofterspace.toolbox.io.JsonFile;
import com.asofterspace.toolbox.io.JsonParseException;
import com.asofterspace.toolbox.Utils;
import com.asofterspace.toolbox.web.WebTemplateEngine;

import java.util.List;

import javax.swing.SwingUtilities;


public class AssBrowser {

	public final static String SERVER_DIR = "server";
	public final static String WEB_ROOT_DIR = "deployed";

	public final static String PROGRAM_TITLE = "assBrowser";
	public final static String VERSION_NUMBER = "0.0.2.6(" + Utils.TOOLBOX_VERSION_NUMBER + ")";
	public final static String VERSION_DATE = "16. December 2020 - 22. July 2023";


	public static void main(String[] args) {

		// let the Utils know in what program it is being used
		Utils.setProgramTitle(PROGRAM_TITLE);
		Utils.setVersionNumber(VERSION_NUMBER);
		Utils.setVersionDate(VERSION_DATE);

		if (args.length > 0) {
			if (args[0].equals("--version")) {
				System.out.println(Utils.getFullProgramIdentifierWithDate());
				return;
			}

			if (args[0].equals("--version_for_zip")) {
				System.out.println("version " + Utils.getVersionNumber());
				return;
			}
		}


		System.out.println(Utils.getFullProgramIdentifierWithDate());


		System.out.println("Loading database...");

		Database database = new Database();

		System.out.println("Saving database...");

		database.save();

		PathCtrl.setDatabase(database);


		System.out.println("Looking at directories...");

		Directory serverDir = new Directory(SERVER_DIR);
		Directory webRoot = new Directory(WEB_ROOT_DIR);


		System.out.println("Starting up system console...");

		ConsoleCtrl consoleCtrl = new ConsoleCtrl(database);


		System.out.println("Starting up console line GUI...");

		GUI gui = new GUI(database, consoleCtrl);
		SwingUtilities.invokeLater(gui);
		consoleCtrl.setGUI(gui);


		try {

			JsonFile jsonConfigFile = new JsonFile(serverDir, "webengine.json");
			JSON jsonConfig = jsonConfigFile.getAllContents();
			jsonConfig.inc("version");
			jsonConfigFile.save(jsonConfig);

			List<String> whitelist = jsonConfig.getArrayAsStringList("files");

			System.out.println("Templating the web application...");

			WebTemplateEngine engine = new WebTemplateEngine(serverDir, jsonConfig);

			engine.compileTo(webRoot);


			System.out.println("Starting the server on port " + database.getPort() + "...");

			Server server = new Server(webRoot, serverDir, database, consoleCtrl);

			server.setWhitelist(whitelist);

			boolean async = true;

			server.serve(async);

		} catch (JsonParseException e) {

			System.out.println("Oh no! The input could not be parsed: " + e);
		}
	}

}
