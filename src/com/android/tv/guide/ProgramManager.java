/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tv.guide;

import android.support.annotation.MainThread;
import android.util.Log;

import com.android.tv.common.CollectionUtils;
import com.android.tv.data.Channel;
import com.android.tv.data.ChannelDataManager;
import com.android.tv.data.GenreItems;
import com.android.tv.data.Program;
import com.android.tv.data.ProgramDataManager;
import com.android.tv.util.TvInputManagerHelper;
import com.android.tv.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Manages the channels and programs for the program guide.
 */
@MainThread
public class ProgramManager {
    private static final String TAG = "ProgramManager";
    private static final boolean DEBUG = false;

    /**
     * If the first entry's visible duration is shorter than this value, we clip the entry out.
     * Note: If this value is larger than 1 min, it could cause mismatches between the entry's
     * position and detailed view's time range.
     */
    static final long FIRST_ENTRY_MIN_DURATION = TimeUnit.MINUTES.toMillis(1);

    private static final long INVALID_ID = -1;

    private final TvInputManagerHelper mTvInputManagerHelper;
    private final ChannelDataManager mChannelDataManager;
    private final ProgramDataManager mProgramDataManager;

    private long mStartUtcMillis;
    private long mEndUtcMillis;
    private long mFromUtcMillis;
    private long mToUtcMillis;
    private Program mSelectedProgram;

    /**
     * Entry for program guide table. An "entry" can be either an actual program or a gap between
     * programs. This is needed for {@link ProgramListAdapter} because
     * {@link android.support.v17.leanback.widget.HorizontalGridView} ignores margins between items.
     */
    public static class TableEntry {
        /** Channel ID which this entry is included. */
        public final long channelId;

        /** Program corresponding to the entry. {@code null} means that this entry is a gap. */
        public final Program program;

        /** Start time of entry in UTC milliseconds. */
        public final long entryStartUtcMillis;

        /** End time of entry in UTC milliseconds */
        public final long entryEndUtcMillis;

        private final boolean mIsBlocked;

        private TableEntry(long startUtcMillis, long endUtcMillis) {
            this(INVALID_ID, null, startUtcMillis, endUtcMillis, false);
        }

        private TableEntry(long channelId, long startUtcMillis, long endUtcMillis) {
            this(channelId, null, startUtcMillis, endUtcMillis, false);
        }

        private TableEntry(long channelId, long startUtcMillis, long endUtcMillis,
                boolean blocked) {
            this(channelId, null, startUtcMillis, endUtcMillis, blocked);
        }

        private TableEntry(long channelId, Program program,
                long entryStartUtcMillis, long entryEndUtcMillis) {
            this(channelId, program, entryStartUtcMillis, entryEndUtcMillis, false);
        }

        private TableEntry(long channelId, Program program,
                long entryStartUtcMillis, long entryEndUtcMillis, boolean isBlocked) {
            this.channelId = channelId;
            this.program = program;
            this.entryStartUtcMillis = entryStartUtcMillis;
            this.entryEndUtcMillis = entryEndUtcMillis;
            mIsBlocked = isBlocked;
        }

        /**
         * Returns true if this is a gap.
         */
        public boolean isGap() {
            return !Program.isValid(program);
        }

        /**
         * Returns true if this channel is blocked.
         */
        public boolean isBlocked() {
            return mIsBlocked;
        }

        /**
         * Returns true if this program is on the air.
         */
        public boolean isCurrentProgram() {
            long current = System.currentTimeMillis();
            return entryStartUtcMillis <= current && entryEndUtcMillis > current;
        }

        /**
         * Returns if this program has the genre.
         */
        public boolean hasGenre(int genreId) {
            return !isGap() && program.hasGenre(genreId);
        }

        /**
         * Returns the width of table entry, in pixels.
         */
        public int getWidth() {
            return GuideUtils.convertMillisToPixel(entryStartUtcMillis, entryEndUtcMillis);
        }

