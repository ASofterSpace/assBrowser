/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.paths;

import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.utils.Stringifier;


public class DirectoryStringifier implements Stringifier<Directory> {

	public DirectoryStringifier() {
	}

	@Override
	public String getString(Directory dir) {
		return dir.getLocalDirname().toLowerCase();
	}
}
