package com.josesamuel.logviewer.log.device;

import com.android.ddmlib.IDevice;
import com.josesamuel.logviewer.log.LogDataProvider;
import com.josesamuel.logviewer.log.LogSource;

public class DeviceLogSource implements LogSource {

    private IDevice device;
    private DeviceLogDataProvider logDataProvider;

    public DeviceLogSource(IDevice device) {
        this.device = device;
        this.logDataProvider = new DeviceLogDataProvider(device);
    }

    @Override
    public IDevice getSource() {
        return device;
    }

    @Override
    public LogDataProvider getLogProvider() {
        return logDataProvider;
    }

    @Override
    public String toString() {
        return device.getSerialNumber() +" " + super.toString();
    }
}
