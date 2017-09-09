
package com.josesamuel.logviewer.view;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.ddms.DeviceContext;
import com.android.tools.idea.ddms.DevicePropertyUtil;
import com.google.common.collect.Maps;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.ColoredTextContainer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.UIUtil;
import icons.AndroidIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

/**
 * The drop down that shows the message sources
 * Currently only shows connected device.
 */
public class LogSourcePanel implements AndroidDebugBridge.IDeviceChangeListener, AndroidDebugBridge.IDebugBridgeChangeListener,
        Disposable {
    private final DeviceContext myDeviceContext;
    @NotNull
    private final Project myProject;
    @NotNull
    private final Map<String, String> myPreferredClients;
    public boolean myIgnoreActionEvents;
    private JPanel myPanel;
    @Nullable
    private AndroidDebugBridge myBridge;
    @NotNull
    private JComboBox myDeviceCombo;

    /**
     * Initialize the source panel view
     */
    public LogSourcePanel(@NotNull Project project, @NotNull DeviceContext context) {
        myProject = project;
        myDeviceContext = context;
        myPreferredClients = Maps.newHashMap();
        Disposer.register(myProject, this);

        initializeDeviceCombo();

        AndroidDebugBridge.addDeviceChangeListener(this);
        AndroidDebugBridge.addDebugBridgeChangeListener(this);
    }

    private void initializeDeviceCombo() {
        myDeviceCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (myIgnoreActionEvents) return;

                Object sel = myDeviceCombo.getSelectedItem();
                IDevice device = (sel instanceof IDevice) ? (IDevice) sel : null;
                myDeviceContext.fireDeviceSelected(device);
            }
        });

        myDeviceCombo.setRenderer(new DeviceComboBoxRenderer("No Connected Devices"));
        Dimension size = myDeviceCombo.getMinimumSize();
        myDeviceCombo.setMinimumSize(new Dimension(200, size.height));
    }


    @Override
    public void dispose() {
        if (myBridge != null) {
            AndroidDebugBridge.removeDeviceChangeListener(this);

            AndroidDebugBridge.removeDebugBridgeChangeListener(this);

            myBridge = null;
        }
    }

    public JPanel getComponent() {
        return myPanel;
    }

    @Override
    public void bridgeChanged(final AndroidDebugBridge bridge) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                myBridge = bridge;
                updateDeviceCombo();
            }
        });
    }

    @Override
    public void deviceConnected(final IDevice device) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                updateDeviceCombo();
            }
        });
    }

    @Override
    public void deviceDisconnected(final IDevice device) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                updateDeviceCombo();
            }
        });
    }

    @Override
    public void deviceChanged(final IDevice device, final int changeMask) {
        UIUtil.invokeLaterIfNeeded(new Runnable() {
            @Override
            public void run() {
                if ((changeMask & IDevice.CHANGE_CLIENT_LIST) != 0) {

                } else if ((changeMask & IDevice.CHANGE_STATE) != 0) {
                    updateDeviceCombo();
                }
                if (device != null) {
                    myDeviceContext.fireDeviceChanged(device, changeMask);
                }
            }
        });
    }


    private void updateDeviceCombo() {
        myIgnoreActionEvents = true;

        boolean update = true;
        IDevice selected = (IDevice) myDeviceCombo.getSelectedItem();
        myDeviceCombo.removeAllItems();
        boolean shouldAddSelected = true;
        if (myBridge != null) {
            for (IDevice device : myBridge.getDevices()) {
                myDeviceCombo.addItem(device);
                boolean isSelectedReattached =
                        selected != null && !selected.isEmulator() && selected.getSerialNumber().equals(device.getSerialNumber());
                if (selected == device || isSelectedReattached) {
                    myDeviceCombo.setSelectedItem(device);
                    shouldAddSelected = false;
                    update = selected != device;
                }
            }
        }
        if (selected != null && shouldAddSelected) {
            myDeviceCombo.addItem(selected);
            myDeviceCombo.setSelectedItem(selected);
        }

        if (update) {
            myDeviceContext.fireDeviceSelected((IDevice) myDeviceCombo.getSelectedItem());
        }

        myIgnoreActionEvents = false;
    }


    public static class DeviceComboBoxRenderer extends ColoredListCellRenderer {
        @NotNull
        private String myEmptyText;

        public DeviceComboBoxRenderer(@NotNull String emptyText) {
            this.myEmptyText = emptyText;
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

        static void renderDeviceName(@NotNull IDevice d, @NotNull ColoredTextContainer component) {
            component.setIcon(d.isEmulator() ? AndroidIcons.Ddms.Emulator2 : AndroidIcons.Ddms.RealDevice);
            String name;
            if (d.isEmulator()) {
                String avdName = d.getAvdName();
                if (avdName == null) {
                    avdName = "unknown";
                }

                name = String.format("%1$s %2$s ", "Emulator", avdName);
            } else {
                name = String.format("%1$s %2$s ", DevicePropertyUtil.getManufacturer(d, ""), DevicePropertyUtil.getModel(d, ""));
            }

            component.append(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
            IDevice.DeviceState deviceState = d.getState();
            if (deviceState != IDevice.DeviceState.ONLINE) {
                String state = String.format("%1$s [%2$s] ", d.getSerialNumber(), d.getState());
                component.append(state, SimpleTextAttributes.GRAYED_BOLD_ATTRIBUTES);
            }

            if (deviceState != IDevice.DeviceState.DISCONNECTED && deviceState != IDevice.DeviceState.OFFLINE) {
                component.append(DevicePropertyUtil.getBuild(d), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }

        }
    }

}
