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
package org.jjazz.musiccontrol.api;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Transmitter;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.midi.api.MidiConst;
import org.jjazz.midi.api.MidiUtilities;
import org.jjazz.midi.api.JJazzMidiSystem;
import org.jjazz.musiccontrol.api.playbacksession.EndOfPlaybackActionProvider;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythmmusicgeneration.api.ContextChordSequence;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.util.api.ResUtil;
import org.jjazz.util.api.Utilities;
import org.openide.util.Exceptions;
import org.jjazz.musiccontrol.api.playbacksession.ControlTrackProvider;
import org.jjazz.rhythmmusicgeneration.api.SongSequenceBuilder;
import org.jjazz.rhythm.api.UserErrorGenerationException;
import org.jjazz.song.api.Song;
import org.openide.awt.StatusDisplayer;

/**
 * Control the music playback of a PlaybackSession.
 * <p>
 * The PlaybackSession provides the Sequence to be played and various related data which impact how the MusicController manage the
 * Sequence.
 * <p>
 * Property changes are fired for:<br>
 * - start/pause/stop/disabled state changes<br>
 * <p>
 * Use NoteListener to get notified of note ON/OFF events. Use PlaybackListener to get notified of other events (e.g. bar/beat
 * changes) during playback. Note that listeners will be notified out of the Swing EDT.<br>
 * The current output synth latency is taken into account to fire events to NoteListeners and PlaybackListeners.
 * <p>
 * Use acquireSequencer()/releaseSequencer() if you want to use the Java system sequencer independently.
 */
public class MusicController implements PropertyChangeListener, MetaEventListener, ControllerEventListener
{

    public static final String PROP_STATE = "PropPlaybackState";   //NOI18N 
    public static final String PROP_PLAYBACK_SESSION = "PropPlaybackSession";   //NOI18N 

    /**
     * The playback states.
     * <p>
     * Property change listeners are notified with property PROP_STATE.
     */
    public enum State
    {
        DISABLED,
        STOPPED,
        PAUSED,
        PLAYING
    }
    private static MusicController INSTANCE;
    private Sequencer sequencer;
    private PlaybackSession playbackSession;

    private State state;
    /**
     * The current beat position during playback (for ControlTrackProvider sessions only).
     */
    private Position currentBeatPosition = new Position();
    /**
     * The current chord symbol during playback (for ControlTrackProvider sessions only).
     */
    private CLI_ChordSymbol currentChordSymbol;
    /**
     * The current song part during playback (for ControlTrackProvider sessions only).
     */
    private SongPart currentSongPart;

    /**
     * Sequencer lock by an external entity.
     */
    private Object sequencerLockHolder;

    /**
     * The tempo factor to go from MidiConst.SEQUENCER_REF_TEMPO to song tempo.
     */
    private float songTempoFactor;
    /**
     * The tempo factor of the SongPart being played.
     */
    private float songPartTempoFactor = 1;
    private int audioLatency;
    /**
     * Keep track of active timers used to compensate the audio latency.
     * <p>
     * Needed to force stop them when sequencer is stopped/paused by user.
     */
    private Set<Timer> audioLatencyTimers = new HashSet<>();
    /**
     * Our MidiReceiver to be able to fire events to NoteListeners and PlaybackListener (midiActivity).
     */
    private McReceiver receiver;
    /**
     * The list of the controller changes listened to
     */
    private static final int[] listenedControllers =
    {
        MidiConst.CTRL_CHG_JJAZZ_BEAT_CHANGE,
        MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR
    };
    /**
     * If true display built sequence when it is built
     */
    private boolean debugPlayedSequence = false;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final List<PlaybackListener> playbackListeners = new ArrayList<>();
    private final List<NoteListener> noteListeners = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(MusicController.class.getSimpleName());  //NOI18N

