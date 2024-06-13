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
package org.jjazz.musiccontrol.api;

import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.harmony.api.Position;
import org.jjazz.songstructure.api.SongPart;

/**
 * Listener of events occuring during song playback.
 * <p>
 * MusicController will fire the events taking into account the user-defined output synth latency. Events are fired by the
 * MusicController on the Event Dispatching Thread. Event handling must be time-efficient.
 * <p>
 */
public interface PlaybackListener
{

    /**
     * The enable status has changed.
     *
     * @param b If false the MusicController is not able anymore to send events, except for the midiActivity which will continue
     * to work.
     */
    void enabledChanged(boolean b);

    /**
     * Called on each beat change.
     *
     * @param oldPos
     * @param newPos
     * @param newPosInBeats Might be -1 when information is not available
     */
    void beatChanged(Position oldPos, Position newPos, float newPosInBeats);

     /**
     * Called on chord symbol change.
     *
     * @param chordSymbol The current chord symbol with an absolute position (position within the entire song structure).
     */
    void chordSymbolChanged(CLI_ChordSymbol chordSymbol);

    /**
     * Called on SongPart change.
     *
     * @param spt
     */
    void songPartChanged(SongPart spt);

    /**
     * Indicates some musical activity on specified channel at specified time.
     * <p>
     * Should be used only for non-accurate "musical activity " indicators : if several notes are played in a brief period time,
     * only one event will be fired.
     *
     * @param tick The approximate tick of the Midi activity. Can be -1 if no tick information available.
     * @param channel
     */
    void midiActivity(long tick, int channel);
}
