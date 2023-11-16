package com.kanbanbox.nimmsta_sdk;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nimmsta.barcode.Barcode;
import com.nimmsta.core.android.device.NIMMSTADeviceExtension;
import com.nimmsta.core.android.framework.NIMMSTAConnectionManager;
import com.nimmsta.core.android.framework.NIMMSTAServiceConnection;
import com.nimmsta.core.shared.device.BluetoothDeviceMacAddress;
import com.nimmsta.core.shared.device.NIMMSTADevice;
import com.nimmsta.core.shared.device.NIMMSTAEventHandler;
import com.nimmsta.core.shared.exception.bluetooth.BluetoothDisconnectedException;
import com.nimmsta.core.shared.layout.element.Button;
import com.nimmsta.core.shared.layout.event.ButtonClickEvent;
import com.nimmsta.core.shared.promise.NIMMSTADoneCallback;
import com.nimmsta.core.shared.promise.NIMMSTAFailCallback;
import com.nimmsta.core.shared.promise.Task;
import com.nimmsta.core.shared.textprotocol.event.Event;
import com.nimmsta.core.shared.textprotocol.event.RequestShutdown;
import com.nimmsta.core.shared.textprotocol.event.ScanEvent;
import com.nimmsta.core.shared.textprotocol.event.TouchEvent;
import com.nimmsta.core.shared.textprotocol.request.ChangeTriggerModeRequest;
import com.nimmsta.core.shared.textprotocolapi.softwareupdate.model.SoftwareUpdateProgress;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * NimmstaSdkPlugin
 */
