package com.josesamuel.logviewer.view;

import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.tools.idea.actions.BrowserHelpAction;
import com.android.tools.idea.ddms.DeviceContext;
import com.android.tools.idea.logcat.*;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.diagnostic.logging.*;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.SideBorder;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import com.josesamuel.logviewer.gist.GistCreator;
import com.josesamuel.logviewer.log.LogDataListener;
import com.josesamuel.logviewer.log.LogProcess;
import com.josesamuel.logviewer.log.LogSource;
import com.josesamuel.logviewer.log.LogSourceManager;
import com.josesamuel.logviewer.log.dnd.DnDHandler;
import com.josesamuel.logviewer.util.SingleTaskBackgroundExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The main view of the logviewer
 *
 * @author js
 */
public abstract class LogView implements Disposable, GistCreator.GistListener, LogSourceManager.LogSourceManagerListener, LogDataListener {

    private static final String CONSOLE_VIEW_POPUP_MENU = "ConsoleView.PopupMenu";
    private static final String groupName = "LogViewer Folding";
    private static final long RESET_DELAY = 600;
    private final Project myProject;
    private final AndroidLogConsole myLogConsole;
    private LogSourceManager logSourceManager;
    private LogSourcePanel logSourcePanel;
    private JPanel myPanel;
    private JPanel editorPanel;
    private JSplitPane splitPane;
    private JCheckBox vCheckBox;
    private JCheckBox dCheckBox;
    private JCheckBox iCheckBox;
    private JList processList;
    private JTextArea textFilters;
    private JCheckBox wCheckBox;
    private JCheckBox eCheckBox;
    private JCheckBox aCheckBox;
    private JLabel processFilterTitle;
    private volatile LogSource myLogSource;
    private boolean filterOpen;
    private DefaultListModel<LogProcess> processListModel;
    private java.util.Set<LogProcess> processFilter;
    private java.util.Set<String> addFilters;
    private java.util.Set<String> removeFilters;
    private GistCreator gistCreator;
    private LogViewerFilterModel myLogFilterModel = new LogViewerFilterModel();
    private AndroidLogcatFormatter logFormatter;
    private int defaultCycleBufferSize;
    private Timer timer;
    private long textFilterUpdateCreateTime;
    private boolean liveLogPaused;

