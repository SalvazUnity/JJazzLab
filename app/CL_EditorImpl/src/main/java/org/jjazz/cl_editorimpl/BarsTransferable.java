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
package org.jjazz.cl_editorimpl;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.ChordLeadSheetFactory;
import org.jjazz.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.harmony.api.Position;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.importers.api.TextReader;
import org.jjazz.utilities.api.IntRange;
import org.openide.util.Exceptions;

/**
 * A transferable for copy/cut/paste operations on bars.
 * <p>
 * Provides 2 data flavors: custom DATA_FLAVOR and javaStringFlavor.
 */
public class BarsTransferable implements Transferable
{

    public static final DataFlavor DATA_FLAVOR = new DataFlavor(BarsTransferable.Data.class, "Items");
    public static final DataFlavor[] DATA_FLAVORS = new DataFlavor[]
    {
        DATA_FLAVOR, DataFlavor.stringFlavor
    };

    private final Data data;

    public BarsTransferable(Data data)
    {
        this.data = data;
    }


    @Override
    public DataFlavor[] getTransferDataFlavors()
    {
        return DATA_FLAVORS;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor)
    {
        return Arrays.asList(DATA_FLAVORS).contains(flavor);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException
    {
        if (!isDataFlavorSupported(flavor))
        {
            throw new UnsupportedFlavorException(flavor);
        }
        Object res = data;      // DATA_FLAVOR by default
        if (flavor == DataFlavor.stringFlavor)
        {
            res = TextReader.toText(buildCls(), 4);
        }

        return res;
    }


    // ========================================================================================================
    // Private methods
    // ========================================================================================================
    /**
     * Create a ChordLeadSheet from the stored items.
     * <p>
     * Shift items so that barRange.from becomes bar 0.
     *
     * @return
     */
    private ChordLeadSheet buildCls()
    {

        TimeSignature ts0 = TimeSignature.FOUR_FOUR;
        if (!data.items.isEmpty())
        {
            ChordLeadSheet cls = data.items.first().getContainer();
            if (cls != null)
            {
                ts0 = cls.getSection(data.barRange.from).getData().getTimeSignature();
            }
        }

        var res = ChordLeadSheetFactory.getDefault().createEmptyLeadSheet("A", ts0, data.barRange.size(), null);

        
        for (var item : data.getItemsCopy(0))
        {
            if (item instanceof CLI_Section sectionItem)
            {
                try
                {
                    if (sectionItem.getPosition().getBar() == 0)
                    {
                        CLI_Section s0 = res.getSection(0);
                        res.setSectionName(s0, sectionItem.getData().getName());
                        res.setSectionTimeSignature(s0, sectionItem.getData().getTimeSignature());
                    } else
                    {
                        res.addSection(sectionItem);
                    }
                } catch (UnsupportedEditException ex)
                {
                    // Should never happen
                    Exceptions.printStackTrace(ex);
                }
            } else
            {
                res.addItem(item);
            }
        }


        return res;
    }

    // ========================================================================================================
    // Inner classes
    // ========================================================================================================
    /**
     * Store a bar range plus an ordered list of ChordLeadSheetItems.
     */
    public static class Data
    {

        private final TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
        private final IntRange barRange;

        /**
         *
         * @param barRange
         * @param items    Can be empty
         */
        public Data(IntRange barRange, List<? extends ChordLeadSheetItem> items)
        {
            if (items == null || barRange == null)
            {
                throw new IllegalArgumentException("barRange=" + barRange + " items=" + items);
            }
            items.stream().forEach(item -> this.items.add(item.getCopy(null)));
            this.barRange = barRange;
        }

        /**
         * @return int The number of ChordLeadSheetItems
         */
        public int getItemsSize()
        {
            return items.size();
        }

        public IntRange getBarRange()
        {
            return barRange;
        }

        /**
         * Return a copy of the items adjusted to targetBarIndex and with the specified container.
         * <p>
         * The items are shifted so that barRange.from matches targetBarIndex.
         *
         * @param targetBarIndex The barIndex where items are copied to. If barIndex&lt;0, positions are not changed.
         * @return Items are returned ordered by position.
         */
        public List<ChordLeadSheetItem> getItemsCopy(int targetBarIndex)
        {
            List<ChordLeadSheetItem> res = new ArrayList<>();
            int minBarIndex = items.first().getPosition().getBar();
            int itemShift = minBarIndex - barRange.from;
            int barShift = targetBarIndex < 0 ? 0 : targetBarIndex + itemShift - minBarIndex;
            for (ChordLeadSheetItem<?> item : items)
            {
                Position newPos = item.getPosition().getMoved(barShift, 0);
                ChordLeadSheetItem<?> newItem = item.getCopy(newPos);
                res.add(newItem);
            }

            return res;
        }
    }
}
