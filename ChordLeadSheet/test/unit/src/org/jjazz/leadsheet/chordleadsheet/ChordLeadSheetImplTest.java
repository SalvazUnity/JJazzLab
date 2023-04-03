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
package org.jjazz.leadsheet.chordleadsheet;

import java.text.ParseException;
import java.util.List;
import java.util.TreeSet;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.leadsheet.chordleadsheet.api.UnsupportedEditException;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_Section;
import org.jjazz.leadsheet.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.leadsheet.chordleadsheet.api.item.ExtChordSymbol;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_SectionImpl;
import org.jjazz.leadsheet.chordleadsheet.item.CLI_ChordSymbolImpl;
import org.jjazz.leadsheet.chordleadsheet.api.item.Position;
import org.jjazz.undomanager.api.JJazzUndoManager;
import static org.junit.Assert.assertTrue;
import org.junit.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import org.openide.util.Exceptions;

public class ChordLeadSheetImplTest
{

    JJazzUndoManager undoManager;
    ChordLeadSheetImpl cls1;
    ChordLeadSheetImpl cls2;
    CLI_SectionImpl cliSection34_b3;
    CLI_SectionImpl cliSection44_b4;
    CLI_SectionImpl cliSection54_b5;
    CLI_ChordSymbolImpl cliChordSymbolF_b3_3;
    CLI_ChordSymbolImpl cliChordSymbolG_b6_0;
    CLI_ChordSymbolImpl cliChordSymbolA_b12_2;

    public ChordLeadSheetImplTest()
    {
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
    }

