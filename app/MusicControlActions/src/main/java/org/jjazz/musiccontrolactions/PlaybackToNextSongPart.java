/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 *  This file is part of the JJazzLab software.
 *   
 *  JJazzLab is free software: you can redistribute it and/or modify
 *  it under the terms of the Lesser GNU General Public License (LGPLv3) 
 *  as published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  JJazzLab is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 * 
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 * 
 *  Contributor(s): 
 */
package org.jjazz.musiccontrolactions;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Sequencer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jjazz.activesong.spi.ActiveSongManager;
import org.jjazz.harmony.api.Position;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.MusicController.State;
import org.jjazz.musiccontrol.api.playbacksession.PlaybackSession;
import org.jjazz.musiccontrol.api.playbacksession.SongContextProvider;
import org.jjazz.songcontext.api.SongContext;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.song.api.Song;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.musiccontrolactions.api.RemoteAction;
import org.jjazz.musiccontrolactions.api.RemoteActionProvider;
import org.jjazz.utilities.api.ResUtil;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Make playback jump to next song part.
 * <p>
 * Action is enabled when playback is on.<p>
 */
@ActionID(category = "MusicControls", id = "org.jjazz.musiccontrolactions.playbacktonextsongpart")
@ActionRegistration(displayName = "#CTL_PlaybackToNextSongPart", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Shortcuts", name = "F2")
        })
public class PlaybackToNextSongPart extends AbstractAction implements PropertyChangeListener, LookupListener
{

    private final Lookup.Result<Song> lookupResult;
    private Song currentSong;
    private static final Logger LOGGER = Logger.getLogger(PlaybackToNextSongPart.class.getSimpleName());

