package de.felixbruns.jotify.gui.swing.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent.EventType;

import de.felixbruns.jotify.Jotify;
import de.felixbruns.jotify.gui.JotifyApplication;
import de.felixbruns.jotify.gui.JotifyPlaybackQueue;
import de.felixbruns.jotify.gui.listeners.BrowseListener;
import de.felixbruns.jotify.gui.listeners.JotifyBroadcast;
import de.felixbruns.jotify.gui.listeners.PlaylistListener;
import de.felixbruns.jotify.gui.listeners.QueueListener;
import de.felixbruns.jotify.gui.listeners.SearchListener;
import de.felixbruns.jotify.gui.swing.components.JotifyTable;
import de.felixbruns.jotify.gui.swing.components.JotifyTableModel;
import de.felixbruns.jotify.gui.swing.dnd.TrackListTransferable;
import de.felixbruns.jotify.media.Album;
import de.felixbruns.jotify.media.Artist;
import de.felixbruns.jotify.media.Link;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.Result;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.media.Link.InvalidSpotifyURIException;

@SuppressWarnings("serial")
public class JotifyContentPanel extends JPanel implements HyperlinkListener, PlaylistListener, QueueListener, SearchListener, BrowseListener {
	private JotifyBroadcast         broadcast;
	private JEditorPane             infoPane;
	private JLabel                  imageLabel;
	private JScrollPane             scrollPane;
	private JTable                  table;
	private JotifyTableModel<Track> tableModel;
	
	private ImageIcon coverImage;
	private boolean   isShowingQueue;
	
	private final Jotify jotify;
	
	public JotifyContentPanel(final Jotify jotify){
		this.broadcast = JotifyBroadcast.getInstance();
		this.jotify    = jotify;
		
		this.coverImage = new ImageIcon(
			new ImageIcon(
				JotifyApplication.class.getResource("images/cover.png")
			).getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH)
		);
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
		
		this.imageLabel = new JLabel();
		this.imageLabel.setOpaque(false);
		this.imageLabel.setVerticalAlignment(JLabel.TOP);
		this.imageLabel.setBorder(new EmptyBorder(0, 5, 0, 5));
		this.imageLabel.setVisible(false);
		this.imageLabel.setPreferredSize(new Dimension(210, 210));
		this.add(this.imageLabel, BorderLayout.WEST);
		
		/* Create table model. */
		this.tableModel = new JotifyTableModel<Track>();
		
		/* Create table. */
		this.table = new JotifyTable(this.tableModel);
		//TODO: implement sorting of actual list in the background to guarantee that positions match
		//this.table.setAutoCreateRowSorter(true);
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
					JPopupMenu contextMenu      = new JPopupMenu();
					JMenuItem  playItem         = new JMenuItem("Play");
					JMenuItem  queueItem        = new JMenuItem("Queue");
					JMenuItem  browseArtistItem = new JMenuItem("Browse Artist");
					JMenuItem  browseAlbumItem  = new JMenuItem("Browse Album");
					JMenuItem  uriItem          = new JMenuItem("Copy Spotify URI");
					JMenuItem  linkItem         = new JMenuItem("Copy HTTP Link");
					
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
					
					browseArtistItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							try{
								Track track   = tableModel.get(table.getSelectedRow());
								Artist artist = jotify.browse(track.getArtist());
								
								broadcast.fireClearSelection();
								broadcast.fireBrowsedArtist(artist);
							}
							catch(TimeoutException ex){
								ex.printStackTrace();
							}
						}
					});
					
					browseAlbumItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							try{
								Track track = tableModel.get(table.getSelectedRow());
								Album album = jotify.browse(track.getAlbum());
								
								broadcast.fireClearSelection();
								broadcast.fireBrowsedAlbum(album);
							}
							catch(TimeoutException ex){
								ex.printStackTrace();
							}
						}
					});
					
					uriItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							
							StringSelection uri = new StringSelection(track.getLink().asString());
							
							Toolkit.getDefaultToolkit().getSystemClipboard().setContents(uri, uri);
						}
					});
					
					linkItem.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e){
							Track track = tableModel.get(table.getSelectedRow());
							
							StringSelection uri = new StringSelection(track.getLink().asHTTPLink());
							
							Toolkit.getDefaultToolkit().getSystemClipboard().setContents(uri, uri);
						}
					});
					
					contextMenu.add(playItem);
					contextMenu.addSeparator();
					contextMenu.add(queueItem);
					contextMenu.addSeparator();
					contextMenu.add(browseArtistItem);
					contextMenu.add(browseAlbumItem);
					contextMenu.addSeparator();
					contextMenu.add(uriItem);
					contextMenu.add(linkItem);
					
					contextMenu.show(table, e.getX(), e.getY());
				}
			}
		});
		this.table.setDragEnabled(true);
		this.table.setTransferHandler(new TransferHandler(){
			public boolean canImport(TransferHandler.TransferSupport support){
				return false;
			}
			
			protected Transferable createTransferable(JComponent c){
				if(table.getSelectedRowCount() == 0){
					return null;
				}
				
				final JotifyTableModel<Track> model = tableModel;
				final List<Track> selectedTracks    = new LinkedList<Track>();
				
				for(int selectedRow : table.getSelectedRows()){
					selectedTracks.add(model.get(selectedRow));
				}
				
				return new TrackListTransferable(selectedTracks);
			}
			
			public int getSourceActions(JComponent c){
				return TransferHandler.COPY;
			}
		});
		
		/* Create scoll pane. */
		this.scrollPane = new JScrollPane(this.table);
		this.scrollPane.setOpaque(false);
		this.scrollPane.getViewport().setOpaque(false);
		this.scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
		this.add(this.scrollPane, BorderLayout.CENTER);
	}
	
	/* TODO: Beautify :-P */
	public void showArtist(final Artist artist){
		List<Track> tracks = new ArrayList<Track>();
		
		this.infoPane.setText(
			"<html>" +
			"	<table style=\"font: bold 12pt Dialog;\">" +
			"		<tr>" +
			"			<td valign=\"top\" style=\"width: 50%;\">" +
			"				<span style=\"color: #ffffff;\">" +
								artist.getName() +
								"<div style=\"font: normal 10pt Dialog; color: #ffffff;\">" +
								(artist.getBios().isEmpty()?"":artist.getBios().get(0).getText()) +
								"</div>" +
							"</span>" +
			"			</td>" +
			"		</tr>" +
			"	</table>" +
			"</html>"
		);
		
		for(Album album : artist.getAlbums()){
			if(album.getArtist().equals(artist)){
				tracks.addAll(album.getTracks());
			}
		}
		
		this.showTracks(tracks);
		
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(true);
		
		/* Load portrait. */
		new Thread("Image-Loading-Thread"){
			public void run(){
				if(artist.getPortrait() != null){
					try{
						imageLabel.setIcon(new ImageIcon(
							jotify.image(artist.getPortrait()))
						);
					}
					catch(TimeoutException ex){
						ex.printStackTrace();
					}
				}
			}
		}.start();
	}
	
	public void showAlbum(final Album album){
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
		
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(true);
		
		/* Load cover. */
		if(album.getCover() != null){
			new Thread("Image-Loading-Thread"){
				public void run(){
					try{
						imageLabel.setIcon(new ImageIcon(
							jotify.image(album.getCover()).getScaledInstance(200, 200, Image.SCALE_SMOOTH))
						);
					}
					catch(TimeoutException ex){
						ex.printStackTrace();
					}
				}
			}.start();
		}
	}
	
	public void showResult(final Result result){
		Iterator<Artist> artists = result.getArtists().iterator();
		Iterator<Album>  albums  = result.getAlbums().iterator();
		
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(false);
		
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
	
	public void showPlaylist(final Playlist playlist){
		this.showTracks(playlist.getTracks());
		
		/* Check for description. */
		if(playlist.getDescription() != null){
			this.infoPane.setText(
				"<html>" +
				"	<table style=\"font: bold 12pt Dialog;\">" +
				"		<tr>" +
				"			<td valign=\"top\" style=\"width: 50%;\">" +
				"				<span style=\"color: #ffffff;\">" +
									playlist.getDescription() +
								"</span>" +
				"			</td>" +
				"		</tr>" +
				"	</table>" +
				"</html>"
			);
		}
		else{
			this.infoPane.setText("");
		}
		
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(true);
		
		/* Load picture. */
		if(playlist.getPicture() != null){
			new Thread("Image-Loading-Thread"){
				public void run(){
					try{
						imageLabel.setIcon(new ImageIcon(
							jotify.image(playlist.getPicture()).getScaledInstance(200, 200, Image.SCALE_SMOOTH))
						);
					}
					catch(TimeoutException ex){
						ex.printStackTrace();
					}
				}
			}.start();
		}
		
		this.isShowingQueue = false;
	}
	
	public void showQueue(final JotifyPlaybackQueue queue){
		List<Track> tracks = new ArrayList<Track>();
		
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(false);
		
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
	
	private void showTracks(final List<Track> tracks){
		this.imageLabel.setIcon(this.coverImage);
		this.imageLabel.setVisible(false);
		
		this.tableModel.removeAll();
		this.tableModel.addAll(tracks);
		
		this.isShowingQueue = false;
	}
	
	public void hyperlinkUpdate(final HyperlinkEvent e){
		if(e.getEventType() == EventType.ACTIVATED){
			try{
				Link link = Link.create(e.getDescription());
				
				if(link.isArtistLink()){
					Artist artist = this.jotify.browseArtist(link.getId());
					
					broadcast.fireClearSelection();
					broadcast.fireBrowsedArtist(artist);
				}
				else if(link.isAlbumLink()){
					Album album = this.jotify.browseAlbum(link.getId());
					
					broadcast.fireClearSelection();
					broadcast.fireBrowsedAlbum(album);
				}
				else if(link.isTrackLink()){
					Track track = this.jotify.browseTrack(link.getId());
					
					broadcast.fireControlSelect(track);
					broadcast.fireControlPlay();
				}
			}
			catch(InvalidSpotifyURIException ex){
				/* Ignore. */
			}
			catch(TimeoutException ex){
				/* Ignore. */
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
		/* TODO: compare with current playlist. */
		/* TODO: if this playlist is selected. */
		/*if(!isShowingQueue){
			this.showTracks(playlist.getTracks());
		}*/
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
		this.showArtist(artist);
	}
	
	public void browsedAlbum(Album album){
		this.showAlbum(album);
	}
	
	public void browsedTracks(Result result){
		this.showResult(result);
	}
}
