package nl.pascaldevink.jotify.gui.swing.plaf;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;

import nl.pascaldevink.jotify.gui.swing.components.JotifySlider;

public class JotifySliderUI extends BasicSliderUI {
	private Image imageNormal;
	private Image imageProgress;
	private Image imageDisabled;
	private Image imageThumbNormal;
	private Image imageThumbPressed;
	
	public JotifySliderUI(JSlider slider){
		super(slider);
		
		/* Load slider images (10x10 and 9x9 pixels). */
		this.imageNormal       = new ImageIcon(JotifySliderUI.class.getResource("images/slider.png")).getImage();
		this.imageProgress     = new ImageIcon(JotifySliderUI.class.getResource("images/slider_progress.png")).getImage();
		this.imageDisabled     = new ImageIcon(JotifySliderUI.class.getResource("images/slider_disabled.png")).getImage();
		this.imageThumbNormal  = new ImageIcon(JotifySliderUI.class.getResource("images/slider_thumb.png")).getImage();
		this.imageThumbPressed = new ImageIcon(JotifySliderUI.class.getResource("images/slider_thumb_pressed.png")).getImage();
	}
	
	public void paint(Graphics graphics, JComponent component){
		/* Get bounds (width & height), button and 2D graphics. */
		Rectangle    bounds     = component.getBounds();
		JotifySlider slider     = (JotifySlider)component;
		Graphics2D   graphics2D = (Graphics2D)graphics;
		int          w          = bounds.width;
		int          h          = bounds.height;
		int          y          = h / 2 - 10 / 2;
		
		/* Select button image based on status. */
		if(!slider.isEnabled()){
			/* Draw track (cut out slices). */
			graphics2D.drawImage(this.imageDisabled,     0, y,     5, y + 10, 0, 0,  5, 10, null); /* left */
			graphics2D.drawImage(this.imageDisabled, w - 5, y,     w, y + 10, 5, 0, 10, 10, null); /* right */
			graphics2D.drawImage(this.imageDisabled,     5, y, w - 5, y + 10, 4, 0,  6, 10, null); /* center */
		}
		else{
			float progress = (float)slider.getValue() / (float)(slider.getMaximum() - slider.getMinimum());
			
			/* Draw left track end (cut out slice). */
			if(slider.getDrawProgress() && progress > 0.0f){
				graphics2D.drawImage(this.imageProgress, 0, y, 5, y + 10, 0, 0,  5, 10, null); /* left */
			}
			else{
				graphics2D.drawImage(this.imageNormal, 0, y, 5, y + 10, 0, 0,  5, 10, null); /* left */
			}
			
			/* Draw right track end (cut out slice). */
			if(!slider.getDrawProgress() || progress < 100.0f){
				graphics2D.drawImage(this.imageNormal, w - 5, y, w, y + 10, 5, 0, 10, 10, null); /* right */
			}
			else{
				graphics2D.drawImage(this.imageProgress, w - 5, y, w, y + 10, 5, 0, 10, 10, null); /* right */
			}
			
			/* Draw track and progress (cut out slices). */
			if(slider.getDrawProgress()){
				graphics2D.drawImage(this.imageProgress, 5, y, (int)(5 + (w - 10) * progress), y + 10, 4, 0, 6, 10, null); /* center */
				graphics2D.drawImage(this.imageNormal, (int)(5 + (w - 10) * progress), y, w - 5, y + 10, 4, 0, 6, 10, null); /* center */
			}
			else{
				graphics2D.drawImage(this.imageNormal, 5, y, w - 5, y + 10, 4, 0, 6, 10, null); /* center */
			}
			
			/* Draw thumb. */
			if(slider.getValueIsAdjusting()){
				graphics2D.drawImage(this.imageThumbPressed, 1 + (int)((w - 10) * progress), y, null);
			}
			else{
				graphics2D.drawImage(this.imageThumbNormal, 1 + (int)((w - 10) * progress), y, null);
			}
		}
	}
}
