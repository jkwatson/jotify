package de.felixbruns.jotify.gui.swing.components;

import java.util.Set;
import java.util.HashSet;
import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;

@SuppressWarnings("serial")
public class JotifyList extends JList implements PlaylistListener {
	private ImageIcon searchIcon;
	private ImageIcon playlistIcon;
	private Set<Playlist> playlists = new HashSet<Playlist>();
	
	public JotifyList(){
		/* Use default table model. */
		this.setModel(new DefaultListModel());
		
		/* Load icons. */
		this.searchIcon   = new ImageIcon(JotifyList.class.getResource("images/search_icon.png"));
		this.playlistIcon = new ImageIcon(JotifyList.class.getResource("images/playlist_icon.png"));
		
		/* Only allow single selections. */
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		/* This is always a vertical list and aligned left. */
		this.setLayoutOrientation(JList.VERTICAL);
		this.setAlignmentX(LEFT_ALIGNMENT);
		
		/* Don't draw a background, set foreground and selection color. */
		this.setOpaque(false);
		this.setForeground(Color.WHITE);
		this.setSelectionForeground(Color.LIGHT_GRAY);
		
		JotifyBroadcast.getInstance().addPlaylistListener(this);
		
		/* Set a custom cell renderer. */
		this.setCellRenderer(new DefaultListCellRenderer()  {
			public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus){
				/* Get label from superclass. */
				JLabel label = (JLabel)super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus
				);
				
				/* Set icon depending on value. */
				if(value instanceof Result){
					label.setIcon(searchIcon);
				}
				else if(value instanceof Playlist){
					label.setIcon(playlistIcon);
				}
				else if(value instanceof JotifyListElement){
					label.setIcon(((JotifyListElement)value).getIcon());
				}
				
				/* Set text depending on value. */
				if(value instanceof Result){
					label.setText(((Result)value).getQuery());
				}
				else if(value instanceof Playlist){				  
				  if (JotifyList.this.playlists.contains((Playlist)value)) {				  
						label.setEnabled(true);
						label.setText(((Playlist)value).getName());
					}
					else{
						label.setEnabled(false);
						label.setText("Loading...");
					}
				}
				else if(value instanceof JotifyListElement){
					label.setText(((JotifyListElement)value).getText());
				}
				
				/* Label also needs to be transparent! And we want an empty border. */
				label.setOpaque(false);
				label.setBorder(new EmptyBorder(2, 2, 2, 2));
				
				/* Return cell renderer component. */
				return label;
			}
		});
	}
	
	/* Append an element to the list. */
	public void appendElement(Object element){
		DefaultListModel model = (DefaultListModel)this.getModel();
		
		/* Check if list already contains this object. */
		if(model.contains(element)){
			model.removeElement(element);
		}
		
		/* Add element at end index. */
		model.add(model.getSize(), element);
	}
	
	/* Prepend an element to the list. */
	public void prependElement(Object element){
		DefaultListModel model = (DefaultListModel)this.getModel();
		
		/* Check if list already contains this object. */
		if(model.contains(element)){
			model.removeElement(element);
		}
		
		/* Add element at index 0. */
		model.add(0, element);
	}
	
	/* Update an element to the list. */
	public void updateElement(Object element){
		DefaultListModel model = (DefaultListModel)this.getModel();
		
		if(model.contains(element)){
			int index = model.indexOf(element);
			
			model.removeElement(element);
			
			model.add(index, element);
		}
	}
	
	/* Remove an element from the list. */
	public void removeElement(Object element){
		DefaultListModel model = (DefaultListModel)this.getModel();
		
		if(model.contains(element)){
			model.removeElement(element);
		}
	}
	
	public void appendElement(ImageIcon icon, String text){
		this.appendElement(new JotifyListElement(icon, text));
	}
	
	public void playlistAdded(Playlist playlist) { }
	public void playlistUpdated(Playlist playlist) { 
	  playlists.add(playlist);
	} 
	public void playlistRemoved(Playlist playlist) { 
	  playlists.remove(playlist);
	}
	public void playlistSelected(Playlist playlist) { }
  
	
	public class JotifyListElement {
		private ImageIcon icon;
		private String    text;
		
		public JotifyListElement(ImageIcon icon, String text){
			this.icon = icon;
			this.text = text;
		}
		
		public ImageIcon getIcon(){
			return this.icon;
		}
		
		public String getText(){
			return this.text;
		}
	}
}
