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
package org.jjazz.ss_editor.actions;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import org.jjazz.ss_editor.api.SS_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Objects;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.midimix.api.MidiMixManager;
import org.jjazz.rhythm.api.RhythmParameter;
import org.jjazz.ss_editor.api.SS_SelectionUtilities;
import org.jjazz.songstructure.api.SongPartParameter;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.jjazz.songstructure.api.SongStructure;
import org.jjazz.songstructure.api.SongPart;
import org.jjazz.ss_editor.api.SS_ContextActionListener;
import org.jjazz.utilities.api.ResUtil;
import org.jjazz.song.api.Song;
import org.jjazz.songcontext.api.SongPartContext;
import org.jjazz.ss_editor.api.SS_Editor;
import org.jjazz.ss_editor.api.SS_EditorTopComponent;
import org.openide.util.Exceptions;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import org.jjazz.ss_editor.rpviewer.spi.RpCustomEditorFactory;

@ActionID(category = "JJazz", id = "org.jjazz.ss_editor.actions.editrpwithcustomeditor")
@ActionRegistration(displayName = "#CTL_EditRhythmParameter", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/RhythmParameter", position = 10),
        })
public final class EditRpWithCustomEditor extends AbstractAction implements ContextAwareAction, SS_ContextActionListener
{

    private Lookup context;
    private SS_ContextActionSupport cap;
    private String undoText = ResUtil.getString(getClass(), "CTL_EditRhythmParameter");

    public EditRpWithCustomEditor()
    {
        this(Utilities.actionsGlobalContext());
    }

    public EditRpWithCustomEditor(Lookup context)
    {
        this.context = context;
        cap = SS_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);                          // For popupmenu 
    }

    @SuppressWarnings(
            {
                "unchecked", "rawtypes"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        SS_SelectionUtilities selection = cap.getSelection();
        List<SongPartParameter> sptps = selection.getSelectedSongPartParameters();
        RhythmParameter<?> rp0 = sptps.get(0).getRp();
        SongPart spt0 = sptps.get(0).getSpt();


        // Open custom editor if supported
        var factory = RpCustomEditorFactory.findFactory(rp0);
        if (factory != null)
        {
            SS_Editor editor = SS_EditorTopComponent.getActive().getEditor();


            // Prepare our dialog
            Song song = editor.getSongModel();
            MidiMix mm = null;
            try
            {
                mm = MidiMixManager.getInstance().findMix(song);
            } catch (MidiUnavailableException ex)
            {
                // Should never happen 
                Exceptions.printStackTrace(ex);
                return;
            }
            SongPartContext sptContext = new SongPartContext(song, mm, spt0);
            Object value = spt0.getRPValue(rp0);
            var dlgEditor = factory.getEditor((RhythmParameter) rp0);
            assert dlgEditor != null : "rp=" + rp0;
            dlgEditor.preset(value, sptContext);


            // Set location
            Rectangle r = editor.getRpViewerRectangle(spt0, rp0);
            Point p = r.getLocation();
            int x = p.x - ((dlgEditor.getWidth() - r.width) / 2);
            int y = p.y - dlgEditor.getHeight();
            x = Math.max(x, 0);
            y = Math.max(y, 0);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int delta = x + dlgEditor.getWidth() - screenSize.width;
            if (delta > 3)
            {
                x -= delta;
            }
            delta = y + dlgEditor.getHeight() - screenSize.height;
            if (delta > 3)
            {
                y -= delta;
            }
            dlgEditor.setLocation(x, y);
            dlgEditor.setVisible(true);


            // Process the result
            Object newValue = dlgEditor.getRpValue();
            if (dlgEditor.isExitOk() && !Objects.equals(value, newValue))
            {
                SongStructure sgs = editor.getModel();
                JJazzUndoManagerFinder.getDefault().get(sgs).startCEdit(undoText);

                for (SongPartParameter sptp : sptps)
                {
                    sgs.setRhythmParameterValue(sptp.getSpt(), (RhythmParameter) sptp.getRp(), newValue);
                }

                JJazzUndoManagerFinder.getDefault().get(sgs).endCEdit(undoText);
            }

        } else
        {
            // Just highlight the SptEditor
            TopComponent tcSptEditor = WindowManager.getDefault().findTopComponent("SptEditorTopComponent");
            if (tcSptEditor != null)
            {
                tcSptEditor.requestVisible();
                tcSptEditor.requestAttention(true);
            }
        }

    }

    @Override
    public void selectionChange(SS_SelectionUtilities selection)
    {
        setEnabled(selection.isRhythmParameterSelected());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new EditRpWithCustomEditor(context);
    }

}
