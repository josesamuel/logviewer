package com.josesamuel.logviewer;


import com.android.ddmlib.AndroidDebugBridge;
import com.android.tools.idea.ddms.DeviceContext;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.impl.ConsoleBuffer;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerAdapter;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.josesamuel.logviewer.view.AdbBridgeFactory;
import com.josesamuel.logviewer.view.LogView;
import icons.LogviewerPluginIcons;
import org.jetbrains.android.sdk.AndroidSdkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.concurrent.Executor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.io.File;


/**
 * Factory that creates the logviewer toolwindow
 *
 * @author js
 */
public class LogviewFactory implements ToolWindowFactory, DumbAware {

    public static final String TOOL_WINDOW_ID = "Log Viewer";
    public static final Key<LogView> LOG_VIEW_KEY = Key.create("LOG_VIEWER_KEY");
    private static final String ANDROID_LOGCAT_CONTENT_ID = "Log Viewer Logcat";

    /**
     * Creates the Content to be used for the logviewer
     */
    private static Content createLogcatContent(RunnerLayoutUi layoutUi, final Project project, DeviceContext deviceContext) {
        final int defaultCycleBufferSize = ConsoleBuffer.getCycleBufferSize();
        System.setProperty("idea.cycle.buffer.size", "disabled");
        //Create the view
        final LogView logView = new LogView(project, deviceContext, defaultCycleBufferSize) {
            @Override
            protected boolean isActive() {
                ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
                return window.isVisible();
            }
        };

        //Add listener to activate the logview
        ToolWindowManagerEx.getInstanceEx(project).addToolWindowManagerListener(new ToolWindowManagerAdapter() {
            boolean myToolWindowVisible;

            @Override
            public void stateChanged() {
                ToolWindow window = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID);
                if (window != null) {
                    boolean visible = window.isVisible();
                    if (visible != myToolWindowVisible) {
                        myToolWindowVisible = visible;
                        if (visible) {
                            System.setProperty("idea.cycle.buffer.size", "disabled");
                            logView.activate();
                        } else {
                            System.setProperty("idea.cycle.buffer.size", String.valueOf(defaultCycleBufferSize));
                            logView.deactivate();
                        }
                    }
                }
            }
        });


        JPanel logcatContentPanel = logView.getContentPanel();

        final Content logcatContent =
                layoutUi.createContent(ANDROID_LOGCAT_CONTENT_ID, logcatContentPanel, TOOL_WINDOW_ID, LogviewerPluginIcons.TOOL_ICON, null);
        logcatContent.putUserData(LOG_VIEW_KEY, logView);
        logcatContent.setDisposer(logView);
        logcatContent.setCloseable(false);
        logcatContent.setPreferredFocusableComponent(logcatContentPanel);

        return logcatContent;
    }

    @Override
    public void createToolWindowContent(@NotNull final Project project, @NotNull final ToolWindow toolWindow) {

        final File adb = AndroidSdkUtils.getAdb(project);
        ExecutionManager.getInstance(project).getContentManager();

        RunnerLayoutUi layoutUi = RunnerLayoutUi.Factory.getInstance(project).create("LogViewer", TOOL_WINDOW_ID, "Logview Tools", project);

        toolWindow.setIcon(LogviewerPluginIcons.TOOL_ICON);
        toolWindow.setAvailable(true, null);
        toolWindow.setToHideOnEmptyContent(true);
        toolWindow.setTitle(TOOL_WINDOW_ID);


        DeviceContext deviceContext = new DeviceContext();

        Content logcatContent = createLogcatContent(layoutUi, project, deviceContext);
        final LogView logcatView = logcatContent.getUserData(LOG_VIEW_KEY);
        layoutUi.addContent(logcatContent, 0, PlaceInGrid.center, false);

        final JBLoadingPanel loadingPanel = new JBLoadingPanel(new BorderLayout(), project);
        loadingPanel.add(layoutUi.getComponent(), BorderLayout.CENTER);

        final ContentManager contentManager = toolWindow.getContentManager();
        Content c = contentManager.getFactory().createContent(loadingPanel, "", true);
        c.putUserData(LOG_VIEW_KEY, logcatView);
        contentManager.addContent(c);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                logcatView.activate();
            }
        }, project.getDisposed());


        if (adb != null) {
            loadingPanel.setLoadingText("Initializing ADB");
            loadingPanel.startLoading();

            //ListenableFuture<AndroidDebugBridge> future = AdbService.getInstance().getDebugBridge(adb);
            ListenableFuture<AndroidDebugBridge> future = AdbBridgeFactory.getAdb(adb);
            Futures.addCallback(future, new FutureCallback<AndroidDebugBridge>() {
                @Override
                public void onSuccess(@Nullable AndroidDebugBridge bridge) {
                    Logger.getInstance(LogviewFactory.class).info("Successfully obtained debug bridge");
                    loadingPanel.stopLoading();
                }

                @Override
                public void onFailure(@NotNull Throwable t) {
                    loadingPanel.stopLoading();
                    Logger.getInstance(LogviewFactory.class).info("Unable to obtain debug bridge", t);
                    String msg;
                    if (t.getMessage() != null) {
                        msg = t.getMessage();
                    } else {
                        msg = String.format("Unable to establish a connection to adb",
                                ApplicationNamesInfo.getInstance().getProductName(), adb.getAbsolutePath());
                    }
                    Messages.showErrorDialog(msg, "ADB Connection Error");
                }
            }, EdtExecutor.INSTANCE);
        } else {
            logcatView.showHint("No adb connection!.\n\nDrag and drop log files to view them.");
        }
    }


    public static class EdtExecutor implements Executor {
        public static EdtExecutor INSTANCE = new EdtExecutor();

        private EdtExecutor() {
        }

        public void execute(@NotNull Runnable runnable) {
            UIUtil.invokeLaterIfNeeded(runnable);
        }
    }

}
