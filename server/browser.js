window.browser = {

	// are we currently editing the entry?
	editingMode: false,

	// are we preventing the firing of entry change events to turn the save button red?
	preventEntryChangeFire: false,

	// are we currently editing the folder?
	folderEditingMode: false,

	// are we preventing the firing of folder change events to turn the save button red?
	preventFolderChangeFire: false,

	COLOR_SAVE_GREEN: "rgb(0, 187, 0)",
	COLOR_SAVE_RED: "rgb(238, 0, 34)" ,


	onResize: function() {

		var retry = false;

		var body = document.getElementById("body");
		if (body) {
			body.style.height = window.innerHeight + "px";
		} else {
			retry = true;
		}

		var mainContent = document.getElementById("mainContent");
		if (mainContent) {
			mainContent.style.height = (window.innerHeight - 31) + "px";
		} else {
			retry = true;
		}

		if (retry) {
			// if we could not fully resize now, then let's do it later...
			window.setTimeout(function() {
				window.browser.onResize();
			}, 100);
		}
	},

	expandConsole: function() {
		this.closeMoreActions();
		if (document.getElementById("expandConsoleBtn").innerHTML == "Collapse Console") {
			document.getElementById("fileContentContainer").style.height = "90%";
			document.getElementById("consoleContainer").style.height = "1.5%";
			document.getElementById("expandConsoleBtn").innerHTML = "Expand Console";
		} else {
			document.getElementById("fileContentContainer").style.height = "15%";
			document.getElementById("consoleContainer").style.height = "76.5%";
			document.getElementById("expandConsoleBtn").innerHTML = "Collapse Console";
		}
	},

	openFolderInOS: function(which) {

		this.closeMoreActions();

		var request = new XMLHttpRequest();
		request.open("POST", "openFolderInOS", true);
		request.setRequestHeader("Content-Type", "application/json");

		var data = {
			path: which,
		};

		if (!which) {
			data.path = window.data.path;
		}

		request.send(JSON.stringify(data));
	},

	openFileInOS: function(which) {

		this.closeMoreActions();

		var request = new XMLHttpRequest();
		request.open("POST", "openFileInOS", true);
		request.setRequestHeader("Content-Type", "application/json");

		var data = {
			path: which,
		};

		request.send(JSON.stringify(data));
	},

	openUploadModal: function() {
		this.closeMoreActions();
		document.getElementById("modalBackground").style.display = "block";
		document.getElementById("uploadFileModal").style.display = "block";
	},

	closeUploadModal: function() {
		this.closeMoreActions();
		document.getElementById("modalBackground").style.display = "none";
		document.getElementById("uploadFileModal").style.display = "none";
	},

	openComicView: function() {
		this.closeMoreActions();

		var imageStrip = document.getElementById("imageStrip");
		imageStrip.style.position = "fixed";
		imageStrip.style.width = "100%";
		imageStrip.style.height = "100%";
		imageStrip.style.zIndex = 100;
		imageStrip.style.backgroundColor = "#FFFFFF";

		var body = document.getElementsByTagName("body")[0];
		body.style.padding = "0px";
		body.style.margin = "0px";

		var closeComicViewBtn = document.getElementById('closeComicViewBtn');
		closeComicViewBtn.style.display = 'block';
	},

	openTileView: function() {
		this.closeMoreActions();

		var imageStrip = document.getElementById("imageStrip");
		imageStrip.style.display = 'none';

		var tileStripsContainer = document.getElementById("tileStripsContainer");
		tileStripsContainer.style.display = 'block';

		tileStripsContainer.style.position = "fixed";
		tileStripsContainer.style.width = "100%";
		tileStripsContainer.style.height = "100%";
		tileStripsContainer.style.zIndex = 100;
		tileStripsContainer.style.backgroundColor = "rgb(64,0,128)";

		var body = document.getElementsByTagName("body")[0];
		body.style.padding = "0px";
		body.style.margin = "0px";
	},

	closeView: function() {
		this.closeMoreActions();

		var tileStripsContainer = document.getElementById("tileStripsContainer");
		tileStripsContainer.style.display = 'none';

		var imageStrip = document.getElementById("imageStrip");
		imageStrip.style.display = 'block';
		imageStrip.style.position = "unset";
		imageStrip.style.width = "15%";
		imageStrip.style.height = "90%";
		imageStrip.style.backgroundColor = "unset";

		var body = document.getElementsByTagName("body")[0];
		body.style.padding = "0pt 60pt 0pt 4pt";
		body.style.margin = "8px";

		var closeComicViewBtn = document.getElementById('closeComicViewBtn');
		closeComicViewBtn.style.display = 'none';
	},

	loadFullFolderView: function() {

		var request = new XMLHttpRequest();
		request.open("GET", "getFolder?editingMode=false&quickView=false&path=" + encodeURI(window.data.path) +
			"&file=" + encodeURI(window.data.file), true);
		request.setRequestHeader("Content-Type", "application/json");

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if (window.data.path == result.path) {
					document.getElementById("folderContainer").innerHTML = result.content;

					browser.scrollIfNecessary();
				}
			}
		}

		request.send();
	},


	toggleEditEntry: function() {
		this.closeMoreActions();
		this.editingMode = !this.editingMode;
		var entryScrollBefore = 0;

		if (this.editingMode) {
			entryScrollBefore = document.getElementById("fileContentContainer").scrollTop /
				document.getElementById("fileContentContainer").scrollTopMax;
			document.getElementById("edit-btn").innerText = "Show";
			document.getElementById("fileContentTextarea").style.width =
				(document.getElementById("fileContentContainer").clientWidth - 20) + "px";
			document.getElementById("fileContentContainer").style.display = "none";
			document.getElementById("fileContentTextarea").style.display = "block";
			browser.preventEntryChangeFire = true;
			document.getElementById("fileContentTextarea").value = "Loading for Edit...";
			window.setTimeout(function() {
				browser.preventEntryChangeFire = false;
			}, 100);
			document.getElementById("save-btn").style.background =
				document.getElementById("edit-btn").style.background;
			document.getElementById("save-btn").style.display = "inline";
			var enableEditBtns = document.getElementsByClassName("editBtnDisabled");
			for (var i = enableEditBtns.length - 1; i >= 0; i--) {
				enableEditBtns[i].className = 'button editBtnEnabled';
			}
		} else {
			entryScrollBefore = document.getElementById("fileContentTextarea").scrollTop /
				document.getElementById("fileContentTextarea").scrollTopMax;
			document.getElementById("edit-btn").innerText = "Edit";
			document.getElementById("save-btn").style.display = "none";
			document.getElementById("fileContentContainer").style.display = "block";
			document.getElementById("fileContentTextarea").style.display = "none";
			document.getElementById("fileContentContainer").innerHTML = "Loading View...";
			var disableEditBtns = document.getElementsByClassName("editBtnEnabled");
			for (var i = disableEditBtns.length - 1; i >= 0; i--) {
				disableEditBtns[i].className = 'button editBtnDisabled';
			}
		}

		var request = new XMLHttpRequest();
		request.open("GET", "getEntry?editingMode=" + this.editingMode +
			"&path=" + encodeURI(window.data.path) +
			"&file=" + encodeURI(window.data.file), true);
		request.setRequestHeader("Content-Type", "application/json");

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if ((window.data.path == result.path) &&
					(window.data.file == result.file)) {
					if (window.browser.editingMode) {
						browser.preventEntryChangeFire = true;

						document.getElementById("fileContentTextarea").value = browser.decodeToTextarea(result.entry);
						document.getElementById("fileContentTextarea").focus();

						window.setTimeout(function() {
							browser.preventEntryChangeFire = false;

							document.getElementById("fileContentTextarea").scrollTo(0,
								entryScrollBefore * document.getElementById("fileContentTextarea").scrollTopMax);
						}, 100);
					} else {
						document.getElementById("fileContentContainer").innerHTML = result.entry;

						document.getElementById("fileContentContainer").scrollTo(0,
							entryScrollBefore * document.getElementById("fileContentContainer").scrollTopMax);
					}
				}
			}
		}

		request.send();
	},

	entryChanged: function() {
		if (!browser.preventEntryChangeFire) {
			document.getElementById("save-btn").style.background = browser.COLOR_SAVE_RED;
		}
	},

	saveEntry: function() {

		var request = new XMLHttpRequest();
		request.open("POST", "saveEntry", true);
		request.setRequestHeader("Content-Type", "application/json");

		var savedContent = document.getElementById("fileContentTextarea").value;

		var data = {
			path: window.data.path,
			file: window.data.file,

			content: browser.encodeFromTextarea(savedContent)
		};

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if ((window.data.path == result.path) &&
					(window.data.file == result.file) &&
					(savedContent == document.getElementById("fileContentTextarea").value)) {
					document.getElementById("save-btn").style.background = browser.COLOR_SAVE_GREEN;
					window.setTimeout(function() {
						if (document.getElementById("save-btn").style.backgroundColor == browser.COLOR_SAVE_GREEN) {
							document.getElementById("save-btn").style.background =
								document.getElementById("edit-btn").style.background;
						}
					}, 2500);
				}
			}
		}

		request.send(JSON.stringify(data));
	},


	toggleEditFolder: function() {
		this.closeMoreActions();
		this.folderEditingMode = !this.folderEditingMode;
		var scrollBefore = 0;
		if (this.folderEditingMode) {
			scrollBefore = document.getElementById("folderContainer").scrollTop /
				document.getElementById("folderContainer").scrollTopMax;
			document.getElementById("edit-folder-btn").innerText = "Show";
			document.getElementById("folderTextarea").style.width =
				document.getElementById("folderContainer").clientWidth + "px";
			document.getElementById("folderContainer").style.display = "none";
			document.getElementById("folderTextarea").style.display = "block";
			browser.preventFolderChangeFire = true;
			document.getElementById("folderTextarea").value = "Loading for Edit...";
			window.setTimeout(function() {
				browser.preventFolderChangeFire = false;
			}, 100);
			document.getElementById("save-folder-btn").style.background =
				document.getElementById("edit-folder-btn").style.background;
			document.getElementById("save-folder-btn").style.display = "inline";
		} else {
			scrollBefore = document.getElementById("folderTextarea").scrollTop /
				document.getElementById("folderTextarea").scrollTopMax;
			document.getElementById("edit-folder-btn").innerText = "Edit";
			document.getElementById("save-folder-btn").style.display = "none";
			document.getElementById("folderContainer").style.display = "block";
			document.getElementById("folderTextarea").style.display = "none";
			document.getElementById("folderContainer").innerHTML = "Loading View...";
		}

		var request = new XMLHttpRequest();
		request.open("GET", "getFolder?editingMode=" + this.folderEditingMode +
			"&quickView=false&path=" + encodeURI(window.data.path) + "&file=" + encodeURI(window.data.file), true);
		request.setRequestHeader("Content-Type", "application/json");

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if (window.data.path == result.path) {
					if (window.browser.folderEditingMode) {
						browser.preventFolderChangeFire = true;

						if (result.content == null) {
							result.content = document.getElementById("folderContainer").innerText;
						}

						document.getElementById("folderTextarea").value = browser.decodeToTextarea(result.content);
						document.getElementById("folderTextarea").focus();

						window.setTimeout(function() {
							browser.preventFolderChangeFire = false;

							document.getElementById("folderTextarea").scrollTo(0,
								scrollBefore * document.getElementById("folderTextarea").scrollTopMax);
						}, 100);
					} else {
						document.getElementById("folderContainer").innerHTML = result.content;

						window.setTimeout(function() {
							document.getElementById("folderContainer").scrollTo(0,
								scrollBefore * document.getElementById("folderContainer").scrollTopMax);
						}, 100);
					}
				}
			}
		}

		request.send();
	},


	doToggleSomeEditingMode: function() {

		if (browser.editingMode) {
			if (browser.folderEditingMode) {
				if (document.getElementById("save-folder-btn").style.backgroundColor == browser.COLOR_SAVE_RED) {
					browser.toggleEditEntry();
				} else {
					browser.toggleEditFolder();
				}
			} else {
				browser.toggleEditEntry();
			}
		} else {
			if (browser.folderEditingMode) {
				browser.toggleEditFolder();
			} else {
				browser.toggleEditEntry();
			}
		}
	},

	folderChanged: function() {
		if (!browser.preventFolderChangeFire) {
			document.getElementById("save-folder-btn").style.background = browser.COLOR_SAVE_RED;
		}
	},

	saveFolder: function() {

		var request = new XMLHttpRequest();
		request.open("POST", "saveFolder", true);
		request.setRequestHeader("Content-Type", "application/json");

		var savedContent = document.getElementById("folderTextarea").value;

		var data = {
			path: window.data.path,
			file: window.data.file,

			content: browser.encodeFromTextarea(savedContent)
		};

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if ((window.data.path == result.path) &&
					(savedContent == document.getElementById("folderTextarea").value)) {
					document.getElementById("save-folder-btn").style.background = browser.COLOR_SAVE_GREEN;
					window.setTimeout(function() {
						if (document.getElementById("save-folder-btn").style.backgroundColor == browser.COLOR_SAVE_GREEN) {
							document.getElementById("save-folder-btn").style.background =
								document.getElementById("edit-folder-btn").style.background;
						}
					}, 2500);
				}
			}
		}

		request.send(JSON.stringify(data));
	},

	decodeToTextarea: function(content) {
		return content
			.split("\u0080").join("€")
			.split("\u0082").join("‚")
			.split("\u0084").join("„")
			.split("\u0085").join("…")
			.split("\u0091").join("‘")
			.split("\u0092").join("’")
			.split("\u0093").join("“")
			.split("\u0094").join("”")
			.split("\u0096").join("–")
			.split("\u0097").join("—")
			.split("\u009A").join("š");
	},

	encodeFromTextarea: function(content) {
		return content
			.split("€").join("\u0080")
			.split("‚").join("\u0082")
			.split("„").join("\u0084")
			.split("…").join("\u0085")
			.split("‘").join("\u0091")
			.split("’").join("\u0092")
			.split("“").join("\u0093")
			.split("”").join("\u0094")
			.split("–").join("\u0096")
			.split("—").join("\u0097")
			.split("š").join("\u009A");
	},

	scrollIfNecessary: function() {
		var params = new URLSearchParams(window.location.search);
		var scroll = params.get("scroll");

		// if no scroll is given, scroll roughly to the currently opened entry, if we find one
		if (scroll == null) {
			var nodes = document.getElementById("folderContainer").childNodes;
			for (var i = 0; i < nodes.length; i++) {
				if ((" " + nodes[i].className + " ").indexOf(" opened ") > -1) {
					scroll = i / nodes.length;
					break;
				}
			}
		}

		if (scroll != null) {
			window.setTimeout(function() {
				document.getElementById("folderContainer").scrollTo(0,
					1*scroll * document.getElementById("folderContainer").scrollTopMax);
			}, 100);
		}
	},

	navigateTo: function(newUrl) {
		var el = document.getElementById("folderContainer");

		if (this.folderEditingMode) {
			el = document.getElementById("folderTextarea");
		}

		if (el.scrollTopMax > 0) {
			newUrl = newUrl + "&scroll=" + (el.scrollTop / el.scrollTopMax);
		}

		window.location = newUrl;
	},

	closeMoreActions: function() {
		var textActionsContainer = document.getElementById('text-actions-container');
		if (textActionsContainer) {
			textActionsContainer.style.display = 'none';
		}
		var textActionsBtn = document.getElementById('text-actions-btn');
		if (textActionsBtn) {
			textActionsBtn.innerText = 'Text Actions...';
			textActionsBtn.className = 'button';
		}

		var moreActionsContainer = document.getElementById('more-actions-container');
		if (moreActionsContainer) {
			moreActionsContainer.style.display = 'none';
		}
		var moreActionsBtn = document.getElementById('more-actions-btn');
		if (moreActionsBtn) {
			moreActionsBtn.innerText = 'Other Actions...';
			moreActionsBtn.className = 'button';
		}
	},

	toggleTextActions: function() {
		var container = document.getElementById('text-actions-container');
		if (container && (container.style.display == 'none')) {
			this.closeMoreActions();
			container.style.display = 'block';
			var textActionsBtn = document.getElementById('text-actions-btn');
			if (textActionsBtn) {
				textActionsBtn.innerText = 'Close Actions...';
				textActionsBtn.className = 'button activeInBackground';
				container.style.left = (13 + textActionsBtn.offsetLeft + (textActionsBtn.offsetWidth / 2) -
					(container.clientWidth / 2)) + "px";
			}
		} else {
			this.closeMoreActions();
		}
	},

	toggleMoreActions: function() {
		var container = document.getElementById('more-actions-container');
		if (container && (container.style.display == 'none')) {
			this.closeMoreActions();
			container.style.display = 'block';
			var moreActionsBtn = document.getElementById('more-actions-btn');
			if (moreActionsBtn) {
				moreActionsBtn.innerText = 'Close Actions...';
				moreActionsBtn.className = 'button activeInBackground';
				container.style.left = (13 + moreActionsBtn.offsetLeft + (moreActionsBtn.offsetWidth / 2) -
					(container.clientWidth / 2)) + "px";
			}
		} else {
			this.closeMoreActions();
		}
	},

	getCurrentEntryText: function() {

		if (this.editingMode) {
			return document.getElementById("fileContentTextarea").value;
		}

		return document.getElementById("fileContentContainer").innerText;
	},

	extractTLDR: function() {
		var content = this.getCurrentEntryText();
		var commentText = "";

		content = content.split("TL;DR:");
		if (content.length == 1) {
			content = content[0].split("Summary:");
		}
		if (content.length > 1) {
			content = content[1];
			content = content.split("\n\n");
			content = content[0];
			content = content.trim();

			// move comment below
			var commentStartStr = "\nMoya: ";
			content = content.split(commentStartStr);
			if (content.length > 1) {
				commentText = commentStartStr + content[1];
			}
			content = content[0].trim();

			// only replace " by ', but do not fully switch them, as ' also just appears regularly in the text
			// in contractions
			// so fixing ' to " manually is less work than fixing " back to ' where it shouldn't have been replaced
			content = content.split('"').join("'");

			content = 'see for: "' + content + '":' + "\n";
		} else {
			content = content[0].split("\n");
			content = 'see also for ' + content[0].toLowerCase() + ':' + "\n";
		}

		var fileTitle = window.data.file;
		if (fileTitle.endsWith(".stpu")) {
			fileTitle = fileTitle.substring(0, fileTitle.length - 5);
		}

		content += "%[" + window.data.path.split("/").join("\\") + "\\" + fileTitle + "]";

		content += commentText;

		this.copyToClipboard(content);

		this.closeMoreActions();
	},

	copyToClipboard: function(content) {
		var clipboardHelper = document.getElementById("clipboardHelper");
		clipboardHelper.style.display = 'inline';
		clipboardHelper.value = content;
		clipboardHelper.select();
		clipboardHelper.setSelectionRange(0, 99999);
		navigator.clipboard.writeText(clipboardHelper.value);
		clipboardHelper.style.display = 'none';
	},

	// Adjust the open browser field on the right-hand side to point to
	// the same location as we are currently pointing to
	adjustOpenBrowserField: function() {
		var elems = document.getElementsByTagName("a");
		var compareTo = "http://localhost:" + window.location.port + "/";
		for (var i = 0; i < elems.length; i++) {
			if (elems[i].href == compareTo) {
				elems[i].href = window.location.href;
			}
		}
	},

	showModalBackground: function() {
		document.getElementById('renameDeleteModalBackground').style.display = 'block';
	},

	hideModalBackground: function() {
		document.getElementById('renameDeleteModalBackground').style.display = 'none';
	},

	showRenameModal: function() {
		this.showModalBackground();
		document.getElementById('renameInput').value = window.data.file;
		document.getElementById('renameTextFileName').innerText = window.data.file;
		document.getElementById('renameModal').style.display = 'block';
	},

	doRename: function() {

		var request = new XMLHttpRequest();
		request.open("POST", "doRename", true);
		request.setRequestHeader("Content-Type", "application/json");

		var data = {
			path: window.data.path,
			file: window.data.file,
			newName: document.getElementById("renameInput").value
		};

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				if (result.error) {
					alert(result.error);
				} else {
					browser.navigateTo("/?path=" + encodeURI(window.data.path) + "&file=" + encodeURI(result.newName));
				}
			}
		}

		request.send(JSON.stringify(data));

		this.closeRenameModal();
	},

	closeRenameModal: function() {
		this.hideModalBackground();
		document.getElementById('renameModal').style.display = 'none';
	},

	showDeleteModal: function() {
		this.showModalBackground();
		document.getElementById('deleteModal').style.display = 'block';
	},

	doDelete: function() {
		alert("Sorry, not yet implemented!");
		this.closeDeleteModal();
	},

	closeDeleteModal: function() {
		this.hideModalBackground();
		document.getElementById('deleteModal').style.display = 'none';
	},

	unspoil: function(which) {
		var el = document.getElementById("spoiler_" + which);
		if (el) {
			var spoiledClass = 'spoiled';
			if (el.className == spoiledClass) {
				el.className = 'notspoiled';
			} else {
				el.className = spoiledClass;
			}
		}
	}

}



