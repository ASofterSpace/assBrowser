/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.gui;

import com.asofterspace.assBrowser.AssBrowser;
import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.console.ConsoleResult;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.assBrowser.paths.PathCtrl;
import com.asofterspace.toolbox.coders.UrlEncoder;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.BarListener;
import com.asofterspace.toolbox.gui.BarMenuItemForMainMenu;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.images.ColorRGBA;
import com.asofterspace.toolbox.io.Directory;
import com.asofterspace.toolbox.io.File;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.io.TextFile;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.awt.AWTKeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.Clip;
import javax.sound.sampled.Line;
import javax.swing.AbstractAction;
import javax.swing.border.LineBorder;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class GUI extends MainWindow {

	private Database database;

	private ConsoleCtrl consoleCtrl;

	private JTextField consoleField1;
	private JTextField consoleField2;
	private JTextField consoleField3;
	private BarMenuItemForMainMenu volumeItem;
	private JLabel boostAudioLabel;
	private JLabel counterLabel;
	private JLabel quoteLabel1;
	private JLabel quoteLabel2;
	private JLabel quoteLabel3;
	private JLabel quoteLabel4;
	private JLabel quoteLabel5;
	private JLabel quoteLabel6;
	private JLabel newlineLabel;
	private JLabel heartLabel;
	private JLabel batteryLabel;
	private JLabel cpuLabel;
	private JLabel ramLabel;
	private JLabel swapLabel;
	private JLabel clockLabel;
	private BarMenuItemForMainMenu brightnessItem;

	private boolean timerRunning = false;

	final static ColorRGBA bgColor = new ColorRGBA(0, 0, 0);
	final static Color bgColorCol = bgColor.toColor();
	private final static ColorRGBA borderColor = new ColorRGBA(96, 0, 192);
	private final static ColorRGBA fgColorMain = new ColorRGBA(255, 255, 255);
	final static ColorRGBA fgColor = new ColorRGBA(167, 62, 249);
	final static Color fgColorCol = fgColor.toColor();
	private final static ColorRGBA highlightColor = new ColorRGBA(240, 150, 255);
	private final static ColorRGBA errorColor = new ColorRGBA(255, 0, 64);
	private final static Color errorColorCol = errorColor.toColor();

	private EmojiSelectorGUI emojiSelectorGUI = null;

	private final static int left = 0;
	private final static int top = 0;
	private final static int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private final static int INIT_HEIGHT = 22;
	private static int height = INIT_HEIGHT;

	private int batteryDisplayCounter = 0;
	private int cpuDisplayCounter = 0;
	private int ramDisplayCounter = 0;
	private int swapDisplayCounter = 0;
	private int cpuAboveThresholdCounter = 0;
	// private long lastVolumeTime = 0;
	private String audioPathGet;
	private String audioPathSet;
	private List<String> topCmdAndArgs;

	private TextFile brightnessFile;
	private Integer brightnessMax;

	// above this threshold of battery percent, do not flash battery label when there are problems (default: 100)
	private int batteryFlashingErrorThreshold = 100;

	Font sharedFont = null;

	private MouseAdapter mouseListenerToMaximize = new MouseAdapter() {
		@Override
		public void mouseEntered(MouseEvent e) {
			if (height < INIT_HEIGHT) {
				maximize();
			}
		}
	};


	public GUI(Database database, ConsoleCtrl consoleCtrl) {
		this.database = database;
		this.consoleCtrl = consoleCtrl;
		this.audioPathGet = database.getAudioPathGet();
		this.audioPathSet = database.getAudioPathSet();
		this.batteryFlashingErrorThreshold = database.getBatteryFlashingErrorThreshold();

		this.topCmdAndArgs = new ArrayList<>();
		this.topCmdAndArgs.add("top");
		this.topCmdAndArgs.add("-b");

		// enable anti-aliasing for swing
		System.setProperty("swing.aatext", "true");
		// enable anti-aliasing for awt
		System.setProperty("awt.useSystemAAFontSettings", "on");
	}

	@Override
	public void run() {

		super.create();

		// remove title bar
		mainFrame.setUndecorated(true);

		refreshTitleBar();

		createMainPanel(mainFrame);

		// do not call super.show, as we are doing things a little bit
		// differently around here (including restoring from previous
		// position...)
		// super.show();

		GUI outerThis = this;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// Request to always be on top as this replaces the main OS shell
				mainFrame.setAlwaysOnTop(true);

				// Actually display the whole jazz
				mainFrame.setVisible(true);

				resetGuiLocation();

				startTimerThread();

				emojiSelectorGUI = new EmojiSelectorGUI(outerThis);
			}
		});
	}

	private JPanel createMainPanel(JFrame parent) {

		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(bgColorCol);
		mainPanel.setPreferredSize(new Dimension(800, 500));
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);
		mainPanel.addMouseListener(mouseListenerToMaximize);

		int num = 0;

		consoleField1 = addConsoleField(mainPanel, num++);
		consoleField2 = addConsoleField(mainPanel, num++);
		consoleField3 = addConsoleField(mainPanel, num++);

		if (database.getSetAudioVolumetoSilenceAtStartup()) {
			if ("amixer".equals(this.audioPathSet)) {
				IoUtils.executeAsync(this.audioPathSet + " -q sset Master 0%");
			} else if ("pactl".equals(this.audioPathSet)) {
				IoUtils.executeAsync(this.audioPathSet + " -- set-sink-volume @DEFAULT_SINK@ 0%");
				IoUtils.executeAsync(this.audioPathSet + " -- set-sink-mute @DEFAULT_SINK@ 1");
			} else {
				IoUtils.executeAsync(this.audioPathSet + " setsysvolume 0");
				IoUtils.executeAsync(this.audioPathSet + " mutesysvolume 0");
			}
		}

		int DEFAULT_MAX_VOLUME = 100;
		volumeItem = new BarMenuItemForMainMenu();
		volumeItem.setBackground(bgColorCol);
		volumeItem.setForeground(fgColor.toColor());
		volumeItem.setBarPosition(null, false);
		volumeItem.setMaximum(DEFAULT_MAX_VOLUME);
		volumeItem.setSendUpdateOnMousePress(true);
		volumeItem.addBarListener(new BarListener() {
			@Override
			public void onBarMove(Integer position) {
				adjustVolume(position);
				hideEmojiSelector();
			}

			@Override
			public void onBarDisplay(Integer position) {
				/*
				long curTime = System.currentTimeMillis();
				// send updates every 500 ms on draw
				if (curTime - lastVolumeTime > 500) {
					adjustVolume(position);
				}
				*/
			}
		});
		mainPanel.add(volumeItem, new Arrangement(num++, 0, 0.0, 1.0));

		boostAudioLabel = createLabel("+ ");
		mainPanel.add(boostAudioLabel, new Arrangement(num++, 0, 0.0, 1.0));

		boostAudioLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				clickHighlight(boostAudioLabel);
				boostAudioLabel.setText("! ");

				if (SwingUtilities.isRightMouseButton(e)) {
					Integer barPos = volumeItem.getBarPosition();
					if (barPos != null) {
						if (barPos > DEFAULT_MAX_VOLUME) {
							boolean notifyListeners = true;
							volumeItem.setBarPosition(DEFAULT_MAX_VOLUME, notifyListeners);
						}
					}
					volumeItem.setMaximum(DEFAULT_MAX_VOLUME);
					boostAudioLabel.setText("+ ");
				} else {
					volumeItem.setMaximum(volumeItem.getMaximum() * 2);
					boostAudioLabel.setText("++ ");
				}

				hideEmojiSelector();
			}
		});

		counterLabel = createLabel("0 ");
		mainPanel.add(counterLabel, new Arrangement(num++, 0, 0.0, 1.0));

		counterLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				clickHighlight(counterLabel);
				int num = StrUtils.strToInt(counterLabel.getText().trim()) + 1;
				if (SwingUtilities.isRightMouseButton(e)) {
					num = 0;
				}
				counterLabel.setText(num + " ");

				hideEmojiSelector();
			}
		});

		quoteLabel1 = createLabel("„");
		mainPanel.add(quoteLabel1, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("„“");
				clickHighlight(quoteLabel1);
				clickHighlight(quoteLabel2);

				hideEmojiSelector();
			}
		});

		quoteLabel2 = createLabel("“");
		mainPanel.add(quoteLabel2, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("„“”");
				clickHighlight(quoteLabel1);
				clickHighlight(quoteLabel2);
				clickHighlight(quoteLabel3);

				hideEmojiSelector();
			}
		});

		quoteLabel3 = createLabel("” ");
		mainPanel.add(quoteLabel3, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel3.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("“”");
				clickHighlight(quoteLabel2);
				clickHighlight(quoteLabel3);

				hideEmojiSelector();
			}
		});

		quoteLabel4 = createLabel("‚");
		mainPanel.add(quoteLabel4, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel4.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("‚‘");
				clickHighlight(quoteLabel4);
				clickHighlight(quoteLabel5);

				hideEmojiSelector();
			}
		});

		quoteLabel5 = createLabel("‘");
		mainPanel.add(quoteLabel5, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel5.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("‚‘’");
				clickHighlight(quoteLabel4);
				clickHighlight(quoteLabel5);
				clickHighlight(quoteLabel6);

				hideEmojiSelector();
			}
		});

		quoteLabel6 = createLabel("’ ");
		mainPanel.add(quoteLabel6, new Arrangement(num++, 0, 0.0, 1.0));

		quoteLabel6.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("‘’");
				clickHighlight(quoteLabel5);
				clickHighlight(quoteLabel6);

				hideEmojiSelector();
			}
		});

		newlineLabel = createLabel("\\n ");
		mainPanel.add(newlineLabel, new Arrangement(num++, 0, 0.0, 1.0));

		newlineLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard("\n");
				clickHighlight(newlineLabel);

				hideEmojiSelector();
			}
		});

		heartLabel = createLabel(EmojiSelectorGUI.PURPLE_HEART + " ");
		mainPanel.add(heartLabel, new Arrangement(num++, 0, 0.0, 1.0));

		heartLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard(EmojiSelectorGUI.PURPLE_HEART);
				clickHighlight(heartLabel);

				if (emojiSelectorGUI != null) {
					emojiSelectorGUI.toggle();
				}
			}
		});

		// set a text that is a useful default for what is usually the max size of this label
		cpuLabel = createLabel("C: 00% ");
		cpuLabel.setOpaque(true);
		cpuLabel.setForeground(errorColor.toColor());
		// CPU percentage jumps around a lot during runtime, making the whole GUI jump around a lot,
		// so fix this one in size - and just this one (as the others change but less often)
		Dimension dim = cpuLabel.getPreferredSize();
		cpuLabel.setPreferredSize(new Dimension(dim.width, dim.height));
		cpuLabel.setText("C: ? ");
		mainPanel.add(cpuLabel, new Arrangement(num++, 0, 0.0, 1.0));

		cpuLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				checkCpuAndRamStatus();
				resetGuiLocation();
				clickHighlight(cpuLabel);
				hideEmojiSelector();
			}
		});

		ramLabel = createLabel("M: ? ");
		ramLabel.setOpaque(true);
		ramLabel.setForeground(errorColor.toColor());
		mainPanel.add(ramLabel, new Arrangement(num++, 0, 0.0, 1.0));

		ramLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				checkCpuAndRamStatus();
				resetGuiLocation();
				clickHighlight(ramLabel);
				hideEmojiSelector();
			}
		});

		swapLabel = createLabel("S: ? ");
		swapLabel.setOpaque(true);
		swapLabel.setForeground(errorColor.toColor());
		mainPanel.add(swapLabel, new Arrangement(num++, 0, 0.0, 1.0));

		swapLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				checkCpuAndRamStatus();
				resetGuiLocation();
				clickHighlight(swapLabel);
				hideEmojiSelector();
			}
		});

		if (database.getBatteryStateScriptPath() != null) {
			batteryLabel = createLabel("B: UNINITIALIZED ");
			batteryLabel.setOpaque(true);
			batteryLabel.setForeground(errorColor.toColor());
			mainPanel.add(batteryLabel, new Arrangement(num++, 0, 0.0, 1.0));

			batteryLabel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					checkBatteryStatus();
					resetGuiLocation();
					clickHighlight(batteryLabel);
					hideEmojiSelector();
				}
			});
		}

		clockLabel = createLabel("00:00 ");
		mainPanel.add(clockLabel, new Arrangement(num++, 0, 0.0, 1.0));

		clockLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				clickHighlight(clockLabel);
				GuiUtils.copyToClipboard(DateUtils.getCurrentDateTimeStamp());
				resetGuiLocation();

				hideEmojiSelector();
			}
		});

		TextFile brightnessMaxFile = new TextFile("/sys/class/backlight/intel_backlight/max_brightness");
		brightnessMax = StrUtils.strToInt(brightnessMaxFile.getContent());
		if (brightnessMax != null) {
			brightnessFile = new TextFile("/sys/class/backlight/intel_backlight/brightness");
			Integer brightnessCur = StrUtils.strToInt(brightnessFile.getContent());
			if (brightnessCur == null) {
				brightnessCur = brightnessMax;
			}
			brightnessItem = new BarMenuItemForMainMenu();
			brightnessItem.setBackground(bgColorCol);
			brightnessItem.setForeground(fgColor.toColor());
			brightnessItem.setBarPosition((100 * brightnessCur) / brightnessMax, false);
			brightnessItem.setMaximum(100);
			brightnessItem.setSendUpdateOnMousePress(true);
			brightnessItem.addBarListener(new BarListener() {
				@Override
				public void onBarMove(Integer position) {
					adjustBrightness(position);
					hideEmojiSelector();
				}

				@Override
				public void onBarDisplay(Integer position) {
				}
			});
			mainPanel.add(brightnessItem, new Arrangement(num++, 0, 0.0, 1.0));
		}


		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private JTextField addConsoleField(JPanel mainPanel, int num) {
		JTextField consoleField = new JTextField();
		consoleField.setBackground(bgColorCol);
		consoleField.setForeground(fgColorMain.toColor());
		consoleField.setCaretColor(fgColor.toColor());
		consoleField.setBorder(new LineBorder(borderColor.toColor()));
		mainPanel.add(consoleField, new Arrangement(num, 0, 1.0, 1.0));

		// prevent [TAB] from causing focus traversal
		Set<AWTKeyStroke> noKeys = new HashSet<>();
		consoleField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, noKeys);

		consoleField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
			}

			public void keyTyped(KeyEvent event) {
			}

			public void keyPressed(KeyEvent event) {
				hideEmojiSelector();

				// [F1] to add „“
				if (event.getKeyCode() == KeyEvent.VK_F1) {
					insertTextForFunctionKey("„“", consoleField);
					event.consume();
					return;
				}

				// [F2] to add “”
				if (event.getKeyCode() == KeyEvent.VK_F2) {
					insertTextForFunctionKey("“”", consoleField);
					event.consume();
					return;
				}

				// [F3] to add ‚‘
				if (event.getKeyCode() == KeyEvent.VK_F3) {
					insertTextForFunctionKey("‚‘", consoleField);
					event.consume();
					return;
				}

				// [F4] to add ’ (as that is useful more often than ‘’)
				if (event.getKeyCode() == KeyEvent.VK_F4) {
					insertTextForFunctionKey("’", consoleField);
					event.consume();
					return;
				}

				// [F6] to add a date-time-stamp
				if (event.getKeyCode() == KeyEvent.VK_F6) {
					insertTextForFunctionKey(DateUtils.getCurrentDateTimeStamp(), consoleField);
					event.consume();
					return;
				}

				// [TAB] for tab completion
				if (event.getKeyCode() == KeyEvent.VK_TAB) {
					event.consume();
					String txt = consoleField.getText();
					// tab complete syntax reminder
					String newTxt = consoleCtrl.applyCommandlineSyntaxReminder(txt);
					if (newTxt == null) {
						// tab complete files / folders
						if (txt.contains(" ~")) {
							txt = StrUtils.replaceAll(txt, " ~", " " + database.getHomeDir().getCanonicalDirname());
						}
						int index = txt.indexOf(" /");
						if (index >= 0) {
							String beforepath = txt.substring(0, index+1);
							String dirpath = txt.substring(index+1);
							index = dirpath.lastIndexOf("/");
							String fileLocalStart = dirpath.substring(index+1);
							dirpath = dirpath.substring(0, index+1);
							Directory parentDir = new Directory(dirpath);
							boolean recursively = false;
							List<Directory> dirs = parentDir.getAllDirectories(recursively);
							List<File> files = parentDir.getAllFiles(recursively);
							boolean foundOne = false;
							String fileLocal = "";
							for (Directory dir : dirs) {
								if (dir.getLocalDirname().startsWith(fileLocalStart)) {
									if (foundOne) {
										return;
									}
									foundOne = true;
									fileLocal = dir.getLocalDirname() + "/";
								}
							}
							for (File file : files) {
								if (file.getLocalFilename().startsWith(fileLocalStart)) {
									if (foundOne) {
										return;
									}
									foundOne = true;
									fileLocal = file.getLocalFilename();
								}
							}

							newTxt = beforepath + dirpath + fileLocal;
						}
					}
					if (newTxt != null) {
						consoleField.setText(newTxt);
						consoleField.setSelectionStart(newTxt.length());
						consoleField.setSelectionEnd(newTxt.length());
					}
					return;
				}

			}
		});

		consoleField.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {

				hideEmojiSelector();

				String command = consoleField.getText();
				String previousPath = "";
				previousPath = PathCtrl.ensurePathIsSafe(previousPath);

				boolean fromOutside = true;
				ConsoleResult consoleResult = consoleCtrl.interpretCommand(command, previousPath, fromOutside);

				String newCommand = consoleResult.getCommand();
				consoleField.setText(newCommand);
				String newPath = consoleResult.getPath();
				newPath = PathCtrl.ensurePathIsSafe(newPath);
				if (!newPath.equals(previousPath)) {
					String browserPath = database.getBrowserPath();
					IoUtils.executeAsync(browserPath + " http://localhost:3013/?link=" + UrlEncoder.encode(newPath));
					/*
					List<String> args = StrUtils.split(browserPath, " ");
					List<String> actualArgs = new ArrayList<>();
					for (int i = 1; i < args.size() - 1; i++) {
						actualArgs.add(args.get(i));
					}
					actualArgs.add(args.get(args.size() - 1) + " http://localhost:3013/?link=" + UrlEncoder.encode(newPath));
					try {
						IoUtils.executeAsync(args.get(0), actualArgs);
					} catch (IOException curErr) {
						System.err.println("There was an I/O Exception while executing an external command asynchronously: " + curErr);
					}
					*/
				}
			}
		});

		consoleField.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				hideEmojiSelector();
			}
		});

		return consoleField;
	}

	private void adjustVolume(Integer position) {

		// this.lastVolumeTime = System.currentTimeMillis();

		if (position == null) {
			position = 0;
		}

		if ("pactl".equals(this.audioPathSet)) {
			IoUtils.executeAsync(this.audioPathSet + " set-sink-volume @DEFAULT_SINK@ " + position + "%");
			if (position > 0) {
				IoUtils.executeAsync(this.audioPathSet + " set-sink-mute @DEFAULT_SINK@ 0");
			} else {
				IoUtils.executeAsync(this.audioPathSet + " set-sink-mute @DEFAULT_SINK@ 1");
			}
		} else {
			if ("amixer".equals(this.audioPathSet)) {
				if (position > 100) {
					IoUtils.execute(this.audioPathSet + " -q sset Master 100%");
					IoUtils.executeAsync("pactl set-sink-volume @DEFAULT_SINK@ " + position + "%");
				} else {
					IoUtils.executeAsync(this.audioPathSet + " -q sset Master " + position + "%");
				}
			} else {
				int volPerc = position * 655;
				if (position <= 100) {
					volPerc = Math.min(65535, position * 656);
				}
				IoUtils.executeAsync(this.audioPathSet + " setsysvolume " + volPerc);
			}
		}
	}

	private void adjustBrightness(Integer position) {
		if (position == null) {
			position = 0;
		}

		// do not set below a reasonable minimum as otherwise the screen will not be visible at all ^^'
		if (position < database.getBacklightMinimum()) {
			position = database.getBacklightMinimum();
		}

		brightnessFile.saveContent("" + ((position * brightnessMax) / 100));
	}

	private void refreshTitleBar() {
		mainFrame.setTitle(AssBrowser.PROGRAM_TITLE);
	}

	public void close() {
		timerRunning = false;
	}

	private void startTimerThread() {

		timerRunning = true;

		Thread timerThread = new Thread() {

			public void run() {

				while (timerRunning) {
					try {
						// adjust clock
						clockLabel.setText(DateUtils.serializeTimeShort(DateUtils.now()) + " ");

						// ensure the GUI is located where it should be
						resetGuiLocation();

						// check battery status
						checkBatteryStatus();

						// check cpu and ram status
						checkCpuAndRamStatus();

						// check audio status
						checkAudioStatus();

						// update every 3 seconds
						Thread.sleep(3 * 1000);

					} catch (InterruptedException e) {
						// just keep sleeping...
					}
				}
			}
		};
		timerThread.start();
	}

	private void clickHighlight(JLabel labelThatIsHighlit) {
		int CLICK_WAIT_TIME = 800;
		labelThatIsHighlit.setForeground(highlightColor.toColor());
		Thread miniThread = new Thread() {
			public void run() {
				try {
					Thread.sleep(CLICK_WAIT_TIME);
				} catch (InterruptedException e) {
				}
				labelThatIsHighlit.setForeground(fgColor.toColor());
			}
		};
		miniThread.start();
	}

	private JLabel createLabel(String text) {
		JLabel result = new JLabel(text);
		result.setBackground(bgColorCol);
		result.setForeground(fgColorCol);
		if (sharedFont == null) {
			sharedFont = result.getFont();
			sharedFont = new Font(sharedFont.getName(), sharedFont.getStyle(), (sharedFont.getSize() * 13) / 10);
		}
		result.setFont(sharedFont);
		return result;
	}

	private void resetGuiLocation() {

		mainFrame.setSize(width, height);

		mainFrame.setPreferredSize(new Dimension(width, height));

		mainFrame.setLocation(new Point(left, top));
	}

	private void checkBatteryStatus() {

		String batScriptPath = database.getBatteryStateScriptPath();
		if ((batScriptPath == null) || (batteryLabel == null)) {
			return;
		}

		try {

			if (batScriptPath.startsWith("/sys/class/")) {
				// Linux approach: read numbers directly from battery files

				String acOnline = "0";

				List<String> batScriptPaths = StrUtils.split(batScriptPath, " ");

				/*
				TextFile txtFile = new TextFile(batScriptPaths.get(0) + "charge_full");
				chargeFull = StrUtils.strToInt(txtFile.getContent());

				txtFile = new TextFile(batScriptPaths.get(0) + "charge_now");
				chargeNow = StrUtils.strToInt(txtFile.getContent());
				*/

				TextFile txtFile = new TextFile(batScriptPaths.get(0) + "capacity");
				Integer batteryCharge = StrUtils.strToInt(txtFile.getContent());

				txtFile = new TextFile(batScriptPaths.get(1) + "online");
				acOnline = txtFile.getContent();

				if ((batteryCharge != null) && (acOnline != null)) {
					if (acOnline.startsWith("1")) {
						batteryLabel.setText("B: ~ (" + batteryCharge + "%) ");
						batteryLabel.setForeground(fgColorCol);
						batteryLabel.setBackground(bgColorCol);
					} else {
						setBatteryProblemText("B: " + batteryCharge + "% ", batteryCharge);
					}
				} else {
					setBatteryProblemText("B: ERR 3 ", 0);
				}

			} else {
				// Windows approach: call script
				List<String> cmdAndArgs = new ArrayList<>();
				cmdAndArgs.add(batScriptPath);

				ProcessBuilder processBuilder = new ProcessBuilder(cmdAndArgs);

				processBuilder.redirectErrorStream(true);

				Process proc = processBuilder.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
					String curline = reader.readLine();

					while (curline != null) {
						String trimline = curline.trim();
						if (!"".equals(trimline) && !trimline.contains("Battery")) {
							String[] values = trimline.split(" ");

							if (values.length > 1) {
								// BatteryStatus returns 1 if running on battery / unplugged
								String powerState = values[0];

								// EstimatedChargeRemaining is the percentage of the battery charge remaining
								String batteryCharge = values[values.length-1];

								if ("1".equals(powerState)) {
									int batteryChargeAsInt = StrUtils.strToInt(batteryCharge, 0);
									setBatteryProblemText("B: " + batteryCharge + "% ", batteryChargeAsInt);

								} else {
									batteryLabel.setText("B: ~ (" + batteryCharge + "%) ");
									batteryLabel.setForeground(fgColorCol);
									batteryLabel.setBackground(bgColorCol);
								}
							}
						}
						curline = reader.readLine();
					}
				} catch (IOException e) {
					setBatteryProblemText("B: ERR 2 ", 0);
				}
			}

		} catch (IOException ex) {
			setBatteryProblemText("B: ERR 1 ", 0);
		}
	}

	private void setBatteryProblemText(String text, int batValueToCheckAgainstThreshold) {

		if (batteryLabel == null) {
			return;
		}

		batteryLabel.setText(text);

		// as long as we are still above the flashing error threshold...
		if (batValueToCheckAgainstThreshold > batteryFlashingErrorThreshold) {
			// ... just show red text, regular background, without blinking!
			batteryDisplayCounter = 0;
		}

		if (batteryDisplayCounter == 0) {
			batteryDisplayCounter = 1;
			batteryLabel.setForeground(errorColorCol);
			batteryLabel.setBackground(bgColorCol);
		} else {
			batteryDisplayCounter = 0;
			batteryLabel.setForeground(bgColorCol);
			batteryLabel.setBackground(errorColorCol);
		}
	}

	private void checkCpuAndRamStatus() {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(topCmdAndArgs);
			Process proc = processBuilder.start();

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				String curline = reader.readLine();
				String prevMemLine = null;
				int linenum = 0;

				while (curline != null) {
					if (linenum == 2) {
						// System.out.println(curline + "\nus: '" + curline.substring(8, 11) + curline.substring(12, 13) + "' sys: '" + curline.substring(17, 20) + curline.substring(21, 22) + "'");
						Integer cpuUs = StrUtils.strToInt(curline.substring(8, 11) + curline.substring(12, 13));
						Integer cpuSy = StrUtils.strToInt(curline.substring(17, 20) + curline.substring(21, 22));
						if ((cpuUs == null) || (cpuSy == null)) {
							setCpuProblemText("C: ? ");
						} else {
							int cpuSum = (cpuUs + cpuSy) / 10;
							boolean setRegularCpuLabel = true;
							String cpuText = "C: " + cpuSum + "% ";
							if (cpuSum > 90) {
								if (cpuSum > 99) {
									cpuText = "C:" + cpuSum + "% ";
								}
								cpuAboveThresholdCounter++;
								if (cpuAboveThresholdCounter > 5) {
									// if CPU above threshold multiple times, alert as problem
									setCpuProblemText(cpuText);
									setRegularCpuLabel = false;
								}
							} else {
								if (cpuSum < 10) {
									cpuText = "C:  " + cpuSum + "% ";
								}
								cpuAboveThresholdCounter = 0;
							}
							if (setRegularCpuLabel) {
								cpuLabel.setText(cpuText);
								cpuLabel.setForeground(fgColorCol);
								cpuLabel.setBackground(bgColorCol);
							}
						}
					}
					if (linenum == 3) {
						prevMemLine = curline;
					}
					if (linenum == 4) {
						// System.out.println(prevMemLine + "\nRAM: '" + prevMemLine.substring(9, 16) + "' '" + prevMemLine.substring(40, 47) + "'");
						// System.out.println(curline + "\nswap: '" + curline.substring(9, 16) + "' '" + curline.substring(40, 47) + "'");
						Integer ramMax = StrUtils.strToInt(prevMemLine.substring(9, 16));
						Integer ramUsed = StrUtils.strToInt(prevMemLine.substring(40, 47));
						if ((ramMax == null) || (ramUsed == null)) {
							setRamProblemText("M: ? ");
						} else {
							int ramSum = (ramUsed * 100) / ramMax;
							if (ramSum > 95) {
								setRamProblemText("M: " + ramSum + "% ");
							} else {
								ramLabel.setText("M: " + ramSum + "% ");
								ramLabel.setForeground(fgColorCol);
								ramLabel.setBackground(bgColorCol);
							}
						}
						Integer swapMax = StrUtils.strToInt(curline.substring(9, 16));
						Integer swapUsed = StrUtils.strToInt(curline.substring(40, 47));
						if ((swapMax == null) || (swapUsed == null)) {
							setSwapProblemText("S: ? ");
						} else {
							int swapSum = 100;
							if (swapMax > 0) {
								swapSum = (swapUsed * 100) / swapMax;
							}
							if (swapSum > 75) {
								setSwapProblemText("S: " + swapSum + "% ");
							} else {
								swapLabel.setText("S: " + swapSum + "% ");
								swapLabel.setForeground(fgColorCol);
								swapLabel.setBackground(bgColorCol);
							}
						}
						return;
					}
					curline = reader.readLine();
					linenum++;
				}
			} catch (IOException e) {
				setCpuProblemText("C: - ");
				setRamProblemText("M: - ");
				setSwapProblemText("S: - ");
			}
		} catch (IOException ex) {
			setCpuProblemText("C: X ");
			setRamProblemText("M: X ");
			setSwapProblemText("S: X ");
			System.out.println(ex);
		}
	}

	private void setCpuProblemText(String text) {

		cpuLabel.setText(text);

		if (cpuDisplayCounter == 0) {
			cpuDisplayCounter = 1;
			cpuLabel.setForeground(errorColorCol);
			cpuLabel.setBackground(bgColorCol);
		} else {
			cpuDisplayCounter = 0;
			cpuLabel.setForeground(bgColorCol);
			cpuLabel.setBackground(errorColorCol);
		}
	}

	private void setRamProblemText(String text) {

		ramLabel.setText(text);

		if (ramDisplayCounter == 0) {
			ramDisplayCounter = 1;
			ramLabel.setForeground(errorColorCol);
			ramLabel.setBackground(bgColorCol);
		} else {
			ramDisplayCounter = 0;
			ramLabel.setForeground(bgColorCol);
			ramLabel.setBackground(errorColorCol);
		}
	}

	private void setSwapProblemText(String text) {

		swapLabel.setText(text);

		if (swapDisplayCounter == 0) {
			swapDisplayCounter = 1;
			swapLabel.setForeground(errorColorCol);
			swapLabel.setBackground(bgColorCol);
		} else {
			swapDisplayCounter = 0;
			swapLabel.setForeground(bgColorCol);
			swapLabel.setBackground(errorColorCol);
		}
	}

	private void checkAudioStatus() {

		// TODO :: add audio status checking also for other audio-accessing ways? ^^'
		if ("amixer".equals(this.audioPathGet)) {

			try {
				ProcessBuilder processBuilder = new ProcessBuilder(this.audioPathGet, "sget", "Master");
				Process proc = processBuilder.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
					String curline = reader.readLine();
					String line1 = null;
					String line2 = null;

					while (curline != null) {

						/* example output of amixer:
						Simple mixer control 'Master',0
						  Capabilities: pvolume pswitch pswitch-joined
						  Playback channels: Front Left - Front Right
						  Limits: Playback 0 - 65536
						  Mono:
						  Front Left: Playback 20316 [31%] [on]
						  Front Right: Playback 20316 [31%] [on]
						-> our plan is: we gather the first two lines we find that contain %],
						   if we only find one line then we use the value from that, if we find two
						   lines we use the average of both
						*/

						if (curline.contains("%]")) {
							if (line1 == null) {
								line1 = curline;
							} else {
								line2 = curline;
								break;
							}
						}

						curline = reader.readLine();
					}

					if (line1 == null) {
						return;
					}

					Integer newAudioPosition = 0;
					if (!line1.contains("[off]")) {
						line1 = line1.substring(line1.indexOf("[") + 1);
						line1 = line1.substring(0, line1.indexOf("%]"));
						newAudioPosition = StrUtils.strToInt(line1);
					}

					if (line2 != null) {
						Integer newAudioPos2 = 0;
						if (!line2.contains("[off]")) {
							line2 = line2.substring(line2.indexOf("[") + 1);
							line2 = line2.substring(0, line2.indexOf("%]"));
							newAudioPos2 = StrUtils.strToInt(line2);
						}
						newAudioPosition = (newAudioPosition + newAudioPos2) / 2;
					}

					checkAudioStatusSetBar(newAudioPosition);

				} catch (IOException e) {
					// System.out.println(e);
				}
			} catch (IOException ex) {
				// System.out.println(ex);
			}

		} else if ("pactl".equals(this.audioPathGet)) {

			boolean isMute = false;
			try {
				ProcessBuilder processBuilder = new ProcessBuilder(this.audioPathGet, "--", "get-sink-mute", "@DEFAULT_SINK@");
				Process proc = processBuilder.start();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
					String curline = reader.readLine();
					while (curline != null) {
						/* example output of pactl -- get-sink-mute 0:
						Mute: yes
						*/
						if (curline.startsWith("Mute: yes")) {
							isMute = true;
						}
						curline = reader.readLine();
					}
				} catch (IOException e) {
					// System.out.println(e);
				}
			} catch (IOException ex) {
				// System.out.println(ex);
			}

			Integer newAudioPosition = 0;

			if (!isMute) {
				try {
					ProcessBuilder processBuilder = new ProcessBuilder(this.audioPathGet, "--", "get-sink-volume", "@DEFAULT_SINK@");
					Process proc = processBuilder.start();

					try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
						String curline = reader.readLine();

						while (curline != null) {

							/* example output of pactl -- get-sink-volume 0:
							Volume: font-left: 32768 /   50% / -18.06 dB,  front-right: 32768 /   50% / -18.06 dB
									balance 0.00
							*/

							if (curline.startsWith("Volume: ")) {

								int newAudioPos = 0;
								int newAudioPosCounter = 0;
								List<String> percentStrs = StrUtils.split(curline, "%");
								for (int i = 0; i < percentStrs.size() - 1; i++) {
									String cur = percentStrs.get(i);
									Integer curInt = StrUtils.strToInt(cur.substring(cur.length() - 3));
									if (curInt != null) {
										newAudioPos += curInt;
										newAudioPosCounter++;
									}
								}

								if (newAudioPosCounter > 0) {
									newAudioPosition = newAudioPos / newAudioPosCounter;
								}

								break;
							}

							curline = reader.readLine();
						}

					} catch (IOException e) {
						// System.out.println(e);
					}
				} catch (IOException ex) {
					// System.out.println(ex);
				}
			}

			checkAudioStatusSetBar(newAudioPosition);
		}
	}

	private void checkAudioStatusSetBar(Integer newAudioPosition) {

		if (newAudioPosition < 1) {
			newAudioPosition = null;
		}

		boolean notifyListeners = false;

		if (newAudioPosition != null) {
			while (newAudioPosition > volumeItem.getMaximum()) {
				// sanity check - above 800% audio everything is too distorted anyway,
				// plus the volumeItem bar would get too large, so clip it there
				if (newAudioPosition > 800) {
					newAudioPosition = 800;
				}
				volumeItem.setMaximum(volumeItem.getMaximum() * 2);
				boostAudioLabel.setText("++ ");
			}
		}

		volumeItem.setBarPosition(newAudioPosition, notifyListeners);
	}

	private void insertTextForFunctionKey(String textToInsert, JTextField decoratedEditor) {
		String txt = decoratedEditor.getText();
		int selStart = decoratedEditor.getSelectionStart();
		int selEnd = decoratedEditor.getSelectionEnd();

		decoratedEditor.setText(
			txt.substring(0, selStart) +
			textToInsert +
			txt.substring(selEnd)
		);

		decoratedEditor.setSelectionStart(selStart + textToInsert.length());
		decoratedEditor.setSelectionEnd(selStart + textToInsert.length());
	}

	public void minimize() {
		height = 1;
		resetGuiLocation();

		// remove everything to have just black line at the top, nothing else
		setElementVisibility(false);
	}

	public void maximize() {
		height = INIT_HEIGHT;
		resetGuiLocation();

		// reset everything to visible
		setElementVisibility(true);
	}

	private void setElementVisibility(boolean visible) {
		consoleField1.setVisible(visible);
		consoleField2.setVisible(visible);
		consoleField3.setVisible(visible);
		volumeItem.setVisible(visible);
		boostAudioLabel.setVisible(visible);
		counterLabel.setVisible(visible);
		quoteLabel1.setVisible(visible);
		quoteLabel2.setVisible(visible);
		quoteLabel3.setVisible(visible);
		quoteLabel4.setVisible(visible);
		quoteLabel5.setVisible(visible);
		quoteLabel6.setVisible(visible);
		newlineLabel.setVisible(visible);
		heartLabel.setVisible(visible);
		if (batteryLabel != null) {
			batteryLabel.setVisible(visible);
		}
		clockLabel.setVisible(visible);
		if (brightnessItem != null) {
			brightnessItem.setVisible(visible);
		}
	}

	private void hideEmojiSelector() {
		if (emojiSelectorGUI != null) {
			emojiSelectorGUI.hide();
		}
	}

}
