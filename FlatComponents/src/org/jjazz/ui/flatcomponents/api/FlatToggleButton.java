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
package org.jjazz.ui.flatcomponents.api;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import org.openide.util.actions.BooleanStateAction;

/**
 * A flat toggle button.
 * <p>
 * Selected button can use a specific foreground and icon.
 */
public class FlatToggleButton extends FlatButton
{

    private Icon unselectedIcon;
    private Icon selectedIcon;
    private Color saveForeground;
    private Color selectedForeground;
    private boolean isSelected;
    private static final Logger LOGGER = Logger.getLogger(FlatToggleButton.class.getSimpleName());

    /**
     * Equivalent of FlatToggleButton(true, true, false)
     */
    public FlatToggleButton()
    {
        this(true, true, false);
    }

    /**
     * Equivalent of FlatToggleButton(null, enablePressedBorder, enableEnteredBorder, enableDrag)
     *
     * @param enablePressedBorder
     * @param enableEnteredBorder
     * @param enableDrag
     */
    public FlatToggleButton(boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
    {
        this(null, enablePressedBorder, enableEnteredBorder, enableDrag);
    }

    /**
     * Equivalent of FlatToggleButton(bsa, true, true, false)
     */
    public FlatToggleButton(BooleanStateAction bsa)
    {
        this(bsa, true, true, false);
    }

    /**
     * Create a toggle button initialized with the specified action.
     *
     * @param bsa
     * @param enablePressedBorder
     * @param enableEnteredBorder
     * @param enableDrag
     */
    public FlatToggleButton(BooleanStateAction bsa, boolean enablePressedBorder, boolean enableEnteredBorder, boolean enableDrag)
    {
        super(bsa, enablePressedBorder, enableEnteredBorder, enableDrag);
        selectedForeground = Color.RED;
        isSelected = false;

        if (bsa != null)
        {
            setSelectedIcon((Icon) bsa.getValue(Action.LARGE_ICON_KEY));
            setSelected(bsa.getBooleanState());
        }
    }

    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Change the selected status.
     * <p>
     * This does NOT fire a change event. Use doClick() to simulate a user click.
     *
     * @param b
     */
    public void setSelected(boolean b)
    {
        if (b == isSelected)
        {
            return;
        }
        isSelected = b;
        if (isSelected)
        {
            super.setIcon(selectedIcon);
            saveForeground = getForeground();
            setForeground(selectedForeground);
        } else
        {
            super.setIcon(unselectedIcon);
            setForeground(saveForeground);
        }
    }

    /**
     * Simulate a user click.
     */
    public void doClick()
    {
        buttonClicked(new MouseEvent(this, MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, 0, false));
    }

    /**
     * Redirected to setUnselectedIcon(icon).
     *
     * @param icon
     */
    @Override
    public void setIcon(Icon icon)
    {
        setUnselectedIcon(icon);
    }

    /**
     * Overridden to authorize only BooleanStateActions.
     * <p>
     * When this togglebutton is clicked it just calls action.actionPerformed(). Button selected state will follow the
     * BooleanStateAction.PROP_BOOLEAN_STATE action's property changes.
     * <p>
     * Reuse the following properties:<br>
     * - "LARGE_ICON_KEY" property =&gt; setSelectedIcon() <br>
     * - BooleanStateAction.PROP_BOOLEAN_STATE =&gt; setSelected()
     *
     * @param bsa A non-null BooleanStateAction.
     */
    public void setAction(BooleanStateAction bsa)
    {
        super.setAction(bsa);
        setSelectedIcon((Icon) bsa.getValue(Action.LARGE_ICON_KEY));
        setSelected(bsa.getBooleanState());
    }

    public void setUnselectedIcon(Icon icon)
    {
        this.unselectedIcon = icon;
        if (!isSelected())
        {
            super.setIcon(unselectedIcon);
        }
    }

    public Icon getUnselectedIcon()
    {
        return unselectedIcon;
    }

    public void setSelectedIcon(Icon selectedIcon)
    {
        if (selectedIcon == null)
        {
            throw new NullPointerException("selectedIcon");   //NOI18N
        }
        this.selectedIcon = selectedIcon;
        if (isSelected())
        {
            super.setIcon(selectedIcon);
        }
    }

    public Icon getSelectedIcon()
    {
        return selectedIcon;
    }

    public void setSelectedForeground(Color c)
    {
        if (c == null)
        {
            throw new NullPointerException("c");   //NOI18N
        }
        this.selectedForeground = c;
        if (isSelected())
        {
            setForeground(selectedForeground);
        }
    }

    public Color getSelectedForeground()
    {
        return selectedForeground;
    }

    // ======================================================================
    // PropertyChangeListener interface
    // ======================================================================    
    /**
     * Overridden to add the support of the LARGE_ICON_KEY and BooleanState property.
     *
     * @param evt
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        super.propertyChange(evt);

        LOGGER.fine("propertyChange() this.action=" + (getAction() != null ? getAction().getValue(Action.NAME) : "") + ", evt=" + evt);   //NOI18N
        if (evt.getSource() == getAction())
        {
            if (evt.getPropertyName() == Action.LARGE_ICON_KEY)
            {
                setSelectedIcon((Icon) evt.getNewValue());
            } else if (evt.getPropertyName() == BooleanStateAction.PROP_BOOLEAN_STATE)
            {
                setSelected(evt.getNewValue().equals(Boolean.TRUE));
            }
        }
    }

    /**
     * Overridden.
     * <p>
     * If a BooleanStateAction is associated to this button, just call its actionPerformed(): this button listens to the action's
     * PROP_BOOLEAN_STATE and will update itself if action actually switches its state.<br>
     * If no action defined directly change the button's state.<br>
     * All ActionListeners are also notified.
     *
     * @param e
     */
    @Override
    protected void buttonClicked(MouseEvent e)
    {
        ActionEvent ae = new ActionEvent(this, 0, "Click", e.getModifiersEx());
        if (getAction() != null)
        {
            getAction().actionPerformed(ae);
        } else
        {
            setSelected(!isSelected());
        }
        fireActionPerformed(ae);
    }
}