    @Before
    public void setUp()
    {
        undoManager = new JJazzUndoManager();

        cls1 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        cls2 = new ChordLeadSheetImpl("Section1", TimeSignature.FOUR_FOUR, 8);
        try
        {
            // Test leadsheet init
            cls1.setSizeInBars(8);
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Dm7"), new Position(0, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("F#7"), new Position(1, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Bbmaj7#5"), new Position(1, 3)));
            cls1.addSection(new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("D7b9b5"), new Position(2, 0)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("FM7#11"), new Position(4, 1)));
            cls1.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Eb7b9#5"), new Position(5, 0.75f)));
            cls1.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Db"), new Position(7, 3f)));

            cls1.addUndoableEditListener(undoManager);

            // COPY
            cls2.setSizeInBars(8);
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Dm7"), new Position(0, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("F#7"), new Position(1, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Bbmaj7#5"), new Position(1, 3)));
            cls2.addSection(new CLI_SectionImpl("Section2", TimeSignature.THREE_FOUR, 2));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("D7b9b5"), new Position(2, 0)));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("FM7#11"), new Position(4, 1)));
            cls2.addSection(new CLI_SectionImpl("Section3", TimeSignature.FOUR_FOUR, 5));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Eb7b9#5"), new Position(5, 0.75f)));
            cls2.addItem(new CLI_ChordSymbolImpl(ExtChordSymbol.get("Db"), new Position(7, 3f)));

            // Items to play with
            cliSection34_b3 = new CLI_SectionImpl("NewSection34", TimeSignature.THREE_FOUR, 3);
            cliSection44_b4 = new CLI_SectionImpl("NewSection44", TimeSignature.FOUR_FOUR, 4);
            cliSection54_b5 = new CLI_SectionImpl("NewSection54", TimeSignature.FIVE_FOUR, 5);
            cliChordSymbolF_b3_3 = new CLI_ChordSymbolImpl(ExtChordSymbol.get("F-"), new Position(3, 3));
            cliChordSymbolG_b6_0 = new CLI_ChordSymbolImpl(ExtChordSymbol.get("G-"), new Position(6, 0));
            cliChordSymbolA_b12_2 = new CLI_ChordSymbolImpl(ExtChordSymbol.get("A-"), new Position(12, 2));

            // Start one edit
            undoManager.startCEdit("UT-edit");

        } catch (ParseException ex)
        {
            throw new IllegalStateException("ParseException ex=" + ex);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @After
    public void tearDown()
    {
        undoManager.endCEdit("UT-edit");
        System.out.println(cls1.toDumpString() + '\n');
        undoAll();
        redoAll();
        undoAll();
        assertTrue(diffCls(cls1, cls2));
    }

    // getItems() --------------------------------------------------
    @Test
    public void testChordLeadSheetItemCreate() throws ParseException
    {
        System.out.println("=== testGetItemsCustom() ");
        TreeSet<ChordLeadSheetItem> items = new TreeSet<>();
        CLI_Section initSection = new CLI_SectionImpl("NewSection34", TimeSignature.THREE_FOUR, 0);
        var chord0 = new CLI_ChordSymbolImpl(ExtChordSymbol.get("Dm7"), new Position(0, 0));
        var chord4 = new CLI_ChordSymbolImpl(ExtChordSymbol.get("C"), new Position(4, 0));
        items.add(initSection);
        items.add(chord0);
        items.add(cliChordSymbolF_b3_3);
        items.add(cliSection44_b4);
        items.add(chord4);
        items.add(cliChordSymbolG_b6_0);
        System.out.println("all items=" + items);

        var res = items.subSet(initSection, true, cliChordSymbolG_b6_0, false);
        System.out.println("subItems1(true-false=" + res);
        res = items.subSet(initSection, false, cliChordSymbolG_b6_0, true);
        System.out.println("subItems2(false-true)=" + res);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), true), true, cliChordSymbolG_b6_0, true);
        System.out.println("subItems3(true0, true)=" + res);
        assertSame(res.first(), initSection);
        assertSame(res.last(), cliChordSymbolG_b6_0);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(6, 0), true), true);
        System.out.println("subItems4(false0, true6)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), cliChordSymbolG_b6_0);


        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(6, 0), false), false);
        System.out.println("subItems5(false0, false6)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), chord4);


        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(4, 0), true), true);
        System.out.println("subItems6(false0, true4)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), chord4);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), false), false,
                ChordLeadSheetItem.createItemTo(new Position(4, 0), false), false);
        System.out.println("subItems7(false0, false4)=" + res);
        assertSame(res.first(), cliChordSymbolF_b3_3);
        assertSame(res.last(), cliChordSymbolF_b3_3);

        res = items.subSet(ChordLeadSheetItem.createItemFrom(new Position(0, 0), true), true,
                ChordLeadSheetItem.createItemTo(new Position(4, 0), false), false);
        System.out.println("subItems8(true0, false4)=" + res);
        assertSame(res.first(), initSection);
        assertSame(res.last(), cliChordSymbolF_b3_3);

    }

    @Test
    public void testGetItems()
    {
        System.out.println("=== getItems() ");
        var res = cls1.getItems(new Position(0, 0),
                true,
                new Position(2, 0),
                false,
                ChordLeadSheetItem.class,
                cli -> true);
        assertEquals(res.size(), 4);
        assertSame(res.get(0), cls1.getSection(0));
        assertEquals(res.get(3).getPosition(), new Position(1, 3));


        res = cls1.getItems(new Position(0, 0),
                true,
                new Position(2, 0),
                true,
                ChordLeadSheetItem.class,
                cli -> true);
        assertEquals(res.size(), 6);
        assertSame(res.get(0), cls1.getSection(0));
        assertSame(res.get(4), cls1.getSection(2));


        res = cls1.getItems(new Position(0, 0),
                false,
                new Position(2, 0),
                true,
                ChordLeadSheetItem.class,
                cli -> true);
        assertEquals(res.size(), 4);
        assertTrue(res.get(0) != cls1.getSection(0));
        assertEquals(res.get(0).getPosition(), new Position(1, 0));
        assertSame(res.get(2), cls1.getSection(2));


        res = cls1.getItems(new Position(4, 1),
                false,
                new Position(7, 3f),
                false,
                ChordLeadSheetItem.class,
                cli -> true);
        assertEquals(res.size(), 2);
        assertSame(res.get(0), cls1.getSection(5));
        assertEquals(res.get(1).getPosition(), new Position(5, 0.75f));

    }

    // AddItem() --------------------------------------------------
    @Test
    public void testAddItem()
    {
        System.out.println("=== addItem ChordSymbol");
        cls1.addItem(cliChordSymbolG_b6_0);
        assertTrue(cls1.getItems(6, 6, ChordLeadSheetItem.class).get(0) == cliChordSymbolG_b6_0);
    }

    @Test
    public void testAddItemOutOfTimeSignature()
    {
        System.out.println("=== addItem ChordSymbol out of time signature");
        cls1.addItem(cliChordSymbolF_b3_3);
        assertTrue(cliChordSymbolF_b3_3.getPosition().getBeat() == 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddItemOutOfBounds()
    {
        System.out.println("=== addItem ChordSymbol out of bounds");
        cls1.addItem(cliChordSymbolA_b12_2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddItemAsSection()
    {
        System.out.println("=== addItem as Section");
        cls1.addItem(cliSection34_b3);
    }

    // AddSection() --------------------------------------------------
    @Test
    public void testAddSection()
    {
        System.out.println("=== addSection");
        try
        {
            cls1.addSection(cliSection34_b3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        var res = cls1.getItems(cliSection34_b3, ChordLeadSheetItem.class);
        System.out.println("res=" + res);
        assertEquals(1, res.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddSectionOver()
    {
        System.out.println("=== addSection over another");
        try
        {
            cls1.addSection(cliSection54_b5);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @Test
    public void testAddSectionAdjustItemPosition()
    {
        System.out.println("=== addSection adjust position of items");
        cliSection34_b3.setPosition(new Position(1, 0));
        try
        {
            cls1.addSection(cliSection34_b3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertSame(cls1.getItems(1, 1, CLI_Section.class).get(0), cliSection34_b3);
        var res = cls1.getItems(cliSection34_b3, ChordLeadSheetItem.class);
        assertEquals(new Position(1, 2), res.get(1).getPosition());
    }

    // RemoveSection() --------------------------------------------------
    @Test
    public void testRemoveSection()
    {
        System.out.println("=== RemoveSection Section2");
        CLI_Section cliSection = cls1.getSection("Section2");
        try
        {
            cls1.removeSection(cliSection);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(5, cls1.getBarRange(cls1.getSection(0)).size());
    }

    @Test
    public void testRemoveSectionAndAdjustItemPositions()
    {
        System.out.println("=== RemoveSection and adjust items positions");
        CLI_Section cliSection = cls1.getSection("Section3");
        try
        {
            cls1.removeSection(cliSection);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(new Position( 7,2), cls1.getItems(cls1.getSection("Section2"), ChordLeadSheetItem.class).get(3).getPosition());
    }

    // MoveItem() --------------------------------------------------
    @Test
    public void testMoveItemAndAdjustPosition()
    {
        System.out.println("=== MoveItem and adjust item position");
        ChordLeadSheetItem<?> cli = cls1.getItems(1, 1, ChordLeadSheetItem.class).get(1);
        cls1.moveItem(cli, new Position(2, 3));
        assertEquals(new Position(2, 2), cli.getPosition());
    }

    // MoveSection() --------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testMoveSection0()
    {
        System.out.println("=== testMoveSection0 move init section");
        CLI_Section cliSection0 = cls1.getSection(0);
        try
        {
            cls1.moveSection(cliSection0, 3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(new Position(2, 2), cliSection0.getPosition());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMoveSection1()
    {
        System.out.println("=== testMoveSection1 move section on another");
        CLI_Section cliSection0 = cls1.getSection(5);
        try
        {
            cls1.moveSection(cliSection0, 2);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
    }

    @Test
    public void testMoveSection2()
    {
        System.out.println("=== testMoveSection2 moved section does not cross other sections");
        CLI_Section cliSection0 = cls1.getSection(2);
        try
        {
            cls1.moveSection(cliSection0, 1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(cls1.getItems(1, 1, CLI_ChordSymbol.class).get(1).getPosition().equals(new Position(1, 2)));
        assertEquals(1, cls1.getSection(1).getPosition().getBar());
    }

    @Test
    public void testMoveSection3()
    {
        System.out.println("=== testMoveSection3 moved section crosses other sections");
        CLI_Section cliSection0 = cls1.getSection(5);
        try
        {
            cls1.moveSection(cliSection0, 1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(cls1.getItems(7, 7, ChordLeadSheetItem.class, cli -> true).get(0).getPosition().equals(new Position(7, 2)));
        assertSame(cliSection0, cls1.getSection(1));
    }

    // InsertBars() --------------------------------------------------
    @Test
    public void testInsertBars0()
    {
        System.out.println("insertBars start of leadsheet bar=0  nbBars=3");
        cls1.insertBars(0, 3);
        assertEquals(11, cls1.getSizeInBars());
        assertEquals(0, cls1.getSection(0).getPosition().getBar());
        assertEquals("Section2", cls1.getSection(5).getData().getName());
    }

    @Test
    public void testInsertBars2()
    {
        System.out.println("insertBars end of section bar=2  nbBars=2");
        cls1.insertBars(2, 2);
        assertEquals(10, cls1.getSizeInBars());
        assertEquals("Section2", cls1.getSection(5).getData().getName());
    }

    @Test
    public void testInsertBars6()
    {
        System.out.println("insertBars middle of section bar=6  nbBars=2");
        cls1.insertBars(6, 2);
        assertEquals(10, cls1.getSizeInBars());
    }

    @Test
    public void testInsertBars8()
    {
        System.out.println("insertBars end of leadsheet last bar=8  nbBars=5");
        cls1.insertBars(8, 5);
        assertEquals(13, cls1.getSizeInBars());
    }

    // DeleteBars() --------------------------------------------------
    @Test
    public void testDeleteBarsFromStartUntilEndOfSection()
    {
        System.out.println("deleteBars from start until end of section barFrom=0  barTo=1");
        try
        {
            cls1.deleteBars(0, 1);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(6, cls1.getSizeInBars());
        assertEquals(2, cls1.getItems(CLI_Section.class).size());
        assertEquals("Section2", cls1.getSection(0).getData().getName());
    }

    @Test
    public void testDeleteBarsFromStartUntilMiddleOfSection()
    {
        System.out.println("deleteBars from start middle of section barFrom=0  barTo=3");
        try
        {
            cls1.deleteBars(0, 3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        CLI_Section cliSection0 = cls1.getSection(0);
        assertEquals(4, cls1.getSizeInBars());
        assertEquals("Section1", cliSection0.getData().getName());
        assertEquals(1, cls1.getBarRange(cliSection0).size());
    }

    @Test
    public void testDeleteInitialBarWithSectionOnBar1()
    {
        System.out.println("deleteBars on initial bar 0 with a section on bar 1");
        CLI_Section newSection = new CLI_SectionImpl("SectionBar1", TimeSignature.FIVE_FOUR, 1);
        try
        {
            cls1.addSection(newSection);
            cls1.deleteBars(0, 0);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertTrue(cls1.getSizeInBars() == 7 && cls1.getSection(0).getData().equals(newSection.getData()));
    }

    // delete with item position adjustment
    @Test
    public void testDeleteBarsToEnd()
    {
        System.out.println("deleteBars until end barFrom=4  barTo=7");
        try
        {
            cls1.deleteBars(4, 7);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(cls1.getSizeInBars(), 4);
    }

    @Test
    public void testDeleteBarsMiddle()
    {
        System.out.println("deleteBars multi sections barFrom=1  barTo=5");
        try
        {
            cls1.deleteBars(1, 5);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(3, cls1.getSizeInBars());
        assertEquals("Section1", cls1.getSection(2).getData().getName());
    }

    // SetSection() --------------------------------------------------
    @Test
    public void testSetSectionTimeSignature()
    {
        System.out.println("setSectionTimeSignature section 0 => 3/4");
        CLI_Section cliSection0 = cls1.getSection(0);
        try
        {
            cls1.setSectionTimeSignature(cliSection0, TimeSignature.THREE_FOUR);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        var res = cls1.getItems(1, 1, ChordLeadSheetItem.class, cli -> true);
        assertEquals(new Position(1, 2f), res.get(1).getPosition());
    }

    @Test
    public void testSetSectionNameOK()
    {
        System.out.println("setSectionName Yeaaaah section 0");
        CLI_Section cliSection0 = cls1.getSection(0);
        cls1.setSectionName(cliSection0, "Yeaaah");
        assertEquals("Yeaaah", cliSection0.getData().getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetSectionNameAlreadyExist()
    {
        System.out.println("setSectionName name already exist");
        CLI_Section cliSection0 = cls1.getSection(0);
        cls1.setSectionName(cliSection0, "Section3");
    }

    // SetSize() --------------------------------------------------
    @Test
    public void testSetSize()
    {
        System.out.println("setSize() new size = 3");
        try
        {
            cls1.setSizeInBars(3);
        } catch (UnsupportedEditException ex)
        {
            Exceptions.printStackTrace(ex);
        }
        assertEquals(3, cls1.getSizeInBars());
        assertEquals(6, cls1.getItems(ChordLeadSheetItem.class).size());
    }

    // Undo --------------------------------------------------
    private void undoAll()
    {
        while (undoManager.canUndo())
        {
            undoManager.undo();
        }
    }

    private void redoAll()
    {
        while (undoManager.canRedo())
        {
            undoManager.redo();
        }
    }

    private boolean diffCls(ChordLeadSheetImpl ls1, ChordLeadSheetImpl ls2)
    {
        if (ls1.getSizeInBars() != ls2.getSizeInBars())
        {
            System.out.println("DIFF difference size ls1=" + ls1.toDumpString() + '\n');
            return false;
        }
        List<? extends ChordLeadSheetItem> items1 = ls1.getItems(ChordLeadSheetItem.class);
        List<? extends ChordLeadSheetItem> items2 = ls2.getItems(ChordLeadSheetItem.class);
        if (items1.size() != items2.size())
        {
            System.out.println("DIFF difference items1 items2 size ls1=" + ls1.toDumpString() + '\n');
            return false;
        }
        int index = 0;
        for (ChordLeadSheetItem<?> item : items1)
        {
            if (!itemEquals(item, items2.get(index)))
            {
                System.out.println("DIFF difference " + item + "/" + items2.get(index) + " ls1=" + ls1.toDumpString() + '\n');
                return false;
            }
            index++;
        }
        return true;
    }

    private boolean itemEquals(ChordLeadSheetItem<?> item1, ChordLeadSheetItem<?> item2)
    {
        if (item1 instanceof CLI_ChordSymbol)
        {
            if (!(item2 instanceof CLI_ChordSymbol))
            {
                return false;
            }
            CLI_ChordSymbol cs1 = (CLI_ChordSymbol) item1;
            CLI_ChordSymbol cs2 = (CLI_ChordSymbol) item2;
            return cs1.getData().equals(cs2.getData()) && cs1.getPosition().equals(cs2.getPosition());
        } else if (item1 instanceof CLI_Section)
        {
            if (!(item2 instanceof CLI_Section))
            {
                return false;
            }
            CLI_Section b1 = (CLI_Section) item1;
            CLI_Section b2 = (CLI_Section) item2;
            return b1.getData().equals(b2.getData()) && b1.getPosition().equals(b2.getPosition());
        } else
        {
            return false;
        }
    }
}
