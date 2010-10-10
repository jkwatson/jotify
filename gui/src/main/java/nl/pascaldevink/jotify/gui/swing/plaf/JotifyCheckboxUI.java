package nl.pascaldevink.jotify.gui.swing.plaf;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicCheckBoxUI;

public class JotifyCheckboxUI extends BasicCheckBoxUI {
	private Image imageNormal;
	private Image imagePressed;
	private Image imageNormalChecked;
	private Image imagePressedChecked;
	
	public JotifyCheckboxUI(){
		/* Load button images (15x17 pixels). */
		this.imageNormal         = new ImageIcon(JotifyCheckboxUI.class.getResource("images/checkbox.png")).getImage();
		this.imagePressed        = new ImageIcon(JotifyCheckboxUI.class.getResource("images/checkbox_pressed.png")).getImage();
		this.imageNormalChecked  = new ImageIcon(JotifyCheckboxUI.class.getResource("images/checkbox_checked.png")).getImage();
		this.imagePressedChecked = new ImageIcon(JotifyCheckboxUI.class.getResource("images/checkbox_pressed_checked.png")).getImage();
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get bounds (height), button and 2D graphics. */
		Rectangle      bounds     = component.getBounds();
		AbstractButton button     = (AbstractButton)component;
		Graphics2D     graphics2D = (Graphics2D)graphics;
		Image          image      = null;
		int            h          = bounds.height;
		
		/* Select button image based on status. */
		if(button.getModel().isPressed() && button.getModel().isSelected()){
			image = this.imagePressedChecked;
		}
		else if(button.getModel().isSelected()){
			image = this.imageNormalChecked;
		}
		else if(button.getModel().isPressed()){
			image = this.imagePressed;
		}
		else{
			image = this.imageNormal;
		}
		
		/* Draw checkbox image. */
		graphics2D.drawImage(image, button.getIconTextGap(), h / 2 - (17 + 2) / 2, null);
		
		/* Set anti-aliasing to on. */
		graphics2D.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON
		);
		
		/* Get font metric. */
		FontMetrics metrics = graphics2D.getFontMetrics();
		
		/* Calculate text offset. */
		int offset = 15 + button.getIconTextGap() * 2;
		
		/* Set color. */
		graphics2D.setColor(button.getForeground());
		
		/* Draw text. */
		graphics2D.drawString(
			button.getText(), offset,
			h / 2 + (metrics.getAscent() - metrics.getDescent()) / 2
		);
	}
}
