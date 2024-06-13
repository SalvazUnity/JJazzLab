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
package org.jjazz.rhythmselectiondialog.api;

import org.jjazz.rhythmselectiondialog.spi.RhythmPreviewer;
import javax.swing.JDialog;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.openide.windows.WindowManager;

/**
 * A dialog to pick a rhythm, possibly with rhythm previewing capability.
 * <p>
 */
abstract public class RhythmSelectionDialog extends JDialog
{


    /**
     * Dialog is automatically owned by WindowManager.getDefault().getMainWindow()
     */
    protected RhythmSelectionDialog()
    {
        super(WindowManager.getDefault().getMainWindow());
    }

    /**
     * Initialize the dialog for the specified song rhythm.
     *
     * @param ri
     * @param rpp If null then the rhythm preview feature is disabled. If not null caller is responsible to call rpp.cleanup() when rpp is not used anymore.
     */
    abstract public void preset(RhythmInfo ri, RhythmPreviewer rpp);

    /**
     * @return True if dialog was exited OK, false if dialog operation was cancelled.
     */
    abstract public boolean isExitOk();

    /**
     * Return the selected rhythm.
     *
     * @return Null if no valid rhythm was selected, or user chose Cancel
     */
    abstract public RhythmInfo getSelectedRhythm();

    /**
     * Set the title of the dialogm eg "Rhythm for bar XX".
     *
     * @param title
     */
    abstract public void setTitleText(String title);


    /**
     * Return if the rhythm should be also applied to the next songParts which have the same rhythm than the sptModel.
     *
     * @return
     */
    abstract public boolean isApplyRhythmToNextSongParts();

    /**
     * Return if the rhythm's preferred tempo should be applied to current song
     *
     * @return
     */
    abstract public boolean isUseRhythmTempo();

    /**
     * Cleanup references to preset data and dialog results.
     */
    abstract public void cleanup();


}
