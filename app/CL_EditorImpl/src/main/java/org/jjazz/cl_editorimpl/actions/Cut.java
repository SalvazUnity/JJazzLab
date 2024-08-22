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
package org.jjazz.cl_editorimpl.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import org.jjazz.cl_editor.api.CL_ContextActionListener;
import org.jjazz.cl_editor.api.CL_ContextActionSupport;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import static javax.swing.Action.ACCELERATOR_KEY;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.cl_editorimpl.ItemsTransferable;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editorimpl.BarsTransferable;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.undomanager.api.JJazzUndoManager;
import org.jjazz.undomanager.api.JJazzUndoManagerFinder;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.CutAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.cut")
@ActionRegistration(displayName = "cl-cut-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 1000),
            @ActionReference(path = "Actions/ChordSymbol", position = 1000, separatorBefore = 950),
            @ActionReference(path = "Actions/Bar", position = 1000),
            @ActionReference(path = "Actions/BarAnnotation", position = 1000, separatorBefore = 999)
        })
public class Cut extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, ClipboardOwner
{

    private Lookup context;
    private CL_ContextActionSupport cap;
    private final String undoText = ResUtil.getCommonString("CTL_Cut");

    public Cut()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Cut(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        Icon icon = SystemAction.get(CutAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_X));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Cut(context);
    }

    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    @Override
    public void actionPerformed(ActionEvent e)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        Transferable t = null;
        List<ChordLeadSheetItem> items = new ArrayList<>();

        
        JJazzUndoManager um = JJazzUndoManagerFinder.getDefault().get(cls);
        um.startCEdit(undoText);


        // Prepare the transferable        
        if (selection.isBarSelectedWithinCls())
        {
            for (Integer modelBarIndex : selection.getSelectedBarIndexesWithinCls())
            {
                items.addAll(cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
            }


            IntRange barRange = selection.getBarRangeWithinCls();
            assert barRange != null;
            var data = new BarsTransferable.Data(barRange, items);
            t = new BarsTransferable(data);

            try
            {
                cls.deleteBars(barRange.from, barRange.to);
            } catch (UnsupportedEditException ex)
            {
                String msg = "Impossible to cut bars.\n" + ex.getLocalizedMessage();
                um.abortCEdit(undoText, msg);
                return;
            }
        } else if (selection.isItemSelected())
        {
            items.addAll(selection.getSelectedItems());

            var data = new ItemsTransferable.Data(items);
            t = new ItemsTransferable(data);


            // Remove the items
            for (ChordLeadSheetItem item : items)
            {
                if (item instanceof CLI_Section)
                {
                    CLI_Section section = (CLI_Section) item;
                    if (section.getPosition().getBar() > 0)
                    {
                        try
                        {
                            cls.removeSection(section);
                        } catch (UnsupportedEditException ex)
                        {
                            String msg = "Impossible to cut section " + section.getData().getName() + ".\n" + ex.getLocalizedMessage();
                            um.abortCEdit(undoText, msg);
                            return;
                        }
                    }
                } else
                {
                    cls.removeItem(item);
                }
            }
        }


        // Store into clipboard
        assert t != null;
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(t, this);
        
        

        um.endCEdit(undoText);
    }

    @Override
    public void selectionChange(CL_SelectionUtilities selection)
    {
        boolean b = false;
        if (selection.isItemSelected() || selection.isContiguousBarboxSelectionWithinCls())
        {
            b = true;
        }
        setEnabled(b);
    }

    @Override
    public void sizeChanged(int oldSize, int newSize)
    {
        selectionChange(cap.getSelection());
    }
    

    // =========================================================================================================
    // ClipboardOwner
    // =========================================================================================================    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents)
    {
        // Nothing
    }
    
    
}
