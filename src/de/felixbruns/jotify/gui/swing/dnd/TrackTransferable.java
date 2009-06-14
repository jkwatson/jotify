package de.felixbruns.jotify.gui.swing.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import de.felixbruns.jotify.media.Track;


/**
 * Carries the information of a track over a drag-and-drop operation.
 */
public class TrackTransferable implements Transferable {
  public static final DataFlavor TRACK_FLAVOR = new DataFlavor(Track.class, "Jotify track");
  
  private final Track track;

  public TrackTransferable(final Track track) {
    this.track = track;
  }

  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (flavor.equals(DataFlavor.stringFlavor)) {
      return track.getId();
    }

    if (flavor.equals(TRACK_FLAVOR)) {
      return track;
    }

    throw new UnsupportedFlavorException(flavor);
  }

  public DataFlavor[] getTransferDataFlavors() {
    return new DataFlavor[] {DataFlavor.stringFlavor, TRACK_FLAVOR};
  }

  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return flavor.equals(TRACK_FLAVOR) || flavor.equals(DataFlavor.stringFlavor);
  }

}
