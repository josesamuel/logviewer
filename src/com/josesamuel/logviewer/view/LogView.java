
package com.josesamuel.logviewer.view;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Client;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.tools.idea.actions.BrowserHelpAction;
import com.android.tools.idea.ddms.ClientCellRenderer;
import com.android.tools.idea.ddms.DeviceContext;
import com.android.tools.idea.logcat.*;
import com.google.common.collect.Lists;
import com.intellij.diagnostic.logging.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The logviewer view
 *
 * @author js
 */
public abstract class LogView implements Disposable, AndroidDebugBridge.IClientChangeListener {


    private final Project myProject;
    private final DeviceContext myDeviceContext;
    private final AndroidLogConsole myLogConsole;
    private final IDevice myPreselectedDevice;
    private JPanel myPanel;
    private JPanel editorPanel;
    private JPanel filterPanel;
    private JSplitPane splitPane;
    private JCheckBox vCheckBox;
    private JCheckBox dCheckBox;
    private JCheckBox iCheckBox;
    private JList processList;
    private JTextArea textFilters;
    private JCheckBox wCheckBox;
    private JCheckBox eCheckBox;
    private JCheckBox aCheckBox;
    private volatile IDevice myDevice;
    private boolean filterOpen;
    private DefaultListModel processListModel;
    private java.util.Set<Client> processFilter;
    private java.util.Set<String> addFilters;
    private java.util.Set<String> removeFilters;
    //Gets the message from the logcat service
    private AndroidLogcatService.LogcatListener myLogcatReceiver = new AndroidLogcatService.LogcatListener() {
        private LogCatHeader myActiveHeader;

        @Override
        public void onLogLineReceived(@NotNull LogCatMessage line) {
            if (!line.getHeader().equals(myActiveHeader)) {
                myActiveHeader = line.getHeader();
                String message = AndroidLogcatFormatter.formatMessageFull(myActiveHeader, line.getMessage());
                receiveFormattedLogLine(message);
            } else {
                String message = AndroidLogcatFormatter.formatContinuation(line.getMessage());
                receiveFormattedLogLine(message);
            }
        }

        protected void receiveFormattedLogLine(@NotNull String line) {
            myLogConsole.addLogLine(line);
        }

        @Override
        public void onCleared() {
            if (myLogConsole.getConsole() != null) {
                myLogConsole.clear();
            }
        }
    };
    private LogViewerFilterModel myLogFilterModel = new LogViewerFilterModel();

    /**
     * Logcat view with device obtained from {@link DeviceContext}
     */
    public LogView(@NotNull final Project project, @NotNull DeviceContext deviceContext) {
        this(project, null, deviceContext);
    }

