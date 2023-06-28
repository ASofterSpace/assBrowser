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
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.images.ColorRGBA;
import com.asofterspace.toolbox.io.IoUtils;
import com.asofterspace.toolbox.utils.DateUtils;
import com.asofterspace.toolbox.utils.StrUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	private JLabel counterLabel;
	private JLabel quoteLabel1;
	private JLabel quoteLabel2;
	private JLabel batteryLabel;
	private JLabel clockLabel;

	private boolean timerRunning = false;

	private final static ColorRGBA bgColor = new ColorRGBA(0, 0, 0);
	private final static Color bgColorCol = bgColor.toColor();
	private final static ColorRGBA borderColor = new ColorRGBA(96, 0, 192);
	private final static ColorRGBA fgColorMain = new ColorRGBA(255, 255, 255);
	private final static ColorRGBA fgColor = new ColorRGBA(167, 62, 249);
	private final static Color fgColorCol = fgColor.toColor();
	private final static ColorRGBA errorColor = new ColorRGBA(255, 0, 64);
	private final static Color errorColorCol = errorColor.toColor();

	private final static int left = 0;
	private final static int top = 0;
	private final static int width = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private final static int height = 22;

	private int batteryDisplayCounter = 0;
	// private long lastVolumeTime = 0;
	private String nircmdPath;

	private Font sharedFont = null;


	public GUI(Database database, ConsoleCtrl consoleCtrl) {
		this.database = database;
		this.consoleCtrl = consoleCtrl;
		this.nircmdPath = database.getNircmdPath();
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

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// Actually display the whole jazz
				mainFrame.setVisible(true);

				resetGuiLocation();

				startTimerThread();
			}
		});
	}

	private JPanel createMainPanel(JFrame parent) {

		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(bgColorCol);
		mainPanel.setPreferredSize(new Dimension(800, 500));
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);

		JTextField consoleField = new JTextField();
		consoleField.setBackground(bgColorCol);
		consoleField.setForeground(fgColorMain.toColor());
		consoleField.setCaretColor(fgColor.toColor());
		consoleField.setBorder(new LineBorder(borderColor.toColor()));
		mainPanel.add(consoleField, new Arrangement(0, 0, 1.0, 1.0));

		consoleField.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
			}

			public void keyTyped(KeyEvent event) {
			}

			public void keyPressed(KeyEvent event) {
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

			}
		});

		consoleField.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {

				String command = consoleField.getText();
				String previousPath = PathCtrl.DESKTOP;
				previousPath = PathCtrl.ensurePathIsSafe(previousPath);

				boolean fromOutside = true;
				ConsoleResult consoleResult = consoleCtrl.interpretCommand(command, previousPath, fromOutside);

				String newCommand = consoleResult.getCommand();
				consoleField.setText(newCommand);
				String newPath = consoleResult.getPath();
				newPath = PathCtrl.ensurePathIsSafe(newPath);
				if (!newPath.equals(previousPath)) {
					IoUtils.executeAsync(
						database.getBrowserPath() + " http://localhost:3013/?link=" + UrlEncoder.encode(newPath)
					);
				}
			}
		});

		IoUtils.executeAsync(this.nircmdPath + " setsysvolume 0");
		IoUtils.executeAsync(this.nircmdPath + " mutesysvolume 0");

		BarMenuItemForMainMenu volumeItem = new BarMenuItemForMainMenu();
		volumeItem.setBackground(bgColorCol);
		volumeItem.setForeground(fgColor.toColor());
		volumeItem.setBarPosition(null, false);
		volumeItem.setMaximum(100);
		volumeItem.setSendUpdateOnMousePress(true);
		volumeItem.addBarListener(new BarListener() {
			@Override
			public void onBarMove(Integer position) {
				adjustVolume(position);
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
		mainPanel.add(volumeItem, new Arrangement(1, 0, 0.0, 1.0));

		counterLabel = createLabel(" 0 ", bgColor, fgColor);
		mainPanel.add(counterLabel, new Arrangement(2, 0, 0.0, 1.0));

		counterLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int num = StrUtils.strToInt(counterLabel.getText().trim()) + 1;
				if (SwingUtilities.isRightMouseButton(e)) {
					num = 0;
				}
				counterLabel.setText(" " + num + " ");
			}
		});

		quoteLabel1 = createLabel("„“”", bgColor, fgColor);
		mainPanel.add(quoteLabel1, new Arrangement(3, 0, 0.0, 1.0));

		quoteLabel1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				copyToClipboard("„“”");
			}
		});

		quoteLabel2 = createLabel("‚‘’ ", bgColor, fgColor);
		mainPanel.add(quoteLabel2, new Arrangement(4, 0, 0.0, 1.0));

		quoteLabel2.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				copyToClipboard("‚‘’");
			}
		});

		batteryLabel = createLabel("BATTERY STATE UNINITIALIZED ", bgColor, errorColor);
		batteryLabel.setOpaque(true);
		mainPanel.add(batteryLabel, new Arrangement(5, 0, 0.0, 1.0));

		batteryLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				checkBatteryStatus();
				resetGuiLocation();
			}
		});

		clockLabel = createLabel("00:00 ", bgColor, fgColor);
		mainPanel.add(clockLabel, new Arrangement(6, 0, 0.0, 1.0));

		clockLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				copyToClipboard(DateUtils.getCurrentDateTimeStamp());
				resetGuiLocation();
			}
		});


		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private void adjustVolume(Integer position) {

		// this.lastVolumeTime = System.currentTimeMillis();

		if (position == null) {
			position = 0;
		}

		IoUtils.executeAsync(this.nircmdPath + " setsysvolume " +
			Math.min(65535, position * 656));
	}

	private void refreshTitleBar() {
		mainFrame.setTitle(AssBrowser.PROGRAM_TITLE);
	}

	private void copyToClipboard(String toBeCopiedText) {
		StringSelection selection = new StringSelection(toBeCopiedText);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(selection, selection);
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

						// update time every 30 seconds
						Thread.sleep(30 * 1000);

					} catch (InterruptedException e) {
						// just keep sleeping...
					}
				}
			}
		};
		timerThread.start();
	}

	private JLabel createLabel(String text, ColorRGBA bgColor, ColorRGBA fgColor) {
		JLabel result = new JLabel(text);
		result.setBackground(bgColorCol);
		result.setForeground(fgColor.toColor());
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
		if (batScriptPath == null) {
			setBatteryProblemText("BATTERY STATE UNKNOWN ");
			return;
		}

		try {

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
								setBatteryProblemText(" ON BATTERY :: " + batteryCharge + "% ");

							} else {
								batteryLabel.setText("~ (" + batteryCharge + "%) ");
								batteryLabel.setForeground(fgColorCol);
								batteryLabel.setBackground(bgColorCol);
							}
						}
					}
					curline = reader.readLine();
				}
			} catch (IOException e) {
				setBatteryProblemText("BATTERY STATE ERROR 2 ");
			}

		} catch (IOException ex) {
			setBatteryProblemText("BATTERY STATE ERROR 1 ");
		}
	}

	private void setBatteryProblemText(String text) {

		batteryLabel.setText(text);

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

}
