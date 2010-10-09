package de.felixbruns.jotify.gui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.JotifyApplication;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.swing.panels.JotifyContentPanel;
import de.felixbruns.jotify.gui.swing.panels.JotifyControlPanel;
import de.felixbruns.jotify.gui.swing.panels.JotifySearchPanel;
import de.felixbruns.jotify.gui.swing.panels.JotifySidePanel;

@SuppressWarnings("serial")
public class JotifyFrame extends JFrame {
	private JPanel             panel;
	public  JotifySearchPanel  searchPanel;
	public  JotifySidePanel    sidePanel;
	public  JotifyContentPanel contentPanel;
	public  JotifyControlPanel controlPanel;
	private JotifyBroadcast    broadcast;
	
	public JotifyFrame(final Jotify jotify){
		this.broadcast = JotifyBroadcast.getInstance();
		
		/* Load icons. */
		List<Image> icons = new ArrayList<Image>();
		
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_16.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_32.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_64.png")).getImage());
		icons.add(new ImageIcon(JotifyApplication.class.getResource("images/icon_128.png")).getImage());
		
		/* Set some options. */
		this.setTitle("Jotify");
		this.setIconImages(icons);
		this.setBounds(100, 100, 1000, 600);
		this.setMinimumSize(new Dimension(640, 480));
		this.setDefaultCloseOperation(JDialog.EXIT_ON_CLOSE);
		
		/* Add main panel. */
		this.panel = new JPanel();
		this.panel.setBackground(new Color(55, 55, 55));
		this.getContentPane().add(this.panel);
		
		/* Set layout. */
		this.panel.setLayout(new BorderLayout());
		
		/* Create and add search panel. */
		this.searchPanel = new JotifySearchPanel(jotify);
		this.panel.add(this.searchPanel, BorderLayout.NORTH);
		
		/* Create and add side panel in a scroll pane. */
		this.sidePanel = new JotifySidePanel(jotify);
		this.sidePanel.setPreferredSize(new Dimension(180, 300));
		
		JScrollPane sidePane = new JScrollPane(
			this.sidePanel,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
		);
		sidePane.setOpaque(false);
		sidePane.getViewport().setOpaque(false);
		sidePane.setBorder(BorderFactory.createEmptyBorder());
        
		this.panel.add(sidePane, BorderLayout.WEST);

		/* Create and add content panel. */
		this.contentPanel = new JotifyContentPanel(jotify);
		this.panel.add(this.contentPanel, BorderLayout.CENTER);
		
		/* Create and add control panel. */
		this.controlPanel = new JotifyControlPanel();
		this.panel.add(this.controlPanel, BorderLayout.SOUTH);
		
		/* Add listeners to lists. */
		this.broadcast.addPlaylistListener(this.sidePanel);
		this.broadcast.addPlaylistListener(this.contentPanel);
		this.broadcast.addQueueListener(this.contentPanel);
		this.broadcast.addQueueListener(this.sidePanel);
		this.broadcast.addQueueListener(this.controlPanel);
		this.broadcast.addSearchListener(this.sidePanel);
		this.broadcast.addSearchListener(this.contentPanel);
		this.broadcast.addSearchListener(this.searchPanel);
		this.broadcast.addBrowseListener(this.contentPanel);
		this.broadcast.addPlayerListener(this.sidePanel);
		this.broadcast.addPlayerListener(this.controlPanel);
		this.broadcast.addClearSelectionListener(this.sidePanel);
	}
}