    /**
     * Initialize the views
     */
    private LogView(final Project project, @Nullable IDevice preselectedDevice, @Nullable DeviceContext deviceContext) {
        myDeviceContext = deviceContext;
        myProject = project;
        myPreselectedDevice = preselectedDevice;

        processFilter =  Collections.newSetFromMap(new ConcurrentHashMap<Client, Boolean>());
        addFilters =  Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        removeFilters =  Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        Disposer.register(myProject, this);

        AndroidDebugBridge.addClientChangeListener(this);

        //initialize the process list
        processList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        processListModel = new DefaultListModel();
        processList.setModel(processListModel);
        processList.setCellRenderer(new ClientCellRenderer());
        processList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent) {
                if (!listSelectionEvent.getValueIsAdjusting()) {
                    processFilter.clear();
                    for (Object selection : processList.getSelectedValuesList()) {
                        processFilter.add((Client) selection);
                    }
                    myLogConsole.refresh();
                }
            }
        });

        //set up filters
        textFilters.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                addFilters.clear();
                removeFilters.clear();
                StringTokenizer tokenizer = new StringTokenizer(textFilters.getText(), "\n");
                String token;
                while (tokenizer.hasMoreElements()) {
                    token = tokenizer.nextToken().trim().toLowerCase();
                    if (!token.isEmpty()) {
                        if (token.startsWith("-")) {
                            if (token.length() > 1) {
                                removeFilters.add(token.substring(1));
                            }
                        } else {
                            if (token.startsWith("\\-")) {
                                token = token.substring(1);
                            }
                            addFilters.add(token);
                        }
                    }
                }
                myLogConsole.refresh();
            }
        });

        ChangeListener levelChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent changeEvent) {
                myLogConsole.refresh();
            }
        };

        vCheckBox.addChangeListener(levelChangeListener);
        dCheckBox.addChangeListener(levelChangeListener);
        iCheckBox.addChangeListener(levelChangeListener);
        wCheckBox.addChangeListener(levelChangeListener);
        eCheckBox.addChangeListener(levelChangeListener);
        aCheckBox.addChangeListener(levelChangeListener);


        filterOpen = false;
        splitPane.setDividerLocation(0);
        splitPane.setDividerSize(0);


        AndroidLogcatFormatter logFormatter = new AndroidLogcatFormatter(AndroidLogcatPreferences.getInstance(project));
        myLogConsole = new AndroidLogConsole(project, myLogFilterModel, logFormatter);

        if (preselectedDevice == null && deviceContext != null) {
            DeviceContext.DeviceSelectionListener deviceSelectionListener =
                    new DeviceContext.DeviceSelectionListener() {
                        @Override
                        public void deviceSelected(@Nullable IDevice device) {
                            notifyDeviceUpdated(false);
                        }

                        @Override
                        public void deviceChanged(@NotNull IDevice device, int changeMask) {
                            if (device == myDevice && ((changeMask & IDevice.CHANGE_STATE) == IDevice.CHANGE_STATE)) {
                                notifyDeviceUpdated(true);
                            }
                        }

                        @Override
                        public void clientSelected(@Nullable final Client c) {
                        }
                    };
            deviceContext.addListener(deviceSelectionListener, this);
        }

        updateProcessList();


        JComponent consoleComponent = myLogConsole.getComponent();

        final ConsoleView console = myLogConsole.getConsole();
        if (console != null) {
            DefaultActionGroup editorActions = new DefaultActionGroup();
            editorActions.addSeparator();
            editorActions.add(new ToggleAction("Show/Hide Filters", "Configure Log Viewer Filters", AllIcons.General.Filter) {

                @Override
                public boolean isSelected(AnActionEvent anActionEvent) {
                    return filterOpen;
                }

                @Override
                public void setSelected(AnActionEvent anActionEvent, boolean b) {
                    filterOpen = b;
                    if (filterOpen) {
                        splitPane.setDividerLocation(400);
                        splitPane.setDividerSize(3);
                    } else {
                        splitPane.setDividerLocation(0);
                        splitPane.setDividerSize(0);
                    }
                }

            });

            editorActions.addSeparator();
            editorActions.add(new MyConfigureLogcatHeaderAction());
            editorActions.addSeparator();
            editorActions.add(myLogConsole.getOrCreateActions());
            editorActions.addSeparator();
            editorActions.add(new BrowserHelpAction("LogViewer", "https://josesamuel.com/logviewer/"));

            final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN,
                    editorActions, false);
            toolbar.setTargetComponent(console.getComponent());

            final JComponent tbComp1 = toolbar.getComponent();
            myPanel.add(tbComp1, BorderLayout.WEST);
        }

        LogSourcePanel logSourcePanel = new LogSourcePanel(project, deviceContext);
        JPanel panel = logSourcePanel.getComponent();
        panel.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));

        myPanel.add(panel, BorderLayout.NORTH);


        editorPanel.add(consoleComponent, BorderLayout.CENTER);
        Disposer.register(this, myLogConsole);

        updateLogConsole();
    }

    private void notifyDeviceUpdated(final boolean forceReconnect) {
        UIUtil.invokeAndWaitIfNeeded(new Runnable() {
            @Override
            public void run() {
                if (myProject.isDisposed()) {
                    return;
                }
                if (forceReconnect) {
                    if (myDevice != null) {
                        AndroidLogcatService.getInstance().removeListener(myDevice, myLogcatReceiver);
                    }
                    myDevice = null;
                }
                updateLogConsole();
            }
        });
    }

    protected abstract boolean isActive();

    public final void activate() {
        if (isActive()) {
            updateLogConsole();
        }
        if (myLogConsole != null) {
            myLogConsole.activate();
        }
    }

    private void updateLogConsole() {
        IDevice device = getSelectedDevice();
        if (myDevice != device) {
            AndroidLogcatService androidLogcatService = AndroidLogcatService.getInstance();
            if (myDevice != null) {
                androidLogcatService.removeListener(myDevice, myLogcatReceiver);
            }
            if (myLogConsole.getConsole() != null) {
                myLogConsole.clear();
            }
            myLogFilterModel.processingStarted();
            myDevice = device;
            androidLogcatService.addListener(myDevice, myLogcatReceiver, true);
        }
    }

    @Nullable
    public final IDevice getSelectedDevice() {
        if (myPreselectedDevice != null) {
            return myPreselectedDevice;
        } else if (myDeviceContext != null) {
            return myDeviceContext.getSelectedDevice();
        } else {
            return null;
        }
    }

    @NotNull
    public final JPanel getContentPanel() {
        return myPanel;
    }

    @Override
    public final void dispose() {
        if (myDevice != null) {
            AndroidLogcatService.getInstance().removeListener(myDevice, myLogcatReceiver);
        }
        AndroidDebugBridge.removeClientChangeListener(this);
    }

    @Override
    public void clientChanged(Client client, int changeMask) {
        if ((changeMask & Client.CHANGE_NAME) != 0) {
            updateProcessList();
        }
    }

    private void updateProcessList() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                IDevice device = getSelectedDevice();
                processListModel.removeAllElements();
                if (device != null) {
                    java.util.List<Client> clients = Lists.newArrayList(device.getClients());
                    Collections.sort(clients, new ClientCellRenderer.ClientComparator());
                    for (Client client : clients) {
                        processListModel.addElement(client);
                    }
                }
            }
        });
    }

    /**
     * Check whether the given line can be shown based on current filters
     */
    private boolean canAccept(LogCatMessage line) {
        return canAcceptLevel(line) && canAcceptProcess(line) && canAcceptMessage(line);
    }

    /**
     * Check whether the  log level of the given message is acceptable
     */
    private boolean canAcceptLevel(LogCatMessage line) {
        switch (line.getHeader().getLogLevel()) {
            case ASSERT:
                return aCheckBox.isSelected();
            case DEBUG:
                return dCheckBox.isSelected();
            case ERROR:
                return eCheckBox.isSelected();
            case INFO:
                return iCheckBox.isSelected();
            case VERBOSE:
                return vCheckBox.isSelected();
            case WARN:
                return wCheckBox.isSelected();
        }
        return true;
    }

    /**
     * Check whether the  process of the given message is acceptable
     */
    private boolean canAcceptProcess(LogCatMessage line) {
        return processFilter.isEmpty() || canAcceptProcessName(line.getAppName()) || canAcceptPid(line.getPid());
    }

    /**
     * Check whether the  process name of the given message is acceptable
     */
    private boolean canAcceptProcessName(String processName) {
        for (Client client : processFilter) {
            if (client.getClientData().getClientDescription().equalsIgnoreCase(processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the  process id of the given message is acceptable
     */
    private boolean canAcceptPid(int pid) {
        for (Client client : processFilter) {
            if (client.getClientData().getPid() == pid) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the message of the given message is acceptable
     */
    private boolean canAcceptMessage(LogCatMessage line) {
        return (addFilters.isEmpty() || isInFilter(line, addFilters))
                && (removeFilters.isEmpty() || !isInFilter(line, removeFilters));
    }

    /**
     * Check whether given message matches with any filter in the given set
     */
    private boolean isInFilter(LogCatMessage line, Set<String> filter) {
        return isInFilter(line.getMessage(), filter) || isInFilter(line.getTag(), filter) || isInFilter(line.getAppName(), filter) || isInFilter("" + line.getPid(), filter);
    }

    private boolean isInFilter(String text, Set<String> filters) {
        if (text != null) {
            text = text.toLowerCase();
            for (String filter : filters) {
                if (text.contains(filter)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * console that shows the messages
     */
    final class AndroidLogConsole extends LogConsoleBase {
        private final RegexFilterComponent myRegexFilterComponent = new RegexFilterComponent("LOG_FILTER_HISTORY", 5);
        private final AndroidLogcatPreferences myPreferences;

        public AndroidLogConsole(Project project, LogFilterModel logFilterModel, LogFormatter logFormatter) {
            super(project, null, "", false, logFilterModel, GlobalSearchScope.allScope(project), logFormatter);
            ConsoleView console = getConsole();
            if (console instanceof ConsoleViewImpl) {
                ConsoleViewImpl c = ((ConsoleViewImpl) console);
            }
            myPreferences = AndroidLogcatPreferences.getInstance(project);
            myRegexFilterComponent.setFilter(myPreferences.TOOL_WINDOW_CUSTOM_FILTER);
            myRegexFilterComponent.setIsRegex(myPreferences.TOOL_WINDOW_REGEXP_FILTER);
            myRegexFilterComponent.addRegexListener(new RegexFilterComponent.Listener() {
                @Override
                public void filterChanged(RegexFilterComponent filter) {
                    myPreferences.TOOL_WINDOW_CUSTOM_FILTER = filter.getFilter();
                    myPreferences.TOOL_WINDOW_REGEXP_FILTER = filter.isRegex();
                }
            });
        }

        @Override
        public boolean isActive() {
            return LogView.this.isActive();
        }

        @NotNull
        @Override
        protected Component getTextFilterComponent() {
            return myRegexFilterComponent;
        }

        public void addLogLine(@NotNull String line) {
            super.addMessage(line);
        }

        public void refresh() {
            onTextFilterChange();
        }
    }

    /**
     * Action that performs the configuration
     */
    private final class MyConfigureLogcatHeaderAction extends AnAction {
        public MyConfigureLogcatHeaderAction() {
            super("Configure Header", "Configure Header", AllIcons.General.GearPlain);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ConfigureLogcatFormatDialog dialog = new ConfigureLogcatFormatDialog(myProject);
            if (dialog.showAndGet()) {
                myLogConsole.refresh();
            }
        }
    }

    /**
     * filter model
     */
    private class LogViewerFilterModel extends LogFilterModel {

        private final List<LogFilterListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
        private final StringBuilder myMessageSoFar = new StringBuilder();
        @Nullable
        private LogCatHeader myPrevHeader;
        private boolean myCustomApplicable = false;
        private List filters = new ArrayList();

        @Override
        public String getCustomFilter() {
            return "";
        }

        @Override
        public void addFilterListener(LogFilterListener listener) {
            this.myListeners.add(listener);
        }

        @Override
        public void removeFilterListener(LogFilterListener listener) {
            this.myListeners.remove(listener);
        }

        private void fireTextFilterChange() {
            for (LogFilterListener listener : myListeners) {
                listener.onTextFilterChange();
            }
        }

        @Override
        public List<? extends LogFilter> getLogFilters() {
            return filters;
        }

        @Override
        public boolean isFilterSelected(LogFilter filter) {
            return false;
        }

        @Override
        public void selectFilter(LogFilter filter) {
        }


        public void processingStarted() {
            this.myPrevHeader = null;
            this.myCustomApplicable = false;
            this.myMessageSoFar.setLength(0);
        }

        private boolean isMessageApplicable(LogCatMessage message) {
            return canAccept(message);
        }

        @NotNull
        @Override
        public MyProcessingResult processLine(String line) {
            LogCatMessage message = AndroidLogcatFormatter.tryParseMessage(line);
            String continuation = message == null ? AndroidLogcatFormatter.tryParseContinuation(line) : null;
            boolean validContinuation = continuation != null && this.myPrevHeader != null;
            if (message == null && !validContinuation) {
                return new MyProcessingResult(ProcessOutputTypes.STDOUT, false, (String) null);
            } else {
                if (message != null) {
                    this.myPrevHeader = message.getHeader();
                    this.myCustomApplicable = this.isMessageApplicable(message);
                    this.myMessageSoFar.setLength(0);
                }

                boolean isApplicable = this.myCustomApplicable;
                if (!isApplicable) {
                    this.myMessageSoFar.append(line);
                    this.myMessageSoFar.append('\n');
                }

                Key key = AndroidLogcatUtils.getProcessOutputType(this.myPrevHeader.getLogLevel());
                MyProcessingResult result = new MyProcessingResult(key, isApplicable, this.myMessageSoFar.toString());
                if (isApplicable) {
                    this.myMessageSoFar.setLength(0);
                }

                return result;
            }
        }
    }
}
