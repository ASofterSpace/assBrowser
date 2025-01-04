/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.paths;

import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.utils.Stringifier;


public class FileStringifier implements Stringifier<File> {

	public FileStringifier() {
	}

	@Override
	public String getString(File file) {
		return file.getLocalFilename();
	}
}
