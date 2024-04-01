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
package org.jjazz.ss_editorimpl.actions;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.songstructure.api.SongPart;

public class JumpToEnd extends AbstractAction
{

    @Override
    public void actionPerformed(ActionEvent e)
    {
        var activeTc = SS_EditorTopComponent.getActive();
        if (activeTc == null)
        {
            return;
        }
        SS_Editor editor = activeTc.getEditor();
        SS_SelectionUtilities selection = new SS_SelectionUtilities(editor.getLookup());
        SongPart spt = editor.getModel().getSongParts().get(editor.getModel().getSongParts().size() - 1);
        if (selection.isSongPartSelected() || selection.isEmpty())
        {
            selection.unselectAll(editor);
            editor.selectSongPart(spt, true);
            editor.setFocusOnSongPart(spt);
            editor.makeSptViewerVisible(spt);
        } else
        {
            SongPartParameter sptp = selection.getSelectedSongPartParameters().get(selection.getSelectedSongPartParameters().size() - 1);
            selection.unselectAll(editor);
            // Find first compatible rp
            RhythmParameter<?> rp = RhythmParameter.findFirstCompatibleRp(spt.getRhythm().getRhythmParameters(), sptp.getRp());
            if (rp != null)
            {
                editor.selectRhythmParameter(spt, rp, true);
                editor.setFocusOnRhythmParameter(spt, rp);
                editor.makeSptViewerVisible(spt);
            } else
            {
                editor.selectSongPart(spt, true);
                editor.setFocusOnSongPart(spt);
                editor.makeSptViewerVisible(spt);
            }
        }
    }
}