        @Override
        public String toString() {
            return "TableEntry{"
                    + "hashCode=" + hashCode()
                    + ", channelId=" + channelId
                    + ", program=" + program
                    + ", startTime=" + Utils.toTimeString(entryStartUtcMillis)
                    + ", endTimeTime=" + Utils.toTimeString(entryEndUtcMillis) + "}";
        }
    }

    private List<Channel> mChannels = new ArrayList<>();
    private final Map<Long, List<TableEntry>> mChannelIdEntriesMap = new HashMap<>();
    private final List<List<Channel>> mGenreChannelList = new ArrayList<>();
    private final List<Integer> mFilteredGenreIds = new ArrayList<>();

    // Position of selected genre to filter channel list.
    private int mSelectedGenreId = GenreItems.ID_ALL_CHANNELS;
    // Channel list after applying genre filter.
    // Should be matched with mSelectedGenreId always.
    private List<Channel> mFilteredChannels = mChannels;

    private final Set<Listener> mListeners = CollectionUtils.createSmallSet();
    private final Set<TableEntriesUpdatedListener> mTableEntriesUpdatedListeners = CollectionUtils
            .createSmallSet();

    private final ChannelDataManager.Listener mChannelDataManagerListener =
            new ChannelDataManager.Listener() {
                @Override
                public void onLoadFinished() {
                    updateChannels(true, false);
                }

                @Override
                public void onChannelListUpdated() {
                    updateChannels(true, false);
                }

                @Override
                public void onChannelBrowsableChanged() {
                    updateChannels(true, false);
                }
            };

    private final ProgramDataManager.Listener mProgramDataManagerListener =
            new ProgramDataManager.Listener() {
                @Override
                public void onProgramUpdated() {
                    updateTableEntries(true, true);
                }
            };

    public ProgramManager(TvInputManagerHelper tvInputManagerHelper,
            ChannelDataManager channelDataManager,
            ProgramDataManager programDataManager) {
        mTvInputManagerHelper = tvInputManagerHelper;
        mChannelDataManager = channelDataManager;
        mProgramDataManager = programDataManager;
    }

    public void programGuideVisibilityChanged(boolean visible) {
        mProgramDataManager.setPauseProgramUpdate(visible);
        if (visible) {
            mChannelDataManager.addListener(mChannelDataManagerListener);
            mProgramDataManager.addListener(mProgramDataManagerListener);
        } else {
            mChannelDataManager.removeListener(mChannelDataManagerListener);
            mProgramDataManager.removeListener(mProgramDataManagerListener);
        }
    }

    /**
     * Add a {@link Listener}.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Register a listener to be invoked when table entries are updated.
     */
    public void addTableEntriesUpdatedListener(TableEntriesUpdatedListener listener) {
        mTableEntriesUpdatedListeners.add(listener);
    }

    /**
     * Remove a {@link Listener}.
     */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Remove a previously installed table entries update listener.
     */
    public void removeTableEntriesUpdatedListener(TableEntriesUpdatedListener listener) {
        mTableEntriesUpdatedListeners.remove(listener);
    }

    /**
     * Build genre filters based on the current programs.
     * This categories channels by its current program's canonical genres
     * and subsequent @{link resetChannelListWithGenre(int)} calls will reset channel list
     * with built channel list.
     * This is expected to be called whenever program guide is shown.
     */
    public void buildGenreFilters() {
        if (DEBUG) Log.d(TAG, "buildGenreFilters");

        mGenreChannelList.clear();
        for (int i = 0; i < GenreItems.getGenreCount(); i++) {
            mGenreChannelList.add(new ArrayList<Channel>());
        }
        for (Channel channel : mChannels) {
            // TODO: Use programs in visible area instead of using current programs only.
            Program currentProgram = mProgramDataManager.getCurrentProgram(channel.getId());
            if (currentProgram != null && currentProgram.getCanonicalGenres() != null) {
                for (String genre : currentProgram.getCanonicalGenres()) {
                    mGenreChannelList.get(GenreItems.getId(genre)).add(channel);
                }
            }
        }
        mGenreChannelList.set(GenreItems.ID_ALL_CHANNELS, mChannels);
        mFilteredGenreIds.clear();
        mFilteredGenreIds.add(0);
        for (int i = 1; i < GenreItems.getGenreCount(); i++) {
            if (mGenreChannelList.get(i).size() > 0) {
                mFilteredGenreIds.add(i);
            }
        }
        mSelectedGenreId = GenreItems.ID_ALL_CHANNELS;
        mFilteredChannels = mChannels;
        notifyGenresUpdated();
    }

