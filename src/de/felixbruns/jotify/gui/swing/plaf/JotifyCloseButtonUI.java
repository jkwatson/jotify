package de.felixbruns.jotify.gui.swing.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class JotifyCloseButtonUI extends BasicButtonUI {
	private Image imageNormal;
	private Image imagePressed;
	
	public JotifyCloseButtonUI(){
		/* Load button images (10x10 pixels). */
		this.imageNormal  = new ImageIcon(JotifyCloseButtonUI.class.getResource("images/close.png")).getImage();
		this.imagePressed = new ImageIcon(JotifyCloseButtonUI.class.getResource("images/close_pressed.png")).getImage();
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get button and 2D graphics. */
		AbstractButton button     = (AbstractButton)component;
		Graphics2D     graphics2D = (Graphics2D)graphics;
		Image          image      = null;
		
		/* Select button image based on status. */
		if(button.getModel().isPressed()){
			image = this.imagePressed;
		}
		else{
			image = this.imageNormal;
		}
		
		/* Draw button. */
		graphics2D.drawImage(image, 0, 0, null);
	}
}
