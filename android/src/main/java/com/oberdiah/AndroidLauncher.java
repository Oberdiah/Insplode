package com.oberdiah;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.oberdiah.Main;
import com.oberdiah.Utils.Printer;

import org.jetbrains.annotations.NotNull;

class AndroidPrinter implements Printer {
	@Override
	public void print(@NotNull String string) {
		Log.i("Bomb Survival", string);
	}
}

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.hideStatusBar = false;
		config.useImmersiveMode = true;
		initialize(new Main(new AndroidPrinter()), config);
	}

	@Override
	protected void onResume() {
		super.onResume();
	}
}