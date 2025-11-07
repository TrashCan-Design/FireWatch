package com.example.firewatch;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class PollerWorker extends Worker {
    public PollerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Intent serviceIntent = new Intent(getApplicationContext(), StatusPollerService.class);
        ContextCompat.startForegroundService(getApplicationContext(), serviceIntent);
        return Result.success();
    }
}