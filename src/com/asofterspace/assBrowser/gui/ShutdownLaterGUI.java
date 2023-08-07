/**
 * Unlicensed code created by A Softer Space, 2023
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.gui;

import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.Utils;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class ShutdownLaterGUI {

	private GUI gui;
	private ConsoleCtrl consoleCtrl;

	private JDialog dialogWindow;

	private JLabel explanationLabel = null;
	private JTextField executionTargetEdit = null;

	private Thread timerThread = null;

	private boolean paused = false;

	private boolean keepTimerRunning = false;

	private long executeShutdownAt = 0l;
	private long remainingBeforePause = 0l;


	public ShutdownLaterGUI(GUI gui, ConsoleCtrl consoleCtrl) {

		this.gui = gui;

		this.consoleCtrl = consoleCtrl;

		this.dialogWindow = createGUI();
	}

	private JDialog createGUI() {

		// Create the window
		boolean blockMainGUI = false;
		final JDialog dialogWindow = new JDialog(gui.getMainFrame(), "Timer", blockMainGUI);
		GridBagLayout dialogWindowLayout = new GridBagLayout();
		dialogWindow.setLayout(dialogWindowLayout);
		dialogWindow.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		dialogWindow.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent e) {
				stopTimerThread();
			}

			public void windowClosing(WindowEvent e) {
				stopTimerThread();
			}
		});


		// Populate the window
		explanationLabel = new JLabel();
		explanationLabel.setText("");
		explanationLabel.setFont(new Font("Starfleet BdEx BT", Font.PLAIN, 24));
		dialogWindow.add(explanationLabel, new Arrangement(0, 0, 1.0, 0.0));

		executionTargetEdit = new JTextField();
		executionTargetEdit.setText("");
		dialogWindow.add(executionTargetEdit, new Arrangement(0, 1, 1.0, 0.0));


		// Button Row to change timer
		JPanel buttonRowTimerChange = new JPanel();
		GridLayout buttonRowTimeChangeLayout = new GridLayout(1, 2);
		buttonRowTimeChangeLayout.setHgap(8);
		buttonRowTimerChange.setLayout(buttonRowTimeChangeLayout);
		dialogWindow.add(buttonRowTimerChange, new Arrangement(0, 2, 1.0, 0.0));

		JButton minus1hourButton = new JButton("- 1 hour");
		minus1hourButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executeShutdownAt -= 1000 * 60 * 60;
			}
		});
		buttonRowTimerChange.add(minus1hourButton);

		JButton minus15minButton = new JButton("- 15 min");
		minus15minButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executeShutdownAt -= 1000 * 60 * 15;
			}
		});
		buttonRowTimerChange.add(minus15minButton);

		JButton plus15minButton = new JButton("+ 15 min");
		plus15minButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executeShutdownAt += 1000 * 60 * 15;
			}
		});
		buttonRowTimerChange.add(plus15minButton);

		JButton plus1hourButton = new JButton("+ 1 hours");
		plus1hourButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				executeShutdownAt += 1000 * 60 * 60;
			}
		});
		buttonRowTimerChange.add(plus1hourButton);


		// Button Row to Pause / Cancel
		JPanel buttonRow = new JPanel();
		GridLayout buttonRowLayout = new GridLayout(1, 2);
		buttonRowLayout.setHgap(8);
		buttonRow.setLayout(buttonRowLayout);
		dialogWindow.add(buttonRow, new Arrangement(0, 3, 1.0, 0.0));

		JButton pauseButton = new JButton("Pause");
		pauseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				paused = !paused;
				if (paused) {
					remainingBeforePause = executeShutdownAt - System.currentTimeMillis();
					pauseButton.setText("Resume");
					stopTimerThread();
				} else {
					executeShutdownAt = remainingBeforePause + System.currentTimeMillis();
					pauseButton.setText("Pause");
					startTimerThread();
				}
			}
		});
		buttonRow.add(pauseButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stopTimerThread();
				dialogWindow.dispose();
			}
		});
		buttonRow.add(cancelButton);

		// Set the preferred size of the dialog
		int width = 450;
		int height = 200;
		dialogWindow.setSize(width, height);
		dialogWindow.setPreferredSize(new Dimension(width, height));

		return dialogWindow;
	}

	public void show(String executionTargetStr) {

		long defaultTimeMS =
			1000 * // milliseconds
			60 * // seconds
			60 * // minutes
			1; // hours

		this.executeShutdownAt = System.currentTimeMillis() + defaultTimeMS;

		GuiUtils.centerAndShowWindow(dialogWindow);

		executionTargetEdit.setText(executionTargetStr);

		startTimerThread();
	}

	public void displayRemainingTime(Long remainingTime) {

		if (explanationLabel == null) {
			return;
		}

		if (remainingTime == null) {
			explanationLabel.setText("");
			return;
		}

		if (remainingTime < 0) {
			remainingTime = 0l;
		}

		remainingTime = remainingTime / 1000;

		String hours = "" + ((int) Math.floor(remainingTime / (60*60)));
		String minutes = "" + ((int) Math.floor((remainingTime % (60*60)) / 60));
		if (minutes.length() < 2) {
			minutes = "0" + minutes;
		}
		String seconds = "" + (remainingTime % 60);
		if (seconds.length() < 2) {
			seconds = "0" + seconds;
		}

		explanationLabel.setText(hours + ":" + minutes + ":" + seconds + " until execution of:");
	}

	private void startTimerThread() {

		keepTimerRunning = true;

		if (timerThread != null) {
			return;
		}

		timerThread = new Thread() {

			public void run() {

				while (keepTimerRunning) {
					try {
						long remainingTime = executeShutdownAt - System.currentTimeMillis();
						displayRemainingTime(remainingTime);
						if (remainingTime < 0) {
							if (!"".equals(executionTargetEdit.getText())) {
								String command = executionTargetEdit.getText();
								String previousPath = ".";
								boolean calledFromOutside = true;
								consoleCtrl.interpretCommand(command, previousPath, calledFromOutside);
							}
							stopTimerThread();
							dialogWindow.dispose();
						}

						Thread.sleep(1000);

					} catch (InterruptedException e) {
						// just keep sleeping...
					}
				}

				timerThread = null;
			}
		};
		timerThread.start();
	}

	private void stopTimerThread() {
		keepTimerRunning = false;
	}

}
