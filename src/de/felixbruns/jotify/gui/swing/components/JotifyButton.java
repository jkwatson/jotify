package de.felixbruns.jotify.gui.swing.components;

import javax.swing.JButton;

import de.felixbruns.jotify.gui.swing.plaf.JotifyButtonUI;

@SuppressWarnings("serial")
public class JotifyButton extends JButton {
	public JotifyButton(){
		this(null);
	}

	public JotifyButton(String text){
		super(text);
		
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
		this.setOpaque(false);
		this.setUI(new JotifyButtonUI());
	}
}
