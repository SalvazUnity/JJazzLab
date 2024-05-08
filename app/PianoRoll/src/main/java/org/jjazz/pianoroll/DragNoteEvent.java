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
package org.jjazz.pianoroll;

import org.jjazz.phrase.api.NoteEvent;

/**
 * A special kind of NoteEvent only used to show the destination place during mouse dragging.
 */
public class DragNoteEvent extends NoteEvent
{

    public DragNoteEvent(int pitch, float dur, int vel, float posInBeats)
    {
        super(pitch, dur, vel, posInBeats);
    }

    @Override
    public DragNoteEvent setAll(int pitch, float duration, int velocity, float posInBeats, boolean copyProperties)
    {
        var res = new DragNoteEvent(pitch < 0 ? getPitch() : pitch,
                duration < 0 ? getDurationInBeats() : duration,
                velocity < 0 ? getVelocity() : velocity,
                posInBeats < 0 ? getPositionInBeats() : posInBeats
        );
        if (copyProperties)
        {
            res.getClientProperties().set(getClientProperties());
        }
        return res;
    }


    @Override
    public String toString()
    {
        return "DNE" + super.toString() + "id=" + System.identityHashCode(this);
    }
}