public class NimmstaSdkPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, NIMMSTAEventHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;

    NIMMSTAServiceConnection serviceConnection;
    NIMMSTAConnectionManager connectionManager;

    private Activity activity;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "nimmsta_sdk_methods");
        channel.setMethodCallHandler(this);
    }

    //MethodChannel

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Log.d("NIMMSTA SDK", call.method);

        if (call.method.equals("isConnected")) {
            result.success(this.isConnected());
            return;
        }

        if (call.method.equals("connect")) {
            this.connect();
            result.success(null);
            return;
        }

        if (call.method.equals("reconnect")) {
            this.reconnect(call.argument("deviceAddress"));
            result.success(null);
            return;
        }

        if (call.method.equals("disconnect")) {
            this.disconnect();
            result.success(null);
            return;
        }

        if (call.method.equals("setLayout")) {
            Map<String, String> dataToInject = call.argument("dataToInject");
            this.setLayout((String) call.argument("layoutResource"), dataToInject);
            result.success(null);
            return;
        }

        if (call.method.equals("setScreenInfoAsync")) {
            Map<String, String> dataToInject = call.argument("dataToInject");
            this.setScreenInfoAsync(dataToInject);
            result.success(null);
            return;
        }

        if (call.method.equals("pushSettings")) {
            Map<String, String> settings = call.argument("settings");
            try {
                this.pushSettings(settings);
            } catch (Exception e) {
                e.printStackTrace();
            }
            result.success(null);
            return;
        }

        if (call.method.equals("setLEDColor")) {
            this.setLEDColor((Integer) call.argument("r"), (Integer) call.argument("g"), (Integer) call.argument("b"));
            result.success(null);
            return;
        }

        if (call.method.equals("triggerLEDBurst")) {
            this.triggerLEDBurst((Integer) call.argument("repeat"), (Integer) call.argument("duration"), (Integer) call.argument("pulseDuration"), (Integer) call.argument("r"), (Integer) call.argument("g"), (Integer) call.argument("b"));
            result.success(null);
            return;
        }

        if (call.method.equals("triggerVibrationBurst")) {
            this.triggerVibrationBurst((Integer) call.argument("repeat"), (Integer) call.argument("duration"), (Integer) call.argument("pulseDuration"), (Integer) call.argument("intensity"));
            result.success(null);
            return;
        }

        if (call.method.equals("triggerBeeperBurst")) {
            this.triggerBeeperBurst((Integer) call.argument("repeat"), (Integer) call.argument("duration"), (Integer) call.argument("pulseDuration"), (Integer) call.argument("intensity"));
            result.success(null);
            return;
        }

        result.notImplemented();
    }

    public void connect() {
        serviceConnection = NIMMSTAServiceConnection.bindServiceToActivity(activity, this)
                .onComplete(new NIMMSTADoneCallback<Task<NIMMSTAServiceConnection>>() {
                    @Override
                    public void onDone(Task<NIMMSTAServiceConnection> result) {
                        try {
                            // this is the point in time when the (background) task completes and the result throws if an error occurred.
                            result.getResult();
                            if (getConnectedDevice() == null) {
                                serviceConnection.displayConnectionActivity();
                            }
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
                }).onError(new NIMMSTAFailCallback<Throwable>() {
                    @Override
                    public void onFail(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                });
    }

    public void disconnect() {
        serviceConnection.close();
        activity.runOnUiThread(() -> channel.invokeMethod("didDisconnect", null));
    }

    public void reconnect(String deviceAddress) {
        serviceConnection = NIMMSTAServiceConnection.bindServiceToActivity(activity, this).onComplete(new NIMMSTADoneCallback<Task<NIMMSTAServiceConnection>>() {
            @Override
            public void onDone(Task<NIMMSTAServiceConnection> result) {
                try {
                    // this is the point in time when the (background) task completes and the result throws if an error occurred.
                    result.getResult();

                    connectionManager = serviceConnection.getConnectionManager();
                    if (connectionManager != null) {
                        connectionManager.connectAsync(new BluetoothDeviceMacAddress(deviceAddress));
                    }

                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        });
    }

    private @Nullable
    NIMMSTADevice getConnectedDevice() {
        if (serviceConnection == null) {
            return null;
        }

        connectionManager = serviceConnection.getConnectionManager();
        if (connectionManager != null) {
            Set<NIMMSTADevice> deviceList = connectionManager.getDevices().getConnectedDevices();
            for (NIMMSTADevice device : deviceList) {
                if (device.isConnected()) {
                    return device;
                }
            }
        }

        return null;
    }

    public boolean isConnected() {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return false;
        }

        activity.runOnUiThread(() -> channel.invokeMethod("connectedWithDeviceAddress", device.getAddress().toString()));

        return true;
    }

    public void setLayout(String layoutResource, Map<String, String> dataToInject) {
        int layoutResourceId = this.activity.getResources().getIdentifier(layoutResource, "raw", this.activity.getPackageName());
        if (layoutResourceId == 0) {
            Log.e("NIMMSTA SDK", "Layout not found R.raw." + layoutResource);
            return;
        }

        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        NIMMSTADeviceExtension.INSTANCE.setLayout(
                device,
                layoutResourceId,
                dataToInject
        );
    }

    public void setScreenInfoAsync(Map<String, String> dataToInject) {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        device.setScreenInfoAsync(dataToInject, false);
    }

    public void pushSettings(Map<String, String> settings) throws Exception {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        if (settings.containsKey("prefersReconnect")) {
            device.setPrefersReconnect(Boolean.parseBoolean(settings.get("prefersReconnect")));
        }

        if (settings.containsKey("prefersShutdownOnCharge")) {
            device.setPrefersShutdownOnCharge(Boolean.parseBoolean(settings.get("prefersShutdownOnCharge")));
        }

        if (settings.containsKey("preferredTriggerMode")) {
            ChangeTriggerModeRequest.ScanTriggerMode triggerMode;
            switch (Objects.requireNonNull(settings.get("preferredTriggerMode"))) {
                case "ScanTriggerMode.Button":
                    triggerMode = ChangeTriggerModeRequest.ScanTriggerMode.Button;
                    break;
                case "ScanTriggerMode.ButtonAndTouch":
                    triggerMode = ChangeTriggerModeRequest.ScanTriggerMode.ButtonAndTouch;
                    break;
                case "ScanTriggerMode.Touch":
                    triggerMode = ChangeTriggerModeRequest.ScanTriggerMode.Touch;
                    break;
                case "ScanTriggerMode.Disabled":
                    triggerMode = ChangeTriggerModeRequest.ScanTriggerMode.Disabled;
                    break;
                default:
                    throw new Exception("Invalid setting given for preferredTriggerMode: " + settings.get("preferredTriggerMode"));
            }
            device.setPreferredTriggerMode(triggerMode);
        }

        if (settings.containsKey("preferredPickingMode")) {
            ChangeTriggerModeRequest.ScanPickingMode pickingMode;
            switch (Objects.requireNonNull(settings.get("preferredPickingMode"))) {
                case "ScanPickingMode.ENABLED":
                    pickingMode = ChangeTriggerModeRequest.ScanPickingMode.ENABLED;
                    break;
                case "ScanPickingMode.DISABLED":
                    pickingMode = ChangeTriggerModeRequest.ScanPickingMode.DISABLED;
                    break;
                default:
                    throw new Exception("Invalid setting given for preferredPickingMode: " + settings.get("preferredPickingMode"));
            }
            device.setPreferredPickingMode(pickingMode);
        }
    }

    public void setLEDColor(int r, int g, int b) {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        device.getApi().setLEDColor(r, g, b, null);
    }

    public void triggerLEDBurst(int repeat, int duration, int pulseDuration, int r, int g, int b) {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        device.getApi().triggerLEDBurst(repeat, duration, pulseDuration, r, g, b, null);
    }

    public void triggerVibrationBurst(int repeat, int duration, int pulseDuration, int intensity) {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        device.getApi().triggerVibratorBurst(repeat, duration, pulseDuration, intensity, null);
    }

    public void triggerBeeperBurst(int repeat, int duration, int pulseDuration, int intensity) {
        @Nullable NIMMSTADevice device = getConnectedDevice();
        if (device == null) {
            return;
        }

        device.getApi().triggerBeeperBurst(repeat, duration, pulseDuration, intensity, null);
    }

    //FlutterPlugin

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    //ActivityAware

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        this.activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivity() {
    }

    //Nimmsta SDK

    @Override
    public boolean deviceWillShutdown(@NotNull NIMMSTADevice nimmstaDevice, boolean b) {
        return false;
    }

    @Override
    public void didClickButton(@NotNull NIMMSTADevice nimmstaDevice, @Nullable final Button button, @NotNull ButtonClickEvent buttonClickEvent) {
        Log.i("NIMMSTA SDK", "didClickButton()");
        if (button != null) {
            activity.runOnUiThread(() -> channel.invokeMethod("didClickButton", button.getName()));
        }

        Log.i("NIMMSTA SDK", "didTouch()");
    }

    @Override
    public void didConnectAndInit(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didConnectAndInit()");

        // If is reconnect
        if (nimmstaDevice.getConnectCount() > 1) {
            activity.runOnUiThread(() -> channel.invokeMethod("didReconnectAndInit", nimmstaDevice.getAddress().toString()));

            return;
        }

        activity.runOnUiThread(() -> channel.invokeMethod("didConnectAndInit", nimmstaDevice.getAddress().toString()));
    }

    @Override
    public void didDisconnect(@NotNull NIMMSTADevice nimmstaDevice, @NotNull BluetoothDisconnectedException.Reason reason) {
        activity.runOnUiThread(() -> channel.invokeMethod("didDisconnect", null));
        Log.i("NIMMSTA SDK", "didDisconnect()");
    }

    @Override
    public void didReceiveEvent(@NotNull NIMMSTADevice nimmstaDevice, @NotNull Event event) {
        Log.i("NIMMSTA SDK", "didReceiveEvent()");
    }

    @Override
    public void didScanBarcode(@NotNull NIMMSTADevice nimmstaDevice, @NotNull final Barcode barcode, @NotNull ScanEvent scanEvent) {
        Log.i("NIMMSTA SDK", "didScanBarcode() with barcode " + barcode.getBarcode());
        activity.runOnUiThread(() -> channel.invokeMethod("didScanBarcode", barcode.getBarcode()));
    }

    @Override
    public void didTouch(@NotNull NIMMSTADevice nimmstaDevice, final double x, final double y, int screen, @NotNull final TouchEvent touchEvent) {
        activity.runOnUiThread(() -> {
            final Map<String, String> coordinates = new HashMap<String, String>();
            coordinates.put("x", String.valueOf(x));
            coordinates.put("y", String.valueOf(y));
            channel.invokeMethod("didTouch", coordinates);
        });
        Log.i("NIMMSTA SDK", "didTouch()");
    }

    @Override
    public boolean allowShutdownByUser(@NotNull NIMMSTADevice nimmstaDevice, @NotNull RequestShutdown requestShutdown) {
        Log.i("NIMMSTA SDK", "allowShutdownByUser()");
        return false;
    }

    @Override
    public void batteryLevelChanged(@NotNull NIMMSTADevice nimmstaDevice, int i) {
        Log.i("NIMMSTA SDK", "batteryLevelChanged()");
    }

    @Override
    public boolean deviceShouldHandover(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "deviceShouldHandover()");
        return false;
    }

    @Override
    public void didStartCharging(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didStartCharging()");
    }

    @Override
    public void didStartConnecting(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didStartConnecting()");
    }

    @Override
    public void didStartReconnectSearch(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didStartReconnectSearch()");
    }

    @Override
    public void didStopCharging(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didStopCharging()");
    }

    @Override
    public void didStopReconnectSearch(@NotNull NIMMSTADevice nimmstaDevice) {
        Log.i("NIMMSTA SDK", "didStopReconnectSearch()");
    }

    @Override
    public boolean onError(@Nullable NIMMSTADevice nimmstaDevice, @NotNull Throwable throwable) {
        Log.i("NIMMSTA SDK", "onError()");
        return false;
    }

    @Override
    public void softwareUpdateProgress(@NotNull NIMMSTADevice nimmstaDevice, @NotNull SoftwareUpdateProgress softwareUpdateProgress) {

    }

    @Override
    public void softwareUpgradeProgress(@NotNull NIMMSTADevice nimmstaDevice, @NotNull SoftwareUpdateProgress softwareUpdateProgress) {

    }
}
