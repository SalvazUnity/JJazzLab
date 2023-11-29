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
package org.jjazz.helpers;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jjazz.helpers.FixMidiMixDialog.FixChoice;
import org.jjazz.midi.api.Instrument;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.musiccontrol.api.MusicController;
import org.jjazz.musiccontrol.api.PlaybackSettings;
import org.jjazz.outputsynth.api.OutputSynth;
import org.jjazz.outputsynth.api.OutputSynthManager;
import org.jjazz.songcontext.api.SongContext;
import org.openide.windows.OnShowing;

/**
 * Listen to pre-playback events, show the FixMidiMixDialog and fix the instruments if needed.
 */
@OnShowing              // Used only to get the automatic object creation upon startup
public class FixMidiMixAction implements VetoableChangeListener, Runnable
{

    private static FixMidiMixDialog DIALOG;
    private static final Logger LOGGER = Logger.getLogger(FixMidiMixAction.class.getSimpleName());
    FixChoice savedChoice = FixChoice.CANCEL;

    public FixMidiMixAction()
    {
        // Register for song playback
        PlaybackSettings.getInstance().addPlaybackStartVetoableListener(this);
    }

    @Override
    public void run()
    {
        // Do nothing, we just use @OnShowing just to get the automatic object creation...
    }

    /**
     * Listen to pre-playback events.
     *
     * @param evt
     * @throws PropertyVetoException Not used
     */
    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException
    {
        LOGGER.log(Level.FINE, "vetoableChange() -- evt={0}", evt);   

        if (evt.getSource() != PlaybackSettings.getInstance()
                || !evt.getPropertyName().equals(PlaybackSettings.PROP_VETO_PRE_PLAYBACK)
                || !MusicController.getInstance().getState().equals(MusicController.State.STOPPED))  // Don't check in pause mode
        {
            return;
        }

        SongContext context = (SongContext) evt.getNewValue();
        if (context == null)
        {
            // No context, we can't check anything
            return;
        }


        MidiMix midiMix = context.getMidiMix();
        OutputSynth outputSynth = OutputSynthManager.getInstance().getDefaultOutputSynth();
        if (outputSynth == null)
        {
            return;
        }
        HashMap<Integer, Instrument> mapNewInstruments = outputSynth.getNeedFixInstruments(midiMix);
        List<Integer> reroutableChannels = midiMix.getChannelsNeedingDrumsRerouting(mapNewInstruments);

        if (!mapNewInstruments.isEmpty() || !reroutableChannels.isEmpty())
        {
            switch (savedChoice)
            {
                case CANCEL:
                    FixMidiMixDialog dialog = getDialog();
                    dialog.preset(mapNewInstruments, reroutableChannels, midiMix);
                    dialog.setVisible(true);
                    FixChoice choice = dialog.getUserChoice();
                    switch (choice)
                    {
                        case CANCEL:
                            throw new PropertyVetoException(null, evt); // null msg to prevent user notifications by exception handlers
                        case FIX:
                            outputSynth.fixInstruments(midiMix, true);
                            break;
                        case DONT_FIX:
                            // Do nothing, leave toBeFixedChannels empty
                            break;
                        default:
                            throw new IllegalStateException("choice=" + choice);   
                    }
                    if (dialog.isRememberChoiceSelected())
                    {
                        savedChoice = choice;
                    }
                    break;
                case FIX:
                    outputSynth.fixInstruments(midiMix, true);
                    break;
                case DONT_FIX:
                    // Do nothing, leave toBeFixedChannels empty
                    break;
                default:
                    throw new IllegalStateException("savedChoice=" + savedChoice);   
            }
        }
    }

    private FixMidiMixDialog getDialog()
    {
        if (DIALOG == null)
        {
            DIALOG = new FixMidiMixDialog();
        }
        return DIALOG;
    }

}
