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
package org.jjazz.songeditormanager.api;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.VetoableChangeListener;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.SwingUtilities;
import org.jjazz.activesong.api.ActiveSongManager;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midi.api.DrumKit;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.midimix.api.UserRhythmVoice;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.pianoroll.api.PianoRollEditor;
import org.jjazz.pianoroll.api.PianoRollEditorTopComponent;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhrase;
import org.jjazz.rhythm.api.rhythmparameters.RP_SYS_CustomPhraseValue;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.song.api.SongFactory;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ui.cl_editor.api.CL_EditorTopComponent;
import org.jjazz.ui.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.util.api.ResUtil;
import org.openide.util.Exceptions;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * The central place where all song editors are created (from scratch, loaded from file, etc.) and managed.
 * <p>
 * You can register to get some change events (opened, saved, closed) from any song managed by this object.
 */
public class SongEditorManager implements PropertyChangeListener
{

    /**
     * This property change event is fired when a song is opened.
     * <p>
     * NewValue is the song object.
     */
    public static final String PROP_SONG_OPENED = "SongOpened";
    /**
     * This property change event is fired when a song is closed.
     * <p>
     * NewValue is the song object. Note that a Song object also fires a Closed event when it is closed by the SongEditorManager.
     */
    public static final String PROP_SONG_CLOSED = "SongClosed";
    /**
     * This property change event is fired when a song is saved.
     * <p>
     * NewValue is the song object. This is just a forward of a Song Saved event.
     */
    public static final String PROP_SONG_SAVED = "SongSaved";

    private static SongEditorManager INSTANCE;
    private final HashMap<Song, Editors> mapSongEditors;       // Don't use WeakHashMap here
    private final transient PropertyChangeSupport pcs = new java.beans.PropertyChangeSupport(this);
    private static final Logger LOGGER = Logger.getLogger(SongEditorManager.class.getSimpleName());

    static public SongEditorManager getInstance()
    {
        synchronized (SongEditorManager.class)
        {
            if (INSTANCE == null)
            {
                INSTANCE = new SongEditorManager();
            }
        }
        return INSTANCE;
    }

    public SongEditorManager()
    {
        mapSongEditors = new HashMap<>();
        TopComponent.getRegistry().addPropertyChangeListener(this);

    }

    /**
     * Programmatically close all the editors associated to a song.
     *
     * @param song
     * @param enforce If true, close the song unconditionnaly (block user prompt if song is not saved)
     * @return True if all editors were successfully closed, false if user blocked the closing due to unsaved song
     */
    public boolean closeSong(Song song, boolean enforce)
    {
        var editors = getEditors(song);
        assert editors != null : "song=" + song + " mapSongEditors=" + mapSongEditors;


        // CL_Editor
        var clTc = editors.getCL_EditorTc();
        if (clTc.isOpened())
        {
            if (enforce)
            {
                editors.getCL_EditorTc().closeSilent();
            } else if (!clTc.close())
            {
                return false;
            }
        }

        // SS_Editor
        var ssTc = editors.getSS_EditorTc();
        if (ssTc.isOpened())
        {
            assert ssTc.close() == true;           // No need to enforce: if we're here clTc is closed and there should not be a user confirmation anymore
        }


        // PianoRollEditor
        if (editors.getPianoRollTc() != null)
        {
            editors.getPianoRollTc().close();   // This never triggers user confirmation dialog
        }


        // Cleanup
        song.removePropertyChangeListener(this);
        song.removeUndoableEditListener(JJazzUndoManagerFinder.getDefault().get(song));
        mapSongEditors.remove(song);
        pcs.firePropertyChange(PROP_SONG_CLOSED, false, song); // Event used for example by RecentSongProvider
        song.close(true);  // This will trigger an "activeSong=null" event from the ActiveSongManager
        updateActiveSong();


        return true;
    }

