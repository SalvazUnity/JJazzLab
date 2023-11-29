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
package org.jjazz.chordleadsheet.api.event;

import org.jjazz.chordleadsheet.api.ChordLeadSheet;

/**
 * An event to indicate that a high-level ChordLeadSheet action that changes the leadsheet has started or is complete.
 * <p>
 * All other ClsChangeEvents are always preceded and followed by one ClsActionEvent. This can be used by listener to group lower-level
 * change events by actionId. The actionId must be the corresponding method name from the ChordLeadSheet interface, e.g. "addItem".
 * <p>
 * There is the guarantee that if a start ClsActionEvent is received, the complete ClsActionEvent will be received on the same actionId.
 * It's possible that no lower-level change event occur between 2 started/complete action events on the same actionId.
 */
public class ClsActionEvent extends ClsChangeEvent
{

    private final boolean startedOrComplete;      // false = started
    private final String actionId;
    private final boolean isUndo;
    private final Object data;

    /**
     *
     * @param src
     * @param actionId          The corresponding method name from the ChordLeadSheet interface which performs the change, e.g. "addItem".
     * @param startedOrComplete False means action has started, true action is complete
     * @param undo              If true this action is part of
     * @param data              An optional data associated to the event
     */
    public ClsActionEvent(ChordLeadSheet src, String actionId, boolean startedOrComplete, boolean undo, Object data)
    {
        super(src);
        if (actionId == null)
        {
            throw new IllegalArgumentException("src=" + src + " actionId=" + actionId
                    + " startedOrComplete=" + startedOrComplete + " undo=" + undo);
        }
        this.startedOrComplete = startedOrComplete;
        this.actionId = actionId;
        this.isUndo = undo;
        this.data = data;
    }

    /**
     * An optional data associated to the event.
     * <p>
     * Check the source code to know which object is associated to which actionId.
     *
     * @return Can be null
     */
    public Object getData()
    {
        return data;
    }

    public boolean isActionStarted()
    {
        return !startedOrComplete;
    }

    public boolean isActionComplete()
    {
        return startedOrComplete;
    }

    public String getActionId()
    {
        return actionId;
    }

    public boolean isUndo()
    {
        return isUndo;
    }

    @Override
    public String toString()
    {
        return "ClsActionEvent(" + actionId + ", complete=" + startedOrComplete + ", isUndo=" + isUndo + ")";
    }
}
