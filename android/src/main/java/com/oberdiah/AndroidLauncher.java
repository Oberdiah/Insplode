package com.oberdiah;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.DefaultAndroidAudio;
import com.oberdiah.utils.PlatformInterface;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import games.rednblack.miniaudio.MiniAudio;

class AndroidPlatformInterface implements PlatformInterface {
    AssetManager assetManager;

    @Override
    public void print(@NotNull String string) {
        Log.i("Bomb Survival", string);
    }

    @Override
    public void injectAssetManager(@NotNull MiniAudio miniAudio) {
        miniAudio.setupAndroid(assetManager);
    }
}

/**
 * Launches the Android application.
 */
public class AndroidLauncher extends AndroidApplication {
    AndroidPlatformInterface platformInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useImmersiveMode = true;
        platformInterface = new AndroidPlatformInterface();
        initialize(new Main(platformInterface), config);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public AndroidAudio createAudio(Context context, AndroidApplicationConfiguration config) {
        System.out.println("Creating audio!");
        try {
            System.out.println(Arrays.toString(context.getAssets().list("")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        platformInterface.assetManager = context.getAssets();
        return new DefaultAndroidAudio(context, config);
    }
}