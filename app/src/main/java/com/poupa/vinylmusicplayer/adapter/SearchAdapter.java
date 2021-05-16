package com.poupa.vinylmusicplayer.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.poupa.vinylmusicplayer.R;
import com.poupa.vinylmusicplayer.adapter.base.MediaEntryViewHolder;
import com.poupa.vinylmusicplayer.databinding.ItemListBinding;
import com.poupa.vinylmusicplayer.databinding.SubHeaderBinding;
import com.poupa.vinylmusicplayer.databinding.SubHeaderMaterialBinding;
import com.poupa.vinylmusicplayer.glide.GlideApp;
import com.poupa.vinylmusicplayer.glide.VinylGlideExtension;
import com.poupa.vinylmusicplayer.helper.MusicPlayerRemote;
import com.poupa.vinylmusicplayer.helper.menu.SongMenuHelper;
import com.poupa.vinylmusicplayer.model.Album;
import com.poupa.vinylmusicplayer.model.Artist;
import com.poupa.vinylmusicplayer.model.Song;
import com.poupa.vinylmusicplayer.util.ImageTheme.ThemeStyleUtil;
import com.poupa.vinylmusicplayer.util.MusicUtil;
import com.poupa.vinylmusicplayer.util.NavigationUtil;
import com.poupa.vinylmusicplayer.util.PlayingSongDecorationUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private static final int HEADER = 0;
    private static final int ALBUM = 1;
    private static final int ARTIST = 2;
    private static final int SONG = 3;

    private final AppCompatActivity activity;
    private List<Object> dataSet;

    public SearchAdapter(@NonNull AppCompatActivity activity, @NonNull List<Object> dataSet) {
        this.activity = activity;
        this.dataSet = dataSet;
    }

    public void swapDataSet(@NonNull List<Object> dataSet) {
        this.dataSet = dataSet;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (dataSet.get(position) instanceof Album) return ALBUM;
        if (dataSet.get(position) instanceof Artist) return ARTIST;
        if (dataSet.get(position) instanceof Song) return SONG;
        return HEADER;
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        if (viewType == HEADER) {
            return ThemeStyleUtil.getInstance().HeaderViewHolder(this, inflater, parent, false);
        }
        ItemListBinding binding = ItemListBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding, viewType);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case ALBUM:
                final Album album = (Album) dataSet.get(position);
                holder.title.setText(album.getTitle());
                holder.text.setText(MusicUtil.getAlbumInfoString(activity, album));
                GlideApp.with(activity)
                        .asDrawable()
                        .load(VinylGlideExtension.getSongModel(album.safeGetFirstSong()))
                        .transition(VinylGlideExtension.getDefaultTransition())
                        .songOptions(album.safeGetFirstSong())
                        .into(holder.image);
                break;
            case ARTIST:
                final Artist artist = (Artist) dataSet.get(position);
                holder.title.setText(artist.getName());
                holder.text.setText(MusicUtil.getArtistInfoString(activity, artist));
                GlideApp.with(activity)
                        .asBitmap()
                        .load(VinylGlideExtension.getArtistModel(artist))
                        .transition(VinylGlideExtension.getDefaultTransition())
                        .artistOptions(artist)
                        .into(holder.image);
                break;
            case SONG:
                final Song song = (Song) dataSet.get(position);
                holder.title.setText(song.title);
                holder.text.setText(MusicUtil.getSongInfoString(song));
                GlideApp.with(activity)
                        .asBitmap()
                        .load(VinylGlideExtension.getSongModel(song))
                        .transition(VinylGlideExtension.getDefaultTransition())
                        .songOptions(song)
                        .into(holder.image);

                PlayingSongDecorationUtil.decorate(holder.title, holder.image, holder.imageText, song, activity, ThemeStyleUtil.getInstance().showSongAlbumArt());
                break;
            default:
                ThemeStyleUtil.getInstance().setHeaderText(holder, activity, dataSet.get(position).toString());
                break;
        }
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public class ViewHolder extends MediaEntryViewHolder {
        public ViewHolder(@NonNull SubHeaderBinding binding) {
            super(binding);
            binding.getRoot().setOnLongClickListener(null);
        }

        public ViewHolder(@NonNull SubHeaderMaterialBinding binding) {
            super(binding);
            binding.getRoot().setOnLongClickListener(null);
        }

        public ViewHolder(@NonNull ItemListBinding binding, int itemViewType) {
            super(binding);

            View itemView = binding.getRoot();
            itemView.setOnLongClickListener(null);

            ThemeStyleUtil.getInstance().setSearchCardItemStyle(itemView, activity);

            if (shortSeparator != null) {
                shortSeparator.setVisibility(View.GONE);
            }

            if (menu != null) {
                if (itemViewType == SONG) {
                    menu.setVisibility(View.VISIBLE);
                    menu.setOnClickListener(new SongMenuHelper.OnClickSongMenu(activity) {
                        @Override
                        public Song getSong() {
                            return (Song) dataSet.get(getAdapterPosition());
                        }
                    });
                } else {
                    menu.setVisibility(View.GONE);
                }
            }

            switch (itemViewType) {
                case ALBUM:
                    setImageTransitionName(activity.getString(R.string.transition_album_art));
                    imageBorderTheme.setRadius(activity.getResources().getDimension(R.dimen.album_corner_radius));
                    break;
                case ARTIST:
                    setImageTransitionName(activity.getString(R.string.transition_artist_image));
                    imageBorderTheme.setRadius(activity.getResources().getDimension(R.dimen.artist_corner_radius));
                    break;
                case SONG:
                    imageBorderTheme.setCardBackgroundColor(ATHUtil.resolveColor(activity, R.attr.cardBackgroundColor));
                    break;
            }
        }

        @Override
        public void onClick(View view) {
            Object item = dataSet.get(getAdapterPosition());
            switch (getItemViewType()) {
                case ALBUM:
                    NavigationUtil.goToAlbum(activity,
                            ((Album) item).getId(),
                            Pair.create(image,
                                    activity.getResources().getString(R.string.transition_album_art)
                            ));
                    break;
                case ARTIST:
                    NavigationUtil.goToArtist(activity,
                            ((Artist) item).getId(),
                            Pair.create(image,
                                    activity.getResources().getString(R.string.transition_artist_image)
                            ));
                    break;
                case SONG:
                    ArrayList<Song> playList = new ArrayList<>();
                    playList.add((Song) item);
                    MusicPlayerRemote.openQueue(playList, 0, true);
                    break;
            }
        }
    }
}
