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
package org.jjazz.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import org.jjazz.harmony.api.TimeSignature;
import org.jjazz.rhythmdatabase.api.RhythmDatabase;
import org.jjazz.rhythmdatabase.api.RhythmInfo;
import org.jjazz.rhythmdatabaseimpl.RhythmDbCache;
import org.jjazz.utilities.api.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * For debug purposes...
 */
//@ActionID(category = "JJazz", id = "org.jjazz.test.mytestaction")
//@ActionRegistration(displayName = "MyTestAction")
//@ActionReferences(
//        {
//            @ActionReference(path = "Menu/Edit", position = 870012),
//            @ActionReference(path = "Shortcuts", name = "D-T")      // ctrl T
//        })
public final class MyTestAction implements ActionListener
{

    private static final Logger LOGGER = Logger.getLogger(MyTestAction.class.getSimpleName());

    public MyTestAction()
    {

    }

    @Override
    public void actionPerformed(ActionEvent ae)
    {
        LOGGER.severe("actionPerformed()");

        Map<String, List<RhythmInfo>> map = new HashMap<>();


        File f1, f2;
        try
        {
            f1 = Files.createTempFile("tmp", ".tmp").toFile();
            f2 = Files.createTempFile("tmp", ".tmp").toFile();
        } catch (IOException ex)
        {
            Exceptions.printStackTrace(ex);
            return;
        }

        var rdb = RhythmDatabase.getDefault();
        RhythmInfo ri = rdb.getDefaultRhythm(TimeSignature.FOUR_FOUR);
        var list = new ArrayList<RhythmInfo>();
        list.add(ri);
        list.add(rdb.getDefaultRhythm(TimeSignature.THREE_FOUR));
        var list2 = new ArrayList<RhythmInfo>();
        list2.add(rdb.getDefaultRhythm(TimeSignature.TWO_FOUR));
        map.put("firstrp", list);
        map.put("secondrp", list2);
        LOGGER.severe("map=" + Utilities.toMultilineString(map));

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f1)))
        {
            LOGGER.severe("writing " + f1.getAbsolutePath());
            oos.writeObject(map);
        } catch (IOException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        RhythmInfo ri2 = null;
        Map<String, List<RhythmInfo>> map2 = null;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f1)))
        {
            LOGGER.severe("reading " + f1.getAbsolutePath());
            map2 = (Map<String, List<RhythmInfo>>) ois.readObject();
        } catch (FileNotFoundException ex)
        {
            Exceptions.printStackTrace(ex);
        } catch (IOException | ClassNotFoundException ex)
        {
            Exceptions.printStackTrace(ex);
        }

        // LOGGER.severe("ri2=" + ri);
        LOGGER.severe("map2=" + Utilities.toMultilineString(map2));

//        String s = JOptionPane.showInputDialog("String ?");
//        File f = InstalledFileLocator.getDefault().locate(s, "org.jjazzlab.test", false);
//        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), f.getAbsolutePath());

//        String s = JOptionPane.showInputDialog("String ?");
//        File f = InstalledFileLocator.getDefault().locate(s, "org.jjazzlab.test", false);
//        JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), f.getAbsolutePath());
    }


    private int comp()
    {
        LOGGER.severe("comp() called");
        return 2938;
    }
}
