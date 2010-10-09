package de.felixbruns.jotify.gui.swing.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import de.felixbruns.jotify.gui.JotifyPlaybackQueue;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlayerListener;
import de.felixbruns.jotify.gui.listeners.QueueListener;
import de.felixbruns.jotify.gui.swing.components.JotifyNextButton;
import de.felixbruns.jotify.gui.swing.components.JotifyPlayButton;
import de.felixbruns.jotify.gui.swing.components.JotifyPreviousButton;
import de.felixbruns.jotify.gui.swing.components.JotifySlider;
import de.felixbruns.jotify.gui.swing.components.JotifyVolumeIndicator;
import de.felixbruns.jotify.gui.util.TimeFormatter;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.util.MathUtilities;

@SuppressWarnings("serial")
public class JotifyControlPanel extends JPanel implements QueueListener, PlayerListener {
	private JotifyBroadcast       broadcast;
	private JPanel                westPanel;
	private JPanel                eastPanel;
	private JButton               previousButton;
	private JotifyPlayButton      playButton;
	private JButton               nextButton;
	private JSlider               volumeSlider;
	private JotifyVolumeIndicator volumeIndicator;
	private JSlider               positionSlider;
	private JLabel                positionLabel;
	private JLabel                remainingLabel;
	private JSeparator            separator;
	private Track                 track;
	
	private boolean wasAdjusting = false;
	
