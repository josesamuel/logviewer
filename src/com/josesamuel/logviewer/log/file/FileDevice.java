package com.josesamuel.logviewer.log.file;

import com.android.ddmlib.*;
import com.android.ddmlib.log.LogReceiver;
import com.android.sdklib.AndroidVersion;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * An {@link IDevice} for a {@link File}
 */
public class FileDevice implements IDevice {

    private File file;

    /**
     * Initialize with given file
     */
    FileDevice(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    @Override
    public String getSerialNumber() {
        return file.getName();
    }

    @Override
    public String getAvdName() {
        return "";
    }

    @Override
    public DeviceState getState() {
        return DeviceState.ONLINE;
    }

    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public int getPropertyCount() {
        return 0;
    }

    @Override
    public String getProperty(String s) {
        return null;
    }

    @Override
    public boolean arePropertiesSet() {
        return false;
    }

    @Override
    public String getPropertySync(String s) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        return null;
    }

    @Override
    public String getPropertyCacheOrSync(String s) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {
        return null;
    }

    @Override
    public boolean supportsFeature(Feature feature) {
        return false;
    }

    @Override
    public boolean supportsFeature(HardwareFeature hardwareFeature) {
        return false;
    }

    @Override
    public String getMountPoint(String s) {
        return null;
    }

    @Override
    public boolean isOnline() {
        return true;
    }

    @Override
    public boolean isEmulator() {
        return false;
    }

    @Override
    public boolean isOffline() {
        return false;
    }

    @Override
    public boolean isBootLoader() {
        return false;
    }

    @Override
    public boolean hasClients() {
        return false;
    }

    @Override
    public Client[] getClients() {
        return new Client[0];
    }

    @Override
    public Client getClient(String s) {
        return null;
    }

    @Override
    public SyncService getSyncService() throws TimeoutException, AdbCommandRejectedException, IOException {
        return null;
    }

    @Override
    public FileListingService getFileListingService() {
        return null;
    }

    @Override
    public RawImage getScreenshot() throws TimeoutException, AdbCommandRejectedException, IOException {
        return null;
    }

    @Override
    public RawImage getScreenshot(long l, TimeUnit timeUnit) throws TimeoutException, AdbCommandRejectedException, IOException {
        return null;
    }

    @Override
    public void startScreenRecorder(String s, ScreenRecorderOptions screenRecorderOptions, IShellOutputReceiver iShellOutputReceiver) throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {

    }

    @Override
    public void executeShellCommand(String s, IShellOutputReceiver iShellOutputReceiver, int i) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    }

    @Override
    public void executeShellCommand(String s, IShellOutputReceiver iShellOutputReceiver) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    }

    @Override
    public void runEventLogService(LogReceiver logReceiver) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public void runLogService(String s, LogReceiver logReceiver) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public void createForward(int i, int i1) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public void createForward(int i, String s, DeviceUnixSocketNamespace deviceUnixSocketNamespace) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public void removeForward(int i, int i1) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public void removeForward(int i, String s, DeviceUnixSocketNamespace deviceUnixSocketNamespace) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public String getClientName(int i) {
        return null;
    }

    @Override
    public void pushFile(String s, String s1) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {

    }

    @Override
    public void pullFile(String s, String s1) throws IOException, AdbCommandRejectedException, TimeoutException, SyncException {

    }

    @Override
    public void installPackage(String s, boolean b, String... strings) throws InstallException {

    }

    @Override
    public void installPackages(List<File> list, boolean b, List<String> list1, long l, TimeUnit timeUnit) throws InstallException {

    }

    @Override
    public String syncPackageToDevice(String s) throws TimeoutException, AdbCommandRejectedException, IOException, SyncException {
        return null;
    }

    @Override
    public void installRemotePackage(String s, boolean b, String... strings) throws InstallException {

    }

    @Override
    public void removeRemotePackage(String s) throws InstallException {

    }

    @Override
    public String uninstallPackage(String s) throws InstallException {
        return null;
    }

    @Override
    public void reboot(String s) throws TimeoutException, AdbCommandRejectedException, IOException {

    }

    @Override
    public boolean root() throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
        return false;
    }

    @Override
    public boolean isRoot() throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
        return false;
    }

    @Override
    public Integer getBatteryLevel() throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
        return null;
    }

    @Override
    public Integer getBatteryLevel(long l) throws TimeoutException, AdbCommandRejectedException, IOException, ShellCommandUnresponsiveException {
        return null;
    }

    @Override
    public Future<Integer> getBattery() {
        return null;
    }

    @Override
    public Future<Integer> getBattery(long l, TimeUnit timeUnit) {
        return null;
    }

    @Override
    public List<String> getAbis() {
        return null;
    }

    @Override
    public int getDensity() {
        return 0;
    }

    @Override
    public String getLanguage() {
        return null;
    }

    @Override
    public String getRegion() {
        return null;
    }

    @Override
    public com.android.sdklib.AndroidVersion getVersion() {
        return AndroidVersion.DEFAULT;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public void executeShellCommand(String s, IShellOutputReceiver iShellOutputReceiver, long l, TimeUnit timeUnit) throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException, IOException {

    }

    @Override
    public Future<String> getSystemProperty(String s) {
        return null;
    }
}
