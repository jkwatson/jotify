package nl.pascaldevink.jotify.gui.swing.plaf;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicPanelUI;

import nl.pascaldevink.jotify.gui.swing.components.JotifyPopularityIndicator;

public class JotifyPopularityIndicatorUI extends BasicPanelUI {
	public void paint(Graphics graphics, JComponent component){
		/* Get button and 2D graphics. */
		JotifyPopularityIndicator indicator  = (JotifyPopularityIndicator)component;
		Rectangle                 bounds     = indicator.getBounds();
		Graphics2D                graphics2D = (Graphics2D)graphics;
		int                       bars       = (bounds.width - 10) / 3;
		int                       height     = 8;
		
		/* Get colors. */
		Color foreground1 = indicator.getForeground();
		Color foreground2 = new Color(
			foreground1.getRed(), foreground1.getGreen(),
			foreground1.getBlue(), foreground1.getAlpha() / 2
		);
		
		/* Draw background */
		graphics2D.setColor(indicator.getBackground());
		graphics2D.fillRect(0, 0, bounds.width, bounds.height);
		
		/* Draw indicator. */
		for(int i = 0, x = 5; i < bars; i++, x += 3){
			if(i < bars * indicator.getPopularity()){
				graphics2D.setColor(foreground1);
				graphics2D.fillRect(x, bounds.height / 2 - height / 2 + 1, 2, height);
			}
			else{
				graphics2D.setColor(foreground2);
				graphics2D.fillRect(x, bounds.height / 2 - height / 2 + 1, 2, height);
			}
		}
	}
}