// disable any hotkeys while browser is open, so that the user cannot
// accidentally refresh the page or something silly like that
window.onhelp = function() {
	// prevent F1 function key
	return false;
};

window.onkeydown = function(event) {

	// [Ctrl]+[S] to save - folder or entry
	if ((event.metaKey || event.ctrlKey) && event.keyCode == 83) {
		if (browser.editingMode) {
			if ((browser.folderEditingMode) &&
				(document.getElementById("save-folder-btn").style.backgroundColor == browser.COLOR_SAVE_RED)) {
				browser.saveFolder();
			} else {
				browser.saveEntry();
			}
			event.preventDefault();
			return false;
		}
		if (browser.folderEditingMode) {
			browser.saveFolder();
			event.preventDefault();
			return false;
		}
	}

	// [Ctrl]+[D] (to the right of [S] on German keyboards) for leave editing mode after saving,
	// or even entering edit mode (of the entry) if neither entry nor folder are in editing mode
	if ((event.metaKey || event.ctrlKey) && event.keyCode == 68) {
		browser.doToggleSomeEditingMode();
		event.preventDefault();
		return false;
	}

	// function keys in general
	if ((event.keyCode > 111) && (event.keyCode < 124)) {

		// [F1] to add „“
		if (event.keyCode == 111 + 1) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.insertText(fileContentTextarea, "„“", event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.insertText(folderTextarea, "„“", event);
				}
			}
		}

		// [F2] to add “”
		if (event.keyCode == 111 + 2) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.insertText(fileContentTextarea, "“”", event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.insertText(folderTextarea, "“”", event);
				}
			}
		}

		// [F3] to add ‚‘
		if (event.keyCode == 111 + 3) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.insertText(fileContentTextarea, "‚‘", event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.insertText(folderTextarea, "‚‘", event);
				}
			}
		}

		// [F4] to add ’ (as that is useful more often than ‘’)
		if (event.keyCode == 111 + 4) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.insertText(fileContentTextarea, "’", event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.insertText(folderTextarea, "’", event);
				}
			}
		}

		// [F6] to add a date-time-stamp
		if (event.keyCode == 111 + 6) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.addDateTimeStamp(fileContentTextarea, event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.addDateTimeStamp(folderTextarea, event);
				}
			}
		}

		// prevent function keys
		event.preventDefault();
		return false;
	}

	// [Tab] to indent or unindent selection
	if (event.keyCode == 9) {
		var fileContentTextarea = document.getElementById("fileContentTextarea");
		if (fileContentTextarea && browser.editingMode) {
			toolbox.utils.StrUtils.indentOrUnindent(fileContentTextarea, event);
		} else {
			var folderTextarea = document.getElementById("folderTextarea");
			if (folderTextarea && browser.folderEditingMode) {
				toolbox.utils.StrUtils.indentOrUnindent(folderTextarea, event);
			}
		}
		event.preventDefault();
		return false;
	}

	// [Esc] - close tile or comic view if open, or toggle off editing mode
	if (event.keyCode == 27) {

		var imageStrip = document.getElementById("imageStrip");
		var tileStripsContainer = document.getElementById("tileStripsContainer");
		if ((imageStrip.style.position == "fixed") || (tileStripsContainer.style.display == 'block')) {
			browser.closeView();
		} else {
			browser.doToggleSomeEditingMode();
		}

		// prevent escape
		event.preventDefault();
		return false;
	}

	// allow other keys
	return true;
};

window.addEventListener("resize", window.browser.onResize);

window.browser.onResize();

window.browser.loadFullFolderView();

window.browser.scrollIfNecessary();

window.browser.adjustOpenBrowserField();
