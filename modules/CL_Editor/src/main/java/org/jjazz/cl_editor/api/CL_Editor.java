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
package org.jjazz.cl_editor.api;

import java.awt.Color;
import org.jjazz.cl_editor.spi.CL_EditorSettings;
import org.jjazz.quantizer.api.Quantization;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import javax.swing.JPanel;
import org.jjazz.chordleadsheet.api.ChordLeadSheet;
import org.jjazz.chordleadsheet.api.item.CLI_Section;
import org.jjazz.chordleadsheet.api.item.ChordLeadSheetItem;
import org.jjazz.chordleadsheet.api.item.Position;
import org.jjazz.song.api.Song;
import org.jjazz.cl_editor.barbox.api.BarBoxConfig;
import org.jjazz.cl_editor.barrenderer.api.BarRendererFactory;
import org.jjazz.itemrenderer.api.IR_Type;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;

/**
 * A ChordLeadSheet editor.
 * <p>
 * Editor's lookup contains :<br>
 * - editor's ActionMap<br>
 * - edited ChordLeadSheet<br>
 * - edited Song (container of the ChordLeadSheet if there is one)<br>
 * - editor's Zoomable instances<br>
 * - selected items or bars<p>
 * <p>
 * The editor creates BarBoxes using a BarBoxConfig based on the editor's default config, and on the BarRenderer types obtained from the BarRendererProvider
 * instances found in the global lookup.
 * <p>
 */
public abstract class CL_Editor extends JPanel implements Lookup.Provider
{

    // Property change events fired by this class
    /**
     * oldvalue=old nbCols, newValue=new nbCols.
     * <p>
     * Note that the NbColumns setting is serialized via the ZoomHFactor.
     */
    static public final String PROP_NB_COLUMNS = "PropNbColumns";
    /**
     * oldvalue=CLI_Section, newValue=new quantization value.
     * <p>
     * Don't change the String value, it's also used as client property name during CLI_Section serialization since JJazzLab 4.
     */
    static public final String PROP_SECTION_QUANTIZATION = "PropSectionQuantization";
    /**
     * oldvalue=CLI_Section, newValue=new color.
     * <p>
     * Don't change the String value, it's also used as client property name during CLI_Section serialization since JJazzLab 4.
     */
    static public final String PROP_SECTION_COLOR = "PropSectionColor";
    /**
     * oldvalue=CLI_Section, newValue=new boolean.
     * <p>
     * Don't change the String value, it's also used as client property name during CLI_Section serialization since JJazzLab 4.
     */
    static public final String PROP_SECTION_START_ON_NEW_LINE = "PropSectionStartOnNewLine";
    /**
     * oldValue=old boolean, newValue=new boolean
     * <p>
     * Don't change the String value, it's also used as a client property name during Song serialization since JJazzLab 4.
     * <p>
     */
    static public final String PROP_BAR_ANNOTATION_VISIBLE = "PropBarAnnotationsVisible";
    /**
     * oldValue=old number of liens, newValue=new number of lines
     * <p>
     * Don't change the String value, it's also used as a client property name during Song serialization since JJazzLab 4.
     * <p>
     */
    static public final String PROP_BAR_ANNOTATION_NB_LINES = "PropBarAnnotationsNbLines";


    // =====================================================================================================    
    // Methods
    // =====================================================================================================    
    abstract public void setEditorMouseListener(CL_EditorMouseListener brm);

    /**
     * @return The ChordLeadSheet model for this CL_Editor.
     */
    abstract public ChordLeadSheet getModel();

    /**
     * @return The Song which contains the ChordLeadSheet. Can be null.
     */
    abstract public Song getSongModel();

    /**
     * @return The UndoManager used but this editor.
     */
    abstract public UndoRedo getUndoManager();

    /**
     * The settings of the editor.
     *
     * @return
     */
    abstract public CL_EditorSettings getSettings();

    /**
     * The factory used by this editor via BarBoxes.
     *
     * @return
     */
    abstract public BarRendererFactory getBarRendererFactory();

    /**
     * Set how chords positions are quantized for display.
     * <p>
     * Fires a property change event.
     *
     * @param q
     * @param section If null, set the quantization display for all sections.
     * @see #getSectionQuantizationPropertyName(org.jjazz.chordleadsheet.api.Section)
     */
    abstract public void setDisplayQuantizationValue(CLI_Section section, Quantization q);

    /**
     * @param section
     * @return The quantization used to display the specified section.
     */
    abstract public Quantization getDisplayQuantizationValue(CLI_Section section);

    /**
     * Get the focused SelectedBar, if any.
     *
     * @param includeFocusedItem If true and focus is on a ChordLeadSheetItem, return the SelectedBar for this item.
     * @return Can be null.
     */
    abstract public SelectedBar getFocusedBar(boolean includeFocusedItem);

    /**
     * Set if the bar corresponding to specified section should start on a new line.
     * <p>
     * Fires a property change event.
     *
     * @param section
     * @param b
     * @see #getSectionOnNewLinePropertyName(org.jjazz.chordleadsheet.api.Section)
     */
    abstract public void setSectionStartOnNewLine(CLI_Section section, boolean b);

    /**
     * @param section
     * @return True if section bar should start on new line.
     */
    abstract public boolean isSectionStartOnNewLine(CLI_Section section);

    /**
     * Set the color associated to a section.
     * <p>
     * Fires a property change event.
     *
     * @param section Must be one of the ColorSetManager reference colors.
     * @param c
     * @see #getSectionColorPropertyName(org.jjazz.chordleadsheet.api.Section)
     */
    abstract public void setSectionColor(CLI_Section section, Color c);