    /**
     * Resets channel list with given genre.
     * Caller should call {@link #buildGenreFilters()} prior to call this API to make
     * This notifies channel updates to listeners.
     */
    public void resetChannelListWithGenre(int genreId) {
        if (genreId == mSelectedGenreId) {
            return;
        }
        mFilteredChannels = mGenreChannelList.get(genreId);
        mSelectedGenreId = genreId;
        if (DEBUG) {
            Log.d(TAG, "resetChannelListWithGenre: " + GenreItems.getCanonicalGenre(genreId)
                    + " has " + mFilteredChannels.size() + " channels out of " + mChannels.size());
        }
        if (mGenreChannelList.get(mSelectedGenreId) == null) {
            throw new IllegalStateException("Genre filter isn't ready.");
        }
        notifyChannelsUpdated();
    }

    /**
     * Returns list genre ID's which has a channel.
     */
    public List<Integer> getFilteredGenreIds() {
        return mFilteredGenreIds;
    }

    public int getSelectedGenreId() {
        return mSelectedGenreId;
    }

    // Note that This can be happens only if program guide isn't shown
    // because an user has to select channels as browsable through UI.
    private void updateChannels(boolean notify, boolean clearPreviousTableEntries) {
        if (DEBUG) Log.d(TAG, "updateChannels");
        mChannels = mChannelDataManager.getBrowsableChannelList();
        mSelectedGenreId = GenreItems.ID_ALL_CHANNELS;
        mFilteredChannels = mChannels;
        if (notify) {
            notifyChannelsUpdated();
        }
        updateTableEntries(notify, clearPreviousTableEntries);
    }

    private void updateTableEntries(boolean notify, boolean clear) {
        if (clear) {
            mChannelIdEntriesMap.clear();
        }
        boolean parentalControlsEnabled = mTvInputManagerHelper.getParentalControlSettings()
                .isParentalControlsEnabled();
        for (Channel channel : mChannels) {
            long channelId = channel.getId();
            // Inline the updating of the mChannelIdEntriesMap here so we can only call
            // getParentalControlSettings once.
            List<TableEntry> entries = createProgramEntries(channelId, parentalControlsEnabled);
            mChannelIdEntriesMap.put(channelId, entries);

            int size = entries.size();
            if (DEBUG) {
                Log.d(TAG, "Programs are loaded for channel " + channel.getId()
                        + ", loaded size = " + size);
            }
            if (size == 0) {
                continue;
            }
            TableEntry lastEntry = entries.get(size - 1);
            if (mEndUtcMillis < lastEntry.entryEndUtcMillis
                    && lastEntry.entryEndUtcMillis != Long.MAX_VALUE) {
                mEndUtcMillis = lastEntry.entryEndUtcMillis;
            }
        }
        if (mEndUtcMillis > mStartUtcMillis) {
            for (Channel channel : mChannels) {
                long channelId = channel.getId();
                List<TableEntry> entries = mChannelIdEntriesMap.get(channelId);
                if (entries.isEmpty()) {
                    entries.add(new TableEntry(channelId, mStartUtcMillis, mEndUtcMillis));
                } else {
                    TableEntry lastEntry = entries.get(entries.size() - 1);
                    if (mEndUtcMillis > lastEntry.entryEndUtcMillis) {
                        entries.add(new TableEntry(channelId, lastEntry.entryEndUtcMillis,
                                mEndUtcMillis));
                    } else if (lastEntry.entryEndUtcMillis == Long.MAX_VALUE) {
                        entries.remove(entries.size() - 1);
                        entries.add(new TableEntry(lastEntry.channelId, lastEntry.program,
                                lastEntry.entryStartUtcMillis, mEndUtcMillis,
                                lastEntry.mIsBlocked));
                    }
                }
            }
        }

        if (notify) {
            notifyTableEntriesUpdated();
        }
        buildGenreFilters();
    }

