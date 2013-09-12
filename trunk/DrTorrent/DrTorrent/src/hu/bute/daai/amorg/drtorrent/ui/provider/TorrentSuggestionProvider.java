package hu.bute.daai.amorg.drtorrent.ui.provider;

import android.content.SearchRecentSuggestionsProvider;

public class TorrentSuggestionProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "hu.bute.daai.amorg.drtorrent.provider.TorrentSuggestionProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public TorrentSuggestionProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }
}