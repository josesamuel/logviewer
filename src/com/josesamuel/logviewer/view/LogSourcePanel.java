
package com.josesamuel.logviewer.view;

import com.android.ddmlib.IDevice;
import com.android.tools.idea.ddms.DeviceContext;
import com.android.tools.idea.ddms.DevicePropertyUtil;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.josesamuel.logviewer.log.LogSource;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The drop down that shows the message sources
 */
public class LogSourcePanel {

    private final DeviceContext myDeviceContext;
    private boolean myIgnoreActionEvents;
    private JPanel myPanel;
    @NotNull
    private JComboBox mySourceCombo;

    /**
     * Initialize the source panel view
     */
    LogSourcePanel(@NotNull DeviceContext context) {
        myDeviceContext = context;
        initializeSourceCombo();
    }

    /**
     * Initialize the combo box that shows the log sources
     */
    private void initializeSourceCombo() {
        mySourceCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (myIgnoreActionEvents) return;

                Object sel = mySourceCombo.getSelectedItem();
                IDevice device = (sel instanceof IDevice) ? (IDevice) sel : null;
                myDeviceContext.fireDeviceSelected(device);
            }
        });

        mySourceCombo.setRenderer(new SourceComboBoxRenderer("No Connected Devices"));
        Dimension size = mySourceCombo.getMinimumSize();
        mySourceCombo.setMinimumSize(new Dimension(200, size.height));
    }


    JPanel getComponent() {
        return myPanel;
    }


    /**
     * Updates the combo box with the given list of sources and selected item
     */
    void updateDeviceCombo(java.util.List<LogSource> logSourceList, LogSource selection) {
        myIgnoreActionEvents = true;
        mySourceCombo.removeAllItems();
        if (logSourceList != null) {
            for (LogSource source : logSourceList) {
                IDevice device = source.getSource();
                mySourceCombo.addItem(device);
            }
            if (selection != null && selection.getSource() != null) {
                mySourceCombo.setSelectedItem(selection.getSource());
            }
        }
        myIgnoreActionEvents = false;
    }


    /**
     * Renderer for the drop down list items
     */
    public static class SourceComboBoxRenderer extends ColoredListCellRenderer {
        @NotNull
        private String myEmptyText;

        SourceComboBoxRenderer(@NotNull String emptyText) {
            this.myEmptyText = emptyText;
        }

        static void renderDeviceName(@NotNull IDevice d, @NotNull ColoredTextContainer component) {
            component.setIcon(d.isEmulator() ? AndroidIcons.Ddms.Emulator2 : AndroidIcons.Ddms.RealDevice);
            String name;
            if (d.isEmulator()) {
                String avdName = d.getAvdName();
                if (avdName == null) {
                    avdName = "unknown";
                }

                name = String.format(" %1$s %2$s ", "Emulator", avdName);
            } else {
                name = String.format(" %1$s ", DevicePropertyUtil.getModel(d, ""));
            }

            component.append(d.getSerialNumber(), SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            component.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);

            IDevice.DeviceState deviceState = d.getState();
            if (deviceState != IDevice.DeviceState.ONLINE) {
                String state = String.format("[%1$s] ", d.getState());
                component.append(state, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            }


            if (deviceState != IDevice.DeviceState.DISCONNECTED && deviceState != IDevice.DeviceState.OFFLINE) {
                component.append(DevicePropertyUtil.getBuild(d), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }

        }

        protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
            if (value instanceof String) {
                this.append((String) value, SimpleTextAttributes.ERROR_ATTRIBUTES);
            } else if (value instanceof IDevice) {
                renderDeviceName((IDevice) value, this);
            } else if (value == null) {
                this.append(this.myEmptyText, SimpleTextAttributes.ERROR_ATTRIBUTES);
            }

        }
    }

}
