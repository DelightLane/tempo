package com.cappielloantonio.tempo.ui.fragment;

import android.content.ComponentName;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.session.MediaBrowser;
import androidx.media3.session.SessionToken;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cappielloantonio.tempo.R;
import com.cappielloantonio.tempo.databinding.FragmentSongListPageBinding;
import com.cappielloantonio.tempo.helper.recyclerview.PaginationScrollListener;
import com.cappielloantonio.tempo.interfaces.ClickCallback;
import com.cappielloantonio.tempo.service.MediaManager;
import com.cappielloantonio.tempo.service.MediaService;
import com.cappielloantonio.tempo.ui.activity.MainActivity;
import com.cappielloantonio.tempo.ui.adapter.SongHorizontalAdapter;
import com.cappielloantonio.tempo.util.Constants;
import com.cappielloantonio.tempo.util.MusicUtil;
import com.cappielloantonio.tempo.viewmodel.SongListPageViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;

@UnstableApi
public class SongListPageFragment extends Fragment implements ClickCallback {
    private static final String TAG = "SongListPageFragment";

    private FragmentSongListPageBinding bind;
    private MainActivity activity;
    private SongListPageViewModel songListPageViewModel;

    private SongHorizontalAdapter songHorizontalAdapter;

    private ListenableFuture<MediaBrowser> mediaBrowserListenableFuture;

    private boolean isLoading = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();

        bind = FragmentSongListPageBinding.inflate(inflater, container, false);
        View view = bind.getRoot();
        songListPageViewModel = new ViewModelProvider(requireActivity()).get(SongListPageViewModel.class);

