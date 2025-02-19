package com.poupa.vinylmusicplayer.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.helper.M3UWriter;
import com.poupa.vinylmusicplayer.model.Playlist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.provider.StaticPlaylist;
import com.poupa.vinylmusicplayer.service.MusicService;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 * @author SC (soncaokim)
 */
public class PlaylistsUtil {
    public static void notifyChange(@NonNull final Context context) {
        context.sendBroadcast(new Intent(MusicService.MEDIA_STORE_CHANGED));
    }

    public static boolean doesPlaylistExist(final String name) {
        return (StaticPlaylist.getPlaylist(name) != null);
    }

    public static long createPlaylist(@NonNull final Context context, @NonNull final String name) {
        StaticPlaylist playlist = StaticPlaylist.getPlaylist(name);
        if (playlist != null) {
            return playlist.asPlaylist().id;
        }

        long id = StaticPlaylist.getOrCreatePlaylist(name).asPlaylist().id;
        if (id != -1) {
            notifyChange(context);

            SafeToast.show(context, context.getResources().getString(R.string.created_playlist_x, name));
        } else {
            SafeToast.show(context, context.getResources().getString(R.string.could_not_create_playlist));
        }
        return id;
    }

    public static void deletePlaylists(@NonNull final Context context, @NonNull final ArrayList<Playlist> playlists) {
        for (int i = 0; i < playlists.size(); i++) {
            final String name = playlists.get(i).name;
            StaticPlaylist.removePlaylist(name);
            deletePlaylistFromMediaStore(context, name);
        }
        notifyChange(context);
    }

    private static void deletePlaylistFromMediaStore(@NonNull final Context context, @NonNull final String name) {
        @NonNull final ContentResolver resolver = context.getContentResolver();
        resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Playlists.NAME + "='" + name + "'",
                null);
    }

    public static void addToPlaylist(@NonNull final Context context, final Song song, final long playlistId, final boolean showToastOnFinish) {
        List<Song> helperList = List.of(song);
        addToPlaylist(context, helperList, playlistId, showToastOnFinish);
    }

    public static void addToPlaylist(@NonNull final Context context, @NonNull final List<Song> songs, final long playlistId, final boolean showToastOnFinish) {
        StaticPlaylist list = StaticPlaylist.getPlaylist(playlistId);
        if (list == null) {return;}

        list.addSongs(songs);
        notifyChange(context);

        if (showToastOnFinish) {
            SafeToast.show(
                    context,
                    context.getResources().getString(
                            R.string.inserted_x_songs_into_playlist_x,
                            songs.size(),
                            list.getName())
            );
        }
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final Song song, long playlistId) {
        StaticPlaylist list = StaticPlaylist.getPlaylist(playlistId);
        if (list == null) {return;}

        list.removeSong(song.id);
        notifyChange(context);
    }

    public static void removeFromPlaylist(@NonNull final Context context, @NonNull final List<Song> songs, long playlistId) {
        if (songs.size() == 0) {return;}

        StaticPlaylist list = StaticPlaylist.getPlaylist(playlistId);
        if (list == null) {return;}

        List<Long> songIds = new ArrayList<>();
        for (Song song : songs) {
            songIds.add(song.id);
        }

        list.removeSongs(songIds);
        notifyChange(context);
    }

    public static boolean doesPlaylistContain(final long playlistId, final long songId) {
        StaticPlaylist list = StaticPlaylist.getPlaylist(playlistId);
        if (list == null) {return false;}
        return list.contains(songId);
    }

    public static boolean moveItem(long playlistId, int from, int to) {
        StaticPlaylist list = StaticPlaylist.getPlaylist(playlistId);
        if (list == null) {return false;}

        return list.moveSong(from, to);
    }

    public static void renamePlaylist(@NonNull final Context context, final long id, final String newName) {
        StaticPlaylist list = StaticPlaylist.getPlaylist(id);
        if (list == null) {return;}

        list.rename(newName);
        notifyChange(context);
    }

    public static String getNameForPlaylist(final long id) {
        StaticPlaylist playlist = StaticPlaylist.getPlaylist(id);
        if (playlist == null) {return "";}
        return playlist.getName();
    }

    public static String savePlaylist(@NonNull final Context context, @NonNull final Playlist playlist) throws IOException {
        @NonNull ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/x-mpegurl");
        //Note: Cannot obtain the permission to "Playlists" folder - Android 13 simply disallows non-standard ones
        values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC);
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, playlist.name);

        // Delete existing, if any
        deletePlaylistFromMediaStore(context, playlist.name);
        // Now create a new one
        @NonNull final ContentResolver resolver = context.getContentResolver();
        @NonNull final Uri uri = resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);

        try (final OutputStream stream = resolver.openOutputStream(uri, "wt")) {
            M3UWriter.write(context, stream, playlist);
        }

        return Environment.DIRECTORY_MUSIC + "/" + playlist.name;
    }
}
