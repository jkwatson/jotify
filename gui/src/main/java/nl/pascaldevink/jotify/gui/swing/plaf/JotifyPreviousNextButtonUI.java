package nl.pascaldevink.jotify.gui.swing.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class JotifyPreviousNextButtonUI extends BasicButtonUI {
	public static final int BUTTON_PREVIOUS = 0;
	public static final int BUTTON_NEXT     = 1;
	
	private Image imageNormal;
	private Image imagePressed;
	private Image imageDisabled;
	
	public JotifyPreviousNextButtonUI(int type){
		/* Load button images (25x25 pixels). */
		if(type == BUTTON_PREVIOUS){
			this.imageNormal   = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/previous_button.png")).getImage();
			this.imagePressed  = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/previous_button_pressed.png")).getImage();
			this.imageDisabled = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/previous_button_disabled.png")).getImage();
		}
		else{
			this.imageNormal   = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/next_button.png")).getImage();
			this.imagePressed  = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/next_button_pressed.png")).getImage();
			this.imageDisabled = new ImageIcon(JotifyPlayButtonUI.class.getResource("images/next_button_disabled.png")).getImage();
		}
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get button and 2D graphics. */
		AbstractButton button     = (AbstractButton)component;
		Graphics2D     graphics2D = (Graphics2D)graphics;
		Image          image      = null;
		
		/* Select button image based on status. */
		if(!button.getModel().isEnabled()){
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
