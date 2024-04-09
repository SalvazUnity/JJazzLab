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
package org.jjazz.songeditormanager.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;
import org.jjazz.filedirectorymanager.api.FileDirectoryManager;
import org.jjazz.midimix.api.MidiMix;
import org.jjazz.song.api.Song;
import org.jjazz.song.api.SongCreationException;
import org.jjazz.songeditormanager.spi.SongEditorManager;
import org.jjazz.startup.spi.StartupTask;
import org.jjazz.upgrade.api.UpgradeManager;
import org.jjazz.upgrade.api.UpgradeTask;
import org.jjazz.utilities.api.ResUtil;
import org.netbeans.api.sendopts.CommandException;
import org.netbeans.spi.sendopts.Env;
import org.netbeans.spi.sendopts.Option;
import org.netbeans.spi.sendopts.OptionProcessor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.modules.OnStop;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.ServiceProvider;

/**
 * Manage the opening/closing of song files at startup/shutdown.
 * <p>
 * Upon startup, if file names arguments are passed on the command line, open these files. Otherwise restore the last opened files depending
 * on setting. Upon shutdown ask for user confirmation for unsaved songs.
 */
@ServiceProvider(service = OptionProcessor.class)
@OnStop
public class StartupShutdownSongManager extends OptionProcessor implements Callable<Boolean>
{

    /**
     * Instance is automatically created upon startup.
     */
    private static StartupShutdownSongManager INSTANCE;
    /**
     * Used as the Preference id and property change.
     */
    private static final String PREF_OPEN_RECENT_FILES_UPON_STARTUP = "OpenRecentFilesUponStartup";
    private static final String PREF_FILES_TO_BE_REOPENED_UPON_STARTUP = "FilesToBeReOpenedUponStartup";
    private static final String NO_FILE = "__NO_FILE__";
    private static final int MAX_FILES = 20;
    private final Option openOption = Option.defaultArguments();  // The command line arguments with no -x or --xyz option
    private final List<File> cmdLineFilesToOpen = new ArrayList<>();
    private boolean isUIready = false;
    private static final Preferences prefs = NbPreferences.forModule(StartupShutdownSongManager.class);
    private static final Logger LOGGER = Logger.getLogger(StartupShutdownSongManager.class.getSimpleName());

    static public final StartupShutdownSongManager getInstance()
    {
        if (INSTANCE == null)
        {
            throw new NullPointerException("INSTANCE");   
        }
        return INSTANCE;
    }

    /**
     * Reserved do not use : use getInstance() instead.
     */
    public StartupShutdownSongManager()
    {
        INSTANCE = this;
    }

    public void setOpenRecentFilesUponStartup(boolean b)
    {
        prefs.putBoolean(PREF_OPEN_RECENT_FILES_UPON_STARTUP, b);
    }

    public final boolean isOpenRecentFilesUponStartup()
    {
        return prefs.getBoolean(PREF_OPEN_RECENT_FILES_UPON_STARTUP, true);
    }

    // ==================================================================================
    // OptionProcessor implementation
    // ==================================================================================
    @Override
    protected Set<Option> getOptions()
    {
        HashSet<Option> set = new HashSet<>();
        set.add(openOption);
        return set;
    }

    /**
     * Can be called several times, upon startup, but also later if user reopens a file from explorer while app is running.
     *
     * @param env
     * @param values
     * @throws CommandException
     */
    @Override
    protected void process(Env env, Map<Option, String[]> values) throws CommandException
    {
        LOGGER.log(Level.FINE, "process() --  env={0} values={1}", new Object[]{env, values});   


        cmdLineFilesToOpen.clear();


        if (values.containsKey(openOption))
        {
            var fileNames = values.get(openOption);
            for (String fileName : fileNames)
            {
                LOGGER.log(Level.INFO, "process() Opening command line file: {0}, current dir: {1}", new Object[]{fileName,
                    env.getCurrentDirectory().getAbsolutePath()});   

                // Normally fileName contains the absolute path, but just in case...
                File file = new File(fileName);
                if (file.getParentFile() == null)
                {
                    file = new File(env.getCurrentDirectory().getAbsolutePath(), file.getName());
                }


                if (!file.exists())
                {
                    LOGGER.log(Level.WARNING, "process() Can''t find {0}", file.getAbsolutePath());   
                    continue;

                } else
                {
                    if (isUIready)
                    {
                        try
                        {
                            // Directly open the file and activate it
                            boolean last = fileName == fileNames[fileNames.length - 1];
                            SongEditorManager.getDefault().showSong(file, last, true);
                        } catch (SongCreationException ex)
                        {
                            LOGGER.log(Level.WARNING, "process() Problem opening song file: {0}. ex={1}", new Object[]{file.getAbsolutePath(),
                                ex.getMessage()});   
                        }

                    } else
                    {
                        // Just save file, this will be handled later by run()
                        cmdLineFilesToOpen.add(file);
                    }
                }
            }

        }

    }

