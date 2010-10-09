package de.felixbruns.jotify.gui.swing.components;

import javax.swing.JSlider;

import de.felixbruns.jotify.gui.swing.plaf.JotifySliderUI;

@SuppressWarnings("serial")
public class JotifySlider extends JSlider {
	private boolean drawProgress;
	
	public JotifySlider(boolean drawProgress){
		this.drawProgress = drawProgress;
		
		this.setOpaque(false);
		this.setUI(new JotifySliderUI(this));
	}
	
	public JotifySlider(){
		this(true);
	}
	
	public void setDrawProgress(boolean value){
		this.drawProgress = value;
	}
	
	public boolean getDrawProgress(){
		return this.drawProgress;
	}
}
