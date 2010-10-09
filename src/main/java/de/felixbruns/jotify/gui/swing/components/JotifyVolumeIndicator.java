package de.felixbruns.jotify.gui.swing.components;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JPanel;

import de.felixbruns.jotify.gui.swing.plaf.JotifyVolumeIndicatorUI;

@SuppressWarnings("serial")
public class JotifyVolumeIndicator extends JPanel {
	private float value;
	
	public JotifyVolumeIndicator(){
		this.setOpaque(false);
		this.setForeground(Color.WHITE);
		this.setMinimumSize(new Dimension(15, 25));
		this.setMaximumSize(new Dimension(15, 25));
		this.setPreferredSize(new Dimension(15, 25));
		this.setUI(new JotifyVolumeIndicatorUI());
		
		this.value = 0.0f;
	}
	
	public void setVolume(float volume){
		this.value = volume;
		
		this.repaint();
	}
	
	public float getValue(){
		return this.value;
	}
	
	public float getMinimum(){
		return 0.0f;
	}
	
	public float getMaximum(){
		return 1.0f;
	}
}
