/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright @2019 Jerome Lelasseux. All rights reserved.
 *
 * This file is part of the JJazzLab software.
 *
 * JJazzLab is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License (LGPLv3)
 * as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * JJazzLab is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JJazzLab.  If not, see <https://www.gnu.org/licenses/>
 *
 * Contributor(s):
 *
 */
package org.jjazz.rhythmstubs.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.midi.api.synths.InstrumentFamily;
import org.jjazz.midi.api.synths.GM1Bank;
import org.jjazz.midi.api.synths.GMSynth;
import org.jjazz.rhythm.api.MusicGenerationException;
import org.jjazz.rhythm.api.Rhythm;
import org.jjazz.rhythm.api.RhythmFeatures;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.rhythm.api.RhythmVoice.Type;
import org.jjazz.rhythm.api.rhythmparameters.RP_STD_Variation;

/**
 * A dummy rhythm with only one STD_Variation RhythmParameter.
 */
public class DummyRhythm implements Rhythm
{

    protected String name;
    protected TimeSignature timeSignature;
    protected RhythmFeatures features;

    /**
     * The default RhythmParameters associated to this rhythm.
     */
    protected ArrayList<RhythmParameter<?>> rhythmParameters = new ArrayList<>();
    /**
     * The supported RhythmVoices.
     */
    protected ArrayList<RhythmVoice> rhythmVoices = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DummyRhythm.class.getSimpleName());

    /**
     * Create a dummy rhythm for specified time signature.
     *
     * @param name
     * @param ts
     */
    public DummyRhythm(String name, TimeSignature ts)
    {
        if (ts == null)
        {
            throw new NullPointerException("ts");   
        }

        this.name = name;
        this.timeSignature = ts;
        // Our Rhythm Parameters
        rhythmParameters.add(new RP_STD_Variation(true));

        // Rhythm voices
        GM1Bank gmb = GMSynth.getInstance().getGM1Bank();
        rhythmVoices.add(new RhythmVoice(this, Type.BASS, "Bass", gmb.getDefaultInstrument(InstrumentFamily.Bass), 11));

        features = new RhythmFeatures();

    }

    @Override
    public boolean equals(Object o)
    {
        boolean res = false;
        if (o instanceof DummyRhythm)
        {
            DummyRhythm ar = (DummyRhythm) o;
            res = ar.getUniqueId().equals(getUniqueId());
        }
        return res;
    }

    @Override
    public TimeSignature getTimeSignature()
    {
        return timeSignature;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 47 * hash + Objects.hashCode(this.getUniqueId());
        return hash;
    }

    @Override
    public List<RhythmVoice> getRhythmVoices()
    {
        return new ArrayList<>(rhythmVoices);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<RhythmParameter<?>> getRhythmParameters()
    {
        return new ArrayList<>(rhythmParameters);
    }

    @Override
    public RhythmFeatures getFeatures()
    {
        return features;
    }

    @Override
    public void loadResources() throws MusicGenerationException
    {
        // Do nothing
    }

    /**
     *
     * @return true
     */
    @Override
    public boolean isResourcesLoaded()
    {
        return true;
    }

    /**
     * This implementation does nothing.
     */
    @Override
    public void releaseResources()
    {
        // Do nothing
    }

    @Override
    public int compareTo(Rhythm o)
    {
        return getName().compareTo(o.getName());
    }

    @Override
    public File getFile()
    {
        return new File("");
    }

    @Override
    public String getUniqueId()
    {
        return name + "-ID";
    }

    @Override
    public String getDescription()
    {
        return "Dummy description";
    }

    @Override
    public int getPreferredTempo()
    {
        return 120;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getAuthor()
    {
        return "JL";
    }

    @Override
    public String[] getTags()
    {
        return new String[]
        {
            "dummy"
        };
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        // Nothing
    }

}
