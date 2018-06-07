package com.josesamuel.logviewer.log.device;

import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.tools.idea.logcat.AndroidLogcatFormatter;
import com.android.tools.idea.logcat.AndroidLogcatService;
import com.google.common.collect.Lists;
import com.intellij.util.ui.UIUtil;
import com.josesamuel.logviewer.log.LogDataListener;
import com.josesamuel.logviewer.log.LogDataProvider;
import com.josesamuel.logviewer.log.LogProcess;
import com.josesamuel.logviewer.view.ClientCellRenderer;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link LogDataProvider} for actual device
 */
public class DeviceLogDataProvider implements LogDataProvider, AndroidLogcatService.LogcatListener {

    private LogCatHeader myActiveHeader;
    private LogDataListener logListener;
    private IDevice device;

    /**
     * Initialize this with the given device
     */
    DeviceLogDataProvider(IDevice device) {
        this.device = device;
    }


    @Override
    public void registerLogListener(LogDataListener logListener) {
        this.logListener = logListener;
        AndroidLogcatService.getInstance().addListener(device, this, true);
        updateProcessList();
    }

    @Override
    public void unRegisterLogListener(LogDataListener logListener) {
        this.logListener = null;
        AndroidLogcatService.getInstance().removeListener(device, this);
    }

    @Override
    public void onLogLineReceived(@NotNull LogCatMessage line) {
        try {
            LogProcess logProcess = new LogProcess();
            logProcess.setProcessID(line.getPid());
            String appName = line.getAppName();
            if (appName == null || appName.isEmpty() || appName.equals("?")) {
                appName = line.getTag();
                if (appName == null || appName.isEmpty() || appName.equals("?")) {
                    appName = "TAG";
                }
            }
            logProcess.setProcessName(appName);
            String message = null;

            if (!line.getHeader().equals(myActiveHeader)) {
                myActiveHeader = line.getHeader();
                message = AndroidLogcatFormatter.formatMessageFull(myActiveHeader, line.getMessage());
            } else {
                message = AndroidLogcatFormatter.formatContinuation(line.getMessage());
            }

            if (message != null) {
                notifyLog(message, logProcess);
            }
        } catch (Exception ex) {
            logListener.debug(ex.getMessage());
        }
    }

    /**
     * Notify a new line is added
     */
    private void notifyLog(String logLine, LogProcess process) {
        UIUtil.invokeLaterIfNeeded(() -> {
            try {
                if (logListener != null) {
                    logListener.onLogLine(logLine, process);
                }
            } catch (Exception ignored) {

            }
        });
    }

    @Override
    public void onCleared() {
        UIUtil.invokeLaterIfNeeded(() -> {
            if (logListener != null) {
                logListener.onCleared();
            }
        });
    }

    /**
     * Clean up
     */
    public void dispose() {
        AndroidLogcatService.getInstance().removeListener(device, this);
        this.logListener = null;
    }

    /**
     * Updates the process list
     */
    public void updateProcessList() {
        if (logListener != null) {
            UIUtil.invokeLaterIfNeeded(() -> {
                try {
                    if (device != null && logListener != null) {
                        List<Client> clients = Lists.newArrayList(device.getClients());
                        clients.sort(new ClientCellRenderer.ClientComparator());
                        logListener.onProcessList(toLogProcess(clients));
                    }
                } catch (Exception ignored) {

                }
            });
        }
    }

    /**
     * Convert list of {@link Client} to list of {@link LogProcess}
     */
    private Set<LogProcess> toLogProcess(List<Client> clientList) {
        Set<LogProcess> logProcesses = new HashSet<>();
        for (Client client : clientList) {
            if (client != null) {
                logProcesses.add(new LogProcess(client));
            }
        }
        return logProcesses;
    }
}