    /**
     * Do what's required to show a song in the application.
     * <p>
     * Creates undomanager, creates and shows editors. Opens the possible links in song memo is makeActive is true. Opens the
     * PianoRollEditor if there is a user track.
     * <p>
     * If song is already shown in an editor, just makes its TopComponent active.
     *
     *
     * @param song
     * @param makeActive If true try to make the song musically active, see ActiveSongManager.
     * @param savable    If true, song will appear as modified/savable (save button enabled).
     */
    public void showSong(final Song song, boolean makeActive, boolean savable)
    {
        Preconditions.checkNotNull(song);

        for (Song s : getOpenedSongs())
        {
            if (s == song)
            {
                // Song is already edited, just make its editor active
                getEditors(s).getCL_EditorTc().requestActive();
                return;
            }
        }

        // Retrieve MidiMix
        MidiMix midiMix;
        try
        {
            midiMix = MidiMixManager.getInstance().findMix(song);
        } catch (MidiUnavailableException ex)
        {
            // Should never be there
            Exceptions.printStackTrace(ex);
            return;
        }


        Runnable openEditorsTask = () -> 
        {
            // Create the undo managers
            JJazzUndoManager undoManager = new JJazzUndoManager();
            JJazzUndoManagerFinder.getDefault().put(song, undoManager);
            JJazzUndoManagerFinder.getDefault().put(song.getChordLeadSheet(), undoManager);
            JJazzUndoManagerFinder.getDefault().put(song.getSongStructure(), undoManager);


            // Connect our undoManager to the song (e.g. add/removed UserPhrase)
            // Note that for cls/sgs this will be done in each editor's constructor
            song.addUndoableEditListener(undoManager);


            // Create the editors
            CL_EditorTopComponent clTC = new CL_EditorTopComponent(song);
            Mode mode = WindowManager.getDefault().findMode(CL_EditorTopComponent.MODE);
            mode.dockInto(clTC);
            clTC.open();

            SS_EditorTopComponent ssTC = new SS_EditorTopComponent(song);
            mode = WindowManager.getDefault().findMode(SS_EditorTopComponent.MODE);
            mode.dockInto(ssTC);
            ssTC.open();

            
            var editors = new Editors(clTC, ssTC);
            mapSongEditors.put(song, editors);

            
            var userChannels = midiMix.getUserChannels();
            if (!userChannels.isEmpty())
            {
                var preTc = showPianoRollEditorForUserTrack(song, midiMix, (UserRhythmVoice) midiMix.getRhythmVoice(userChannels.get(0)));
                editors.tcPre = preTc;
            }


            // Bind the editors together
            clTC.setPairedTopComponent(ssTC);
            ssTC.setPairedTopComponent(clTC);
            clTC.requestActive();
            song.addPropertyChangeListener(SongEditorManager.this);
            pcs.firePropertyChange(PROP_SONG_OPENED, false, song);


            // Try to make it active if requested
            var asm = ActiveSongManager.getInstance();
            if (makeActive && asm.isActivable(song) == null)
            {
                // To avoid problem (Issue #109 Tempo sometimes not right after 1st song auto-loaded), make sure activation
                // comes AFTER the clTc.requestActive() above.
                SwingUtilities.invokeLater(() -> asm.setActive(song, midiMix));
            }


            // Upon initialization the song editors will update the song by calling Song.putClientProperty() to store some UI settings
            // like quantization of zoom factors. This makes the song appear as "modified/savable" by default.
            if (!savable)
            {
                SwingUtilities.invokeLater(() -> song.setSaveNeeded(false));
            }

        };

        // Make sure everything is run on the EDT
        SwingUtilities.invokeLater(openEditorsTask);


        // Open the memo links
        Runnable openLinksTask = () -> 
        {
            // Open possible links
            for (URL url : org.jjazz.util.api.Utilities.extractHttpURLs(song.getComments()))
            {
                LOGGER.log(Level.INFO, "showSong() song={0} opening song memo internet link: {1}", new Object[]
                {
                    song.getName(), url
                });
                org.jjazz.util.api.Utilities.openInBrowser(url, true);         // No user notifying
            }
            for (File file : org.jjazz.util.api.Utilities.extractFileURLsAsFiles(song.getComments()))
            {
                LOGGER.log(Level.INFO, "showSong() song={0} opening song memo file link: {1}", new Object[]
                {
                    song.getName(), file
                });
                org.jjazz.util.api.Utilities.openFile(file, true);              // No user notifying
            }
        };
        if (makeActive)
        {
            new Thread(openLinksTask).start();
        }


    }

