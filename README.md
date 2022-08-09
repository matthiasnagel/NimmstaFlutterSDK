# Nimmsta SDK

A Flutter wrapper of nimmsta sdk.

The only supported platform is Android as at the moment Nimmsta offers an SDK just for that platform.

# Usage
Add `nimmsta_sdk: <version>` to your pubspec.yaml

Add this values to your **local.properties** file:
```
nimmsta.username=<username>
nimmsta.password=<password>
```

Username and password are provided by Nimmsta (https://nimmsta.com/).

Modify your manifest adding
``` xml
<application
        ...
        android:theme="@style/NormalTheme">
        <activity
            ...
            <meta-data
                android:name="io.flutter.embedding.android.NormalTheme"
                android:resource="@style/NormalTheme"
                />
            ...
```

Add this style in `/res/values/styles.xml`:
``` xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    ...
    <style name="NormalTheme" parent="Theme.AppCompat.Light.DarkActionBar">
        <item name="android:windowBackground">?android:colorBackground</item>
    </style>
</resources>
```

Add this to your app level gradle:
``` 
android {
    ...
    packagingOptions {
        exclude 'META-INF/*.kotlin_module'
        exclude 'META-INF/DEPENDENCIES'
        exclude 'META-INF/INDEX.LIST'
        exclude 'META-INF/io.netty.versions.properties'
    }
}

dependencies {
    ...
    implementation "androidx.appcompat:appcompat:1.2.0" /* TODO temporary dependency included to make Nimmsta SDK work */
}
```

Your layouts to be loaded on the device should be put in `/res/raw` directory
