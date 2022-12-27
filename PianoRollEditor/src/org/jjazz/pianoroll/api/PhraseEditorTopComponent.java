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
package org.jjazz.pianoroll.api;

import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.phrase.api.NoteEvent;
import org.jjazz.phrase.api.Phrase;
import org.jjazz.phrase.api.SizedPhrase;
import org.jjazz.pianoroll.PianoRollEditorImpl;
import org.jjazz.pianoroll.spi.PianoRollEditorSettings;
import org.jjazz.util.api.FloatRange;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.UndoRedo;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;


/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.jjazz.pianoroll.api//PhraseEditor//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "PhraseEditorTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = true)
@ActionID(category = "Window", id = "org.jjazz.pianoroll.api.PhraseEditorTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_PhraseEditorAction",
        preferredID = "PhraseEditorTopComponent"
)
@Messages(
        {
            "CTL_PhraseEditorAction=PhraseEditor",
            "CTL_PhraseEditorTopComponent=PhraseEditor Window",
            "HINT_PhraseEditorTopComponent=This is a PhraseEditor window"
        })
public final class PhraseEditorTopComponent extends TopComponent
{

    private PianoRollEditorImpl editor;

    public PhraseEditorTopComponent()
    {
        setName(Bundle.CTL_PhraseEditorTopComponent());
        setToolTipText(Bundle.HINT_PhraseEditorTopComponent());


        initComponents();


        int nbBeats = 16;
        int nbNotes = 3;
        SizedPhrase sp = new SizedPhrase(0, new FloatRange(0, 4), TimeSignature.FOUR_FOUR);
//        for (int i = 0; i < nbNotes; i++)
//        {
//            int pitch = 40 + ((int) (Math.random() * 60) - 30);
//            int vel = 64 + ((int) (Math.random() * 100) - 50);
//            float dur = (float) (0.2f + Math.random() * 4);
//            float deltaPos = (float) (Math.random() * 4);
//            float pos = 20 + ((float) nbBeats / (nbNotes + 3)) * i + deltaPos;
//            if (pos + dur < (20 + nbBeats))
//            {
//                p.add(new NoteEvent(pitch, dur, vel, pos));
//            }
//        }   
//        SizedPhrase sp = new SizedPhrase(p.getChannel(), new FloatRange(0, 20 +nbBeats), TimeSignature.FOUR_FOUR);
        sp.add(new NoteEvent(64, 1, 64, 1.6f));
//        sp.add(new NoteEvent(64, 1, 64, 1.6f));
        sp.add(new NoteEvent(67, 1, 64, 1.6f));
        editor = new PianoRollEditorImpl(0, sp, null, PianoRollEditorSettings.getDefault());
        add(editor);

    }

    @Override
    public UndoRedo getUndoRedo()
    {
        return editor.getUndoManager();
    }

    @Override
    public Lookup getLookup()
    {
        return editor.getLookup();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.LINE_AXIS));
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
    @Override
    public void componentOpened()
    {

    }

    @Override
    public void componentClosed()
    {
        // TODO add custom code on component closing
    }

    void writeProperties(java.util.Properties p)
    {
        // better to version settings since initial version as advocated at
        // http://wiki.apidesign.org/wiki/PropertyFiles
        p.setProperty("version", "1.0");
        // TODO store your settings
    }

    void readProperties(java.util.Properties p)
    {
        String version = p.getProperty("version");
        // TODO read your settings according to their version
    }
}
