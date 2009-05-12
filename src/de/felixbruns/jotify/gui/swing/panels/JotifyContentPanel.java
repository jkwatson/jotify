package de.felixbruns.jotify.gui.swing.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import de.felixbruns.jotify.JotifyPool;
import de.felixbruns.jotify.gui.JotifyPlaybackQueue;
import de.felixbruns.jotify.gui.listeners.BrowseListener;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.gui.listeners.QueueListener;
import de.felixbruns.jotify.gui.listeners.SearchListener;
import de.felixbruns.jotify.gui.swing.components.JotifyTable;
import de.felixbruns.jotify.gui.swing.components.JotifyTableModel;

import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;

@SuppressWarnings("serial")
public class JotifyContentPanel extends JPanel implements HyperlinkListener, PlaylistListener, QueueListener, SearchListener, BrowseListener {
	private JotifyBroadcast         broadcast;
	private JEditorPane             infoPane;
	private JScrollPane             scrollPane;
	private JTable                  table;
	private JotifyTableModel<Track> tableModel;
	
	private boolean isShowingQueue;
	
	public JotifyContentPanel(){
		this.broadcast = JotifyBroadcast.getInstance();
		
		this.isShowingQueue = false;
		
		/* Set layout to use. */
		this.setLayout(new BorderLayout());
		
		this.setBackground(new Color(55, 55, 55));
		
		this.infoPane = new JEditorPane("text/html", "");
		this.infoPane.setEditable(false);
		this.infoPane.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
		this.infoPane.setFocusable(false);
		this.infoPane.addHyperlinkListener(this);
		this.infoPane.setOpaque(false);
		this.add(this.infoPane, BorderLayout.NORTH);
		
		/* Create table model. */
		this.tableModel = new JotifyTableModel<Track>();
		
		/* Create table. */
		this.table = new JotifyTable(this.tableModel);
		this.table.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if(e.getClickCount() == 2){
					List<Track> tracks = tableModel.getSubList(
						table.getSelectedRow(), table.getRowCount()
					);
						
					broadcast.fireControlSelect(tracks);
					broadcast.fireControlPlay();
				}
			}
			
			public void mousePressed(MouseEvent e){
				this.mousePopup(e);
			}
			
			public void mouseReleased(MouseEvent e){
				this.mousePopup(e);
			}
			
