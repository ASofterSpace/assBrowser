window.browser = {

	// are we currently editing the entry?
	editingMode: false,

	// are we preventing the firing of entry change events to turn the save button red?
	preventEntryChangeFire: false,


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

	openUploadModal: function() {
		document.getElementById("modalBackground").style.display = "block";
		document.getElementById("uploadFileModal").style.display = "block";
	},

	closeUploadModal: function() {
		document.getElementById("modalBackground").style.display = "none";
		document.getElementById("uploadFileModal").style.display = "none";
	},

	openComicView: function() {
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
		request.open("GET", "getFolderView?quickView=false&path=" + encodeURI(window.data.path), true);
		request.setRequestHeader("Content-Type", "application/json");

		request.onreadystatechange = function() {
			if (request.readyState == 4 && request.status == 200) {
				var result = JSON.parse(request.response);
				// only update if the path didn't change in the meantime
				// (it shouldn't, currently, but maybe in the future...)
				if (window.data.path == result.path) {
					document.getElementById("folderContainer").innerHTML = result.folderContent;
				}
			}
		}

		request.send();
	},

	toggleEditEntry: function() {
		this.editingMode = !this.editingMode;
		if (this.editingMode) {
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
		} else {
			document.getElementById("edit-btn").innerText = "Edit";
			document.getElementById("save-btn").style.display = "none";
			document.getElementById("fileContentContainer").style.display = "block";
			document.getElementById("fileContentTextarea").style.display = "none";
			document.getElementById("fileContentContainer").innerHTML = "Loading View...";
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
						document.getElementById("fileContentTextarea").value = result.entry;
						window.setTimeout(function() {
							browser.preventEntryChangeFire = false;
						}, 100);
					} else {
						document.getElementById("fileContentContainer").innerHTML = result.entry;
					}
				}
			}
		}

		request.send();
	},

	entryChanged: function() {
		if (!browser.preventEntryChangeFire) {
			document.getElementById("save-btn").style.background = "#E02";
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
					document.getElementById("save-btn").style.background = "#0B0";
					window.setTimeout(function() {
						document.getElementById("save-btn").style.background =
							document.getElementById("edit-btn").style.background;
					}, 2500);
				}
			}
		}

		request.send(JSON.stringify(data));
	},

}



// disable any hotkeys while browser is open, so that the user cannot
// accidentally refresh the page or something silly like that
window.onhelp = function() {
	// prevent F1 function key
	return false;
};
window.onkeydown = function(event) {
	if ((event.metaKey || event.ctrlKey) && event.keyCode == 83) {
		if (browser.editingMode) {
			browser.saveEntry();
			// prevent [Ctrl]+[S]
			event.preventDefault();
			return false;
		}
	}
	if ((event.keyCode > 111) && (event.keyCode < 124)) {
		if (event.keyCode == 111 + 6) {
			// if [F6] is pressed, and the log entry textarea is visible...
			var fileContentTextarea = document.getElementById("fileContentTextarea");
			if (fileContentTextarea && browser.editingMode) {
				var start = fileContentTextarea.selectionStart;
				var end = fileContentTextarea.selectionEnd;
				// ... add a date-time-stamp!
				var datetimestamp = toolbox.utils.DateUtils.getCurrentDateTimeStamp();
				fileContentTextarea.value =
					fileContentTextarea.value.substring(0, start) +
					datetimestamp +
					fileContentTextarea.value.substring(end);
				fileContentTextarea.selectionStart = start + datetimestamp.length;
				fileContentTextarea.selectionEnd = start + datetimestamp.length;
			}
		}
		// prevent function keys
		event.preventDefault();
		return false;
	}
	if (event.keyCode == 27) {
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