    public PlaybackToNextSongPart()
    {
        putValue(Action.NAME, ResUtil.getString(getClass(), "CTL_PlaybackToNextSongPart"));        // For our RemoteActionProvider
        putValue(Action.SMALL_ICON, new ImageIcon(getClass().getResource("/org/jjazz/musiccontrolactions/resources/NextSongpart-24x24.png")));
        putValue(Action.SHORT_DESCRIPTION, ResUtil.getString(getClass(), "CTL_PlaybackToNextSongPartTooltip"));
        putValue("hideActionText", true);

        // Listen to playbackState and position changes
        MusicController.getInstance().addPropertyChangeListener(this);

        // Listen to the Midi active song changes
        ActiveSongManager.getDefault().addPropertyListener(this);

        // Listen to the current Song changes
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Song.class);
        lookupResult.addLookupListener(this);
        setEnabled(false);              // By default
        resultChanged(null);            // Might enable the action
      
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        jumpToSongPart(true);
    }


    /**
     * Do the job of both actions.
     *
     * @param nextOrPrevious If true go to next songpart, otherwise go to previous.
     */
    static public void jumpToSongPart(boolean nextOrPrevious)
    {
        var mc = MusicController.getInstance();
        if (!mc.isPlaying() && !mc.isPaused())
        {
            return;
        }

        // Get the song being played
        var session = mc.getPlaybackSession();
        if (!(session instanceof SongContextProvider))
        {
            return;
        }
        SongContext songContext = ((SongContextProvider) session).getSongContext();
        if (mc.isArrangerPlaying())
        {
            // Special case, dont mess with the arranger mode            
            return;
        }


        // Retrieve the song part
        SongPart songPart = mc.getCurrentSongPart();
        Position pos = mc.getCurrentBeatPosition();
        if (songPart == null || pos == null)
        {
            LOGGER.log(Level.WARNING, "actionPerformed() unexpected null value songPart={0} pos={1}", new Object[]{songPart, pos});
            return;
        }


        // Find next SongPart to be played
        var nextPlayingSpt = nextOrPrevious
                ? getNextSongPart(session, songContext.getSongParts(), songPart)
                : getPreviousSongPart(session, songContext.getSongParts(), songPart, pos);
        if (nextPlayingSpt == null)
        {
            return;
        }


        if (mc.isPaused())
        {
            mc.changePausedBar(nextPlayingSpt.getStartBarIndex());

        } else
        {
            // Playing

            // Restart the MusicController on next songpart
            mc.pause();
            try
            {
                mc.play(nextPlayingSpt.getStartBarIndex());
            } catch (MusicGenerationException ex)
            {
                NotifyDescriptor d = new NotifyDescriptor.Message(ex.getMessage(), NotifyDescriptor.ERROR_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        }
    }
    // ======================================================================
    // LookupListener interface
    // ======================================================================  

    @Override
    public synchronized void resultChanged(LookupEvent ev)
    {
        int i = 0;
        Song newSong = null;
        for (Song s : lookupResult.allInstances())
        {
            newSong = s;
            i++;
        }
        assert i < 2 : "i=" + i + " lookupResult.allInstances()=" + lookupResult.allInstances();   
        if (newSong != null)
        {
            // Current song has changed
            currentSong = newSong;
            updateEnabledState();
        } else
        {
            // Do nothing : player is still using the last valid song
        }
    }
    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    

    @Override
    public synchronized void propertyChange(PropertyChangeEvent evt)
    {
        MusicController mc = MusicController.getInstance();
        if (evt.getSource() == mc)
        {
            if (evt.getPropertyName() == MusicController.PROP_STATE)
            {
                updateEnabledState();
            }
        } else if (evt.getSource() == ActiveSongManager.getDefault())
        {
            if (evt.getPropertyName() == ActiveSongManager.PROP_ACTIVE_SONG)
            {
                updateEnabledState();
            }
        }
    }
    // ======================================================================
    // Inner classes
    // ======================================================================   

    @ServiceProvider(service = RemoteActionProvider.class)
    public static class NextSongPartRemoteActionProvider implements RemoteActionProvider
    {

        @Override
        public List<RemoteAction> getRemoteActions()
        {
            RemoteAction ra = RemoteAction.loadFromPreference("MusicControls", "org.jjazz.musiccontrolactions.playbacktonextsongpart");
            if (ra == null)
            {
                ra = new RemoteAction("MusicControls", "org.jjazz.musiccontrolactions.playbacktonextsongpart");
                ra.setMidiMessages(RemoteAction.noteOnMidiMessages(0, 27));
            }
            ra.setDefaultMidiMessages(RemoteAction.noteOnMidiMessages(0, 27));
            return Arrays.asList(ra);
        }
    }

    //=====================================================================================
    // Private methods
    //=====================================================================================    

    private void updateEnabledState()
    {
        MusicController mc = MusicController.getInstance();
        Song activeSong = ActiveSongManager.getDefault().getActiveSong();
        boolean b = (currentSong != null && currentSong == activeSong);
        b &= !mc.isArrangerPlaying() && (mc.isPlaying() || mc.isPaused());
        setEnabled(b);
    }

    static private SongPart getNextSongPart(PlaybackSession session, List<SongPart> spts, SongPart songPart)
    {
        int index = spts.indexOf(songPart);
        assert index != -1 : "songPart=" + songPart + " spts=" + spts;
        index++;
        if (index == spts.size())
        {
            if (session.getLoopCount() != Sequencer.LOOP_CONTINUOUSLY)
            {
                // No next song part, we're done
                return null;
            }
            index = 0;
        }
        return spts.get(index);
    }

    static private SongPart getPreviousSongPart(PlaybackSession session, List<SongPart> spts, SongPart songPart, Position pos)
    {
        int index = spts.indexOf(songPart);
        assert index != -1 : "songPart=" + songPart + " spts=" + spts;
        if (pos.getBar() == songPart.getStartBarIndex() && pos.getBeat() < 2)
        {
            // Go to previous bar only if we're in pause mode, or we're already at the beginning of the song part
            index--;
            if (index < 0)
            {
                if (session.getLoopCount() != Sequencer.LOOP_CONTINUOUSLY)
                {
                    // No next song part, we're done
                    return null;
                }
                index = spts.size() - 1;
            }
        } else
        {
            // Nothing: this will reset position at the beginning of the song part
        }

        return spts.get(index);
    }
}