    // ==================================================================================
    // Callable implementation
    // ==================================================================================
    /**
     * Called upon shutdown.
     * <p>
     * Ask user confirmation if unsaved changes (whatever nb of unsaved files), close properly the opened songs (so that listeners with
     * persistence like RecentFiles are notified).
     * <p>
     * Also save the opened songs for possible reopen at startup (see isOpenRecentFilesUponStartup()).
     */
    @Override
    public Boolean call() throws Exception
    {
        SongEditorManager sem = SongEditorManager.getDefault();


        // Ask user confirmation if there are still files to be saved
        var songsToSave = sem.getOpenedSongs().stream()
                .filter(s -> s.isSaveNeeded())
                .toList();
        if (!songsToSave.isEmpty())
        {
            // Build message and songs which need save
            StringBuilder msg = new StringBuilder();
            msg.append(ResUtil.getString(getClass(), "CTL_UnsavedChangesExitAnyway")).append("\n");            
            for (Song s : songsToSave)
            {
                String strFile = s.getName();
                if (s.getFile() != null)
                {
                    File songMixFile = MidiMix.getSongMixFile(s.getFile());
                    strFile = s.getFile().getAbsolutePath()+", "+songMixFile.getAbsolutePath();                    
                }
                msg.append("  ").append(strFile).append("\n");
            }
            
            NotifyDescriptor nd = new NotifyDescriptor.Confirmation(msg.toString(), NotifyDescriptor.OK_CANCEL_OPTION);
            Object result = DialogDisplayer.getDefault().notify(nd);
            if (result != NotifyDescriptor.OK_OPTION)
            {
                // User cancelled the shutdown
                return Boolean.FALSE;
            }
        }


        // Close the open editors without saving, and update the preferences
        StringBuilder sb = new StringBuilder();
        for (Song s : sem.getOpenedSongs())
        {
            File f = s.getFile();
            if (f != null)
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(f.getAbsolutePath());
            }
            sem.closeSong(s, true);
        }
        String s = sb.toString();
        prefs.put(PREF_FILES_TO_BE_REOPENED_UPON_STARTUP, s.isEmpty() ? NO_FILE : s);

        return Boolean.TRUE;
    }

    // ==================================================================================
    // Private methods
    // ==================================================================================
    // =====================================================================================
    // Startup Task
    // =====================================================================================
    @ServiceProvider(service = StartupTask.class)
    static public class OpenFilesAtStartupTask implements StartupTask
    {

        public final int PRIORITY = 600;            // Right after Rhythm files loading, but before Example songs and MidiWizard

        /**
         * Open command line files and recent files.
         * <p>
         * Called upon startup after UI's ready, so AFTER process() which collects optional files from the command line.
         */
        @Override
        public boolean run()
        {
            LOGGER.fine("OpenFilesAtStartupTask.run() --");   

            // If command line arguments specified, just open them and ignore recent open files
            var instance = StartupShutdownSongManager.getInstance();
            if (!instance.cmdLineFilesToOpen.isEmpty())
            {
                var sem = SongEditorManager.getDefault();

                for (File f : instance.cmdLineFilesToOpen)
                {
                    boolean last = f == instance.cmdLineFilesToOpen.get(instance.cmdLineFilesToOpen.size() - 1);
                    try
                    {
                        sem.showSong(f, last, true);
                    } catch (SongCreationException ex)
                    {
                        LOGGER.log(Level.WARNING, "OpenFilesAtStartupTask.run() Problem opening song file: {0}. ex={1}", new Object[]{f.getAbsolutePath(),
                            ex.getMessage()});   
                    }
                }

            } else if (instance.isOpenRecentFilesUponStartup())
            {
                openRecentFilesUponStartup();
            }
            instance.isUIready = true;

            return true;
        }

        @Override
        public int getPriority()
        {
            return PRIORITY;
        }

        @Override
        public String getName()
        {
            return "Open command line and recent files";
        }

        private void openRecentFilesUponStartup()
        {
            String s = prefs.get(PREF_FILES_TO_BE_REOPENED_UPON_STARTUP, NO_FILE).trim();
            if (!s.equals(NO_FILE))
            {
                final List<String> strFiles = Arrays.asList(s.split(","));
                final int max = Math.min(strFiles.size(), MAX_FILES);         // Robustness
                Runnable run = () -> 
                {
                    for (int i = 0; i < max; i++)
                    {
                        File f = new File(strFiles.get(i).trim());
                        boolean last = (i == max - 1);
                        try
                        {
                            SongEditorManager.getDefault().showSong(f, last, true);
                        } catch (SongCreationException ex)
                        {
                            LOGGER.log(Level.WARNING, "openRecentFilesUponStartup.run() Problem opening song file: {0}. ex={1}", new Object[]{f.getAbsolutePath(),
                                ex.getMessage()});   
                        }
                    }
                };
                SwingUtilities.invokeLater(run);
            }
        }

    }

    // =====================================================================================
    // Upgrade Task
    // =====================================================================================
    @ServiceProvider(service = UpgradeTask.class)
    static public class RestoreSettingsTask implements UpgradeTask
    {

        @Override
        public void upgrade(String oldVersion)
        {
            UpgradeManager um = UpgradeManager.getInstance();
            um.duplicateOldPreferences(prefs);

            // Reset recent files list : avoid opening files upon fresh startup, which can mess the UI with the fresh startup dialogs
            prefs.put(PREF_FILES_TO_BE_REOPENED_UPON_STARTUP, NO_FILE);
        }

    }

}
