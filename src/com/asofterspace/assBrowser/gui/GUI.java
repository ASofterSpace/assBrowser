/**
 * Unlicensed code created by A Softer Space, 2020
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.gui;

import com.asofterspace.assBrowser.AssBrowser;
import com.asofterspace.assBrowser.console.ConsoleCtrl;
import com.asofterspace.assBrowser.Database;
import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.MainWindow;
import com.asofterspace.toolbox.images.ColorRGB;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Point;

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

		parent.add(mainPanel, BorderLayout.CENTER);

		return mainPanel;
	}

	private void refreshTitleBar() {
		mainFrame.setTitle(AssBrowser.PROGRAM_TITLE);
	}

}