    private void notifyGenresUpdated() {
        for (Listener listener : mListeners) {
            listener.onGenresUpdated();
        }
    }

    private void notifyChannelsUpdated() {
        for (Listener listener : mListeners) {
            listener.onChannelsUpdated();
        }
    }

    private void notifyTimeRangeUpdated() {
        for (Listener listener : mListeners) {
            listener.onTimeRangeUpdated();
        }
    }

    private void notifyTableEntriesUpdated() {
        for (TableEntriesUpdatedListener listener : mTableEntriesUpdatedListeners) {
            listener.onTableEntriesUpdated();
        }
    }

    /**
     * Returns the start time of currently managed time range, in UTC millisecond.
     */
    public long getFromUtcMillis() {
        return mFromUtcMillis;
    }

    /**
     * Returns the end time of currently managed time range, in UTC millisecond.
     */
    public long getToUtcMillis() {
        return mToUtcMillis;
    }

    /**
     * Update the initial time range to manage. It updates program entries and genre as well.
     */
    public void updateInitialTimeRange(long startUtcMillis, long endUtcMillis) {
        mStartUtcMillis = startUtcMillis;
        if (endUtcMillis > mEndUtcMillis) {
            mEndUtcMillis = endUtcMillis;
        }

        mProgramDataManager.setPrefetchTimeRange(mStartUtcMillis);
        updateChannels(true, true);
        setTimeRange(startUtcMillis, endUtcMillis);
    }

    private void setTimeRange(long fromUtcMillis, long toUtcMillis) {
        if (DEBUG) {
            Log.d(TAG, "setTimeRange. {FromTime="
                    + Utils.toTimeString(fromUtcMillis) + ", ToTime="
                    + Utils.toTimeString(toUtcMillis) + "}");
        }
        if (mFromUtcMillis != fromUtcMillis || mToUtcMillis != toUtcMillis) {
            mFromUtcMillis = fromUtcMillis;
            mToUtcMillis = toUtcMillis;
            notifyTimeRangeUpdated();
        }
    }

    /**
     * Returns the number of the currently managed channels.
     */
    public int getChannelCount() {
        return mFilteredChannels.size();
    }

