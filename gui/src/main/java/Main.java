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

//            Track track = jotify.browseTrack("spotify:track:5OnyZ56HLhrWOXdzeETqLk");
//            System.out.println("track = " + track);

            JotifyPlayer jotifyPlayer = new JotifyPlayer(jotify);


            //spotify:local:The+Temper+Trap:Conditions:Sweet+Disposition:230

//            Artist artist = new Artist("00000000000000000000000000000000", "The Temper Trap");
//            Album album = new Album("00000000000000000000000000000000", "Conditions", artist);
//            Track replacement = jotify.replacement(new Track("00000000000000000000000000000000", "Sweet Disposition", artist, album));
//            System.out.println("replacement = " + replacement.getId());

//        Playlist testing = jotify.playlistCreate("Testing");
//        System.out.println("testing = " + testing);
//        testing.setTracks(Arrays.asList(track));

//
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

                System.out.println("playlist = " + playlist1.getName());
                if (playlist1 != null && playlist1.hasTracks()) {
                    if (playlist1.getName().startsWith("Cake")) {
                        System.out.println("********************************************************");
                        System.out.println("********************************************************");
                        System.out.println("********************************************************");
                        System.out.println("********************************************************");
                        List<Track> tracks = playlist1.getTracks();
                        List<Track> browse = jotify.browse(tracks);
                        jotifyPlayer.addTracks(browse);
                        jotifyPlayer.controlVolume(.3434f);
                    }
                }
            }
//            List<Track> browse = jotify.browseTracks(Arrays.asList("ea71f01a376649238f72040790f79ea8"));
//            Track browse1 = jotify.browseTrack("f7069b91d1174bc9b71e9e0658cfdb59");
//            jotifyPlayer.addTracks(browse);
//            jotifyPlayer.shuffle();
            jotifyPlayer.addPlaybackListener(new MyPlaybackAdapter());
            jotifyPlayer.controlPlay();

//            jotify.play(track, 256, new MyPlaybackAdapter());
//            System.in.read();
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
