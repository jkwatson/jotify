package de.felixbruns.jotify.gui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;

import de.felixbruns.jotify.media.Track;

/**
 * Carries the information of a track over a drag-and-drop operation.
 */
public class TrackListTransferable implements Transferable {
	public static final DataFlavor TRACKLIST_FLAVOR = new DataFlavor(List.class, "Jotify track list");
	
	private final List<Track> trackList;
	
	public TrackListTransferable(final List<Track> trackList){
		this.trackList = trackList;
	}
	
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if(flavor.equals(DataFlavor.stringFlavor)){
			String ids = "";
			
			for(int i = 0; i < this.trackList.size(); i++){
				ids += ((i > 0)?"\n":"") + this.trackList.get(i).getId();
			}
			
			return ids;
		}
		
		if(flavor.equals(TRACKLIST_FLAVOR)){
			return this.trackList;
		}
		
		throw new UnsupportedFlavorException(flavor);
	}
	
	public DataFlavor[] getTransferDataFlavors(){
		return new DataFlavor[]{DataFlavor.stringFlavor, TRACKLIST_FLAVOR};
	}
	
	public boolean isDataFlavorSupported(DataFlavor flavor){
		return flavor.equals(TRACKLIST_FLAVOR) || flavor.equals(DataFlavor.stringFlavor);
	}
}