			public void mousePopup(MouseEvent e){
				if(e.isPopupTrigger()){
					JPopupMenu contextMenu = new JPopupMenu();
					JMenuItem  playItem    = new JMenuItem("Play");
					JMenuItem  queueItem   = new JMenuItem("Queue");
					JMenuItem  browseItem  = new JMenuItem("Browse Album");
					JMenuItem  uriItem     = new JMenuItem("Copy Spotify URI");
					JMenuItem  linkItem    = new JMenuItem("Copy HTTP Link");
					
					table.getSelectionModel().setSelectionInterval(
						table.rowAtPoint(e.getPoint()),
						table.rowAtPoint(e.getPoint())
					);
					
					playItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							List<Track> tracks = tableModel.getSubList(
								table.getSelectedRow(), table.getRowCount()
							);
							
							broadcast.fireControlSelect(tracks);
							broadcast.fireControlPlay();
						}
					});
					
					queueItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							
							broadcast.fireControlQueue(track);
						}
					});
					
					browseItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							Album album = JotifyPool.getInstance().browse(track.getAlbum());
							
							broadcast.fireClearSelection();
							broadcast.fireBrowsedAlbum(album);
						}
					});
					
					uriItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							
							StringSelection uri = new StringSelection(track.getURI());
							
							Toolkit.getDefaultToolkit().getSystemClipboard().setContents(uri, uri);
						}
					});
					
					linkItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							
							StringSelection uri = new StringSelection(track.getLink());
							
							Toolkit.getDefaultToolkit().getSystemClipboard().setContents(uri, uri);
						}
					});
					
					contextMenu.add(playItem);
					contextMenu.addSeparator();
					contextMenu.add(queueItem);
					contextMenu.addSeparator();
					contextMenu.add(browseItem);
					contextMenu.addSeparator();
					contextMenu.add(uriItem);
					contextMenu.add(linkItem);
					
					contextMenu.show(table, e.getX(), e.getY());
				}
			}
		});
		
		/* Create scoll pane. */
		this.scrollPane = new JScrollPane(this.table);
		this.scrollPane.setOpaque(false);
		this.scrollPane.getViewport().setOpaque(false);
		this.scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.add(this.scrollPane, BorderLayout.CENTER);
	}
	
	public void showAlbum(Album album){
		this.infoPane.setText(
			"<html>" +
			"	<table style=\"font: bold 12pt Dialog;\">" +
			"		<tr>" +
			"			<td valign=\"top\" style=\"width: 50%;\">" +
			"				<span style=\"color: #ffffff;\">" +
								album.getArtist().getName() + 
								" - " +
								album.getName() +
								" (" + album.getYear() + ")" +
							"</span>" +
			"			</td>" +
			"		</tr>" +
			"	</table>" +
			"</html>"
		);
		
		this.showTracks(album.getTracks());
	}
	
	public void showResult(Result result){
		Iterator<Artist> artists = result.getArtists().iterator();
		Iterator<Album>  albums  = result.getAlbums().iterator();
		
		String artistsHtml = "";
		String albumsHtml  = "";
		
		while(artists.hasNext()){
			Artist artist = artists.next();
			
			artistsHtml +=
				"<a style=\"text-decoration: none;\" href=\"artist:" + artist.getName() + "\">" + artist.getName() + "</a>" +
				(artists.hasNext()?" &bull; ":"");
		}
		
		while(albums.hasNext()){
			Album  album  = albums.next();
			Artist artist = album.getArtist();
			
			albumsHtml +=
				"<a style=\"text-decoration: none;\" href=\"album:" + album.getId() + "\">" + album.getName() + "</a>" +
				" <span style=\"color: #545454;\">by " +
				"<a style=\"text-decoration: none;\" href=\"artist:" + artist.getName() + "\">" + artist.getName() + "</a>" +
				"</span>" +
				(albums.hasNext()?" &bull; ":"");
		}
		
		this.infoPane.setText(
			"<html>" +
			"	<table style=\"font: bold 11pt Dialog;\">" +
			"		<tr>" +
			"			<td valign=\"top\" style=\"width: 50%;\">" +
			"				<span style=\"color: #ffffff;\">Artists:</span>" +
			"				<span style=\"color: #93b49f;\">(" + result.getTotalArtists() + ")</span>" +
			"				<span style=\"color: #7f7f7f;\">" + artistsHtml + "</span>" +
			"			</td>" +
			"			<td valign=\"top\" style=\"width: 50%;\">" +
			"				<span style=\"color: #ffffff;\">Albums:</span>" +
			"				<span style=\"color: #93b49f;\">(" + result.getTotalAlbums() + ")</span>" +
			"				<span style=\"color: #7f7f7f;\">" + albumsHtml + "</span>" +
			"			</td>" +
			"		</tr>" +
			"		<tr>" +
			"			<td valign=\"top\" style=\"width: 50%;\"></td>" +
			"			<td valign=\"top\" style=\"width: 50%;\">" +
			"				<span style=\"color: #ffffff;\">Tracks:</span>" +
			"				<span style=\"color: #93b49f;\">(" + result.getTotalTracks() + ")</span>" +
			"			</td>" +
			"		</tr>" +
			"	</table>" +
			"</html>"
		);
		
		this.showTracks(result.getTracks());
	}
	
	public void showPlaylist(Playlist playlist){
		this.infoPane.setText("");
		
		this.showTracks(playlist.getTracks());
		
		this.isShowingQueue = false;
	}
	
	public void showQueue(JotifyPlaybackQueue queue){
		List<Track> tracks = new ArrayList<Track>();
		
		tracks.addAll(queue.getQueue());
		tracks.addAll(queue.getTracks());
		
		if(tracks.isEmpty()){
			this.infoPane.setText(
				"<html><span style=\"font: bold 11pt Dialog; color: #ffffff;\">" +
				"There are no queued tracks!</span></html>"
			);
		}
		else{
			this.infoPane.setText("");
		}
		
		this.showTracks(tracks);
		
		this.isShowingQueue = true;
	}
	
	private void showTracks(List<Track> tracks){
		this.tableModel.removeAll();
		this.tableModel.addAll(tracks);
		
		this.isShowingQueue = false;
	}
	
	/* TODO: Do a browse query here, not a search. */
	public void hyperlinkUpdate(final HyperlinkEvent e){
		if(e.getEventType() == EventType.ACTIVATED){
			String[] parts = e.getDescription().split(":", 2);
			
			if(parts[0].equals("artist")){
				Result result = JotifyPool.getInstance().search(parts[1]);
				
				broadcast.fireSearchResultReceived(result);
			}
			else if(parts[0].equals("album")){
				Album album = JotifyPool.getInstance().browseAlbum(parts[1]);
				
				broadcast.fireClearSelection();
				broadcast.fireBrowsedAlbum(album);
			}
		}
	}
	
	public void playlistAdded(Playlist playlist){
	}
	
	public void playlistRemoved(Playlist playlist){
	}
	
	public void playlistSelected(Playlist playlist){
		this.showPlaylist(playlist);
	}
	
	public void playlistUpdated(Playlist playlist){
	}
	
	public void queueSelected(JotifyPlaybackQueue queue){
		this.showQueue(queue);
	}
	
	public void queueUpdated(JotifyPlaybackQueue queue){
		if(this.isShowingQueue){
			this.showQueue(queue);
		}
	}
	
	public void searchResultReceived(Result result){
		this.showResult(result);
	}
	
	public void searchResultSelected(Result result){
		this.showResult(result);
	}
	
	public void browsedArtist(Artist artist){
		/* TODO */
	}
	
	public void browsedAlbum(Album album){
		this.showAlbum(album);
	}
	
	public void browsedTracks(Result result){
		this.showResult(result);
	}
}
