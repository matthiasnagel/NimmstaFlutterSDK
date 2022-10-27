import 'package:flutter/material.dart';
import 'dart:async';

import 'package:nimmsta_sdk/nimmsta_sdk.dart';
import 'package:nimmsta_sdk/models/scan_picking_mode.dart' as Picking;
import 'package:nimmsta_sdk/models/scan_trigger_mode.dart' as Trigger;

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? _barcode = "Unknown";
  String _connectionFlag = "disconnected";

  late NimmstaSdk nimmstaSdk;

  @override
  void initState() {
    super.initState();
    initNimmsta();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initNimmsta() async {
    nimmstaSdk = NimmstaSdk(
      didConnectAndInitCallback: (deviceAddress) async {
        await nimmstaSdk.setLayout("test_layout", {
          "title": "Device status",
          "subtitle": "didConnectAndInitCallback",
        });
        await nimmstaSdk.pushSettings(true, true,
            Trigger.ScanTriggerMode.Button, Picking.ScanPickingMode.DISABLED);
        setState(() {
          _connectionFlag = "didConnectAndInitCallback";
        });
      },
      didDisconnectCallback: () {
        setState(() {
          _connectionFlag = "disconnected";
        });
      },
      didTouchCallback: (dynamic arguments) {
        debugPrint(
            "NIMMSTA SDK: didTouchCallback with coordinates ${arguments.toString()}");
      },
      didClickButtonCallback: (dynamic action) {
        debugPrint("NIMMSTA SDK: didClickButtonCallback with action $action");
      },
      didScanBarcodeCallback: (dynamic barcode) {
        setState(() {
          _barcode = barcode;
        });
      },
      didReconnectAndInitCallback: (deviceAddress) async {
        await nimmstaSdk.setLayout("test_layout", {
          "title": "Device status",
          "subtitle": "didReconnectAndInitCallback",
        });
        await nimmstaSdk.pushSettings(true, true,
            Trigger.ScanTriggerMode.Button, Picking.ScanPickingMode.DISABLED);
        setState(() {
          _connectionFlag = "didReconnectAndInitCallback";
        });
      },
      connectedWithDeviceAddress: (deviceAddress) async {
        await nimmstaSdk.setLayout("test_layout", {
          "title": "Device status",
          "subtitle": "connectedWithDeviceAddress",
        });
        await nimmstaSdk.pushSettings(true, true,
            Trigger.ScanTriggerMode.Button, Picking.ScanPickingMode.DISABLED);
        setState(() {
          _connectionFlag = "connectedWithDeviceAddress";
        });
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(
            children: [
              Text('Connection status: $_connectionFlag\n'),
              TextButton(
                onPressed: () async {
                  await nimmstaSdk.connect();
                },
                child: Text('Connect Nimmsta device'),
              ),
              Text('Scanned barcode: $_barcode\n'),
              TextButton(
                onPressed: () async {
                  await nimmstaSdk.setLayout("test_layout", {
                    "title": "This is a title!",
                    "subtitle": "And this is a subtitle",
                  });
                },
                child: Text('Change layout'),
              ),
              TextButton(
                onPressed: () async {
                  await nimmstaSdk.triggerLEDBurst(2, 500, 250, Colors.green);
                },
                child: Text('Trigger LED'),
              ),
              TextButton(
                onPressed: () async {
                  await nimmstaSdk.triggerBeeperBurst(1, 100, 50, 100);
                },
                child: Text('Trigger Beeper'),
              ),
              TextButton(
                onPressed: () async {
                  await nimmstaSdk.triggerVibrationBurst(2, 1000, 500, 100);
                },
                child: Text('Trigger Vibration'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