        init();
        initAppBar();
        initButtons();
        initSongListView();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        initializeMediaBrowser();
    }

    @Override
    public void onStop() {
        releaseMediaBrowser();
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        bind = null;
    }

    private void init() {
        if (requireArguments().getString(Constants.MEDIA_RECENTLY_PLAYED) != null) {
            songListPageViewModel.title = Constants.MEDIA_RECENTLY_PLAYED;
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_recently_played);
            bind.pageTitleLabel.setText(R.string.song_list_page_recently_played);
        } else if (requireArguments().getString(Constants.MEDIA_MOST_PLAYED) != null) {
            songListPageViewModel.title = Constants.MEDIA_MOST_PLAYED;
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_most_played);
            bind.pageTitleLabel.setText(R.string.song_list_page_most_played);
        } else if (requireArguments().getString(Constants.MEDIA_RECENTLY_ADDED) != null) {
            songListPageViewModel.title = Constants.MEDIA_RECENTLY_ADDED;
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_recently_added);
            bind.pageTitleLabel.setText(R.string.song_list_page_recently_added);
        } else if (requireArguments().getString(Constants.MEDIA_BY_GENRE) != null) {
            songListPageViewModel.title = Constants.MEDIA_BY_GENRE;
            songListPageViewModel.genre = requireArguments().getParcelable(Constants.GENRE_OBJECT);
            songListPageViewModel.toolbarTitle = songListPageViewModel.genre.getGenre();
            bind.pageTitleLabel.setText(songListPageViewModel.genre.getGenre());
        } else if (requireArguments().getString(Constants.MEDIA_BY_ARTIST) != null) {
            songListPageViewModel.title = Constants.MEDIA_BY_ARTIST;
            songListPageViewModel.artist = requireArguments().getParcelable(Constants.ARTIST_OBJECT);
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_top, songListPageViewModel.artist.getName());
            bind.pageTitleLabel.setText(getString(R.string.song_list_page_top, songListPageViewModel.artist.getName()));
        } else if (requireArguments().getString(Constants.MEDIA_BY_GENRES) != null) {
            songListPageViewModel.title = Constants.MEDIA_BY_GENRES;
            songListPageViewModel.filters = requireArguments().getStringArrayList("filters_list");
            songListPageViewModel.filterNames = requireArguments().getStringArrayList("filter_name_list");
            songListPageViewModel.toolbarTitle = songListPageViewModel.getFiltersTitle();
            bind.pageTitleLabel.setText(songListPageViewModel.getFiltersTitle());
        } else if (requireArguments().getString(Constants.MEDIA_BY_YEAR) != null) {
            songListPageViewModel.title = Constants.MEDIA_BY_YEAR;
            songListPageViewModel.year = requireArguments().getInt("year_object");
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_year, songListPageViewModel.year);
            bind.pageTitleLabel.setText(getString(R.string.song_list_page_year, songListPageViewModel.year));
        } else if (requireArguments().getString(Constants.MEDIA_STARRED) != null) {
            songListPageViewModel.title = Constants.MEDIA_STARRED;
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_starred);
            bind.pageTitleLabel.setText(R.string.song_list_page_starred);
        } else if (requireArguments().getString(Constants.MEDIA_DOWNLOADED) != null) {
            songListPageViewModel.title = Constants.MEDIA_DOWNLOADED;
            songListPageViewModel.toolbarTitle = getString(R.string.song_list_page_downloaded);
            bind.pageTitleLabel.setText(getString(R.string.song_list_page_downloaded));
        } else if (requireArguments().getParcelable(Constants.ALBUM_OBJECT) != null) {
            songListPageViewModel.album = requireArguments().getParcelable(Constants.ALBUM_OBJECT);
            songListPageViewModel.title = Constants.MEDIA_FROM_ALBUM;
            songListPageViewModel.toolbarTitle = songListPageViewModel.album.getName();
            bind.pageTitleLabel.setText(songListPageViewModel.album.getName());
        }
    }

    private void initAppBar() {
        activity.setSupportActionBar(bind.toolbar);

        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (bind != null)
            bind.toolbar.setNavigationOnClickListener(v -> activity.navController.navigateUp());

        if (bind != null)
            bind.appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
                if ((bind.albumInfoSector.getHeight() + verticalOffset) < (2 * ViewCompat.getMinimumHeight(bind.toolbar))) {
                    bind.toolbar.setTitle(songListPageViewModel.toolbarTitle);
                } else {
                    bind.toolbar.setTitle(R.string.empty_string);
                }
            });
    }

    private void initButtons() {
        songListPageViewModel.getSongList().observe(getViewLifecycleOwner(), songs -> {
            if (bind != null) {
                bind.songListShuffleImageView.setOnClickListener(v -> {
                    Collections.shuffle(songs);
                    MediaManager.startQueue(mediaBrowserListenableFuture, songs.subList(0, Math.min(25, songs.size())), 0);
                    activity.setBottomSheetInPeek(true);
                });
            }
        });
    }

    private void initSongListView() {
        bind.songListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        bind.songListRecyclerView.setHasFixedSize(true);

        songHorizontalAdapter = new SongHorizontalAdapter(this, true, false, null);
        bind.songListRecyclerView.setAdapter(songHorizontalAdapter);
        songListPageViewModel.getSongList().observe(getViewLifecycleOwner(), songs -> {
            isLoading = false;
            songHorizontalAdapter.setItems(songs);
        });

        bind.songListRecyclerView.addOnScrollListener(new PaginationScrollListener((LinearLayoutManager) bind.songListRecyclerView.getLayoutManager()) {
            @Override
            protected void loadMoreItems() {
                isLoading = true;
                songListPageViewModel.getSongsByPage(getViewLifecycleOwner());
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    private void initializeMediaBrowser() {
        mediaBrowserListenableFuture = new MediaBrowser.Builder(requireContext(), new SessionToken(requireContext(), new ComponentName(requireContext(), MediaService.class))).buildAsync();
    }

    private void releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture);
    }

    @Override
    public void onMediaClick(Bundle bundle) {
        MediaManager.startQueue(mediaBrowserListenableFuture, bundle.getParcelableArrayList(Constants.TRACKS_OBJECT), bundle.getInt(Constants.ITEM_POSITION));
        activity.setBottomSheetInPeek(true);
    }

    @Override
    public void onMediaLongClick(Bundle bundle) {
        Navigation.findNavController(requireView()).navigate(R.id.songBottomSheetDialog, bundle);
    }
}