    /**
     * Load a song from a file and show it.
     * <p>
     * Load the song from file, fix the MidiMix if required, and call showSong(song, makeActive).
     *
     * @param f
     * @param makeActive
     * @param updateLastSongDirectory If true and the file is not already shown, update the LastSongDirectory in FileDirectoryManager.
     * @return The created song from file f
     * @throws org.jjazz.song.api.SongCreationException
     */
    public Song showSong(File f, boolean makeActive, boolean updateLastSongDirectory) throws SongCreationException
    {

        // Check if file is already opened, if yes just activate it
        for (Song s : getOpenedSongs())
        {
            if (s.getFile() == f)
            {
                getEditors(s).getCL_EditorTc().requestActive();

                if (makeActive)
                {
                    var asm = ActiveSongManager.getInstance();
                    if (asm.isActivable(s) == null)
                    {
                        try
                        {
                            final MidiMix mm = MidiMixManager.getInstance().findMix(s);
                            SwingUtilities.invokeLater(() -> asm.setActive(s, mm)); // So it happen after the requestActive above
                        } catch (MidiUnavailableException ex)
                        {
                            // Should never be there
                            Exceptions.printStackTrace(ex);
                        }
                    }
                }
                return s;
            }
        }

        // File is NOT opened yet

        // Read song from file
        SongFactory sf = SongFactory.getInstance();
        Song song = sf.createFromFile(f);       // Possible SongCreationException here


        // Fix the MidiMix if needed
        try
        {
            var mm = MidiMixManager.getInstance().findMix(song);
            OutputSynthManager.getInstance().getDefaultOutputSynth().fixInstruments(mm, true);
        } catch (MidiUnavailableException ex)
        {
            Exceptions.printStackTrace(ex);
        }


        // Update last song directory
        if (updateLastSongDirectory)
        {
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
        }


        // Show the song
        showSong(song, makeActive, false);


        return song;
    }

    /**
     * Show the PianoRollEditorTopComponent for the specified song.
     * <p>
     * Song must be already edited. Create the PianoRollEditorTopComponent at the appropriate position, or just activate it.
     *
     * @param song
     * @return
     */
    public PianoRollEditorTopComponent showPianoRollEditor(Song song)
    {
        Preconditions.checkNotNull(song);


        var songEditors = getEditors(song);
        if (songEditors == null)
        {
            throw new IllegalArgumentException("song=" + song + " mapSongEditors.keySet()=" + mapSongEditors.keySet());
        }


        var tc = songEditors.getPianoRollTc();
        if (tc != null)
        {
            tc.requestActive();
        } else
        {
            // Create and open next to CL_EditorTopComponent
            tc = new PianoRollEditorTopComponent(song, PianoRollEditorSettings.getDefault());
            int posInMode = songEditors.getCL_EditorTc().getTabPosition();
            assert posInMode != -1;
            tc.openAtTabPosition(posInMode + 1);
            songEditors.setPianoRollEditor(tc);
        }

        return tc;
    }

    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a user track associated to userRhythmVoice.
     *
     * @param song
     * @param midiMix
     * @param userRhythmVoice
     * @return
     */
    public PianoRollEditorTopComponent showPianoRollEditorForUserTrack(Song song, MidiMix midiMix, UserRhythmVoice userRhythmVoice)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(midiMix);
        Preconditions.checkNotNull(userRhythmVoice);

        LOGGER.log(Level.FINE, "showPianoRollEditor() song={0} userRhythmVoice={1}", new Object[]
        {
            song, userRhythmVoice
        });

        var preTc = SongEditorManager.getInstance().showPianoRollEditor(song);

        if (song.getSize() == 0)
        {
            return preTc;
        }


        String initialPhraseName = userRhythmVoice.getName();
        int initialChannel = midiMix.getChannel(userRhythmVoice);
        assert initialChannel != -1 : "midiMix=" + midiMix + " userRhythmVoice=" + userRhythmVoice;


