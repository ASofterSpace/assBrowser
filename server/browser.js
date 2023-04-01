window.browser = {

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

	openFolderInOS: function() {

		var request = new XMLHttpRequest();
		request.open("POST", "openFolderInOS", true);
		request.setRequestHeader("Content-Type", "application/json");

		var data = {
			path: window.data.path,
		};

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

}



window.addEventListener("resize", window.browser.onResize);


window.browser.onResize();

window.browser.loadFullFolderView();