	public JotifyControlPanel(){
		this.broadcast = JotifyBroadcast.getInstance();
		
		/* Use border layout. */
		this.setLayout(new BorderLayout());
		
		/* Create and add panels. */
		this.westPanel   = new JPanel();
		this.eastPanel   = new JPanel();
		this.westPanel.setOpaque(false);
		this.eastPanel.setOpaque(false);
		this.add(this.westPanel, BorderLayout.WEST);
		this.add(this.eastPanel, BorderLayout.EAST);
				
		/* Create and add buttons to west panel. */
		this.previousButton = new JotifyPreviousButton();
		this.previousButton.setEnabled(false);
		this.previousButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				broadcast.fireControlPrevious();
			}
		});
		this.westPanel.add(this.previousButton);
		
		this.playButton = new JotifyPlayButton();
		this.playButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if(playButton.isPause()){
					broadcast.fireControlPause();
				}
				else{
					broadcast.fireControlPlay();
				}
			}
		});
		this.westPanel.add(this.playButton);
		
		this.nextButton = new JotifyNextButton();
		this.nextButton.setEnabled(false);
		this.nextButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				broadcast.fireControlNext();
			}
		});
		this.westPanel.add(this.nextButton);
		
		/* Add a spacer. */
		this.westPanel.add(Box.createHorizontalStrut(10));
		
		/* Add volume slider and volume indicator to west panel. */
		this.volumeSlider = new JotifySlider(false);
		this.volumeSlider.setPreferredSize(new Dimension(80, 10));
		this.volumeSlider.setValue(this.volumeSlider.getMaximum());
		this.volumeSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				/* Get value in right scale. */
				float value = MathUtilities.map(
					volumeSlider.getValue(),
					volumeSlider.getMinimum(),
					volumeSlider.getMaximum(),
					0.0f, 1.0f
				);
				
				/* Set volume. */
				broadcast.fireControlVolume(value);
				
				/* Get value in right scale. */
				value = MathUtilities.map(
					volumeSlider.getValue(),
					volumeSlider.getMinimum(),
					volumeSlider.getMaximum(),
					volumeIndicator.getMinimum(),
					volumeIndicator.getMaximum()
				);
				
				/* Update volume indicator. */
				volumeIndicator.setVolume(value);
			}
		});
		this.westPanel.add(this.volumeSlider);
		
		this.volumeIndicator = new JotifyVolumeIndicator();
		this.volumeIndicator.setVolume(this.volumeIndicator.getMaximum());
		this.westPanel.add(this.volumeIndicator);
		
		/* Add a spacer. */
		this.westPanel.add(Box.createHorizontalStrut(5));
		
		/* Add a vertical separator. */
		this.separator = new JSeparator(JSeparator.VERTICAL);
		this.separator.setPreferredSize(new Dimension(2, 30));
		this.separator.setForeground(new Color(58, 58, 58));
		this.separator.setBackground(new Color(97, 97, 97));
		this.westPanel.add(this.separator);
		
		/* Add a spacer. */
		this.westPanel.add(Box.createHorizontalStrut(5));
		
		/* Add position label. */
		this.positionLabel = new JLabel();
		this.positionLabel.setOpaque(false);
		this.positionLabel.setForeground(Color.WHITE);
		this.positionLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		this.positionLabel.setPreferredSize(new Dimension(30, 30));
		this.westPanel.add(this.positionLabel);
		
		/* Add position slider to center panel. */
		this.positionSlider = new JotifySlider();
		this.positionSlider.setPreferredSize(new Dimension(400, 30));
		this.positionSlider.setMinimum(0);
		this.positionSlider.setMaximum(1000);
		this.positionSlider.setValue(0);
		this.positionSlider.addChangeListener(new ChangeListener(){
			public void stateChanged(ChangeEvent e){
				if(wasAdjusting && !positionSlider.getValueIsAdjusting()){
					JotifyBroadcast.getInstance().fireControlSeek(positionSlider.getValue() / 1000.0f);
				}
				
				wasAdjusting = positionSlider.getValueIsAdjusting();
			}
		});
		this.add(this.positionSlider, BorderLayout.CENTER);
		
		/* Add remaining label. */
		this.remainingLabel = new JLabel();
		this.remainingLabel.setOpaque(false);
		this.remainingLabel.setForeground(Color.WHITE);
		this.remainingLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
		this.remainingLabel.setPreferredSize(new Dimension(35, 30));
		this.eastPanel.add(this.remainingLabel);
		
		/* Add a spacer. */
		this.eastPanel.add(Box.createHorizontalStrut(5));
		
		/* Add a vertical separator. */
		this.separator = new JSeparator(JSeparator.VERTICAL);
		this.separator.setPreferredSize(new Dimension(2, 30));
		this.separator.setForeground(new Color(58, 58, 58));
		this.separator.setBackground(new Color(97, 97, 97));
		this.eastPanel.add(this.separator);
		
		/* TODO: Add shuffle and repeat buttons. */
	}
	
	protected void paintComponent(Graphics graphics){
		/* Get bounds and 2D graphics. */
		Rectangle  bounds     = this.getBounds();
		Graphics2D graphics2D = (Graphics2D)graphics;
		
		/* Draw vertical gradient. */
		graphics2D.setPaint(new GradientPaint(
			0,             0, new Color(96, 96, 96),
			0, bounds.height, new Color(70, 70, 70)
		));
		graphics2D.fillRect(0, 0, bounds.width, bounds.height);
	}
	
	public void queueSelected(JotifyPlaybackQueue queue){
		
	}
	
	public void queueUpdated(JotifyPlaybackQueue queue){
		this.nextButton.setEnabled(queue.hasNext());
		this.previousButton.setEnabled(queue.hasPrevious());
	}
	
	public void playerTrackChanged(Track track){
		this.track = track;
	}
	
	public void playerStatusChanged(Status status){
		this.playButton.setPause(status.equals(Status.PLAY));
	}
	
	public void playerPositionChanged(int ms){
		/* Return if nothing is playing. */
		if(this.track == null || this.positionSlider.getValueIsAdjusting()){
			return;
		}
		
		/* Get playback position and calculate progress. */
		int position  = ms / 1000; /* ms -> s */
		int duration  = this.track.getLength() / 1000; /* ms -> s */
		int remaining = duration - position;
		int progress  = (int)((float)ms / (float)duration); /* slider range is 0 -> 1000. */
		
		/* Update labels and slider. */
		this.positionLabel.setText(TimeFormatter.formatSeconds(position));
		this.remainingLabel.setText(TimeFormatter.formatRemainingSeconds(remaining));
		
		/* Update slider. */
		this.positionSlider.setValue(progress);
	}
}
