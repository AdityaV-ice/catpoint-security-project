package com.udacity.catpoint.security.application;

import com.udacity.catpoint.data.AlarmStatus;

public interface StatusListener {
    void notify(AlarmStatus status);
    void catDetected(boolean catDetected);

    /**
     * Called when the set of sensors or their active states change.
     * Default no-op so implementations only implement it if they need it.
     */
    default void sensorStatusChanged() {}
}
