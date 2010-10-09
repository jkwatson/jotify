package de.felixbruns.jotify.gui.swing.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

import de.felixbruns.jotify.gui.swing.components.JotifyPlayButton;

public class JotifyPlayButtonUI extends BasicButtonUI {
	private Image imageNormal;
	private Image imagePressed;
	private Image imageDisabled;
	private Image imagePauseNormal;
	private Image imagePausePressed;
	private Image imagePauseDisabled;
	
	public JotifyPlayButtonUI(){
		/* Load button images (32x32 pixels). */
		this.imageNormal        = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/play_button.png")).getImage();
		this.imagePressed       = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/play_button_pressed.png")).getImage();
		this.imageDisabled      = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/play_button_disabled.png")).getImage();
		this.imagePauseNormal   = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/pause_button.png")).getImage();
		this.imagePausePressed  = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/pause_button_pressed.png")).getImage();
		this.imagePauseDisabled = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/pause_button_disabled.png")).getImage();
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get button and 2D graphics. */
		JotifyPlayButton button     = (JotifyPlayButton)component;
		Graphics2D       graphics2D = (Graphics2D)graphics;
		Image            image      = null;
		
		/* Select button image based on status. */
		if(button.isPause() && !button.isEnabled()){
			image = this.imagePauseDisabled;
		}
		else if(button.isPause() && button.getModel().isPressed()){
			image = this.imagePausePressed;
		}
		else if(button.isPause()){
			image = this.imagePauseNormal;
		}
		else if(!button.isEnabled()){
			image = this.imageDisabled;
		}
		else if(button.getModel().isPressed()){
			image = this.imagePressed;
		}
		else{
			image = this.imageNormal;
		}
		
		/* Draw button. */
		graphics2D.drawImage(image, 0, 0, null);
	}
}
