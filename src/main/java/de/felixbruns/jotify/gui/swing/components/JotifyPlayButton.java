package de.felixbruns.jotify.gui.swing.components;

import java.awt.Dimension;

import javax.swing.JButton;

import de.felixbruns.jotify.gui.swing.plaf.JotifyPlayButtonUI;

@SuppressWarnings("serial")
public class JotifyPlayButton extends JButton {
	private boolean pause;
	
	public JotifyPlayButton(){
		this.pause = false;
		
		/* Force size. */
		Dimension size = new Dimension(32, 32);
		
		this.setPreferredSize(size);
		this.setMinimumSize(size);
		this.setMaximumSize(size);
		this.setSize(size);
		
		/* Set some options. */
		this.setBorderPainted(false);
		this.setContentAreaFilled(false);
		this.setOpaque(false);
		
		/* Set UI to use. */
		this.setUI(new JotifyPlayButtonUI());
	}
	
	public boolean isPause(){
		return this.pause;
	}
	
	public void setPause(boolean pause){
		this.pause = pause;
		
		this.repaint();
	}
}
