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
package org.jjazz.options;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.TopLevelRegistration(
        categoryName = "#OptionsCategory_Name_Midi",
        iconBase = "org/jjazz/options/resources/MidiIcon32x32.png",
        keywords = "#OptionsCategory_Keywords_Midi",
        keywordsCategory = "Midi",
        position = 100,
        id = "MidiPanelId"
)
@org.openide.util.NbBundle.Messages(
        {
            "OptionsCategory_Name_Midi=Midi", "OptionsCategory_Keywords_Midi=midi"
        })
public final class MidiOptionsPanelController extends OptionsPanelController
{

    private MidiPanel panel;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private boolean changed;
    private static final Logger LOGGER = Logger.getLogger(MidiOptionsPanelController.class.getSimpleName());

    @Override
    public void update()
    {
        LOGGER.log(Level.FINE, "update()");
        getPanel().load();
        changed = false;
    }

    @Override
    public void applyChanges()
    {
        LOGGER.log(Level.FINE, "applyChanges()");
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                getPanel().store();
                changed = false;
            }
        });
    }

    @Override
    public void cancel()
    {
        // need not do anything special, if no changes have been persisted yet
        getPanel().cancel();
        LOGGER.log(Level.FINE, "cancel()");
    }

    @Override
    public boolean isValid()
    {
        return getPanel().valid();
    }

    @Override
    public boolean isChanged()
    {
        return changed;
    }

    @Override
    public HelpCtx getHelpCtx()
    {
        return null; // new HelpCtx("...ID") if you have a help set
    }

    @Override
    public JComponent getComponent(Lookup masterLookup)
    {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        pcs.addPropertyChangeListener(l);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        pcs.removePropertyChangeListener(l);
    }

    private MidiPanel getPanel()
    {
        if (panel == null)
        {
            panel = new MidiPanel(this);
        }
        return panel;
    }

    void changed()
    {
        if (!changed)
        {
            changed = true;
            pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
    }

}
