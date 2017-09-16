package com.josesamuel.logviewer.log;

import com.android.ddmlib.Client;
import com.android.ddmlib.ClientData;

public class LogProcess {
    private String processName = "TAG";
    private int processID;

    public LogProcess() {
    }

    public LogProcess(Client client) {
        ClientData cd = client.getClientData();
        setProcessName(cd.getClientDescription());
        setProcessID(cd.getPid());
    }

    public String getProcessName() {
        return processName;
    }

    public LogProcess setProcessName(String processName) {
        this.processName = processName == null ? "TAG" : processName;
        return this;
    }

    public int getProcessID() {
        return processID;
    }

    public LogProcess setProcessID(int processID) {
        this.processID = processID;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof LogProcess) {
            LogProcess logProcess = (LogProcess) o;
            return processName.equals(logProcess.processName) && processID == logProcess.processID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (processName != null) {
            return processName.hashCode() * 91 + processID;
        } else {
            return super.hashCode();
        }
    }

    @Override
    public String toString() {
        return "" + processName + "[" + processID + "]";
    }
}
