/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.web;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.web.WebServer;
import com.asofterspace.toolbox.web.WebServerRequestHandler;

import java.net.Socket;


public class Server extends WebServer {

	private Database db;

	private Directory serverDir;

	private ConsoleCtrl consoleCtrl;


	public Server(Directory webRoot, Directory serverDir, Database db, ConsoleCtrl consoleCtrl) {

		super(webRoot, db.getPort());

		this.db = db;

		this.serverDir = serverDir;

		this.consoleCtrl = consoleCtrl;
	}

	@Override
	protected WebServerRequestHandler getHandler(Socket request) {
		return new ServerRequestHandler(this, request, webRoot, serverDir, db, consoleCtrl);
	}

}