    /**
     * Returns a {@link Channel} at a given {@code channelIndex} of the currently managed channels.
     * Returns {@code null} if such a channel is not found.
     */
    public Channel getChannel(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= getChannelCount()) {
            return null;
        }
        return mFilteredChannels.get(channelIndex);
    }

    /**
     * Returns the index of provided {@link Channel} within the currently managed channels.
     * Returns -1 if such a channel is not found.
     */
    public int getChannelIndex(Channel channel) {
        return mFilteredChannels.indexOf(channel);
    }

    /**
     * Returns the number of "entries", which lies within the currently managed time range, for a
     * given {@code channelId}.
     */
    public int getTableEntryCount(long channelId) {
        return mChannelIdEntriesMap.get(channelId).size();
    }

    /**
     * Returns an entry as {@link Program} for a given {@code channelId} and {@code index} of
     * entries within the currently managed time range. Returned {@link Program} can be a dummy one
     * (e.g., whose channelId is INVALID_ID), when it corresponds to a gap between programs.
     */
    public TableEntry getTableEntry(long channelId, int index) {
        return mChannelIdEntriesMap.get(channelId).get(index);
    }

    private List<TableEntry> createProgramEntries(long channelId, boolean parentalControlsEnabled) {
        List<TableEntry> entries = new ArrayList<>();
        boolean channelLocked = parentalControlsEnabled
                && mChannelDataManager.getChannel(channelId).isLocked();
        if (channelLocked) {
            entries.add(new TableEntry(channelId, mStartUtcMillis, Long.MAX_VALUE, true));
        } else {
            long lastProgramEndTime = mStartUtcMillis;
            List<Program> programs = mProgramDataManager.getPrograms(channelId, mStartUtcMillis);
            for (Program program : programs) {
                if (program.getChannelId() == INVALID_ID) {
                    // Dummy program.
                    continue;
                }
                long programStartTime = Math.max(program.getStartTimeUtcMillis(),
                        mStartUtcMillis);
                long programEndTime = program.getEndTimeUtcMillis();
                if (programStartTime > lastProgramEndTime) {
                    // Gap since the last program.
                    entries.add(new TableEntry(channelId, lastProgramEndTime,
                            programStartTime));
                    lastProgramEndTime = programStartTime;
                }
                if (programEndTime > lastProgramEndTime) {
                    entries.add(new TableEntry(channelId, program, lastProgramEndTime,
                            programEndTime));
                    lastProgramEndTime = programEndTime;
                }
            }
        }

        if (entries.size() > 1) {
            TableEntry secondEntry = entries.get(1);
            if (secondEntry.entryStartUtcMillis < mStartUtcMillis + FIRST_ENTRY_MIN_DURATION) {
                // If the first entry's width doesn't have enough width, it is not good to show
                // the first entry from UI perspective. So we clip it out.
                entries.remove(0);
                entries.set(0, new TableEntry(secondEntry.channelId, secondEntry.program,
                        mStartUtcMillis, secondEntry.entryEndUtcMillis));
            }
        }
        return entries;
    }

    /**
     * Get the currently selected channel.
     */
    public Channel getSelectedChannel() {
        return mChannelDataManager.getChannel(mSelectedProgram.getChannelId());
    }

    /**
     * Get the currently selected program.
     */
    public Program getSelectedProgram() {
        return mSelectedProgram;
    }

    public interface Listener {
        void onGenresUpdated();
        void onChannelsUpdated();
        void onTimeRangeUpdated();
    }

    public interface TableEntriesUpdatedListener {
        void onTableEntriesUpdated();
    }

    public static class ListenerAdapter implements Listener {
        @Override
        public void onGenresUpdated() { }

        @Override
        public void onChannelsUpdated() { }

        @Override
        public void onTimeRangeUpdated() { }
    }

    /**
     * Shifts the time range by the given time. Also makes ProgramGuide scroll the views.
     */
    public void shiftTime(long timeMillisToScroll) {
        long fromUtcMillis = mFromUtcMillis + timeMillisToScroll;
        long toUtcMillis = mToUtcMillis + timeMillisToScroll;
        if (fromUtcMillis < mStartUtcMillis) {
            fromUtcMillis = mStartUtcMillis;
            toUtcMillis += mStartUtcMillis - fromUtcMillis;
        }
        if (toUtcMillis > mEndUtcMillis) {
            fromUtcMillis -= toUtcMillis - mEndUtcMillis;
            toUtcMillis = mEndUtcMillis;
        }
        setTimeRange(fromUtcMillis, toUtcMillis);
    }

    /**
     * Returned the scrolled(shifted) time in milliseconds.
     */
    public long getShiftedTime() {
        return mFromUtcMillis - mStartUtcMillis;
    }

    /**
     * Returns the start time set by {@link #updateInitialTimeRange}.
     */
    public long getStartTime() {
        return mStartUtcMillis;
    }

    /**
     * Returns the program index of the program at {@code time}.
     */
    public int getProgramIndex(long channelId, long time) {
        List<TableEntry> entries = mChannelIdEntriesMap.get(channelId);
        for (int i = 0; i < entries.size(); ++i) {
            TableEntry entry = entries.get(i);
            if (entry.entryStartUtcMillis <= time
                    && time < entry.entryEndUtcMillis) {
                return i;
            }
        }
        return -1;
    }
}