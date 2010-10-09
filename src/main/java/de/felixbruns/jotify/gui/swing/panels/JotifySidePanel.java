package de.felixbruns.jotify.gui.swing.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.JotifyPlaybackQueue;
import de.felixbruns.jotify.gui.listeners.ClearSelectionListener;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlayerListener;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.gui.listeners.QueueListener;
import de.felixbruns.jotify.gui.listeners.SearchListener;
import de.felixbruns.jotify.gui.swing.JotifyPreferencesDialog;
import de.felixbruns.jotify.gui.swing.components.JotifyList;
import de.felixbruns.jotify.gui.swing.components.JotifyList.JotifyListElement;
import de.felixbruns.jotify.gui.swing.dnd.TrackListTransferable;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;

@SuppressWarnings("serial")
public class JotifySidePanel extends JPanel implements PlaylistListener, QueueListener, SearchListener, PlayerListener, ClearSelectionListener {
	private JotifyBroadcast             broadcast;
	private JotifyList                  menu;
	private JotifyList                  list;
	private JotifyCurrentlyPlayingPanel info;
	private JotifyPlaybackQueue         queue;
	private int                         lastMenuIndex = -1;
	
	public JotifySidePanel(final Jotify jotify){
		/* Set broadcast object. */
		this.broadcast = JotifyBroadcast.getInstance();
		
		/* Flow content to the left. */
		this.setLayout(new BorderLayout());
		
		/* Set background and foreground colors. */
		this.setBackground(new Color(71, 71, 71));
		this.setForeground(Color.WHITE);
		
		this.queue = new JotifyPlaybackQueue();
		
		/* Create and add list to panel. */
		this.menu = new JotifyList();
		this.menu.setBorder(new EmptyBorder(5, 0, 5, 0));
		this.menu.appendElement(
			new ImageIcon(JotifyList.class.getResource("images/preferences_icon.png")),
			new ImageIcon(JotifyList.class.getResource("images/preferences_icon_selected.png")),
			"Preferences"
		);
		this.menu.appendElement(
			new ImageIcon(JotifyList.class.getResource("images/preferences_icon.png")),
			new ImageIcon(JotifyList.class.getResource("images/preferences_icon_selected.png")),
			"Queue"
		);
		this.menu.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				/* Get selected object from list. */
				Object value = menu.getSelectedValue();
				
				if(value instanceof JotifyListElement){
					JotifyListElement element = (JotifyListElement)value;
					
					if(element.getText().equals("Queue")){
						broadcast.fireQueueSelected(queue);
						
						list.clearSelection();
					}
					else if(element.getText().equals("Preferences")){
						menu.repaint();
						
						JotifyPreferencesDialog.showDialog();
						
						if(lastMenuIndex > 0){
							menu.setSelectedIndex(lastMenuIndex);
						}
						else{
							menu.clearSelection();
						}
					}
				}
				
				lastMenuIndex = menu.getSelectedIndex();
			}
		});
		this.add(this.menu, BorderLayout.NORTH);
		
		/* Create and add list to panel. */
		this.list = new JotifyList();
		this.list.setBorder(new EmptyBorder(5, 0, 5, 0));
		this.list.addListSelectionListener(new ListSelectionListener(){
			public void valueChanged(ListSelectionEvent e){
				/* Get selected object from list. */
				Object value = list.getSelectedValue();
				
				/* Show result or playlist in content panel. */
				if(value instanceof Result){
					/* Update search query field. */
					broadcast.fireSearchResultSelected((Result)value);
					
					menu.clearSelection();
				}
				else if(value instanceof Playlist){
					broadcast.firePlaylistSelected((Playlist)value);
					
					menu.clearSelection();
				}
			}
		});
		this.list.setDragEnabled(true);
		this.list.setDropMode(DropMode.ON);
		this.list.setTransferHandler(new TransferHandler(){
			public boolean canImport(TransferHandler.TransferSupport info){
				if(!info.isDataFlavorSupported(TrackListTransferable.TRACKLIST_FLAVOR)){
					return false;
				}
				
				final JList.DropLocation dropLocation = (JList.DropLocation)info.getDropLocation();
				
				return !dropLocation.isInsert() && dropLocation.getIndex() != -1;
			}
			
			public boolean importData(TransferHandler.TransferSupport info){
				if(!canImport(info) || !info.isDrop()){
					return false;
				}
				
				final JList              list         = (JList)info.getComponent();
				final JList.DropLocation dropLocation = (JList.DropLocation)info.getDropLocation();
				final Playlist           playlist     = (Playlist)list.getModel().getElementAt(dropLocation.getIndex());
				
				/* TODO: Actually send the playlist change to the server. */
				try{
					final List<?> trackList = (List<?>)info.getTransferable().getTransferData(TrackListTransferable.TRACKLIST_FLAVOR);
					
					for(Object o : trackList){
						playlist.getTracks().add((Track)o);
					}
					
					broadcast.firePlaylistUpdated(playlist);
					
					return true;
				}
				catch(Exception e){}
				
				return false;
			}
		});
		this.add(this.list, BorderLayout.CENTER);
		
		this.info = new JotifyCurrentlyPlayingPanel(jotify);
		this.add(this.info, BorderLayout.SOUTH);
	}
	
	/* Sets currently playing track. */
	public void setTrack(Track track){
		this.info.setTrack(track);
	}
	
	public void playlistAdded(Playlist playlist){
		this.list.appendElement(playlist);
	}
	
	public void playlistRemoved(Playlist playlist){
		this.list.removeElement(playlist);
	}
	
	public void playlistUpdated(Playlist playlist){
		this.list.updateElement(playlist);
		
		this.repaint();
	}
	
	public void playlistSelected(Playlist playlist){
		/* Do nothing. */
	}
	
	public void queueSelected(JotifyPlaybackQueue queue){
		/* Do nothing. */
	}
	
	public void queueUpdated(JotifyPlaybackQueue queue){
		this.queue = queue;
	}
	
	public void searchResultReceived(Result result){
		this.list.prependElement(result);
		
		/* Automatically select result after addition. */
		this.list.setSelectedIndex(0);
	}
	
	public void searchResultSelected(Result result){
		/* Do nothing. */
	}
	
	public void playerTrackChanged(Track track){
		this.info.setTrack(track);
	}
	
	public void playerStatusChanged(Status status){
		/* Do nothing. */
	}
	
	public void playerPositionChanged(int position){
		/* Do nothing. */
	}
	
	public void clearSelection(){
		this.menu.clearSelection();
		this.list.clearSelection();
	}
}