    /**
     * Initialize the views
     */
    public LogView(final Project project, @Nullable DeviceContext deviceContext, int defaultCycleBufferSize) {
        myProject = project;
        gistCreator = new GistCreator();
        this.defaultCycleBufferSize = defaultCycleBufferSize;
        logSourceManager = new LogSourceManager(project, deviceContext, this);

        processFilter = Collections.newSetFromMap(new ConcurrentHashMap<LogProcess, Boolean>());
        addFilters = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        removeFilters = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

        logFormatter = new AndroidLogcatFormatter(AndroidLogcatPreferences.getInstance(project));
        myLogConsole = new AndroidLogConsole(myProject, myLogFilterModel, logFormatter);
        timer = new Timer(true);

        //Update tasks
        Runnable processUpdateTask = () -> {
            processFilter.clear();
            for (Object selection : processList.getSelectedValuesList()) {
                processFilter.add((LogProcess) selection);
            }
            myLogConsole.refresh("Filtering process");
        };
        Runnable textUpdateTask = this::resetTextFilters;
        Runnable levelUpdateTask = () -> myLogConsole.refresh("Applying level filters");


        //initialize the process list
        processList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        processListModel = new DefaultListModel<LogProcess>();
        processList.setModel(processListModel);
        processList.setCellRenderer(new ClientCellRenderer());
        processList.addListSelectionListener(listSelectionEvent -> {
            if (!listSelectionEvent.getValueIsAdjusting()) {
                resetUpdateTimer(processUpdateTask);
            }
        });


        //set up filters
        textFilters.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent keyEvent) {
                super.keyReleased(keyEvent);
                resetUpdateTimer(textUpdateTask);
            }
        });

        //level checkboxes
        ItemListener levelChangeListener = itemEvent -> resetUpdateTimer(levelUpdateTask);

        vCheckBox.addItemListener(levelChangeListener);
        dCheckBox.addItemListener(levelChangeListener);
        iCheckBox.addItemListener(levelChangeListener);
        wCheckBox.addItemListener(levelChangeListener);
        eCheckBox.addItemListener(levelChangeListener);
        aCheckBox.addItemListener(levelChangeListener);


        filterOpen = false;
        splitPane.setDividerLocation(0);
        splitPane.setDividerSize(0);

        JComponent consoleComponent = myLogConsole.getComponent();

        final ConsoleView console = myLogConsole.getConsole();

        if (console != null) {
            final ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("LogViewer",
                    getEditorActions(), false);
            toolbar.setTargetComponent(console.getComponent());

            final JComponent tbComp1 = toolbar.getComponent();
            myPanel.add(tbComp1, BorderLayout.WEST);
        }

        logSourcePanel = new LogSourcePanel(deviceContext);
        JPanel panel = logSourcePanel.getComponent();
        panel.setBorder(IdeBorderFactory.createBorder(SideBorder.BOTTOM));
        myPanel.add(panel, BorderLayout.NORTH);
        editorPanel.add(consoleComponent, BorderLayout.CENTER);
        Disposer.register(myProject, this);
        Disposer.register(this, myLogConsole);

        updateLogConsole();
        DnDHandler dnDHandler = new DnDHandler(logSourceManager);
        dnDHandler.addDndSupportForComponent(myPanel);
    }

    /**
     * Add the fold related actions to the popup
     */
    private void addFoldingMenu() {
        removeFoldingMenu();
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(CONSOLE_VIEW_POPUP_MENU);
        if (actionGroup != null) {
            DefaultActionGroup foldGroup = new DefaultActionGroup("LogViewer Folding", true);
            foldGroup.add(createFoldTopToCurrentAction());
            foldGroup.add(createFoldSelectiontAction());
            foldGroup.add(createFoldRestAction());
            foldGroup.add(createClearFoldAction());
            actionGroup.add(foldGroup);
        }
    }

    /**
     * Remove custom folding menu
     */
    private void removeFoldingMenu() {
        ActionManager actionManager = ActionManager.getInstance();
        DefaultActionGroup actionGroup = (DefaultActionGroup) actionManager.getAction(CONSOLE_VIEW_POPUP_MENU);

        if (actionGroup != null) {
            for (AnAction action : actionGroup.getChildActionsOrStubs()) {
                if (action.getTemplatePresentation() != null
                        && action.getTemplatePresentation().getText() != null &&
                        action.getTemplatePresentation().getText().equals(groupName)) {
                    actionGroup.remove(action);
                }
            }
        }
    }

    /**
     * Updates the refresh timer
     */
    private void resetUpdateTimer(Runnable task) {
        textFilterUpdateCreateTime = System.currentTimeMillis();
        if (timer == null) {
            timer = new Timer(true);
        }
        timer.schedule(new FilterApplyTask(textFilterUpdateCreateTime, task), RESET_DELAY);
    }

    /**
     * Resets the text filters with the current data
     */
    private void resetTextFilters() {
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
        myLogConsole.refresh("Applying Text Filters");
    }

    /**
     * Returns the updated ActionGroup for the editor
     */
    private ActionGroup getEditorActions() {
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
        editorActions.add(new ToggleAction("Pause/Resume logs", "Pause or Resume live logs", AllIcons.Actions.Pause) {

            @Override
            public boolean isSelected(AnActionEvent anActionEvent) {
                return liveLogPaused;
            }

            @Override
            public void setSelected(AnActionEvent anActionEvent, boolean b) {
                liveLogPaused = b;
            }

            @Override
            public void update(@NotNull AnActionEvent e) {
                super.update(e);
                e.getPresentation().setEnabled(logSourceManager.isDeviceSourceSelected());
            }
        });

        editorActions.add(createGistAction());
        editorActions.add(new BrowserHelpAction("LogViewer", "https://josesamuel.com/logviewer/"));
        editorActions.addSeparator();
        editorActions.add(myLogConsole.getOrCreateActions());
        editorActions.add(new MyConfigureLogcatHeaderAction());
        return editorActions;
    }

    /**
     * Returns an action that creates a gist
     */
    private AnAction createGistAction() {
        return new AnAction("Share Log", "Share log using Gist", AllIcons.Actions.Share) {
            @Override
            public void actionPerformed(AnActionEvent anActionEvent) {
                gistCreator.createGist(myProject, myLogConsole.getSelectedText(true), LogView.this);
            }
        };
    }

    /**
     * Action to fold from top
     */
    private AnAction createFoldTopToCurrentAction() {
        return new AnAction("Fold from top") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                if (myEditor != null) {
                    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
                        SelectionModel selectionModel = myEditor.getSelectionModel();
                        int start = 0;
                        int end = selectionModel.getSelectionStart();
                        FoldRegion foldRegion = myEditor.getFoldingModel().addFoldRegion(start, end,
                                "...");
                        if (foldRegion != null) {
                            foldRegion.setExpanded(false);
                        }
                    }, true);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                e.getPresentation().setEnabled(e.getProject() == myProject && myEditor != null && myEditor.getSelectionModel().getSelectionStart() > 0);
            }
        };
    }

    /**
     * Action to fold selection
     */
    private AnAction createFoldSelectiontAction() {
        return new AnAction("Fold selection") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                if (myEditor != null && myEditor.getSelectionModel().hasSelection()) {
                    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
                        SelectionModel selectionModel = myEditor.getSelectionModel();
                        int start = selectionModel.getSelectionStart();
                        int end = selectionModel.getSelectionEnd();
                        FoldRegion foldRegion = myEditor.getFoldingModel().addFoldRegion(start, end,
                                "...");
                        if (foldRegion != null) {
                            foldRegion.setExpanded(false);
                        }

                    }, true);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                e.getPresentation().setEnabled(e.getProject() == myProject && myEditor != null && myEditor.getSelectionModel().hasSelection());
            }
        };
    }

    /**
     * Action to fold rest
     */
    private AnAction createFoldRestAction() {
        return new AnAction("Fold till end") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                if (myEditor != null) {
                    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
                        SelectionModel selectionModel = myEditor.getSelectionModel();
                        int start = selectionModel.getSelectionStart();
                        int end = myEditor.getDocument().getTextLength();
                        FoldRegion foldRegion = myEditor.getFoldingModel().addFoldRegion(start, end,
                                "...");

                        if (foldRegion != null) {
                            foldRegion.setExpanded(false);
                        }

                    }, true);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                e.getPresentation().setEnabled(e.getProject() == myProject && myEditor != null && myEditor.getSelectionModel().getSelectionStart() < myEditor.getDocument().getTextLength());
            }
        };
    }

    /**
     * Action to clear folds
     */
    private AnAction createClearFoldAction() {
        return new AnAction("Clear all folds") {
            @Override
            public void actionPerformed(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                if (myEditor != null) {
                    myEditor.getFoldingModel().runBatchFoldingOperation(() -> {
                        for (FoldRegion foldRegion : myEditor.getFoldingModel().getAllFoldRegions()) {
                            myEditor.getFoldingModel().removeFoldRegion(foldRegion);
                        }
                    }, true);
                }
            }

            @Override
            public void update(AnActionEvent e) {
                ConsoleView console = myLogConsole.getConsole();
                Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
                e.getPresentation().setEnabled(e.getProject() == myProject && myEditor != null && myEditor.getFoldingModel().getAllFoldRegions().length > 0);
            }
        };
    }


    protected abstract boolean isActive();

    /**
     * Shows the given message as nint
     */
    public void showHint(String message) {
        ConsoleView console = myLogConsole.getConsole();
        Editor myEditor = console != null ? CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
        if (myEditor != null) {
            HintManager.getInstance().showInformationHint(myEditor, message);
        }
    }


    /**
     * Activate the view
     */
    public final void activate() {
        if (isActive()) {
            onSourceListChanged();
            updateLogConsole();
        }
        if (myLogConsole != null) {
            myLogConsole.activate();
        }
        addFoldingMenu();
    }

    /**
     * Deactivates the view
     */
    public void deactivate() {
        logSourceManager.clearFileSources();
        removeFoldingMenu();
    }

    /**
     * Update the log console. Unregisters any previous source, and register with current source
     */
    private void updateLogConsole() {
        LogSource logSource = logSourceManager.getSelectedSource();
        if (myLogSource != logSource) {
            liveLogPaused = false;
            processFilter.clear();
            processListModel.clear();
            if (myLogSource != null) {
                myLogSource.getLogProvider().unRegisterLogListener(this);
            }
            if (myLogConsole.getConsole() != null) {
                myLogConsole.clear();
            }
            myLogFilterModel.processingStarted();
            myLogSource = logSource;
            if (myLogSource != null) {
                myLogSource.getLogProvider().registerLogListener(this);
            }
            if (logSourceManager.isDeviceSourceSelected()) {
                processFilterTitle.setText("Process");
            } else {
                processFilterTitle.setText("Tags");
            }
        }
    }


    @NotNull
    public final JPanel getContentPanel() {
        return myPanel;
    }

    @Override
    public final void dispose() {
        logSourceManager.dispose();
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
        if (processFilter.isEmpty()) {
            return true;
        }
        boolean accept = true;
        if (logSourceManager.isDeviceSourceSelected()) {
            String appName = line.getAppName();
            if (appName != null) {
                accept = canAcceptProcessName(line.getAppName());
                if (!accept && appName.indexOf('.') == -1) {
                    accept = canAcceptProcessName(line.getTag());
                }
            }
            if (accept && line.getPid() != 0) {
                accept = canAcceptPid(line.getPid());
            }
        } else {
            if (line.getTag() != null) {
                accept = canAcceptProcessName(line.getTag());
            }
            if (accept && line.getPid() != 0) {
                accept = canAcceptPid(line.getPid());
            }
        }
        return accept;
    }

    /**
     * Check whether the  process name of the given message is acceptable
     */
    private boolean canAcceptProcessName(String processName) {
        for (LogProcess logProcess : processFilter) {
            if (logProcess.getProcessName().equalsIgnoreCase(processName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether the  process id of the given message is acceptable
     */
    private boolean canAcceptPid(int pid) {
        for (LogProcess client : processFilter) {
            if (client.getProcessID() == pid) {
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
     * Check whether the message of the given message is acceptable
     */
    private boolean canAcceptMessage(String line) {
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

    @Override
    public void onGistCreated(final String gistUrl) {
        UIUtil.invokeLaterIfNeeded(() -> BrowserUtil.browse(gistUrl));
    }

    @Override
    public void onGistFailed(String ex) {
        debug(ex);
    }

    @Override
    public void onSourceSelectionChanged() {
        UIUtil.invokeLaterIfNeeded(this::updateLogConsole);
    }

    @Override
    public void onSourceListChanged() {
        UIUtil.invokeLaterIfNeeded(() -> {
            if (!myProject.isDisposed() && logSourcePanel != null && logSourceManager != null) {
                logSourcePanel.updateDeviceCombo(logSourceManager.getLogSourceList(), logSourceManager.getSelectedSource());
                updateLogConsole();
            }
        });
    }

    @Override
    public void onLogLine(String log, LogProcess logProcess) {
        if (!liveLogPaused) {
            myLogConsole.addLogLine(log);
            if (!processListModel.contains(logProcess)) {
                processListModel.addElement(logProcess);
            }
        }
    }

    @Override
    public void onLogData(String log) {
        myLogConsole.addLogData(log);
        myLogConsole.refresh("Loading logs");
    }

    @Override
    public void onCleared() {
        myLogConsole.clear();
    }

    @Override
    public void debug(String log) {
        if (log != null) {
            UIUtil.invokeLaterIfNeeded(() -> {
                myLogConsole.addLogLine(log);
            });
        }
    }


    @Override
    public void onProcessList(Set<LogProcess> processList) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final List<LogProcess> sortedList = new ArrayList<>(processList);
            sortedList.sort((l, r) -> {
                int c = l.getProcessName().compareTo(r.getProcessName());
                if (c == 0) {
                    c = l.getProcessID() < r.getProcessID() ? -1 : 1;
                }
                return c;
            });
            UIUtil.invokeLaterIfNeeded(() -> {
                processListModel.removeAllElements();

                for (LogProcess client : sortedList) {
                    processListModel.addElement(client);
                }
            });
        });

    }

    /**
     * console that shows the messages
     */
    final class AndroidLogConsole extends LogConsoleBase {
        private final RegexFilterComponent myRegexFilterComponent = new RegexFilterComponent("LOG_FILTER_HISTORY", 5);
        private final AndroidLogcatPreferences myPreferences;

        AndroidLogConsole(Project project, LogFilterModel logFilterModel, LogFormatter logFormatter) {
            super(project, null, "", false, logFilterModel, GlobalSearchScope.allScope(project), logFormatter);
            myPreferences = AndroidLogcatPreferences.getInstance(project);
            myRegexFilterComponent.setFilter(myPreferences.TOOL_WINDOW_CUSTOM_FILTER);
            myRegexFilterComponent.setIsRegex(myPreferences.TOOL_WINDOW_REGEXP_FILTER);
            myRegexFilterComponent.addRegexListener(filter -> {
                myPreferences.TOOL_WINDOW_CUSTOM_FILTER = filter.getFilter();
                myPreferences.TOOL_WINDOW_REGEXP_FILTER = filter.isRegex();
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

        void addLogLine(@NotNull String line) {
            super.addMessage(line);
            if (getOriginalDocument().length() > defaultCycleBufferSize) {
                if (getConsole() != null) {
                    ((ConsoleViewImpl) getConsole()).flushDeferredText();
                }
                getOriginalDocument().delete(0, defaultCycleBufferSize / 4);
                refresh("Clearing old logs");
            }
        }

        /**
         * Adds the bulk log data
         */
        void addLogData(String logData) {
            getOriginalDocument().append(logData).append("\n");
        }

        /**
         * Refreshes the log console in background
         */
        void refresh(final String task) {
            SingleTaskBackgroundExecutor.executeIfPossible(myProject, new SingleTaskBackgroundExecutor.BackgroundTask() {
                @Override
                public void run(ProgressIndicator progressIndicator) {
                    try {
                        UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                            progressIndicator.setFraction(0);
                            doFilter(progressIndicator);
                        });
                    } catch (Throwable ex) {
                        debug("Exception " + ex.getMessage());
                    }
                }

                @Override
                public String getTaskName() {
                    return task;
                }
            });
        }


        /**
         * Filters the console
         */
        private synchronized void doFilter(ProgressIndicator progressIndicator) {
            final ConsoleView console = getConsole();
            String allLInes = getOriginalDocument().toString();
            final String[] lines = allLInes.split("\n");
            if (console != null) {
                console.clear();
            }
            myLogFilterModel.processingStarted();
            int size = lines.length;
            float current = 0;
            for (String line : lines) {
                printMessageToConsole(line);
                current++;
                progressIndicator.setFraction(current / size);
            }
            if (console != null) {
                ((ConsoleViewImpl) console).requestScrollingToEnd();
            }
        }

        /**
         * Prints the message to console
         */
        private void printMessageToConsole(String line) {
            final ConsoleView console = getConsole();
            final LogFilterModel.MyProcessingResult processingResult = myLogFilterModel.processLine(line);
            if (processingResult.isApplicable()) {
                final Key key = processingResult.getKey();
                if (key != null) {
                    ConsoleViewContentType type = ConsoleViewContentType.getConsoleViewType(key);
                    if (type != null) {
                        final String messagePrefix = processingResult.getMessagePrefix();
                        if (messagePrefix != null) {
                            String formattedPrefix = logFormatter.formatPrefix(messagePrefix);
                            if (console != null) {
                                console.print(formattedPrefix, type);
                            }
                        }
                        String formattedMessage = logFormatter.formatMessage(line);
                        if (console != null) {
                            console.print(formattedMessage + "\n", type);
                        }
                    }
                }
            }
        }


        /**
         * Returns the selected text if there is any selection. If not return all based on parameter
         *
         * @param defaultToAll If no selection, then this decides whether to return all text
         */
        String getSelectedText(boolean defaultToAll) {
            ConsoleView console = this.getConsole();
            Editor myEditor = console != null ? (Editor) CommonDataKeys.EDITOR.getData((DataProvider) console) : null;
            if (myEditor != null) {
                Document document = myEditor.getDocument();
                final SelectionModel selectionModel = myEditor.getSelectionModel();
                if (selectionModel.hasSelection()) {
                    return selectionModel.getSelectedText();
                } else if (defaultToAll) {
                    return document.getText();
                }
            }
            return null;
        }
    }

    /**
     * Action that performs the configuration
     */
    private final class MyConfigureLogcatHeaderAction extends AnAction {
        MyConfigureLogcatHeaderAction() {
            super("Configure Header", "Configure Header", AllIcons.General.GearPlain);
        }

        @Override
        public void actionPerformed(AnActionEvent e) {
            ConfigureLogcatFormatDialog dialog = new ConfigureLogcatFormatDialog(myProject);
            if (dialog.showAndGet()) {
                myLogConsole.refresh("Reloading logs");
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
        private List<? extends LogFilter> filters = new ArrayList();

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
            LogCatMessage message = null;
            String continuation = null;
            boolean validContinuation = false;
            try {
                message = AndroidLogcatFormatter.tryParseMessage(line);
                continuation = message == null ? AndroidLogcatFormatter.tryParseContinuation(line) : null;
                validContinuation = continuation != null && this.myPrevHeader != null;
            } catch (Exception ignored) {
            }

            if (message == null && !validContinuation) {
                return new MyProcessingResult(ProcessOutputTypes.STDOUT, canAcceptMessage(line), null);
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

    /**
     * A task that runs only if it is the current task.
     */
    class FilterApplyTask extends TimerTask {

        private long createTme;
        private Runnable task;

        FilterApplyTask(long createTme, Runnable task) {
            this.createTme = createTme;
            this.task = task;
        }

        @Override
        public void run() {
            try {
                if (this.createTme == textFilterUpdateCreateTime) {
                    UIUtil.invokeAndWaitIfNeeded((Runnable) () -> {
                        try {
                            task.run();
                        } catch (Exception ignored) {
                        }
                    });
                }
            } catch (Exception t) {
                debug(t.getMessage());
            }
        }
    }
}
