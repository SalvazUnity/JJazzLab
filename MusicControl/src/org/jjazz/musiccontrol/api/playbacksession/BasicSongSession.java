/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLabX software.
 *   
 *  JJazzLabX is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLabX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLabX.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.musiccontrol.api.playbacksession;

import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongContext;

/**
 * This SongSession gets outdated as soon as there is an incompatible change in the context (song change, etc.).
 */
public class BasicSongSession extends SongSession
{

    private static final List<BasicSongSession> sessions = new ArrayList<>();
    private static final ClosedSessionsListener CLOSED_SESSIONS_LISTENER = new ClosedSessionsListener();


    /**
     * Create a basic session based for the specified parameters.
     * <p>
     * Take into account all settings of the PlaybackSettings instance.
     * <p>
     * Sessions are cached: if an existing targetSession in the NEW or GENERATED state already exists for the same parameters then
     * return it, otherwise a new session is created.
     * <p>
     *
     * @param sgContext
     * @param enablePlaybackTransposition If true apply the playback transposition
     * @param enableClickTrack If true add the click track, and its muted/unmuted state will depend on the PlaybackSettings
     * @param enablePrecountTrack If true add the precount track, and loopStartTick will depend on the PlaybackSettings
     * @param enableControlTrack if true add a control track (beat positions + chord symbol markers)
     * @param loopCount See Sequencer.setLoopCount(). Use PLAYBACK_SETTINGS_LOOP_COUNT to rely on the PlaybackSettings instance
     * value.
     * @param endOfPlaybackAction Action executed when playback is stopped. Can be null.
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public BasicSongSession getSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        if (sgContext == null)
        {
            throw new IllegalArgumentException("sgContext=" + sgContext);
        }
        BasicSongSession session = findSession(sgContext,
                enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack,
                loopCount,
                endOfPlaybackAction);
        if (session == null)
        {
            final BasicSongSession newSession = new BasicSongSession(sgContext,
                    enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack,
                    loopCount,
                    endOfPlaybackAction);

            newSession.addPropertyChangeListener(CLOSED_SESSIONS_LISTENER);
            sessions.add(newSession);
            return newSession;
        } else
        {
            return session;
        }
    }

    /**
     * Same as getSession(sgContext, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
     * <p>
     *
     * @param sgContext
     * @return A targetSession in the NEW or GENERATED state.
     */
    static public BasicSongSession getSession(SongContext sgContext)
    {
        return getSession(sgContext, true, true, true, true, PLAYBACK_SETTINGS_LOOP_COUNT, null);
    }

    private BasicSongSession(SongContext sgContext, boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecountTrack, boolean enableControlTrack, int loopCount, ActionListener endOfPlaybackAction)
    {
        super(sgContext, enablePlaybackTransposition, enableClickTrack, enablePrecountTrack, enableControlTrack, loopCount, endOfPlaybackAction);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        if (!getState().equals(PlaybackSession.State.GENERATED))
        {
            return;
        }

        // LOGGER.fine("propertyChange() e=" + e);

        boolean outdated = false;

        if (e.getSource() == getSongContext().getSong())
        {
            if (e.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED))
            {
                if ((Boolean) e.getNewValue() == true)
                {
                    outdated = true;
                }
            }
        } else if (e.getSource() == getSongContext().getMidiMix())
        {
            switch (e.getPropertyName())
            {
                case MidiMix.PROP_CHANNEL_INSTRUMENT_MIX:
                case MidiMix.PROP_CHANNEL_DRUMS_REROUTED:
                case MidiMix.PROP_INSTRUMENT_TRANSPOSITION:
                case MidiMix.PROP_INSTRUMENT_VELOCITY_SHIFT:
                case MidiMix.PROP_DRUMS_INSTRUMENT_KEYMAP:
                    outdated = true;
                    break;

                default:   // PROP_DRUMS_INSTRUMENT_KEYMAP
                    // Do nothing
                    break;
            }

        } else if (e.getSource() == PlaybackSettings.getInstance())
        {
            switch (e.getPropertyName())
            {
                case PlaybackSettings.PROP_PLAYBACK_KEY_TRANSPOSITION:
                case PlaybackSettings.PROP_CLICK_PITCH_HIGH:
                case PlaybackSettings.PROP_CLICK_PITCH_LOW:
                case PlaybackSettings.PROP_CLICK_PREFERRED_CHANNEL:
                case PlaybackSettings.PROP_CLICK_VELOCITY_HIGH:
                case PlaybackSettings.PROP_CLICK_VELOCITY_LOW:
                case PlaybackSettings.PROP_CLICK_PRECOUNT_MODE:
                case PlaybackSettings.PROP_CLICK_PRECOUNT_ENABLED:
                    outdated = true;
                    break;

                default:   // PROP_VETO_PRE_PLAYBACK, PROP_LOOPCOUNT, PROP_PLAYBACK_CLICK_ENABLED
                    // Do nothing
                    break;
            }
        }

        if (outdated)
        {
            setState(State.OUTDATED);
        }
    }


    /**
     * Find an identical existing session in state NEW or GENERATED.
     *
     * @return Null if not found
     */
    static private BasicSongSession findSession(SongContext sgContext,
            boolean enablePlaybackTransposition, boolean enableClickTrack, boolean enablePrecount, boolean enableControlTrack,
            int loopCount,
            ActionListener endOfPlaybackAction)
    {
        for (var session : sessions)
        {
            if ((session.getState().equals(PlaybackSession.State.GENERATED) || session.getState().equals(PlaybackSession.State.NEW))
                    && sgContext.equals(session.getSongContext())
                    && enablePlaybackTransposition == session.isPlaybackTranspositionEnabled()
                    && enableClickTrack == session.isClickTrackEnabled()
                    && enablePrecount == session.isPrecountTrackEnabled()
                    && enableControlTrack == session.isControlTrackEnabled()
                    && loopCount == session.getLoopCount()
                    && endOfPlaybackAction == session.getEndOfPlaybackAction())
            {
                return session;
            }
        }
        return null;
    }

    // ==========================================================================================================
    // Inner classes
    // ==========================================================================================================
    private static class ClosedSessionsListener implements PropertyChangeListener
    {

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            BasicSongSession session = (BasicSongSession) evt.getSource();
            if (evt.getPropertyName().equals(PlaybackSession.PROP_STATE) && session.getState().equals(PlaybackSession.State.CLOSED))
            {
                sessions.remove(session);
                session.removePropertyChangeListener(this);
            }
        }
    }

}
