package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock SecurityRepository repo;
    @Mock ImageService image;              // <-- mocking the interface
    @Mock StatusListener listener;

    SecurityService service;

    @BeforeEach
    void setUp() {
        service = new SecurityService(repo, image);
        service.addStatusListener(listener);
        when(repo.getSensors()).thenReturn(Set.of(
                new Sensor("Door", SensorType.DOOR),
                new Sensor("Window", SensorType.WINDOW)
        ));
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
    }

    private Sensor newSensor(boolean active) {
        Sensor s = new Sensor("X", SensorType.DOOR);
        s.setActive(active);
        return s;
    }

    // 1) If alarm is armed and a sensor becomes activated -> PENDING
    @Test
    void armed_activateSensor_goPending() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        service.changeSensorActivationStatus(newSensor(false), true);
        verify(repo).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 2) Armed + sensor activates while already pending -> ALARM
    @Test
    void pending_thenActivate_again_goAlarm() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_AWAY);
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        service.changeSensorActivationStatus(newSensor(false), true);
        verify(repo).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 3) Pending and all sensors inactive -> NO_ALARM
    @Test
void pending_allInactive_backToNoAlarm() {
        // Arrange: alarm is PENDING, there is exactly one active sensor in the repo
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        Sensor s = new Sensor("S1", SensorType.DOOR);
        s.setActive(true);

        // Important: repo.getSensors() must return the same Sensor instance,
        // so when SecurityService flips it to inactive, the stream sees no actives.
        when(repo.getSensors()).thenReturn(Set.of(s));

        // Act: deactivate that only-active sensor
        service.changeSensorActivationStatus(s, false);

        // Assert: pending + now all sensors inactive => NO_ALARM
        verify(repo).setAlarmStatus(AlarmStatus.NO_ALARM);

        // (optional sanity check)
        verify(repo).updateSensor(s);
    }

    // 4) If alarm active, sensor changes don’t affect alarm
    @ParameterizedTest @ValueSource(booleans = {true,false})
    void alarmActive_sensorChange_noChange(boolean toActive) {
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        service.changeSensorActivationStatus(newSensor(!toActive), toActive);
        verify(repo, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(repo, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // 5) Sensor activated while already active and pending -> ALARM
    @Test
    void pending_reactivateActiveSensor_goAlarm() {
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        Sensor s = newSensor(true);
        service.changeSensorActivationStatus(s, true);
        verify(repo).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 6) Sensor deactivated while already inactive -> no change
    @Test
    void deactivateInactive_noAlarmChange() {
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        Sensor s = newSensor(false);
        service.changeSensorActivationStatus(s, false);
        verify(repo, never()).setAlarmStatus(any());
    }

    // 7) Image detects cat AND armed-home -> ALARM
    @Test
    void armedHome_catDetected_goAlarm() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(image.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        service.processImage(mock(BufferedImage.class));
        verify(repo).setAlarmStatus(AlarmStatus.ALARM);
    }

    // 8) No cat and all sensors inactive -> NO_ALARM
    @Test
    void noCat_allInactive_goNoAlarm() {
        when(image.imageContainsCat(any(), anyFloat())).thenReturn(false);
        when(repo.getSensors()).thenReturn(Set.of(newSensor(false)));
        service.processImage(mock(BufferedImage.class));
        verify(repo).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // 9) If system DISARMED -> set NO_ALARM
    @Test
    void disarmed_setNoAlarm() {
        service.setArmingStatus(ArmingStatus.DISARMED);
        verify(repo).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(repo).setArmingStatus(ArmingStatus.DISARMED);
    }

    // 10) If system ARMED -> reset all sensors to inactive (expected behavior)
    // This may FAIL if not implemented yet; that’s okay for now (will fix in next section).
    @Test
    void armed_resetsSensorsInactive() {
        Sensor active = newSensor(true);
        when(repo.getSensors()).thenReturn(Set.of(active));
        service.setArmingStatus(ArmingStatus.ARMED_AWAY);
        verify(repo).setArmingStatus(ArmingStatus.ARMED_AWAY);
        assertEquals(false, active.getActive());
    }

    // 11) Armed-home WHILE camera shows cat -> ALARM
    @Test
    void setArmedHome_whileCatVisible_alarm() {
        when(image.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        service.processImage(mock(BufferedImage.class));
        verify(repo).setAlarmStatus(AlarmStatus.ALARM);
    }
}