    /**
     * Get the color associated to a section.
     *
     * @param section
     * @return
     */
    abstract public Color getSectionColor(CLI_Section section);

    /**
     * Set the BarBoxConfig for specified bars.
     *
     * @param bbConfig
     * @param barIndexes If no arg specified, apply bbConfig to all bars
     */
    abstract public void setBarBoxConfig(BarBoxConfig bbConfig, Integer... barIndexes);

    /**
     * Get the BarBoxConfig of the specified bar.
     *
     * @param barIndex
     * @return
     */
    abstract public BarBoxConfig getBarBoxConfig(int barIndex);

    /**
     * Check if bar annotations are visible.
     *
     * @return
     */
    abstract public boolean isBarAnnotationVisible();

    /**
     * Set bar annotations visible or not.
     * <p>
     * Fire a PROP_BAR_ANNOTATIONS_VISIBLE change event.
     *
     * @param b
     */
    abstract public void setBarAnnotationVisible(boolean b);


    /**
     * Get the number of lines of a bar annotation.
     *
     * @return
     */
    abstract public int getBarAnnotationNbLines();

    /**
     * Set the number of lines of a bar annotation.
     * <p>
     * Fire a PROP_BAR_ANNOTATIONS_NB_LINES change event.
     *
     * @param n A value between 1 and 10
     */
    abstract public void setBarAnnotationNbLines(int n);

    abstract public void setNbColumns(int nbCols);

    abstract public int getNbColumns();

    abstract public int getNbBarBoxes();

    /**
     * Clean up everything so component can be garbaged.
     */
    abstract public void cleanup();

    /**
     * Return the Position that correspond to a graphical point in the editor.
     * <p>
     * If point is on a model bar try to retrieve the beat value. If point is on a non-model bar (eg past end), return only the bar, beat is set to 0. If point
     * is somewhere else return null.
     *
     * @param editorPoint A point in the editor's coordinates.
     * @return Null if point does not correspond to a barbox
     *
     */
    abstract public Position getPositionFromPoint(Point editorPoint);

    /**
     * Make sure a specific bar is visible.
     *
     * @param barIndex int The index of the bar to be made visible
     */
    abstract public void makeBarVisible(int barIndex);

    /**
     * Set the zoom vertical factor.
     *
     * @param factor A value between 0 and 100 included.
     */
    abstract public void setZoomVFactor(int factor);

    /**
     * Get the zoom vertical factor.
     *
     * @return A value between 0 and 100 included.
     */
    abstract public int getZoomVFactor();

    /**
     * Select the bars in the specified barIndex range.
     *
     * @param barIndexFrom int
     * @param barIndexTo   int
     * @param b            True to select, False to unselect.
     */
    abstract public void selectBars(int barIndexFrom, int barIndexTo, boolean b);

    /**
     * Select the bars out of the specified barIndex range.
     *
     * @param barIndexFrom int
     * @param barIndexTo   int
     * @param b            True to select, False to unselect.
     */
    abstract public void selectBarsExcept(int barIndexFrom, int barIndexTo, boolean b);

    /**
     * Select the ItemRenderer(s) whose model is item.
     *
     * @param item
     * @param b    True to select, False to unselect.
     */
    abstract public void selectItem(ChordLeadSheetItem<?> item, boolean b);

    /**
     * Replace the current selection with the specified items.
     *
     * @param items
     * @param b     True to select, False to unselect.
     */
    abstract public void selectItems(List<? extends ChordLeadSheetItem> items, boolean b);

    /**
     * Set the focus on one bar in the editor.
     *
     * @param barIndex int The index of the bar.
     */
    abstract public void setFocusOnBar(int barIndex);

    /**
     * Set the focus on an ItemRenderer whose model is item.
     *
     * @param item
     * @param irClass the type of ItemRenderer to focus if there is multiple ItemRenderers for one item. If null, focus on the first ItemRenderer found.
     */
    abstract public void setFocusOnItem(ChordLeadSheetItem<?> item, IR_Type irClass);

    /**
     * Cause the renderer(s) of the specified item to do a brief UI change to request user attention.
     *
     * @param item
     * @throws IllegalArgumentException If ChordLeadSheet model does not contain item.
     */
    abstract public void requestAttention(ChordLeadSheetItem<?> item);

    /**
     * Show an insertion point in the editor for copy/move operations.
     *
     * @param b        Show/hide the insertion point. If false other arguments are not used.
     * @param item     The item for which we show the insertion point for.
     * @param pos      The position of the insertion point.
     * @param copyMode If true insertion point is shown for a copy operation, otherwise it's a move operation.
     */
    abstract public void showInsertionPoint(boolean b, ChordLeadSheetItem<?> item, Position pos, boolean copyMode);

    /**
     * Show a playback point in the editor at specified position.
     *
     * @param b   Show/hide the playback point.
     * @param pos
     */
    abstract public void showPlaybackPoint(boolean b, Position pos);

    /**
     * Get the dimensions of the specified BarBox.
     *
     * @param barBoxIndex
     * @return A Rectangle in the screen coordinates of this editor.
     */
    abstract public Rectangle getBarRectangle(int barBoxIndex);

    /**
     * Clear the editor selection.
     */
    public void unselectAll()
    {
        new CL_SelectionUtilities(getLookup()).unselectAll(this);
    }


}