        // Create editor TopComponent and open it if required
        DrumKit drumKit = midiMix.getInstrumentMix(userRhythmVoice).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        var userPhrase = song.getUserPhrase(initialPhraseName);
        String title = buildPrEditorUserTrackTitle(initialPhraseName, initialChannel);


        preTc.setModelForUserPhrase(userPhrase, initialChannel, keyMap);
        preTc.setTitle(title);


        // Prepare listeners to:
        // - Stop listening when editor is destroyed or its model is changed  
        // - Update title if phrase name or channel is changed
        // - Remove PianoRollEditor if user phrase is removed
        var editor = preTc.getEditor();
        VetoableChangeListener vcl = evt -> 
        {
            if (evt.getSource() == song)
            {
                if (evt.getPropertyName().equals(Song.PROP_VETOABLE_USER_PHRASE))
                {
                    // Close the editor if our phrase is removed
                    if (evt.getOldValue() instanceof String && evt.getNewValue() instanceof Phrase p && p == userPhrase)
                    {
                        preTc.close();
                    }
                }
            }
        };
        PropertyChangeListener pcl = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                // LOGGER.severe("editUserPhrase.propertyChange() e=" + Utilities.toDebugString(evt));
                if (evt.getSource() == editor)
                {
                    switch (evt.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            midiMix.removePropertyChangeListener(this);
                            song.removeVetoableChangeListener(vcl);
                        }
                    }
                } else if (evt.getSource() == midiMix)
                {
                    if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
                    {
                        // Used for UserRhythmVoice name change
                        var newRv = (RhythmVoice) evt.getNewValue();
                        var newRvName = newRv.getName();
                        if (newRv instanceof UserRhythmVoice && song.getUserPhrase(newRvName) == userPhrase)
                        {
                            int channel = midiMix.getChannel(newRv);                // Normally unchanged
                            preTc.setTitle(buildPrEditorUserTrackTitle(newRvName, channel));
                        }

                    } else if (evt.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL))
                    {
                        // Used to change channel of a RhythmVoice
                        int newChannel = (int) evt.getNewValue();
                        var rv = midiMix.getRhythmVoice(newChannel);
                        var rvName = rv.getName();
                        if (rv instanceof UserRhythmVoice && song.getUserPhrase(rvName) == userPhrase)
                        {
                            preTc.setModelForUserPhrase(userPhrase, newChannel, keyMap);
                            preTc.setTitle(buildPrEditorUserTrackTitle(rvName, newChannel));
                        }
                    }
                }
            }
        };


        editor.addPropertyChangeListener(pcl);
        midiMix.addPropertyChangeListener(pcl);
        song.addVetoableChangeListener(vcl);


        preTc.requestActive();

        return preTc;
    }

    /**
     * Open (or show) the song's PianoRollEditor and set it up to edit a custom phrase of a SongPart.
     *
     * @param song
     * @param spt  The SongPart for which a phrase is customized
     * @param rv   The RhythmVoice for which we customize the phrase
     * @param p    The customized phrase
     *
     * @return
     */
    public PianoRollEditorTopComponent showPianoRollEditorForSptCustomPhrase(Song song, MidiMix midiMix, SongPart spt, RhythmVoice rv, Phrase p)
    {
        Preconditions.checkNotNull(song);
        Preconditions.checkNotNull(spt);
        Preconditions.checkNotNull(rv);
        Preconditions.checkNotNull(p);
        Preconditions.checkArgument(spt.getRhythm() == rv.getContainer(), "spt=%s rv=%s", spt, rv);


        // Create editor TopComponent and open it if required
        var preTc = SongEditorManager.getInstance().showPianoRollEditor(song);
        var editor = preTc.getEditor();


        // Update the editor model
        DrumKit drumKit = midiMix.getInstrumentMix(rv).getInstrument().getDrumKit();
        DrumKit.KeyMap keyMap = drumKit == null ? null : drumKit.getKeyMap();
        int channel = midiMix.getChannel(rv);
        preTc.setModelForSongPartCustomPhrase(spt, p, channel, keyMap);
        preTc.setTitle(buildPrEditorSongPartPhraseTitle(rv.getName(), channel));
        preTc.requestActive();


        // Listen to RP value changes while editor edits our model, and MidiMix for channel changes        
        PropertyChangeListener listener = new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent e)
            {
                if (e.getSource() == spt)
                {
                    if (e.getPropertyName().equals(SongPart.PROP_RP_VALUE)
                            && e.getOldValue() instanceof RP_SYS_CustomPhrase
                            && e.getNewValue() instanceof RP_SYS_CustomPhraseValue newRpValue)
                    {
                        // Our rpValue was replaced, check if our customized phrase is still there
                        Phrase newP = newRpValue.getCustomizedPhrase(rv);
                        if (newP != p)
                        {
                            // It's not there anymore, close the editor
                            preTc.close();
                        }
                    }
                } else if (e.getSource() == editor)
                {
                    switch (e.getPropertyName())
                    {
                        case PianoRollEditor.PROP_MODEL_PHRASE, PianoRollEditor.PROP_EDITOR_ALIVE ->
                        {
                            editor.removePropertyChangeListener(this);
                            spt.removePropertyChangeListener(this);
                            midiMix.removePropertyChangeListener(this);
                        }
                    }
                } else if (e.getSource() == midiMix)
                {
                    if (e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE_CHANNEL)
                            || e.getPropertyName().equals(MidiMix.PROP_RHYTHM_VOICE))
                    {
                        int channel = midiMix.getChannel(rv);
                        preTc.setModelForSongPartCustomPhrase(spt, p, channel, keyMap);
                        preTc.setTitle(buildPrEditorSongPartPhraseTitle(rv.getName(), channel));
                    }
                }
            }
        };

        editor.addPropertyChangeListener(listener);
        spt.addPropertyChangeListener(listener);
        midiMix.addPropertyChangeListener(listener);

        return preTc;
    }


    public List<Song> getOpenedSongs()
    {
        return new ArrayList<>(mapSongEditors.keySet());
    }

    /**
     * Get the editors which show song s.
     *
     * @param s
     * @return Can be null.
     */
    public Editors getEditors(Song s)
    {
        if (s == null)
        {
            throw new IllegalArgumentException("s=" + s);
        }
        return mapSongEditors.get(s);
    }

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    //=============================================================================
    // PropertyChangeListener interface
    //=============================================================================
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (evt.getSource() == TopComponent.getRegistry())
        {
            if (evt.getPropertyName().equals(TopComponent.Registry.PROP_TC_CLOSED))
            {
                if (evt.getNewValue() instanceof CL_EditorTopComponent clTc)
                {
                    // User closed a song, close all editors
                    closeSong(clTc.getSongModel(), true);

                } else if (evt.getNewValue() instanceof SS_EditorTopComponent ssTc)
                {
                    // User closed a song, close all editors
                    closeSong(ssTc.getSongModel(), true);

                } else if (evt.getNewValue() instanceof PianoRollEditorTopComponent prTc)
                {
                    getEditors(prTc.getSong()).setPianoRollEditor(null);

                }
            } else if (evt.getPropertyName().equals(TopComponent.Registry.PROP_ACTIVATED))
            {
                if (evt.getNewValue() instanceof CL_EditorTopComponent clTc)
                {
                    // Make the corresponding ssTc visible
                    Song song = clTc.getSongModel();
                    var editors = getEditors(song);
                    if (editors != null)        // Might be null when application is exiting
                    {
                        editors.getSS_EditorTc().requestVisible();
                    }

                } else if (evt.getNewValue() instanceof SS_EditorTopComponent ssTc)
                {
                    // Make the corresponding clTc visible, unless the song pianoroll editor is already visible
                    Song song = ssTc.getSongModel();
                    var editors = getEditors(song);
                    if (editors != null)
                    {
                        var clTc = editors.getCL_EditorTc();
                        var prTc = editors.getPianoRollTc();
                        if (prTc == null || WindowManager.getDefault().findMode(PianoRollEditorTopComponent.MODE).getSelectedTopComponent() != prTc)
                        {
                            clTc.requestVisible();
                        }
                    }
                } else if (evt.getNewValue() instanceof PianoRollEditorTopComponent prTc)
                {
                    // Make the corresponding ssTc visible
                    Song song = prTc.getSong();
                    var editors = getEditors(song);
                    if (editors != null)
                    {
                        editors.getSS_EditorTc().requestVisible();
                    }
                }
            }
        } else if (evt.getSource() instanceof Song s)
        {
            if (evt.getPropertyName().equals(Song.PROP_MODIFIED_OR_SAVED_OR_RESET) && evt.getOldValue() == Boolean.TRUE && evt.getNewValue() == Boolean.FALSE)
            {
                songSaved(s);
            }
        }
    }

    //=============================================================================
    // Private
    //=============================================================================  

    private void songSaved(Song s)
    {
        File f = s.getFile();   // Might be null for a new song if Song.resetModified() is called
        if (f != null)
        {
            FileDirectoryManager.getInstance().setLastSongDirectory(f.getAbsoluteFile().getParentFile());
            pcs.firePropertyChange(PROP_SONG_SAVED, false, s);
        }
    }

    private void updateActiveSong()
    {
        if (mapSongEditors.isEmpty())
        {
            return;
        }

        final Song song = (mapSongEditors.size() == 1) ? mapSongEditors.keySet().iterator().next() : null;

        // Need to wait for the new TopComponent to be selected, hence the runnable on the EDT
        Runnable r = () -> 
        {
            Song sg;
            if (song != null)
            {
                sg = song;
            } else
            {
                // Find the currently selected ChordLeadSheet editor and get its song model
                WindowManager wm = WindowManager.getDefault();
                Mode mode = wm.findMode("editor");
                if (mode == null)
                {
                    return;
                }

                var tc = mode.getSelectedTopComponent();
                if (tc instanceof CL_EditorTopComponent clTc)
                {
                    sg = clTc.getSongModel();
                } else if (tc instanceof PianoRollEditorTopComponent preTc)
                {
                    sg = preTc.getSong();
                } else
                {
                    return;
                }
            }
            activateSong(sg);
        };
        SwingUtilities.invokeLater(r);

    }


    private void activateSong(Song song)
    {
        ActiveSongManager am = ActiveSongManager.getInstance();
        if (am.isActivable(song) == null)
        {
            MidiMix mm = null;
            try
            {
                mm = MidiMixManager.getInstance().findMix(song);
            } catch (MidiUnavailableException ex)
            {
                LOGGER.log(Level.WARNING, "activateSong() Could not find MidiMix for song {0}.\n{1}", new Object[]
                {
                    song.getName(),
                    ex.getMessage()
                });
            }
            if (mm != null)
            {
                am.setActive(song, mm);
            }
        }
    }

    private String buildPrEditorUserTrackTitle(String initialPhraseName, int initialChannel)
    {
        var title = ResUtil.getString(SongEditorManager.class, "UserTrackTitle", initialPhraseName, initialChannel + 1);
        return title;
    }

    private String buildPrEditorSongPartPhraseTitle(String initialPhraseName, int initialChannel)
    {
        var title = ResUtil.getString(SongEditorManager.class, "SongPartCustomPhraseTitle", initialPhraseName, initialChannel + 1);
        return title;
    }

    //=============================================================================
    // Inner classes
    //============================================================================= 
    public class Editors
    {

        private final CL_EditorTopComponent tcCle;
        private final SS_EditorTopComponent tcSse;
        private PianoRollEditorTopComponent tcPre;


        protected Editors(CL_EditorTopComponent tcCle, SS_EditorTopComponent tcSse)
        {
            Preconditions.checkNotNull(tcCle);
            Preconditions.checkNotNull(tcSse);
            this.tcCle = tcCle;
            this.tcSse = tcSse;
        }


        public void setPianoRollEditor(PianoRollEditorTopComponent tc)
        {
            tcPre = tc;
        }

        public PianoRollEditorTopComponent getPianoRollTc()
        {
            return tcPre;
        }

        public CL_EditorTopComponent getCL_EditorTc()
        {
            return tcCle;
        }

        public SS_EditorTopComponent getSS_EditorTc()
        {
            return tcSse;
        }

    }
}
