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
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.images.ColorRGB;
import com.asofterspace.toolbox.io.IoUtils;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.GridBagLayout;
import java.awt.Point;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class GUI extends MainWindow {

	private Database database;

	private ConsoleCtrl consoleCtrl;


	public GUI(Database database, ConsoleCtrl consoleCtrl) {
		this.database = database;
		this.consoleCtrl = consoleCtrl;
	}

	@Override
	public void run() {

		super.create();

		refreshTitleBar();

		createMainPanel(mainFrame);

		// do not call super.show, as we are doing things a little bit
		// differently around here (including restoring from previous
		// position...)
		// super.show();

		final int left = 0;
		final int top = -30;
		final int width = 1600;
		final int height = 60;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				mainFrame.pack();

				// Actually display the whole jazz
				mainFrame.setVisible(true);

				mainFrame.setSize(width, height);

				mainFrame.setPreferredSize(new Dimension(width, height));

				mainFrame.setLocation(new Point(left, top));
			}
		});
	}

	private JPanel createMainPanel(JFrame parent) {

		ColorRGB bgColor = new ColorRGB(0, 0, 0);
		ColorRGB fgColor = new ColorRGB(255, 255, 255);

		JPanel mainPanel = new JPanel();
		mainPanel.setBackground(bgColor.toColor());
		mainPanel.setPreferredSize(new Dimension(800, 500));
		GridBagLayout mainPanelLayout = new GridBagLayout();
		mainPanel.setLayout(mainPanelLayout);

		JTextField consoleField = new JTextField();
		consoleField.setBackground(bgColor.toColor());
		consoleField.setForeground(fgColor.toColor());
		consoleField.setCaretColor(fgColor.toColor());
		mainPanel.add(consoleField, new Arrangement(0, 0, 1.0, 1.0));

		consoleField.addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {

				String command = consoleField.getText();
				String previousPath = PathCtrl.DESKTOP;
				previousPath = PathCtrl.ensurePathIsSafe(previousPath);

				ConsoleResult consoleResult = consoleCtrl.interpretCommand(command, previousPath);

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

		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private void refreshTitleBar() {
		mainFrame.setTitle(AssBrowser.PROGRAM_TITLE);
	}

}
