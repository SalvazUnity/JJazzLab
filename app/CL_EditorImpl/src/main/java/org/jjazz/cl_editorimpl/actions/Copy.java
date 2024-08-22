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
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.Icon;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.cl_editorimpl.ItemsTransferable;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.cl_editorimpl.BarsTransferable;
import org.jjazz.cl_editor.api.CL_SelectionUtilities;
import static org.jjazz.uiutilities.api.UIUtilities.getGenericControlKeyStroke;
import org.jjazz.utilities.api.IntRange;
import org.jjazz.utilities.api.ResUtil;
import org.openide.actions.CopyAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.ContextAwareAction;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.actions.SystemAction;

@ActionID(category = "JJazz", id = "org.jjazz.cl_editor.actions.copy")
@ActionRegistration(displayName = "cl-copy-not-used", lazy = false)
@ActionReferences(
        {
            @ActionReference(path = "Actions/Section", position = 1100),
            @ActionReference(path = "Actions/ChordSymbol", position = 1100),
            @ActionReference(path = "Actions/Bar", position = 1100),
            @ActionReference(path = "Actions/BarAnnotation", position = 1010)
        })
public class Copy extends AbstractAction implements ContextAwareAction, CL_ContextActionListener, ClipboardOwner
{

    private final String undoText = ResUtil.getCommonString("CTL_Copy");
    private Lookup context;
    private CL_ContextActionSupport cap;

    public Copy()
    {
        this(Utilities.actionsGlobalContext());
    }

    private Copy(Lookup context)
    {
        this.context = context;
        cap = CL_ContextActionSupport.getInstance(this.context);
        cap.addListener(this);
        putValue(NAME, undoText);
        Icon icon = SystemAction.get(CopyAction.class).getIcon();
        putValue(SMALL_ICON, icon);
        putValue(ACCELERATOR_KEY, getGenericControlKeyStroke(KeyEvent.VK_C));
        selectionChange(cap.getSelection());
    }

    @Override
    public Action createContextAwareInstance(Lookup context)
    {
        return new Copy(context);
    }

    @SuppressWarnings(
            {
                "rawtypes",
                "unchecked"
            })
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        CL_SelectionUtilities selection = cap.getSelection();
        ChordLeadSheet cls = selection.getChordLeadSheet();
        Transferable t = null;
        List<ChordLeadSheetItem> items = new ArrayList<>();

        // Prepare the transferable
        if (selection.isBarSelectedWithinCls())
        {
            var clsBarIndexes = selection.getSelectedBarIndexesWithinCls();
            for (Integer modelBarIndex : clsBarIndexes)
            {
                items.addAll(cls.getItems(modelBarIndex, modelBarIndex, ChordLeadSheetItem.class));
            }

            IntRange barRange = selection.getBarRangeWithinCls();
            var firstSection = cls.getSection(clsBarIndexes.get(0)).getData();
            var data = new BarsTransferable.Data(firstSection, barRange, items);
            t = new BarsTransferable(data);

        } else if (selection.isItemSelected())
        {
            items.addAll(selection.getSelectedItems());
            var data = new ItemsTransferable.Data(items);
            t = new ItemsTransferable(data);
        }


        // Store into clipboard
        assert t != null;
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(t, this);
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
