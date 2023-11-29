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
package org.jjazz.chordinspector;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import org.jjazz.chordinspector.spi.ChordViewer;
import org.jjazz.chordleadsheet.api.item.CLI_ChordSymbol;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.rhythm.api.RhythmVoice;
import org.jjazz.song.api.Song;
import org.jjazz.instrumentcomponents.guitardiagram.api.GuitarDiagramComponent;
import org.jjazz.instrumentcomponents.guitardiagram.api.TGChord;
import org.jjazz.instrumentcomponents.guitardiagram.api.TGChordCreatorUtil;
import org.jjazz.instrumentcomponents.guitardiagram.api.TGChordSettings;
import org.jjazz.instrumentcomponents.guitardiagram.api.TGChordSettings.ChordMode;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = ChordViewer.class, position = 100)
public class GuitarChordViewer extends javax.swing.JPanel implements ChordViewer
{

    @StaticResource(relative = true)
    final private static String ICON_PATH = "resources/DiagramIcon.png";
    final private static Icon ICON = new ImageIcon(GuitarChordViewer.class.getResource(ICON_PATH));
    private static final Color TONIC_NOTE_COLOR = new Color(231, 83, 35);
    private CLI_ChordSymbol model;
    private final int maxFretSpan = 4;
    private static final Logger LOGGER = Logger.getLogger(GuitarChordViewer.class.getSimpleName());

    public GuitarChordViewer()
    {
        initComponents();

        fbtn_chordMode.setText(TGChordSettings.getInstance().getChordMode().toString());
    }


    // ===================================================================================
    // ChordViewer interface
    // ===================================================================================
    @Override
    public JComponent getComponent()
    {
        return this;
    }

    @Override
    public String getDescription()
    {
        return "Guitar diagrams";
    }

    @Override
    public Icon getIcon()
    {
        return ICON;
    }

    @Override
    public void setContext(Song song, MidiMix midiMix, RhythmVoice rv)
    {
        // Nothing
    }

    @Override
    public void setModel(CLI_ChordSymbol cliCs)
    {
        this.model = cliCs;
        updateDiagrams(cliCs);
    }

    @Override
    public CLI_ChordSymbol getModel()
    {
        return model;
    }

    @Override
    public void setEnabled(boolean b)
    {
        super.setEnabled(b);
        for (GuitarDiagramComponent diagram : getDiagrams())
        {
            diagram.setEnabled(false);
        }
    }

    @Override
    public void cleanup()
    {
        // Nothing
    }

    // ===============================================================================
    // Private methods
    // ===============================================================================

    private void updateDiagrams(CLI_ChordSymbol cliCs)
    {
        pnl_instrument.removeAll();

        if (cliCs != null)
        {
            var cs = cliCs.getData();
            List<TGChord> tgChords = new TGChordCreatorUtil(maxFretSpan).getChords(cs);
            tgChords.stream().limit(30).forEach(tgChord -> 
            {
                GuitarDiagramComponent diagram = new GuitarDiagramComponent(tgChord, cs);
                diagram.setTonicNoteColor(TONIC_NOTE_COLOR);
                pnl_instrument.add(diagram);
            });
        }

        revalidate();
        repaint();

    }

    private List<GuitarDiagramComponent> getDiagrams()
    {
        var res = new ArrayList<GuitarDiagramComponent>();
        for (Component c : pnl_instrument.getComponents())
        {
            if (c instanceof GuitarDiagramComponent gdc)
            {
                res.add(gdc);
            }
        }
        return res;
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of this
     * method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane1 = new javax.swing.JScrollPane();
        pnl_instrument = new javax.swing.JPanel();
        fbtn_chordMode = new org.jjazz.uiutilities.api.SmallFlatDarkLafButton();

        pnl_instrument.setLayout(new java.awt.GridLayout(0, 3, 3, 5));
        jScrollPane1.setViewportView(pnl_instrument);

        org.openide.awt.Mnemonics.setLocalizedText(fbtn_chordMode, "Most common chords"); // NOI18N
        fbtn_chordMode.setToolTipText(org.openide.util.NbBundle.getMessage(GuitarChordViewer.class, "GuitarChordViewer.fbtn_chordMode.toolTipText")); // NOI18N
        fbtn_chordMode.setFont(fbtn_chordMode.getFont().deriveFont(fbtn_chordMode.getFont().getSize()-2f));
        fbtn_chordMode.setMargin(new java.awt.Insets(2, 5, 2, 5));
        fbtn_chordMode.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                fbtn_chordModeActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(85, Short.MAX_VALUE)
                .addComponent(fbtn_chordMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(fbtn_chordMode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void fbtn_chordModeActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_fbtn_chordModeActionPerformed
    {//GEN-HEADEREND:event_fbtn_chordModeActionPerformed
        var tgs = TGChordSettings.getInstance();
        ChordMode mode = tgs.getChordMode().next();
        TGChordSettings.getInstance().setChordMode(mode);
        updateDiagrams(model);
        fbtn_chordMode.setText(mode.toString());
    }//GEN-LAST:event_fbtn_chordModeActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jjazz.uiutilities.api.SmallFlatDarkLafButton fbtn_chordMode;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel pnl_instrument;
    // End of variables declaration//GEN-END:variables
}