    public static MusicController getInstance()
    {
        synchronized (MusicController.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new MusicController();
            }
        }
        return INSTANCE;
    }

    /**
     * The sequencer tempo must be set to 100.
     */
    private MusicController()
    {

        state = State.STOPPED;
        sequencer = JJazzMidiSystem.getInstance().getDefaultSequencer();
        receiver = new McReceiver();
        initSequencer();
        sequencerLockHolder = null;


        // Get notified of the notes sent by the Sequencer
        try
        {
            Transmitter t = sequencer.getTransmitter();
            t.setReceiver(receiver);
        } catch (MidiUnavailableException ex)
        {
            // Should never occur
            Exceptions.printStackTrace(ex);
        }


        // Listen to latency changes
        var osm = OutputSynthManager.getInstance();
        osm.addPropertyChangeListener(this);
        audioLatency = osm.getOutputSynth().getAudioLatency();


    }

    /**
     * Try to temporarily acquire the java sequencer opened and connected to the default Midi out device.
     * <p>
     * You can acquire the sequencer only if MusicController is in the STOPPED state. If acquisition is successful the
     * MusicController is put in DISABLED state since it can't use the sequencer anymore. When the caller is done with the
     * sequencer, he must call releaseSequencer(lockHolder) so that the MusicController can use it again.
     *
     * @param lockHolder Must be non-null
     * @return Null if sequencer has already a different lock or if MusicController is in the PLAYING state.
     */
    public synchronized Sequencer acquireSequencer(Object lockHolder)
    {
        if (lockHolder == null)
        {
            throw new NullPointerException("lockHolder");   //NOI18N
        }

        LOGGER.fine("acquireSequencer() -- lockHolder=" + lockHolder);  //NOI18N

        if (sequencerLockHolder == lockHolder)
        {
            // lock already acquired
            return sequencer;
        } else if (sequencerLockHolder == null && state.equals(State.STOPPED))
        {
            // lock acquired by external entity: get disabled
            sequencerLockHolder = lockHolder;


            closeCurrentPlaybackSession();


            // Remove the MusicController listeners
            releaseSequencer();

            State oldState = getState();
            setState(State.DISABLED);

            LOGGER.fine("acquireSequencer() external lock acquired.  MusicController released the sequencer, oldState=" + oldState + " newState=DISABLED");  //NOI18N

            return sequencer;
        } else
        {
            LOGGER.fine("acquireSequencer() can't give lock to " + lockHolder + ", current lock=" + sequencerLockHolder);  //NOI18N
            return null;
        }
    }

    /**
     * Release the external sequencer lock.
     * <p>
     * Once released, MusicController becomes able to take the Sequencer lock when playing music.
     *
     * @param lockHolder
     * @throws IllegalArgumentException If lockHolder does not match
     */
    public synchronized void releaseSequencer(Object lockHolder)
    {
        if (lockHolder == null || sequencerLockHolder != lockHolder)
        {
            throw new IllegalArgumentException("lockHolder=" + lockHolder + " sequencerLockHolder=" + sequencerLockHolder);   //NOI18N
        }

        LOGGER.fine("releaseSequencer() -- lockHolder=" + lockHolder);  //NOI18N

        sequencerLockHolder = null;

        sequencer.stop(); // Just to make sure

        // Initialize sequencer for MusicController
        initSequencer();

        // Change state
        assert state.equals(State.DISABLED);
        setState(State.STOPPED);
    }

    /**
     * Set the current playback session and try to pre-generate the sequence.
     * <p>
     * If MusicController is paused then current playback is stopped. If pre-generation of the sequence triggers
     * SongSequenceBuilder.UserErrorExceptions notify user "lightly" using the StatusDisplayer.
     *
     * @param session Can be null. If not null, must be in NEW or GENERATED state, and can't be dirty.
     * @throws org.jjazz.rhythm.api.MusicGenerationException E.g. if song is already playing, if section lacks a starting chord,
     * etc.
     * @throws IllegalStateException If session is dirty or is CLOSED.
     * @see PROP_PLAYBACK_SESSION
     */
    public void setPlaybackSession(PlaybackSession session) throws MusicGenerationException
    {

        if (session == playbackSession)
        {
            return;

        }

        if (session != null && (session.isDirty() || session.getState().equals(PlaybackSession.State.CLOSED)))
        {
            throw new IllegalStateException("session=" + session);
        }


        // Check state
        switch (state)
        {
            case DISABLED:
                throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK IS DISABLED"));
            case STOPPED:
                // Nothing
                break;
            case PAUSED:
                stop();
                break;
            case PLAYING:
                throw new MusicGenerationException(ResUtil.getString(getClass(), "A SONG IS ALREADY PLAYING"));
            default:
                throw new AssertionError(state.name());
        }


        try
        {
            // Reset sequence
            sequencer.setSequence((Sequence) null);
        } catch (InvalidMidiDataException ex)
        {
            // Should never happen
            Exceptions.printStackTrace(ex);
        }

        PlaybackSession oldSession = playbackSession;


        // Update the session
        closeCurrentPlaybackSession();
        playbackSession = session;

        if (playbackSession != null)
        {
            playbackSession.addPropertyChangeListener(this);

            // Try to pre-generate the sequence
            if (playbackSession.getState().equals(PlaybackSession.State.NEW))
            {
                try
                {
                    playbackSession.generate(true);         // Throws MusicGenerationException
                } catch (UserErrorGenerationException ex)
                {
                    // playbackSession will remain NEW
                    // Notify user lightly (no blocking dialog) because it is just a pre-generation tentative
                    StatusDisplayer.getDefault().setStatusText(ex.getMessage());
                }
            }
        }


        pcs.firePropertyChange(PROP_PLAYBACK_SESSION, oldSession, playbackSession);
    }

    /**
     * Play the current playback session from the specified bar.
     * <p>
     * Do nothing if no playback session set.
     *
     * @param fromBarIndex
     * @throws MusicGenerationException If a problem occurred which prevents song playing: no Midi out, rhythm music generation
     * problem, MusicController state is not PAUSED nor STOPPED, etc.
     * @throws IllegalStateException If current session is not in the GENERATED state, or if fromBarIndex is invalid
     *
     */
    public void play(int fromBarIndex) throws MusicGenerationException
    {
        if (playbackSession == null)
        {
            return;
        }


        if (!playbackSession.getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalArgumentException("playbackSession=" + playbackSession + " fromBarIndex=" + fromBarIndex);
        }


        // Check that a Midi ouput device is set
        checkMidi();                // throws MusicGenerationException


        // Check fomrBarIndex argument
        if (playbackSession.getBarRange() != null && !playbackSession.getBarRange().contains(fromBarIndex))
        {
            throw new IllegalArgumentException("invalid fromBarIndex=" + fromBarIndex + " playbackSession=" + playbackSession);
        }


        // Check state
        switch (state)
        {
            case DISABLED:
                throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK IS DISABLED"));
            case STOPPED:
            case PAUSED:
                // Nothing
                break;
            case PLAYING:
                throw new MusicGenerationException(ResUtil.getString(getClass(), "A SONG IS ALREADY PLAYING"));
            default:
                throw new AssertionError(state.name());
        }


        if (debugPlayedSequence)
        {
            SongContext sgContext = getSongContext(playbackSession);        // Can be null
            String songName = sgContext != null ? sgContext.getSong().getName() : "unknown";
            LOGGER.info("play() song=" + songName + " sequence :"); //NOI18N
            LOGGER.info(MidiUtilities.toString(playbackSession.getSequence())); //NOI18N
        }


        // Set sequence
        try
        {
            // This also stops sequencer and resets Mute, Solo, LoopStart/End points, LoopCount, tick position
            sequencer.setSequence(playbackSession.getSequence()); // Can raise InvalidMidiDataException

        } catch (InvalidMidiDataException ex)
        {
            closeCurrentPlaybackSession();
            throw new MusicGenerationException(ex.getMessage());
        }


        // Loop settings
        sequencer.setLoopStartPoint(playbackSession.getLoopStartTick());
        sequencer.setLoopEndPoint(playbackSession.getLoopEndTick());
        sequencer.setLoopCount(playbackSession.getLoopCount());


        // Mute/unmute tracks
        updateTracksMuteStatus();
        songPartTempoFactor = 1f;
        songTempoChanged(playbackSession.getTempo());


        // Enable events to PlaybackListeners
        if (playbackSession instanceof ControlTrackProvider)
        {
            firePlaybackListenerEnabledChanged(true);
        }


        // Set sequencer position
        setPosition(fromBarIndex);


        // Start or restart the sequencer
        seqStart();


        // Change state
        setState(State.PLAYING);

    }

    /**
     * Resume playback from the pause state.
     * <p>
     * If state is not PAUSED, nothing is done.
     *
     * @throws MusicGenerationException For example if state==DISABLED
     * @throws IllegalStateException If current playback session is not in the GENERATED state.
     */
    public void resume() throws MusicGenerationException
    {
        if (state == State.DISABLED)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "PLAYBACK IS DISABLED"));
        } else if (!state.equals(State.PAUSED))
        {
            return;
        }

        // Need a clean session
        if (!playbackSession.getState().equals(PlaybackSession.State.GENERATED))
        {
            throw new IllegalStateException("playbackSession.getState()=" + playbackSession.getState());
        }


        checkMidi();                // throws MusicGenerationException


        // Let's go again
        seqStart();


        // Update state
        setState(State.PLAYING);
    }

    /**
     * Stop the playback of the sequence if it was playing, and reset the position to the beginning of the sequence.
     * <p>
     */
    public void stop()
    {
        switch (state)
        {
            case DISABLED:
            case STOPPED:
                return;
            case PAUSED:
                // Nothing
                break;
            case PLAYING:
                sequencer.stop();
                clearPendingEvents();
                break;
            default:
                throw new AssertionError(state.name());
        }


        // Update state
        currentSongPart = null;
        currentChordSymbol = null;
        songPartTempoFactor = 1;
        setState(State.STOPPED);

        // Position must be reset after the stop so that playback beat change tracking listeners are not reset upon stop   
        int barIndex = playbackSession.getBarRange() != null ? playbackSession.getBarRange().from : 0;
        setPosition(barIndex);

        // Action to be fired after state change
        executeEndOfPlaybackAction();

    }

    /**
     * Stop the playback of the sequence and leave the position unchanged.
     * <p>
     * If state is not PLAYING, do nothing. If session is dirty, use stop() instead.
     */
    public void pause()
    {
        if (!state.equals(State.PLAYING))
        {
            return;
        }

        // If session needs to be regenerated use stop() instead
        if (playbackSession.isDirty())
        {
            stop();
            return;
        }


        sequencer.stop();
        clearPendingEvents();


        // Change state
        setState(State.PAUSED);

        // Action to be fired after state change
        executeEndOfPlaybackAction();
    }

    /**
     * Change the current bar when in PAUSED state.
     * <p>
     * Do nothing if not in PAUSED state.
     *
     * @param barIndex
     * @throws IllegalArgumentException If bar index is not valid for the current PlaybackSession.
     */
    public void changePausedBar(int barIndex)
    {
        if (!state.equals(State.PAUSED))
        {
            return;
        }
        if (!playbackSession.getBarRange().contains(barIndex))
        {
            throw new IllegalArgumentException("Invalid barIndex=" + barIndex + " playbackSession.getBarRange()=" + playbackSession.getBarRange());
        }

        setPosition(barIndex);
    }

    /**
     * The current playback position updated at every natural beat (eg 0, 1, 2, 3 in 4/4).
     * <p>
     * Note: value is meaningful only for PlaybackSessions which are also ControlTrackProviders. Otherwise returned value is
     * always bar=0, beat=0.
     *
     * @return
     * @see PlaybackListener
     */
    public Position getCurrentBeatPosition()
    {
        return currentBeatPosition;
    }

    /**
     * The current CLI_ChordSymbol being played.
     * <p>
     * Note: value is meaningful only when playback is on or paused, and for PlaybackSessions which are also
     * ControlTrackProviders. Otherwise return value is null.
     *
     * @return Can be null.
     * @see PlaybackListener
     */
    public CLI_ChordSymbol getCurrentChordSymbol()
    {
        return currentChordSymbol;
    }

    /**
     * The current SongPart being played.
     * <p>
     * Note: value is meaningful only when playback is on or paused, and for PlaybackSessions which are also ControlTrackProviders
     * and SongContextProviders. Otherwise return value is null.
     *
     * @return Can be null.
     * @see PlaybackListener
     */
    public SongPart getCurrentSongPart()
    {
        return currentSongPart;
    }

    public State getState()
    {
        return state;
    }

    /**
     * Get the current playback session.
     *
     * @return Can be null if no song has ever been played.
     */
    public PlaybackSession getPlaybackSession()
    {
        return playbackSession;
    }

    /**
     * True if the current playing session is a special arranger session.
     *
     * @return
     */
    public boolean isArrangerPlaying()
    {
        if (!state.equals(State.PLAYING) || !(playbackSession instanceof SongContextProvider))
        {
            return false;
        }
        Song song = ((SongContextProvider) playbackSession).getSongContext().getSong();
        return song.getName().startsWith("*!ArrangerSONG!*");
    }

    public void setDebugPlayedSequence(boolean b)
    {
        debugPlayedSequence = b;
    }

    public boolean isDebugPlayedSequence()
    {
        return debugPlayedSequence;
    }

    /**
     * Add a listener of note ON/OFF events.
     * <p>
     * Listeners will be called out of the Swing EDT (Event Dispatch Thread).
     *
     * @param listener
     */
    public synchronized void addNoteListener(NoteListener listener)
    {
        if (!noteListeners.contains(listener))
        {
            noteListeners.add(listener);
        }
    }

    public synchronized void removeNoteListener(NoteListener listener)
    {
        noteListeners.remove(listener);
    }

    /**
     * Add a listener to be notified of playback bar/beat changes events etc.
     * <p>
     * Listeners will be called out of the Swing EDT (Event Dispatch Thread).
     *
     * @param listener
     */
    public synchronized void addPlaybackListener(PlaybackListener listener)
    {
        if (!playbackListeners.contains(listener))
        {
            playbackListeners.add(listener);
        }
    }

    public synchronized void removePlaybackListener(PlaybackListener listener)
    {
        playbackListeners.remove(listener);
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    //-----------------------------------------------------------------------
    // Implementation of the ControlEventListener interface
    //-----------------------------------------------------------------------
    /**
     * Handle the listened controllers notifications.
     * <p>
     * CAUTIOUS : the global listenedControllers array must be consistent with this method !
     *
     * @param event
     */
    @Override
    public void controlChange(ShortMessage event)
    {
        int data1 = event.getData1();
        switch (data1)
        {
            case MidiConst.CTRL_CHG_JJAZZ_BEAT_CHANGE:
                if (playbackSession instanceof ControlTrackProvider)
                {
                    ControlTrackProvider controlTrackProvider = (ControlTrackProvider) playbackSession;
                    ControlTrack controlTrack = controlTrackProvider.getControlTrack(); // Might be null for a specific BaseSongSession instance
                    if (controlTrack != null && playbackSession.getLoopStartTick() != -1)
                    {
                        List<Position> naturalBeatPositions = controlTrackProvider.getControlTrack().getSongPositions();
                        long tick = sequencer.getTickPosition() - playbackSession.getLoopStartTick();
                        int index = (int) (tick / MidiConst.PPQ_RESOLUTION);
                        long remainder = tick % MidiConst.PPQ_RESOLUTION;
                        index += (remainder <= MidiConst.PPQ_RESOLUTION / 2) ? 0 : 1;

                        if (index >= naturalBeatPositions.size())
                        {
                            index = naturalBeatPositions.size() - 1;
                        }
                        Position newPos = naturalBeatPositions.get(index);
                        updateCurrentPosition(newPos.getBar(), newPos.getBeat());
                    }
                }
                break;

            case MidiConst.CTRL_CHG_JJAZZ_TEMPO_FACTOR:
                songPartTempoFactor = MidiUtilities.getTempoFactor(event);
                updateTempoFactor();
                break;

            default:
                LOGGER.log(Level.WARNING, "controlChange() controller event not managed data1={0}", data1);  //NOI18N
                break;
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the MetaEventListener interface
    //-----------------------------------------------------------------------
    @Override
    public void meta(MetaMessage meta)
    {
        if (meta.getType() == 47) // Meta Event for end of sequence
        {
            // This method  is called from the Sequencer thread, NOT from the EDT !
            // So if this method impacts the UI, it must use SwingUtilities.InvokeLater() (or InvokeAndWait())
            LOGGER.fine("Sequence end reached");  //NOI18N        
            SwingUtilities.invokeLater(() -> stop());

        } else if (meta.getType() == 6)     // Marker
        {
            String s = Utilities.toString(meta.getData());
            if (s.startsWith("csIndex="))           // Marker for chord symbol
            {
                if (playbackSession instanceof ControlTrackProvider)
                {
                    ControlTrackProvider controlTrackProvider = (ControlTrackProvider) playbackSession;
                    ControlTrack controlTrack = controlTrackProvider.getControlTrack(); // Might be null for a specific BaseSongSession instance
                    if (controlTrack != null)
                    {
                        // Fire chord symbol change
                        ContextChordSequence cSeq = controlTrack.getContextChordGetSequence();
                        int csIndex = Integer.valueOf(s.substring(8));

                        CLI_ChordSymbol cliCs = cSeq.get(csIndex);
                        fireChordSymbolChanged(cliCs);
                    }
                }
            }
        }
    }

    //-----------------------------------------------------------------------
    // Implementation of the PropertiesListener interface
    //-----------------------------------------------------------------------
    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        LOGGER.log(Level.FINE, "propertyChange() e={0}", e);  //NOI18N


        // Always enabled changes
        if (e.getSource() == OutputSynthManager.getInstance())
        {
            if (e.getPropertyName().equals(OutputSynthManager.PROP_AUDIO_LATENCY))
            {
                audioLatency = (int) e.getNewValue();
            }
        }

        if (e.getSource() == playbackSession)
        {
            if (e.getPropertyName().equals(PlaybackSession.PROP_STATE))
            {
                switch (playbackSession.getState())
                {
                    case NEW:
                    case GENERATED:
                        // Nothing
                        break;
                    case CLOSED:
                        stop();
                        closeCurrentPlaybackSession();
                        break;
                    default:
                        throw new AssertionError(playbackSession.getState().name());

                }
            } else if (e.getPropertyName().equals(PlaybackSession.PROP_DIRTY))
            {
                if (state.equals(State.PAUSED))
                {
                    stop();
                }

            } else if (e.getPropertyName().equals(PlaybackSession.PROP_TEMPO))
            {
                songTempoChanged((Integer) e.getNewValue());

            } else if (e.getPropertyName().equals(PlaybackSession.PROP_MUTED_TRACKS))
            {
                updateTracksMuteStatus();

            } else if (e.getPropertyName().equals(PlaybackSession.PROP_LOOP_COUNT))
            {
                if (!state.equals(State.DISABLED))
                {
                    int lc = (Integer) e.getNewValue();
                    sequencer.setLoopCount(lc);
                }

            } else if (e.getPropertyName().equals(ControlTrackProvider.ENABLED_STATE))
            {
                firePlaybackListenerEnabledChanged((Boolean) e.getNewValue());
            }
        }
    }


    // =====================================================================================
    // Private methods
    // =====================================================================================
    /**
     * Initialize the sequencer for our usage.
     */
    private void initSequencer()
    {
        addSequencerListeners();
        sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);
        receiver.setEnabled(true);
    }

    /**
     * Remove our listeners.
     */
    private void releaseSequencer()
    {
        removeSequencerListeners();
        receiver.setEnabled(false);
    }

    private void addSequencerListeners()
    {
        sequencer.addMetaEventListener(this);
        int[] res = sequencer.addControllerEventListener(this, listenedControllers);
        if (res.length != listenedControllers.length)
        {
            LOGGER.severe("This sequencer implementation is limited, music playback may not work");  //NOI18N
        }
    }

    private void removeSequencerListeners()
    {
        sequencer.removeMetaEventListener(this);
        sequencer.removeControllerEventListener(this, listenedControllers);
    }

    private void setPosition(int fromBar)
    {
        assert !state.equals(State.DISABLED) : "state=" + state;
        long tick = Math.max(playbackSession.getTick(fromBar), 0);
        sequencer.setTickPosition(tick);
        updateCurrentPosition(fromBar, 0);
    }

    private void firePlaybackListenerEnabledChanged(boolean b)
    {
        // No need to use a latency aware event
        for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
        {
            pl.enabledChanged(b);
        }
    }

    private void fireChordSymbolChanged(CLI_ChordSymbol cliCs)
    {
        if (cliCs == null)
        {
            throw new IllegalArgumentException("cliCs=" + cliCs);
        }
        if (currentChordSymbol != cliCs)
        {
            currentChordSymbol = cliCs;
            fireLatencyAwareEvent(() ->
            {
                for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
                {
                    pl.chordSymbolChanged(cliCs);
                }
            });
        }
    }

    private void fireBeatChanged(Position oldPos, Position newPos)
    {
        fireLatencyAwareEvent(() ->
        {
            for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
            {
                pl.beatChanged(oldPos, newPos);
            }
        });
    }

    private void fireSongPartChanged(SongPart newSpt)
    {
        if (currentSongPart != newSpt)
        {
            currentSongPart = newSpt;
            fireLatencyAwareEvent(() ->
            {
                for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
                {
                    pl.songPartChanged(newSpt);
                }
            });
        }
    }

    private void fireNoteOn(long tick, int channel, int pitch, int velocity)
    {
        fireLatencyAwareEvent(() ->
        {
            for (NoteListener l : noteListeners.toArray(new NoteListener[0]))
            {
                l.noteOn(tick, channel, pitch, velocity);
            }
        });
    }

    private void fireNoteOff(long tick, int channel, int pitch)
    {
        fireLatencyAwareEvent(() ->
        {
            for (NoteListener l : noteListeners.toArray(new NoteListener[0]))
            {
                l.noteOff(tick, channel, pitch);
            }
        });
    }

    private void fireBarChanged(int oldBar, int newBar)
    {
        fireLatencyAwareEvent(() ->
        {
            for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
            {
                pl.barChanged(oldBar, newBar);
            }
        });
    }

    private void fireMidiActivity(long tick, int channel)
    {
        fireLatencyAwareEvent(() ->
        {
            for (PlaybackListener pl : playbackListeners.toArray(new PlaybackListener[0]))
            {
                pl.midiActivity(tick, channel);
            }
        });
    }

    /**
     * Fire an event on the EDT after a time delay to take into account the current output synth latency.
     * <p>
     * Active timers are available in audioLatencyTimers.
     *
     * @param r
     */
    private void fireLatencyAwareEvent(Runnable r)
    {
        if (audioLatency == 0)
        {
            SwingUtilities.invokeLater(r);
        } else
        {
            Timer t = new Timer(audioLatency, evt ->
            {
                r.run();            // Will be run on the EDT
            });
            t.addActionListener(evt ->
            {
                synchronized (audioLatencyTimers)
                {
                    audioLatencyTimers.remove(t);
                }
            });
            synchronized (audioLatencyTimers)
            {
                audioLatencyTimers.add(t);
            }
            t.setRepeats(false);
            t.start();
        }
    }

    /**
     * When sequencer is stopped or paused, make sure there is no pending events.
     */
    private void clearPendingEvents()
    {
        for (Iterator<Timer> it = audioLatencyTimers.iterator(); it.hasNext();)
        {
            it.next().stop();
            it.remove();
        }
    }

    private SongContext getSongContext(PlaybackSession session)
    {
        SongContext sgContext = session instanceof SongContextProvider ? ((SongContextProvider) session).getSongContext() : null;
        return sgContext;
    }

    /**
     * Start the sequencer with the bug fix (tempo reset at 120 upon each start) + possibly fire a chord change event.
     * <p>
     * If sequencer is already playing or is disabled, do nothing.
     * <p>
     * If playbackSession is a SongContextProvider and a ControlTrackProvider, and if there is no chord symbol at current
     * position, then fire a chord change event using the previous chord symbol (ie the current chord symbol at this start
     * position).
     */
    private void seqStart()
    {
        switch (state)
        {
            case DISABLED:
                LOGGER.warning("seqStart() called with state=" + state);
                break;

            case PLAYING:
                // Nothing
                break;

            case STOPPED:
            case PAUSED:

                SongContext sgContext = getSongContext(playbackSession);
                if (sgContext != null && playbackSession instanceof ControlTrackProvider)
                {
                    ControlTrackProvider controlTrackProvider = (ControlTrackProvider) playbackSession;
                    ControlTrack controlTrack = controlTrackProvider.getControlTrack();    // Might be null if disabled for a BaseSongSession instance
                    if (controlTrack != null)
                    {
                        ContextChordSequence cSeq = controlTrack.getContextChordGetSequence();
                        // Fire chord symbol change if no chord symbol at current position (current chord symbol is the previous one)
                        // Fire a song part change event
                        long loopStartTick = playbackSession.getLoopStartTick();
                        assert loopStartTick != -1 : "loopStartTick=" + loopStartTick + " playbackSession=" + playbackSession;
                        long relativeTick = sequencer.getTickPosition() - loopStartTick;    // Can be negative if precount is ON
                        Position posStart = sgContext.getPosition(relativeTick);
                        if (posStart != null)
                        {
                            CLI_ChordSymbol lastCliCs = cSeq.getChordSymbol(posStart); // Process substitute chord symbols
                            if (lastCliCs != null)
                            {
                                // Fire the event
                                fireChordSymbolChanged(lastCliCs);
                            }

                            SongPart spt = sgContext.getSong().getSongStructure().getSongPart(posStart.getBar());
                            fireSongPartChanged(spt);
                        }
                    }
                }


                sequencer.start();
                // JDK -11 BUG: start() resets tempo at 120 !
                sequencer.setTempoInBPM(MidiConst.SEQUENCER_REF_TEMPO);

                break;
            default:
                throw new AssertionError(state.name());
        }
    }

    private void updateCurrentPosition(int bar, float beat)
    {
        assert !state.equals(State.DISABLED);   //NOI18N
        Position oldPos = new Position(currentBeatPosition);
        currentBeatPosition.setBar(bar);
        currentBeatPosition.setBeat(beat);


        // Fire events
        fireBeatChanged(oldPos, new Position(currentBeatPosition));
        if (beat == 0)
        {
            fireBarChanged(oldPos.getBar(), bar);
        }


        // Possibly fire a songpart change as well
        SongContext sgContext = getSongContext(playbackSession);
        if (sgContext != null)
        {
            SongPart newSpt = sgContext.getSongParts().stream()
                    .filter(spt -> spt.getBarRange().contains(bar))
                    .findFirst().orElse(null);
            if (newSpt != null)
            {
                fireSongPartChanged(newSpt);
            }
        }
    }

    private void closeCurrentPlaybackSession()
    {
        if (playbackSession != null)
        {
            playbackSession.removePropertyChangeListener(this);
            playbackSession.close();
            playbackSession = null;
        }
    }

    private void songTempoChanged(float tempoInBPM)
    {
        songTempoFactor = tempoInBPM / MidiConst.SEQUENCER_REF_TEMPO;
        updateTempoFactor();
    }

    private void setState(State newState)
    {
        if (!newState.equals(getState()))
        {
            State oldState = getState();
            state = newState;
            pcs.firePropertyChange(PROP_STATE, oldState, newState);
        }
    }

    /**
     * Update the tempo factor of the sequencer.
     * <p>
     * Depends on songPartTempoFactor and songTempoFactor. Sequencer tempo must always be MidiConst.SEQUENCER_REF_TEMPO.
     *
     * @param bpm
     */
    private void updateTempoFactor()
    {
        assert !state.equals(State.DISABLED);   //NOI18N

        // Recommended way instead of setTempoInBpm() which cause problems when playback is looped
        float f = songPartTempoFactor * songTempoFactor;
        sequencer.setTempoFactor(f);
    }

    private void checkMidi() throws MusicGenerationException
    {
        if (JJazzMidiSystem.getInstance().getDefaultOutDevice() == null)
        {
            throw new MusicGenerationException(ResUtil.getString(getClass(), "ERR_NoMidiOutputDeviceSet"));
        }
    }

    private void executeEndOfPlaybackAction()
    {
        if (playbackSession instanceof EndOfPlaybackActionProvider)
        {
            var al = ((EndOfPlaybackActionProvider) playbackSession).getEndOfPlaybackAction();
            if (al != null)
            {
                al.actionPerformed(null);
            }
        }
    }

    /**
     * Update the track status of all RhythmVoice tracks using the current session data.
     * <p>
     */
    private void updateTracksMuteStatus()
    {
        var mapTrackMute = playbackSession.getTracksMuteStatus();

        // Make sure sequence is not null: it can happen when setPlayingSession() was called with an UpdatableSongSession but play() 
        // was not called yet, and user changed the song that provoked an update        
        if (mapTrackMute != null && sequencer.getSequence() != null)
        {

            LOGGER.log(Level.FINE, "updateTracksMuteStatus() mapTrackMute={0}", mapTrackMute);
            for (int trackId : mapTrackMute.keySet())
            {
                boolean b = mapTrackMute.get(trackId);
                sequencer.setTrackMute(trackId, b);
                if (sequencer.getTrackMute(trackId) != b)
                {
                    LOGGER.log(Level.FINE, "updateTracksMuteStatus() setTrackMute({0},{1}) failed", new Object[]{trackId, b});
                    LOGGER.log(Level.FINE, "                          sequencer{0}", sequencer.isRunning());
                }
            }
        }
    }

    /**
     * Our Midi Receiver used to fire events to NoteListeners and PlaybackListener.midiActivity().
     * <p>
     * Events are fired taking into account the current output synth latency.
     * <p>
     */
    private class McReceiver implements Receiver
    {

        /**
         * Fire only one Activity event for this period of time, even if there are several notes.
         */
        public static final int ACTIVITY_MIN_PERIOD_MS = 100;

        // Store the last Note On millisecond position for each note. Use -1 if initialized.
        private final long lastNoteOnMs[] = new long[16];
        private boolean enabled;

        public McReceiver()
        {
            setEnabled(true);
        }

        /**
         * @return the enabled
         */
        public boolean isEnabled()
        {
            return enabled;
        }

        /**
         * Enable or disable this receiver.
         *
         *
         * @param enabled the enabled to set
         */
        public final void setEnabled(boolean enabled)
        {
            if (this.enabled != enabled)
            {
                this.enabled = enabled;
                reset();
            }
        }

        @Override
        public void send(MidiMessage msg, long timeStamp)
        {
            if (!enabled)
            {
                return;
            }

            if (msg instanceof ShortMessage)
            {
                ShortMessage sm = (ShortMessage) msg;

                if (playbackListeners.isEmpty() && noteListeners.isEmpty())
                {
                    return;
                }


                if (sm.getCommand() == ShortMessage.NOTE_ON)
                {
                    int pitch = sm.getData1();
                    int velocity = sm.getData2();
                    if (velocity > 0)
                    {
                        noteOnReceived(sm.getChannel(), pitch, velocity);
                    } else
                    {
                        noteOffReceived(sm.getChannel(), pitch);
                    }


                } else if (sm.getCommand() == ShortMessage.NOTE_OFF)
                {
                    int pitch = sm.getData1();
                    noteOffReceived(sm.getChannel(), pitch);
                }
            }
        }

        @Override
        public void close()
        {
            // Do nothing
        }

        /**
         * Reset internal state.
         */
        private void reset()
        {
            for (int i = 0; i < 16; i++)
            {
                lastNoteOnMs[i] = -1;
            }
        }

        private void noteOnReceived(int channel, int pitch, int velocity)
        {
            if (enabled)
            {
                // Midi activity only once for a given channel in the ACTIVITY_MIN_PERIOD_MS period 
                long pos = System.currentTimeMillis();
                long lastPos = lastNoteOnMs[channel];
                if (lastPos < 0 || (pos - lastPos) > ACTIVITY_MIN_PERIOD_MS)
                {
                    fireMidiActivity(-1, channel);
                }
                lastNoteOnMs[channel] = pos;


                fireNoteOn(-1, channel, pitch, velocity);
            }
        }

        private void noteOffReceived(int channel, int pitch)
        {
            if (enabled)
            {
                lastNoteOnMs[channel] = -1;
                fireNoteOff(-1, channel, pitch);
            }
        }

    }

}
