package de.felixbruns.jotify.gui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.Timer;

import de.felixbruns.jotify.media.Track;

@SuppressWarnings("serial")
public class JotifyCurrentlyPlayingLabel extends JPanel {
	private int   titleOffset;
	private int   artistOffset;
	private int   titleStep;
	private int   artistStep;
	private int   titleWait;
	private int   artistWait;
	private Timer timer;
	private Track track;
	
	public JotifyCurrentlyPlayingLabel(){
		/* Set variables. */
		this.titleOffset  =  5;
		this.artistOffset =  5;
		this.titleStep    = -1;
		this.artistStep   = -1;
		this.titleWait    =  0;
		this.artistWait   =  0;
		this.track        = null;
		
		/* Set size and border. */
		this.setPreferredSize(new Dimension(180, 50));
		
		/* Create timers for scrolling text. */
		this.timer = new Timer(25, new ActionListener(){
			public void actionPerformed(ActionEvent e){
				repaint();
			}
		});
	}
	
	public void setTrack(Track track){
		this.track = track;
		
		this.repaint();
		
		if(this.track != null && !this.timer.isRunning()){
			this.timer.start();
		}
		else if(this.timer.isRunning()){
			this.timer.stop();
		}
	}
	
	protected void paintComponent(Graphics graphics){
		/* Get bounds and 2D graphics. */
		Rectangle   bounds     = this.getBounds();
		Graphics2D  graphics2D = (Graphics2D)graphics;
		FontMetrics metrics;
		
		/* Draw vertical rounded gradient. */
		graphics2D.setPaint(new GradientPaint(
			0,             0, new Color(184, 184, 184),
			0, bounds.height, new Color(164, 164, 164)
		));
		graphics2D.fillRect(0, 0, bounds.width, bounds.height);
		graphics2D.setPaint(null);
		
		/* Stop if track is not set. */
		if(this.track == null){
			return;
		}
		
		/* Set anti-aliasing to on. */
		graphics2D.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON
		);
		
		/* Set color. */
		graphics2D.setColor(this.getForeground());
		
		/* Set font for title and get metrics. */
		graphics2D.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
		metrics = graphics2D.getFontMetrics();
		
		/* Draw title (scrolling). */
		if(bounds.width - 10 < metrics.stringWidth(track.getTitle())){
			graphics2D.drawString(
				this.track.getTitle(),
				this.titleOffset,
				10 + metrics.getHeight() / 2
			);
			
			/* Change direction (important: move a bit, so we don't get locked!). */
			if(this.titleOffset + metrics.stringWidth(this.track.getTitle()) < bounds.width - 5 ||
				this.titleOffset > 5){
				this.titleStep   *= -1;
				this.titleWait    = 40; /* 40 * 25 ms = 1 second. */
				this.titleOffset += this.titleStep;
			}
			
			/* Set new scroll offset. */
			if(this.titleWait > 0){
				this.titleWait--;
			}
			else{
				this.titleOffset += this.titleStep;
			}
		}
		/* Draw title (centered). */
		else{
			graphics2D.drawString(
				this.track.getTitle(),
				bounds.width / 2 - metrics.stringWidth(this.track.getTitle()) / 2,
				10 + metrics.getHeight() / 2
			);
		}
		
		/* Set font for artist and get metrics. */
		graphics2D.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
		metrics = graphics2D.getFontMetrics();
		
		/* Build artist string. */
		String artistString =
			((this.track.getArtist() != null)?this.track.getArtist().getName():"Unknown") +
			" (" +
				((this.track.getAlbum() != null)?this.track.getAlbum().getName():"Unknown")
			+ ")";
		
		/* Draw artist (scrolling). */
		if(bounds.width - 10 < metrics.stringWidth(artistString)){
			graphics2D.drawString(
				artistString,
				this.artistOffset,
				30 + metrics.getHeight() / 2
			);
			
			/* Change direction (important: move a bit, so we don't get locked!). */
			if(this.artistOffset + metrics.stringWidth(artistString) < bounds.width - 5 ||
				this.artistOffset > 5){
				this.artistStep   *= -1;
				this.artistWait    = 40; /* 40 * 25 ms = 1 second. */
				this.artistOffset += this.artistStep;
			}
			
			/* Set new scroll offset. */
			if(this.artistWait > 0){
				this.artistWait--;
			}
			else{
				this.artistOffset += this.artistStep;
			}
		}
		/* Draw title (centered). */
		else{
			graphics2D.drawString(
				artistString,
				bounds.width / 2 - metrics.stringWidth(artistString) / 2,
				30 + metrics.getHeight() / 2
			);
		}
	}
}
