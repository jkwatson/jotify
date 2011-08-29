import de.felixbruns.jotify.JotifyPool;
import de.felixbruns.jotify.exceptions.AuthenticationException;
import de.felixbruns.jotify.exceptions.ConnectionException;
import de.felixbruns.jotify.exceptions.ProtocolException;
import de.felixbruns.jotify.media.Link;
import de.felixbruns.jotify.media.Playlist;
import de.felixbruns.jotify.media.PlaylistContainer;
import de.felixbruns.jotify.media.Track;
import de.felixbruns.jotify.player.PlaybackAdapter;
import nl.pascaldevink.jotify.gui.JotifyPlayer;

import javax.sound.sampled.LineUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) throws AuthenticationException, ConnectionException, TimeoutException, IOException, LineUnavailableException, Link.InvalidSpotifyURIException, ProtocolException {
        JotifyPool jotify = new JotifyPool(2);
        try {
            jotify.login("username", "password");
            Track track = jotify.browseTrack("spotify:track:5OnyZ56HLhrWOXdzeETqLk");
            System.out.println("track = " + track);

            JotifyPlayer jotifyPlayer = new JotifyPlayer(jotify);

//        Playlist testing = jotify.playlistCreate("Testing");
//        System.out.println("testing = " + testing);
//        testing.setTracks(Arrays.asList(track));


            PlaylistContainer playlistContainer = jotify.playlistContainer();
            List<Playlist> playlists = playlistContainer.getPlaylists();
            for (Playlist playlist : playlists) {
                Playlist playlist1 = null;
                try {
                    playlist1 = jotify.playlist(playlist.getId());
                } catch (Exception e) {
                    System.out.println("failed to get playlist with id: " + playlist.getId());
                    e.printStackTrace();
                }
                System.out.println("playlist = " + playlist1);
                if (playlist1 != null && playlist1.hasTracks()) {
                    if ("stuff to listen to".equals(playlist1.getName())) {
                        List<Track> tracks = playlist1.getTracks();
                        List<Track> tracks1 = jotify.browse(tracks);
                        jotifyPlayer.addTracks(tracks1);
                    }
                }
            }
            jotifyPlayer.shuffle();
            jotifyPlayer.addPlaybackListener(new MyPlaybackAdapter());
            jotifyPlayer.controlPlay();

            String country = jotify.user().getCountry();
            System.out.println("country = " + country);

//            jotify.play(track, 256, new MyPlaybackAdapter());
            System.in.read();
        } finally {
            jotify.close();
        }
    }

    private static class MyPlaybackAdapter extends PlaybackAdapter {
        @Override
        public void playbackStarted(Track track) {
            System.out.println("Main.playbackStarted: " + track);
        }

        @Override
        public void playbackStopped(Track track) {
            System.out.println("Main.playbackStopped");
        }

        @Override
        public void playbackResumed(Track track) {
            System.out.println("Main.playbackResumed");
        }

        @Override
        public void playbackPosition(Track track, int ms) {
//            System.out.println("Main.playbackPosition : " + ms);
        }

        @Override
        public void playbackFinished(Track track) {
            System.out.println("Main.playbackFinished: " + track);
        }
    }
}
