/**
 * Unlicensed code created by A Softer Space, 2025
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.entry;

import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.coders.UrlEncoder;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;
import com.asofterspace.toolbox.web.WebAccessor;


public class Entry {

	public static final String CYBER_SNAIL_CONST = "cybersnail://";

	private String content = null;

	private boolean encrypted = false;

	private boolean exists = true;


	public Entry(File genericFile, String filenameForView, Database database) {

		if (genericFile.exists()) {
			TextFile file = PathCtrl.getEntryFile(genericFile);
			content = file.getContent();

			if (content.startsWith(CYBER_SNAIL_CONST)) {
				encrypted = true;
				String snailPath = content.substring(CYBER_SNAIL_CONST.length());
				content = WebAccessor.get("http://localhost:" + database.getCyberSnailPort() + "/text?path=" + UrlEncoder.encode(snailPath));
			}

			content = doAutomaticConversions(content);

		} else {

			exists = false;
			if (filenameForView != null) {
				content = filenameForView;
			} else {
				content = genericFile.getLocalFilenameWithoutType();
			}
			content += "\n\n";
		}

		if (content == null) {
			content = "! Unable to load file: " + genericFile.getAbsoluteFilename() + " !";
		}
	}

	public String getStringContent() {
		return content;
	}

	public boolean isEncrypted() {
		return encrypted;
	}

	public boolean getExists() {
		return exists;
	}

	private String doAutomaticConversions(String txt) {

		// convert DE date-time-stamps to EN date-time-stamps
		txt = DateUtils.convertDateTimeStampsDEtoEN(txt);

		// automatically replace datacomx-style |-lead quotation with markdown-style >-lead quotation
		if (txt.startsWith("| ")) {
			txt = "> " + txt.substring(2);
		}
		txt = StrUtils.replaceAll(txt, "\n| ", "\n> ");

		return txt;
	}

}
