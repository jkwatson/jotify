package de.felixbruns.jotify.gui.swing.plaf;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicButtonUI;

public class JotifyButtonUI extends BasicButtonUI {
	private Image imageNormal;
	private Image imagePressed;
	private Image imageDisabled;
	
	public JotifyButtonUI(){
		/* Load button images (22x20 pixels). */
		this.imageNormal   = new ImageIcon(JotifyButtonUI.class.getResource("images/button.png")).getImage();
		this.imagePressed  = new ImageIcon(JotifyButtonUI.class.getResource("images/button_pressed.png")).getImage();
		this.imageDisabled = new ImageIcon(JotifyButtonUI.class.getResource("images/button_disabled.png")).getImage();
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get bounds (width & height), button and 2D graphics. */
		Rectangle      bounds     = component.getBounds();
		AbstractButton button     = (AbstractButton)component;
		Graphics2D     graphics2D = (Graphics2D)graphics;
		Image          image      = null;
		int            w          = bounds.width;
		int            h          = bounds.height;
		
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
		
		/* Draw corners of button (cut out slices). */
		graphics2D.drawImage(image,      0,      0, 10, 10,  0,  0, 10, 10, null); /* top-left */
		graphics2D.drawImage(image, w - 10,      0,  w, 10, 12,  0, 22, 10, null); /* top-right */
		graphics2D.drawImage(image, w - 10, h - 10,  w,  h, 12, 10, 22, 20, null); /* bottom-right */
		graphics2D.drawImage(image,      0, h - 10, 10,  h,  0, 10, 10, 20, null); /* bottom-left */
		
		/* Draw borders (cut out slices). */
		graphics2D.drawImage(image,     10,      0, w - 10,     10, 10,  0, 12, 10, null); /* top */
		graphics2D.drawImage(image,     10, h - 10, w - 10,      h, 10, 10, 12, 20, null); /* bottom */
		graphics2D.drawImage(image,      0,     10,     10, h - 10,  0, 10, 10, 11, null); /* left */
		graphics2D.drawImage(image, w - 10,     10,      w, h - 10, 12, 10, 22, 11, null); /* right */
		
		/* Draw center (cut out slice). */
		graphics2D.drawImage(image, 10, 10, w - 10, h - 10, 10, 10, 12, 11, null); /* center */
		
		/* Set anti-aliasing to on. */
		graphics2D.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON
		);
		
		/* Get font metric. */
		FontMetrics metrics = graphics2D.getFontMetrics();
		
		/* Set color. */
		if(!button.getModel().isEnabled()){
			Color foreground         = button.getForeground();
			Color disabledForeground = new Color(
				foreground.getRed(), foreground.getGreen(),
				foreground.getBlue(), foreground.getAlpha() / 2
			);
			
			graphics2D.setColor(disabledForeground);
		}
		else{
			graphics2D.setColor(button.getForeground());
		}
		
		/* Draw centered text. */
		graphics2D.drawString(
			button.getText(),
			w / 2 - metrics.stringWidth(button.getText()) / 2,
			h / 2 + (metrics.getMaxAscent() - metrics.getMaxDescent()) / 2
		);
	}
}
