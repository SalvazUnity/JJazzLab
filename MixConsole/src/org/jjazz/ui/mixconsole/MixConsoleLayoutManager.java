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
package org.jjazz.ui.mixconsole;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.logging.Logger;
import org.jjazz.ui.mixconsole.api.MixConsole;

/**
 * A LayoutManager to arrange the MixChannelPanels and their UserChannelExtensionPanels in the MixConsole.
 */
public class MixConsoleLayoutManager implements LayoutManager
{
    private static final int H_PADDING = 3;
    private static final int V_PADDING = 2;
    
    private final MixConsole mixConsole;
    private static final Logger LOGGER = Logger.getLogger(MixConsoleLayoutManager.class.getSimpleName());
    
    public MixConsoleLayoutManager(MixConsole mixConsole)
    {
        this.mixConsole = mixConsole;
    }
    
    @Override
    public void layoutContainer(Container container)
    {                
        Insets in = container.getInsets();
        int xMin = in.left + H_PADDING;
        int yMin = in.top + V_PADDING;
        int x = xMin;


        for (var panelSet : mixConsole.getChannelPanelSets().values())     // Sorted by channel
        {
            int y = yMin;

            // MixChannelPanel
            var mcp = panelSet.mixChannelPanel;
            Dimension pd = mcp.getPreferredSize();
            mcp.setSize(pd);
            mcp.setLocation(x, y);
            y += mcp.getHeight() + V_PADDING;
      

            // PhraseViewerPanel below
            var mcpv = panelSet.phraseViewerPanel;
            pd = mcpv.getPreferredSize();
            mcpv.setSize(mcp.getWidth(), pd.height);
            mcpv.setLocation(x, y);


            x += mcp.getWidth() + H_PADDING;
        }

    }

    @Override
    public void addLayoutComponent(String string, Component comp)
    {
        // Nothing
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
     // Nothing
    }

    @Override
    public Dimension preferredLayoutSize(Container container)
    {
        int w = 0;
        int hMax = 0;


        for (var panelSet : mixConsole.getChannelPanelSets().values())     // Sorted by channel
        {
            Dimension pd = panelSet.mixChannelPanel.getPreferredSize();
            w += H_PADDING + pd.width;

            int h = pd.height + V_PADDING + panelSet.phraseViewerPanel.getPreferredSize().height;     

            hMax = Math.max(h, hMax);
        }

        Insets in = container.getInsets();
        w += in.left + H_PADDING + in.right;
        int h = in.top + hMax + in.bottom;
        return new Dimension(w, h);
    }


    @Override
    public Dimension minimumLayoutSize(Container parent
    )
    {
        return new Dimension(100, 100);
    }
    // ===============================================================================
    // Private methods
    // ===============================================================================

  

    // ===============================================================================
    // Private classes
    // ===============================================================================
}
