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
	COLOR_SAVE_RED: "rgb(238, 0, 34)",

	exportView: false,


	onResize: function() {

		var retry = false;

		var body = document.getElementById("body");
		if (body) {
			if (this.exportView) {
				body.style.height = 'unset';
			} else {
				body.style.height = window.innerHeight + "px";
			}
		} else {
			retry = true;
		}

		var mainContent = document.getElementById("mainContent");
		if (mainContent) {
			if (this.exportView) {
				mainContent.style.height = 'unset';
			} else {
				mainContent.style.height = (window.innerHeight - 31) + "px";
			}
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

	openScrollView: function() {
		this.closeMoreActions();

		var imageStrip = document.getElementById("imageStrip");
		var tileStripsContainer = document.getElementById("tileStripsContainer");
		var body = document.getElementsByTagName("body")[0];
		var closeScrollViewBtn = document.getElementById('closeScrollViewBtn');

		if (imageStrip && tileStripsContainer && body && closeScrollViewBtn) {

			imageStrip.style.position = "fixed";
			imageStrip.style.width = "100%";
			imageStrip.style.height = "100%";
			imageStrip.style.zIndex = 100;
			imageStrip.style.backgroundColor = "#FFFFFF";

			tileStripsContainer.style.display = 'none';

			body.style.padding = "0px";
			body.style.margin = "0px";

			closeScrollViewBtn.style.display = 'block';
			closeScrollViewBtn.innerText = "Close View";

			imageStrip.style.width = body.clientWidth + "px";
			imageStrip.style.height = body.clientHeight + "px";
		}
	},

	openComicView: function() {
		this.closeMoreActions();
		this.curComicViewPage = 1;

		var imageStrip = document.getElementById("imageStrip");
		var tileStripsContainer = document.getElementById("tileStripsContainer");
		var body = document.getElementsByTagName("body")[0];
		var closeScrollViewBtn = document.getElementById('closeScrollViewBtn');
		var leftComicViewBtn = document.getElementById('leftComicViewBtn');
		var rightComicViewBtn = document.getElementById('rightComicViewBtn');

		if (imageStrip && tileStripsContainer && body && closeScrollViewBtn) {

			imageStrip.style.position = "fixed";
			imageStrip.style.width = "100%";
			imageStrip.style.height = "100%";
			imageStrip.style.zIndex = 200;
			imageStrip.style.backgroundColor = "#000000";
			imageStrip.style.overflowY = "hidden";

			tileStripsContainer.style.display = 'none';

			body.style.padding = "0px";
			body.style.margin = "0px";

			closeScrollViewBtn.style.display = 'block';
			closeScrollViewBtn.innerText = "X";

			leftComicViewBtn.style.display = 'block';
			rightComicViewBtn.style.display = 'block';

			imageStrip.style.width = body.clientWidth + "px";
			imageStrip.style.height = body.clientHeight + "px";

			window.setTimeout(function() {
				var picWidth = body.clientHeight;
				var picHeight = body.clientWidth;

				var i = 1;
				while (true) {
					var pic = document.getElementById("pic_" + i);
					if (!pic) {
						break;
					}
					pic.style.position = "absolute";
					pic.style.transform = "rotateZ(90deg)";
					pic.style.transformOrigin = "center";
					pic.style.top = "0px";
					pic.style.left = "200pt";
					pic.style.right = "0px";
					pic.style.bottom = "0px";
					pic.style.maxWidth = picWidth + "px";
					pic.style.maxHeight = picHeight + "px";
					pic.style.marginLeft = "auto";
					pic.style.marginRight = "auto";
					if (i > 1) {
						pic.style.display = "none";
					} else {
						pic.style.display = "inline";
					}
					pic.childNodes[0].style.width = "1000%";
					pic.childNodes[0].style.maxWidth = picWidth + "px";
					pic.childNodes[0].style.maxHeight = picHeight + "px";
					i = i + 1;
				}
			}, 500);
		}
	},

	comicViewPrevPage: function() {
		var newPage = document.getElementById("pic_" + (this.curComicViewPage - 1));
		if (newPage) {
			document.getElementById("pic_" + this.curComicViewPage).style.display = "none";
			newPage.style.display = "inline";
			this.curComicViewPage = this.curComicViewPage - 1;
		}
	},

	comicViewNextPage: function() {
		var newPage = document.getElementById("pic_" + (this.curComicViewPage + 1));
		if (newPage) {
			document.getElementById("pic_" + this.curComicViewPage).style.display = "none";
			newPage.style.display = "inline";
			this.curComicViewPage = this.curComicViewPage + 1;
		}
	},

	openTileView: function() {
		this.closeMoreActions();

		var imageStrip = document.getElementById("imageStrip");
		var tileStripsContainer = document.getElementById("tileStripsContainer");
		var body = document.getElementsByTagName("body")[0];

		if (imageStrip && tileStripsContainer && body) {
			imageStrip.style.display = 'none';

			tileStripsContainer.style.display = 'block';

			tileStripsContainer.style.position = "fixed";
			tileStripsContainer.style.width = "100%";
			tileStripsContainer.style.height = "100%";
			tileStripsContainer.style.zIndex = 100;
			tileStripsContainer.style.backgroundColor = "rgb(64,0,128)";

			body.style.padding = "0px";
			body.style.margin = "0px";

			tileStripsContainer.style.width = body.clientWidth + "px";
			tileStripsContainer.style.height = body.clientHeight + "px";
		}
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
		imageStrip.style.overflowY = "scroll";

		var body = document.getElementsByTagName("body")[0];
		body.style.padding = "0pt 60pt 0pt 4pt";
		body.style.margin = "8px";

		var closeScrollViewBtn = document.getElementById('closeScrollViewBtn');
		closeScrollViewBtn.style.display = 'none';

		var leftComicViewBtn = document.getElementById('leftComicViewBtn');
		leftComicViewBtn.style.display = 'none';

		var rightComicViewBtn = document.getElementById('rightComicViewBtn');
		rightComicViewBtn.style.display = 'none';

		var i = 1;
		while (true) {
			var pic = document.getElementById("pic_" + i);
			if (!pic) {
				break;
			}
			pic.style.position = "unset";
			pic.style.transform = "unset";
			pic.style.transformOrigin = "unset";
			pic.style.maxWidth = "unset";
			pic.style.maxHeight = "unset";
			pic.style.marginLeft = "unset";
			pic.style.marginRight = "unset";
			pic.style.display = "inline";
			pic.childNodes[0].style.width = "100%";
			pic.childNodes[0].style.maxWidth = "unset";
			pic.childNodes[0].style.maxHeight = "unset";
			i = i + 1;
		}
	},

	loadFullFolderView: function() {

		var request = new XMLHttpRequest();
		request.open("GET", "getFolder?editingMode=false&quickView=false" +
			"&path=" + encodeURIComponent(window.data.path) +
			"&file=" + encodeURIComponent(window.data.file), true);
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
			document.getElementById("fileContentTextarea").value = "Loading Entry for Edit...";
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
			document.getElementById("fileContentContainer").innerHTML = "Loading Entry View...";
			var disableEditBtns = document.getElementsByClassName("editBtnEnabled");
			for (var i = disableEditBtns.length - 1; i >= 0; i--) {
				disableEditBtns[i].className = 'button editBtnDisabled';
			}
		}

		var request = new XMLHttpRequest();
		request.open("GET", "getEntry?editingMode=" + this.editingMode +
			"&path=" + encodeURIComponent(window.data.path) +
			"&file=" + encodeURIComponent(window.data.file), true);
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
				} else {
					console.log("Canceled entry loading due to mismatch:\n" +
						"Result path is: " + result.path + "\n" +
						"Current path is: " + window.data.path + "\n" +
						"Result file is: " + result.file + "\n" +
						"Current file is: " + window.data.file);
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

			content: savedContent
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
			document.getElementById("folderTextarea").value = "Loading Directory for Edit...";
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
			document.getElementById("folderContainer").innerHTML = "Loading Directory View...";
		}

		var request = new XMLHttpRequest();
		request.open("GET", "getFolder?editingMode=" + this.folderEditingMode +
			"&quickView=false&path=" + encodeURIComponent(window.data.path) +
			"&file=" + encodeURIComponent(window.data.file), true);
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
				} else {
					console.log("Canceled directory loading due to mismatch:\n" +
						"Result path is: " + result.path + "\n" +
						"Current path is: " + window.data.path);
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

			content: savedContent
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

	// replaces legacy ISO nonsense on loading with actual UTF-8 characters - like in ServerRequestHandler.java
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
			newUrl += "&scroll=" + (el.scrollTop / el.scrollTopMax);
		}

		var selEditLinks = document.getElementById('sel-edit-links');
		if (selEditLinks.className == 'selected') {
			newUrl += "&editLinks=true";
		}

		var selFollowLinks = document.getElementById('sel-follow-links');
		if (selFollowLinks.className == 'nonselected') {
			newUrl += "&followLinks=false";
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
				container.style.left = (13 + textActionsBtn.offsetLeft + textActionsBtn.parentElement.offsetLeft +
					(textActionsBtn.offsetWidth / 2) -
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
				container.style.left = (13 + moreActionsBtn.offsetLeft + moreActionsBtn.parentElement.offsetLeft +
					(moreActionsBtn.offsetWidth / 2) -
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

	extractCyberMetaInfo: function() {
		var content = this.getCurrentEntryText();

		var topicName = window.data.path;
		topicName = topicName.split("/").join("\\");
		topicName = topicName.split("\\");
		topicName = topicName[topicName.length - 1];

		var result = "Topic: " + topicName + "\n";

		content = content.split("\n");
		var entryName = content[0];

		result += "Entry: **" + entryName + "**\n";

		var crossLinks = "";
		for (var i = 0; i < content.length - 1; i++) {
			const SEE_ALSO_FOR = "see also for ";
			if (content[i].startsWith(SEE_ALSO_FOR) && content[i].endsWith(":") &&
				content[i+1].startsWith("%[") && content[i+1].endsWith("]")) {

				var cur = content[i+1]
				cur = cur.substring(2);
				cur = cur.substring(0, cur.length - 1);
				cur = cur.split("/").join("\\");;
				cur = cur.split("\\");
				var curTopic = cur[cur.length - 2];
				var curEntry = cur[cur.length - 1];
				crossLinks += "  " + cur[cur.length - 2];
				if (curEntry.length > 0) {
					crossLinks += " > " + cur[cur.length - 1];
				}
				crossLinks += "\n";
			}
		}
		if (crossLinks.length > 0) {
			result += "Cross-linked to:\n" + crossLinks;
		}

		var fileName = window.data.file;
		if (fileName.endsWith(".stpu")) {
			fileName = fileName.substring(0, fileName.length - 5);
		}

		result += "Location: " + window.data.path.split("/").join("\\") + "\\" + fileName + "\n";
		result += "Cyber System Version: " + window.data.version;

		this.copyToClipboard(result);

		this.closeMoreActions();
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

	copyLinkToThis: function() {

		var linkStr = null;

		if (window.data.file == null) {

			linkStr = "http://" +
				window.location.hostname +
				":" +
				window.location.port +
				"?path=" +
				encodeURIComponent(window.data.path).split("%2F").join("/") +
				"\n" +
				"%[" + window.data.path.split("/").join("\\") + "]";

		} else {

			var fileTitle = window.data.file;
			if (fileTitle.endsWith(".stpu")) {
				fileTitle = fileTitle.substring(0, fileTitle.length - 5);
			}

			linkStr = "http://" +
				window.location.hostname +
				":" +
				window.location.port +
				"?path=" +
				encodeURIComponent(window.data.path).split("%2F").join("/") +
				"&file=" +
				encodeURIComponent(window.data.file).split("%2F").join("/") +
				"\n" +
				"%[" + window.data.path.split("/").join("\\") + "\\" + fileTitle + "]";
		}

		this.copyToClipboard(linkStr);

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
					browser.navigateTo("/?path=" + encodeURIComponent(window.data.path) +
						"&file=" + encodeURIComponent(result.newName));
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

	showSearchReplaceModal: function() {
		if (this.editingMode) {
			this.showModalBackground();
			document.getElementById('searchReplaceModal').style.display = 'block';
		}
	},

	doSearchReplace: function() {
		if (this.editingMode) {
			var searchFor = document.getElementById("searchReplaceSearchInput").value;
			var replaceWith = document.getElementById("searchReplaceReplaceInput").value;
			var textarea = document.getElementById("fileContentTextarea");
			if (textarea) {
				textarea.value = textarea.value.split(searchFor).join(replaceWith);
				this.entryChanged();
			}
		}
		this.closeSearchReplaceModal();
	},

	closeSearchReplaceModal: function() {
		this.hideModalBackground();
		document.getElementById('searchReplaceModal').style.display = 'none';
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
	},

	findCrossReferencesSelectedText: function() {
		var fileContentTextarea = document.getElementById('fileContentTextarea');
		if (fileContentTextarea) {
			var text = fileContentTextarea.value.substring(fileContentTextarea.selectionStart, fileContentTextarea.selectionEnd);
			this.findCrossReferences(text);
		}
	},

	findCrossReferencesEntry: function() {
		var text = window.data.path.split("/").join("\\") + "\\" + window.data.file;
		if (text.indexOf(".stpu") == text.length - 5) {
			text = text.substring(0, text.length - 5);
		}
		this.findCrossReferences(text);
	},

	findCrossReferences: function(textToFind) {
		if (textToFind != '') {
			this.showCrossReferenceModal();
			var crossRefResultArea = document.getElementById("crossRefResultArea");
			if (crossRefResultArea) {
				crossRefResultArea.innerHTML = "Searching for cross-references...";
			}

			var request = new XMLHttpRequest();
			request.open("POST", "findCrossReferences", true);
			request.setRequestHeader("Content-Type", "application/json");

			var savedContent = document.getElementById("fileContentTextarea").value;

			var data = {
				path: window.data.path,
				text: textToFind,
			};

			request.onreadystatechange = function() {
				if (request.readyState == 4 && request.status == 200) {
					var result = JSON.parse(request.response);
					if (crossRefResultArea) {
						crossRefResultArea.innerHTML = result.text;
					}
				}
			}

			request.send(JSON.stringify(data));
		} else {
			alert("No text selected!");
		}
	},

	showCrossReferenceModal: function() {
		this.showModalBackground();
		document.getElementById('crossReferenceModal').style.display = 'block';
	},

	closeCrossReferenceModal: function() {
		this.hideModalBackground();
		document.getElementById('crossReferenceModal').style.display = 'none';
	},

	clickEditLinks: function() {
		var selEditLinks = document.getElementById('sel-edit-links');
		var selFollowLinks = document.getElementById('sel-follow-links');
		if (selEditLinks.className == 'nonselected') {
			selEditLinks.className = 'selected';
			selEditLinks.innerText = "✔ Edit Links";
			selFollowLinks.className = 'nonselected';
			selFollowLinks.innerText = "✘ Follow Links";
		} else {
			selEditLinks.className = 'nonselected';
			selEditLinks.innerText = "✘ Edit Links";
		}
	},

	clickFollowLinks: function() {
		var selEditLinks = document.getElementById('sel-edit-links');
		var selFollowLinks = document.getElementById('sel-follow-links');
		if (selFollowLinks.className == 'nonselected') {
			selFollowLinks.className = 'selected';
			selFollowLinks.innerText = "✔ Follow Links";
			selEditLinks.className = 'nonselected';
			selEditLinks.innerText = "✘ Edit Links";
		} else {
			selFollowLinks.className = 'nonselected';
			selFollowLinks.innerText = "✘ Follow Links";
		}
	},
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

		// [F4] to add ‘’
		if (event.keyCode == 111 + 4) {
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				toolbox.utils.StrUtils.insertText(fileContentTextarea, "‘’", event);
			} else {
				var folderTextarea = document.getElementById("folderTextarea");
				if (folderTextarea && browser.folderEditingMode) {
					toolbox.utils.StrUtils.insertText(folderTextarea, "‘’", event);
				}
			}
		}

		// [F5] to add ’ (as that is useful more often than ‘’)
		if (event.keyCode == 111 + 5) {
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
