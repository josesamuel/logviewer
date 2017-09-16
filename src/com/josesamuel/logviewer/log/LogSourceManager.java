package com.josesamuel.logviewer.log;


import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.tools.idea.ddms.DeviceContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.ui.UIUtil;
import com.josesamuel.logviewer.log.device.DeviceLogDataProvider;
import com.josesamuel.logviewer.log.device.DeviceLogSource;
import com.josesamuel.logviewer.log.dnd.DnDListener;
import com.josesamuel.logviewer.log.file.FileDevice;
import com.josesamuel.logviewer.log.file.FileLogSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the different available {@link LogSource}
 * Listens for device conenction events for device sources.
 */
public class LogSourceManager implements Disposable, AndroidDebugBridge.IClientChangeListener,
        AndroidDebugBridge.IDeviceChangeListener,
        AndroidDebugBridge.IDebugBridgeChangeListener,
        DnDListener {

    private DeviceContext deviceContext;
    private Project project;
    private java.util.Map<IDevice, DeviceLogSource> deviceLogSourcesMap;
    private java.util.Map<File, FileLogSource> fileLogSourcesMap;
    private LogSourceManagerListener logSourceManagerListener;
    private LogSource selectedSource;
    private AndroidDebugBridge myBridge;
    private DeviceContext.DeviceSelectionListener deviceSelectionListener;


    /**
     * Initiailzes the {@link LogSourceManager} with the given project, context and listener
     *
     * @param project                  Project instance
     * @param deviceContext            DeviceContext to notify about source changes
     * @param logSourceManagerListener Listener for notifications
     */
    public LogSourceManager(Project project, DeviceContext deviceContext, LogSourceManagerListener logSourceManagerListener) {
        this.project = project;
        this.deviceContext = deviceContext;
        this.deviceLogSourcesMap = new ConcurrentHashMap<>();
        this.fileLogSourcesMap = new ConcurrentHashMap<>();
        this.logSourceManagerListener = logSourceManagerListener;
        observeForDeviceChange();
        Disposer.register(project, this);
    }

    @Override
    public void clientChanged(Client client, int changeMask) {
        DeviceLogSource deviceLogSource = getDeviceSource(client.getDevice());
        if (deviceLogSource != null
                && deviceLogSource == selectedSource
                && ((changeMask & Client.CHANGE_NAME) != 0)) {
            ((DeviceLogDataProvider) deviceLogSource.getLogProvider()).updateProcessList();
        }
    }

    /**
     * Observe for deviec state changes
     */
    private void observeForDeviceChange() {
        deviceSelectionListener =
                new DeviceContext.DeviceSelectionListener() {
                    @Override
                    public void deviceSelected(@Nullable IDevice device) {
                        notifyDeviceUpdated(device);
                    }

                    @Override
                    public void deviceChanged(@NotNull IDevice device, int changeMask) {
                        if ((changeMask & IDevice.CHANGE_STATE) == IDevice.CHANGE_STATE) {
                            notifyDeviceUpdated(device);
                        }
                    }

                    @Override
                    public void clientSelected(@Nullable final Client c) {
                    }
                };
        deviceContext.addListener(deviceSelectionListener, this);
        AndroidDebugBridge.addClientChangeListener(this);
        AndroidDebugBridge.addDeviceChangeListener(this);
        AndroidDebugBridge.addDebugBridgeChangeListener(this);
    }

    @Override
    public void dispose() {
        AndroidDebugBridge.removeClientChangeListener(this);
        for (DeviceLogSource deviceLogSource : deviceLogSourcesMap.values()) {
            deviceLogSource.getLogProvider().dispose();
        }
        for (LogSource fileLogSource : fileLogSourcesMap.values()) {
            fileLogSource.getLogProvider().dispose();
        }
        if (myBridge != null) {
            AndroidDebugBridge.removeDeviceChangeListener(this);
            AndroidDebugBridge.removeDebugBridgeChangeListener(this);
            myBridge = null;
        }

        deviceLogSourcesMap.clear();
    }

    /**
     * Returns the list of available sources
     */
    public List<LogSource> getLogSourceList() {
        List<LogSource> logSources = new ArrayList<>();
        logSources.addAll(deviceLogSourcesMap.values());
        logSources.addAll(fileLogSourcesMap.values());
        return logSources;
    }

    /**
     * Called when device is updated
     */
    private void notifyDeviceUpdated(IDevice device) {
        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
            if (project.isDisposed()) {
                return;
            }
            if (device != null) {
                LogSource logSource = getSource(device);
                if (logSource != selectedSource) {
                    onSourceSelected(logSource);
                }
            }
        });
    }

    /**
     * Called when a source is selected
     */
    private void onSourceSelected(LogSource logSource) {
        selectedSource = logSource;
        logSourceManagerListener.onSourceSelectionChanged();
    }

    /**
     * Returns the {@link DeviceLogSource} for the given device
     */
    private DeviceLogSource getDeviceSource(IDevice device) {
        return deviceLogSourcesMap.get(device);
    }

    /**
     * Returns the {@link FileLogSource} for the given file
     */
    private FileLogSource getFileSource(File file) {
        return fileLogSourcesMap.get(file);
    }

    /**
     * Returns the {@link LogSource} for the given device, creates as needed
     */
    private LogSource getSource(IDevice device) {
        LogSource source = null;
        if (device != null) {
            source = getDeviceSource(device);
            if (source == null && device instanceof FileDevice) {
                source = getFileSource(((FileDevice) device).getFile());
            }
        }
        return source;
    }


    /**
     * Returns the selected {@link LogSource}
     */
    public LogSource getSelectedSource() {
        if (selectedSource == null) {
            if (!deviceLogSourcesMap.isEmpty()) {
                selectedSource = deviceLogSourcesMap.values().iterator().next();
            } else if (!fileLogSourcesMap.isEmpty()) {
                selectedSource = fileLogSourcesMap.values().iterator().next();
            }
        }
        return selectedSource;
    }

    @Override
    public void bridgeChanged(final AndroidDebugBridge bridge) {
        myBridge = bridge;
        updateDeviceList();
    }

    @Override
    public void deviceConnected(final IDevice device) {
        updateDeviceList();
    }

    @Override
    public void deviceDisconnected(final IDevice device) {
        updateDeviceList();
    }

    @Override
    public void deviceChanged(final IDevice device, final int changeMask) {
        if ((changeMask & IDevice.CHANGE_STATE) != 0) {
            updateDeviceList();
        }
        if (device != null) {
            deviceSelectionListener.deviceChanged(device, changeMask);
        }
    }

    /**
     * Updates the device list
     */
    private void updateDeviceList() {
        if (myBridge != null) {
            java.util.List<IDevice> deviceList = new ArrayList<>();
            for (IDevice device : myBridge.getDevices()) {
                deviceList.add(device);
                DeviceLogSource deviceLogSource = getDeviceSource(device);
                if (deviceLogSource == null) {
                    deviceLogSource = new DeviceLogSource(device);
                    deviceLogSourcesMap.put(device, deviceLogSource);
                }
            }
            for (IDevice device : deviceLogSourcesMap.keySet()) {
                if (!deviceList.contains(device)) {
                    deviceLogSourcesMap.get(device).getLogProvider().dispose();
                    deviceLogSourcesMap.remove(device);
                }
            }
            logSourceManagerListener.onSourceListChanged();
        }
    }

    @Override
    public void onFileDropped(File file) {
        FileLogSource fileLogSource = getFileSource(file);
        if (fileLogSource == null) {
            fileLogSource = new FileLogSource(project, file);
            fileLogSourcesMap.put(file, fileLogSource);
        }
        selectedSource = fileLogSource;
        logSourceManagerListener.onSourceListChanged();
    }

    public void clearFileSources() {
        for (LogSource fileLogSource : fileLogSourcesMap.values()) {
            fileLogSource.getLogProvider().dispose();
        }
        fileLogSourcesMap.clear();
        if (selectedSource instanceof FileLogSource) {
            selectedSource = null;
        }
    }

    public boolean isDeviceSourceSelected() {
        return selectedSource instanceof DeviceLogSource;
    }


    /**
     * Listener to get notified about source list and selection changes
     */
    public interface LogSourceManagerListener {
        /**
         * Called when source selection changes
         */
        void onSourceSelectionChanged();

        /**
         * Called when list of available sources changes
         */
        void onSourceListChanged();
    }
}
