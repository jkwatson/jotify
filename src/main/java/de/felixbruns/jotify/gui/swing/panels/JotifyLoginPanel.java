package de.felixbruns.jotify.gui.swing.panels;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JPanel;

import de.felixbruns.jotify.gui.JotifyApplication;

@SuppressWarnings("serial")
public class JotifyLoginPanel extends JPanel {
	private Color from;
	private Color to;
	private Image logo;
	
	public JotifyLoginPanel(){
		this(
			new Color(0, 116, 204), new Color(0, 75, 204),
			JotifyApplication.class.getResource("images/logo.png")
		);
	}
	
	public JotifyLoginPanel(Color from, Color to, String string){
		this(from, to, new ImageIcon(string).getImage());
	}
	
	public JotifyLoginPanel(Color from, Color to, URL url){
		this(from, to, new ImageIcon(url).getImage());
	}
	
	public JotifyLoginPanel(Color from, Color to, Image logo){
		/* Set gradient colors. */
		this.from = from;
		this.to   = to;
		
		/* Set image. */
		this.logo = logo;
	}
	
	protected void paintComponent(Graphics graphics){
		/* Get bounds and 2D graphics. */
		Rectangle  bounds     = this.getBounds();
		Graphics2D graphics2D = (Graphics2D)graphics;
		
		/* Enable anti-aliasing. */
		graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		/* Draw vertical rounded gradient. */
		graphics2D.setPaint(new GradientPaint(0, 0, this.from, 0, bounds.height, this.to));
		graphics2D.fillRoundRect(0, 0, bounds.width, bounds.height, 15, 15);
		
		/* Draw logo. */
		graphics2D.drawImage(this.logo, bounds.width / 2 - this.logo.getWidth(null) / 2, 20, null);
	}
}
