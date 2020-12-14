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
package org.jjazz.rhythm.api;

import java.beans.PropertyChangeListener;
import java.io.File;
import org.jjazz.rhythm.parameters.RhythmParameter;
import java.util.List;
import org.jjazz.harmony.TimeSignature;
import org.openide.util.Lookup;

/**
 * A rhythm can generate music tracks for each of its supported rhythm voices.
 * <p>
 * The class is used as a descriptor of the rhythm, the actual rhythm's capabilites are MidiMusicGenerator object(s) which must be
 * put in the rhythm's lookup.<p>
 * The framework will call the rhythm's loadResources() before accessing the MidiMusicGenerator object(s). This allow to save
 * memory usage when rhythm object is only used in catalogs.
 * <p>
 */
public interface Rhythm extends Lookup.Provider, Comparable<Rhythm>
{

    public static final String PROP_RESOURCES_LOADED = "ResourcesLoaded";   //NOI18N 

    /**
     * The general features of this rhythm.
     *
     * @return
     */
    RhythmFeatures getFeatures();

    /**
     * Tell the rhythm it may load any memory-heavy resources.
     * <p>
     * This will fire a PROP_RESOURCES_LOADED change event with newValue=true.
     *
     * @throws MusicGenerationException
     * @see releaseResources()
     */
    void loadResources() throws MusicGenerationException;

    /**
     * Ask the rhythm to release any memory-heavy resources.
     * <p>
     * This will fire a PROP_RESOURCES_LOADED change event with newValue=false.
     *
     * @see loadResources()
     */
    void releaseResources();

    boolean isResourcesLoaded();

    /**
     * @return The voices for which this rhythm can generate music. Each voice must have a unique name.
     */
    List<RhythmVoice> getRhythmVoices();

    /**
     * @return The RhythmParameters that influence the way this rhythm generates music.
     */
    List<RhythmParameter<?>> getRhythmParameters();

    /**
     * Optional file from which this rhythm was loaded.
     *
     * @return Can't be null, but can be an empty path ("") if no file associated.
     */
    File getFile();

    /**
     * A unique string identifier representing this rhythm.
     * <p>
     * It will be used by other serialized objects who want to refer this rhythm -typically a Song object.
     *
     * @return A non-empty String with spaces trimmed.
     */
    String getUniqueId();

    String getDescription();

    int getPreferredTempo();

    TimeSignature getTimeSignature();

    String getName();

    String getAuthor();

    /**
     *
     * @return Default to "1"
     */
    default String getVersion()
    {
        return "1";
    }

    /**
     * Can be any keyword strings used to describe the rhythm.
     *
     * @return Default to an empty array.
     */
    default String[] getTags()
    {
        return new String[0];
    }

    /**
     * Compare alphabetically on the rhythm's name.
     *
     * @param o
     * @return
     */
    @Override
    default public int compareTo(Rhythm o)
    {
        return getName().compareTo(o.getName());
    }

    public void addPropertyChangeListener(PropertyChangeListener l);

    public void removePropertyChangeListener(PropertyChangeListener l);

}
