package nl.pascaldevink.jotify.gui.swing.plaf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicPanelUI;

import nl.pascaldevink.jotify.gui.swing.components.JotifyVolumeIndicator;
import de.felixbruns.jotify.util.MathUtilities;

public class JotifyVolumeIndicatorUI extends BasicPanelUI {
	public void paint(Graphics graphics, JComponent component){
		/* Get button and 2D graphics. */
		JotifyVolumeIndicator indicator  = (JotifyVolumeIndicator)component;
		Graphics2D            graphics2D = (Graphics2D)graphics;
		
		/* Enable anti-aliasing. */
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		/* Get and set colors. */
		Color c0 = indicator.getForeground();
		Color c1 = new Color(
			c0.getRed(), c0.getGreen(), c0.getBlue(),
			(int)(255 * ((indicator.getValue() < 0.3f)?
				MathUtilities.map(indicator.getValue(), 0.0f, 0.3f, 0.15f, 1.0f):1.0f
			))
		);
		Color c2 = new Color(
			c0.getRed(), c0.getGreen(), c0.getBlue(),
			(int)(255 * ((indicator.getValue() > 0.3f)?
				MathUtilities.map(indicator.getValue(), 0.3f, 1.0f, 0.15f, 1.0f):0.15f
			))
		);
		
		/* Set stroke. */
		graphics2D.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		
		/* Indicator size. */
		int w = 20;
		int h = 25;
		
		/* Draw indicator. */
		graphics2D.setColor(c1);
		graphics2D.drawArc(-w / 2 + 3, 4, w - 8, h - 8, -45, 90);
		graphics2D.setColor(c2);
		graphics2D.drawArc(-w / 2 + 3, 0, w - 3, h    , -45, 90);
	}
}
