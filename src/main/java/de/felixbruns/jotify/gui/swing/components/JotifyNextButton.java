package de.felixbruns.jotify.gui.swing.components;

import java.awt.Dimension;

import javax.swing.JButton;

import de.felixbruns.jotify.gui.swing.plaf.JotifyPreviousNextButtonUI;

@SuppressWarnings("serial")
public class JotifyNextButton extends JButton {
	public JotifyNextButton(){
		/* Force size. */
		Dimension size = new Dimension(25, 25);
		
		this.setPreferredSize(size);
		this.setMinimumSize(size);
		this.setMaximumSize(size);
		this.setSize(size);
		
		/* Set some options. */
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
		this.setOpaque(false);
		
		/* Set UI to use. */
		this.setUI(new JotifyPreviousNextButtonUI(JotifyPreviousNextButtonUI.BUTTON_NEXT));
	}
}
