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
package org.jjazz.yjzcreator;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.event.HyperlinkEvent;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.utilities.api.ResUtil;

public final class YjzCreatorVisualPanel4 extends JPanel
{

    /**
     * Creates new form YjzCreatorVisualPanel1
     */
    public YjzCreatorVisualPanel4()
    {
        initComponents();
    }

    @Override
    public String getName()
    {
        return ResUtil.getString(getClass(),"CREATION");
    }

    public void setBaseRhythm(RhythmInfo ri)
    {
        tf_basePath.setText(YjzCreatorWizardAction.getBaseFileCopy(ri).getAbsolutePath());
        tf_yjzPath.setText(YjzCreatorWizardAction.getExtendedFile(ri).getAbsolutePath());
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The content of
     * this method is always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        jScrollPane2 = new javax.swing.JScrollPane();
        editorPane_intro = new javax.swing.JEditorPane();
        editorPane_intro.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE); // To make setFont work
        jLabel2 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        tf_yjzPath = new javax.swing.JTextField();
        tf_basePath = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();

        jScrollPane2.setBackground(null);
        jScrollPane2.setBorder(null);

        editorPane_intro.setEditable(false);
        editorPane_intro.setBackground(null);
        editorPane_intro.setBorder(null);
        editorPane_intro.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        editorPane_intro.setText(org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.editorPane_intro.text")); // NOI18N
        editorPane_intro.addHyperlinkListener(new javax.swing.event.HyperlinkListener()
        {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt)
            {
                editorPane_introHyperlinkUpdate(evt);
            }
        });
        jScrollPane2.setViewportView(editorPane_intro);

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.jLabel2.text")); // NOI18N

        jLabel16.setFont(jLabel16.getFont().deriveFont(jLabel16.getFont().getStyle() | java.awt.Font.BOLD));
        org.openide.awt.Mnemonics.setLocalizedText(jLabel16, org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.jLabel16.text")); // NOI18N
        jLabel16.setToolTipText(org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.jLabel16.toolTipText")); // NOI18N

        tf_yjzPath.setEditable(false);
        tf_yjzPath.setText("jTextField1"); // NOI18N

        tf_basePath.setEditable(false);
        tf_basePath.setText("jTextField1"); // NOI18N
        tf_basePath.setToolTipText(org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.tf_basePath.toolTipText")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel3, org.openide.util.NbBundle.getMessage(YjzCreatorVisualPanel4.class, "YjzCreatorVisualPanel4.jLabel3.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel16)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(tf_basePath)
                            .addComponent(tf_yjzPath)))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel16)
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tf_yjzPath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(tf_basePath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(33, 33, 33)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 273, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void editorPane_introHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt)//GEN-FIRST:event_editorPane_introHyperlinkUpdate
    {//GEN-HEADEREND:event_editorPane_introHyperlinkUpdate
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            org.jjazz.utilities.api.Utilities.openInBrowser(evt.getURL(), false);
        }
    }//GEN-LAST:event_editorPane_introHyperlinkUpdate

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JEditorPane editorPane_intro;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTextField tf_basePath;
    private javax.swing.JTextField tf_yjzPath;
    // End of variables declaration//GEN-END:variables
}
