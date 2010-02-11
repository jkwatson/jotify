package de.felixbruns.jotify.gui.swing.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.swing.components.JotifyCurrentlyPlayingLabel;
import de.felixbruns.jotify.media.Track;

@SuppressWarnings("serial")
public class JotifyCurrentlyPlayingPanel extends JPanel {
	private JotifyCurrentlyPlayingLabel currentlyPlayingLabel;
	private JLabel                      coverLabel;
	
	private final Jotify jotify;
	
	public JotifyCurrentlyPlayingPanel(final Jotify jotify){
		this.jotify = jotify;
		
		/* Flow content to the left. */
		this.setLayout(new BorderLayout());
		
		/* Create currently playing label (actually a panel!). */
		this.currentlyPlayingLabel = new JotifyCurrentlyPlayingLabel();
		this.currentlyPlayingLabel.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				coverLabel.setVisible(!coverLabel.isVisible());
			}
		});
		this.add(this.currentlyPlayingLabel, BorderLayout.NORTH);
		
		/* Create label for cover. */
		this.coverLabel = new JLabel();
		this.coverLabel.setPreferredSize(new Dimension(180, 180));
		this.coverLabel.setVisible(false);
		this.add(this.coverLabel, BorderLayout.CENTER);
		
		/* Make transparent. */
		this.setOpaque(false);
	}
	
	public void setTrack(final Track track){
		this.currentlyPlayingLabel.setTrack(track);
		
		new Thread("Cover-Loading-Thread"){
			public void run(){
				String cover = track.getCover();
				
				if(cover != null){
					try{
						Image image = jotify.image(cover);
						
						coverLabel.setIcon(new ImageIcon(image.getScaledInstance(180, 180, Image.SCALE_SMOOTH)));
					}
					catch (TimeoutException e){
						e.printStackTrace();
					}
				}
				else{
					coverLabel.setIcon(null);
				}
			}
		}.start();
	}
}
