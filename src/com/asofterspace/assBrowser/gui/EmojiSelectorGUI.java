/**
 * Unlicensed code created by A Softer Space, 2023
 * www.asofterspace.com/licenses/unlicense.txt
 */
package com.asofterspace.assBrowser.gui;

import com.asofterspace.toolbox.gui.Arrangement;
import com.asofterspace.toolbox.gui.GuiUtils;
import com.asofterspace.toolbox.images.ColorRGBA;
import com.asofterspace.toolbox.Utils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;


public class EmojiSelectorGUI {

	private GUI gui;

	private JDialog dialogWindow;

	private boolean currentlyVisible = false;

	final static String PURPLE_HEART = new String(Character.toChars(0x1F49C));
	final static String SCREAM = new String(Character.toChars(0x1F631));
	final static String THUMBS_UP = new String(Character.toChars(0x1F44D));
	final static String THUMBS_DOWN = new String(Character.toChars(0x1F44E));
	final static String FROG = new String(Character.toChars(0x1F438));
	final static String TADA = new String(Character.toChars(0x1F389));
	final static String RAINBOW = new String(Character.toChars(0x1F308));
	final static String SPARKLES = new String(Character.toChars(0x2728));
	final static String FIRE = new String(Character.toChars(0x1F525));

	final static Color colorYellow = new ColorRGBA(255, 255, 0).toColor();
	final static Color colorGreen = new ColorRGBA(0, 196, 0).toColor();
	final static Color colorRed = new ColorRGBA(255, 0, 0).toColor();
	final static Color colorOrange = new ColorRGBA(255, 128, 0).toColor();
	final static Color colorWhite = new ColorRGBA(255, 255, 255).toColor();

	private int colCounter = 0;
	private int rowCounter = 0;


	public EmojiSelectorGUI(GUI gui) {

		this.gui = gui;

		this.dialogWindow = createGUI();
	}

	private JDialog createGUI() {

		// Create the window
		boolean blockMainGUI = false;
		dialogWindow = new JDialog(gui.getMainFrame(), "Emoji Selection", blockMainGUI);
		GridBagLayout dialogWindowLayout = new GridBagLayout();
		dialogWindow.setLayout(dialogWindowLayout);
		dialogWindow.getRootPane().setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		dialogWindow.setBackground(GUI.bgColorCol);
		dialogWindow.getContentPane().setBackground(GUI.bgColorCol);


		// slight offset to center it
		colCounter = 5;
		rowCounter = 0;

		addLabel(PURPLE_HEART);
		addLabel(SCREAM, colorYellow);
		addLabel(THUMBS_UP, colorYellow);
		addLabel(THUMBS_DOWN, colorYellow);
		addLabel(FROG, colorGreen);
		addLabel(TADA, colorRed);
		addLabel(RAINBOW, colorWhite);
		addLabel(SPARKLES, colorYellow);
		addLabel(FIRE, colorOrange);

		colCounter = 0;
		rowCounter = 1;

		addLabel("·");
		addLabel("•");
		addLabel("—");
		addLabel("–");
		addLabel("˜");
		addLabel("†");
		addLabel("‡");
		addLabel("±");
		addLabel("½");
		addLabel("¼");
		addLabel("¾");
		addLabel("¿");
		addLabel("¡");
		addLabel("×");
		addLabel("÷");
		addLabel("ø");
		addLabel("Ø");
		addLabel("¬");

		colCounter = 0;
		rowCounter = 2;

		addLabel("ä");
		addLabel("Ä");
		addLabel("ö");
		addLabel("Ö");
		addLabel("ü");
		addLabel("Ü");
		addLabel("ß");
		addLabel("æ");
		addLabel("Æ");
		addLabel("ð");
		addLabel("Ð");
		addLabel("þ");
		addLabel("Þ");
		addLabel("ï");
		addLabel("ç");
		addLabel("ñ");
		addLabel("å");
		addLabel("Å");

		// remove title bar
		dialogWindow.setUndecorated(true);

		return dialogWindow;
	}

	private void addLabel(String text) {
		addLabel(text, GUI.fgColorCol);
	}

	private void addLabel(String text, Color fgColorCol) {
		JLabel label = new JLabel(text + " ");
		label.setBackground(GUI.bgColorCol);
		label.setForeground(fgColorCol);
		label.setOpaque(true);
		if (gui.sharedFont != null) {
			label.setFont(gui.sharedFont);
		}

		dialogWindow.add(label, new Arrangement(colCounter++, rowCounter, 0.0, 0.0));

		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				GuiUtils.copyToClipboard(text);
				hide();
			}
		});
	}

	public void show() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

		int width = (int) screenSize.getWidth();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Stage everything to be shown
				dialogWindow.pack();

				// Actually display the whole jazz
				dialogWindow.setVisible(true);

				dialogWindow.setLocation(new Point(width - 350, 20));
			}
		});

		currentlyVisible = true;
	}

	public void hide() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				dialogWindow.setVisible(false);
			}
		});

		currentlyVisible = false;
	}

	public void toggle() {
		if (currentlyVisible) {
			hide();
		} else {
			show();
		}
	}

}
