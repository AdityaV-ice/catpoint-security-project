package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

public class SecurityService {

    private final ImageService imageService;
    private final SecurityRepository repo;
    private final Set<StatusListener> statusListeners = new HashSet<>();

    // Track current camera result for “armed-home while cat visible -> ALARM”
    private boolean catCurrentlyVisible = false;

    public SecurityService(SecurityRepository securityRepository, ImageService imageService) {
        this.repo = securityRepository;
        this.imageService = imageService;
    }

    /* ------------ Public API ------------ */

    public void setArmingStatus(ArmingStatus armingStatus) {
        if (armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        } else {
            // Requirement 10: when arming, reset all sensors to inactive
            for (Sensor s : repo.getSensors()) {
                if (Boolean.TRUE.equals(s.getActive())) {
                    s.setActive(false);
                    repo.updateSensor(s);
                }
            }
        }

        repo.setArmingStatus(armingStatus);

        // Requirement 11: If armed-home while camera shows a cat => ALARM
        if (armingStatus == ArmingStatus.ARMED_HOME && catCurrentlyVisible) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
    }

    public void addStatusListener(StatusListener statusListener) { statusListeners.add(statusListener); }
    public void removeStatusListener(StatusListener statusListener) { statusListeners.remove(statusListener); }

    public void setAlarmStatus(AlarmStatus status) {
        repo.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {
    // Requirement 4: If alarm is active, sensor changes do NOT affect alarm
    if (repo.getAlarmStatus() == AlarmStatus.ALARM) {
        sensor.setActive(active);
        repo.updateSensor(sensor);
        return;
    }

    boolean wasActive = Boolean.TRUE.equals(sensor.getActive());
    boolean becomesActive = Boolean.TRUE.equals(active);

    // NEW: Requirement 5 — activating an already-active sensor while PENDING => ALARM
    if (wasActive && becomesActive) {
        if (repo.getAlarmStatus() == AlarmStatus.PENDING_ALARM) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        // Persist state and stop (no other transitions apply)
        sensor.setActive(true);
        repo.updateSensor(sensor);
        return;
    }

    if (!wasActive && becomesActive) {
        // first-time activation
        handleSensorActivated();
    } else if (wasActive && !becomesActive) {
        // deactivation
        handleSensorDeactivated();
    }
    // Requirement 6 is implicitly satisfied: inactive -> inactive does nothing to alarm
    sensor.setActive(active);
    repo.updateSensor(sensor);
}


    public void processImage(BufferedImage currentCameraImage) {
        boolean cat = imageService.imageContainsCat(currentCameraImage, 50.0f);
        catDetected(cat);
    }

    public AlarmStatus getAlarmStatus() { return repo.getAlarmStatus(); }
    public Set<Sensor> getSensors() { return repo.getSensors(); }
    public void addSensor(Sensor sensor) { repo.addSensor(sensor); }
    public void removeSensor(Sensor sensor) { repo.removeSensor(sensor); }
    public ArmingStatus getArmingStatus() { return repo.getArmingStatus(); }

    /* ------------ Internal logic ------------ */

    private void handleSensorActivated() {
        if (repo.getArmingStatus() == ArmingStatus.DISARMED) return; // Requirement 9 covered elsewhere

        switch (repo.getAlarmStatus()) {
            // Requirement 1: armed + a sensor activated => PENDING_ALARM
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            // Requirement 2/5: if already PENDING and a (another or re-)sensor becomes active => ALARM
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
            case ALARM -> { /* ignore per Requirement 4 */ }
        }
    }

    private void handleSensorDeactivated() {
        switch (repo.getAlarmStatus()) {
            // Requirement 3: if PENDING and all sensors inactive => NO_ALARM
            case PENDING_ALARM -> {
                boolean anyActive = repo.getSensors().stream().anyMatch(Sensor::getActive);
                if (!anyActive) {
                    setAlarmStatus(AlarmStatus.NO_ALARM);
                }
            }
            case ALARM -> { /* Requirement 4: ignore while ALARM */ }
            case NO_ALARM -> { /* nothing */ }
        }
    }

    private void catDetected(boolean cat) {
        catCurrentlyVisible = cat;

        if (cat && repo.getArmingStatus() == ArmingStatus.ARMED_HOME) {
            // Requirement 7 & 11
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat) {
            // Requirement 8: no cat AND no sensors active => NO_ALARM
            boolean anyActive = repo.getSensors().stream().anyMatch(Sensor::getActive);
            if (!anyActive) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }
}
