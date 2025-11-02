package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Extra coverage tests to exercise the branches JaCoCo showed as missed.
 * These are additive; they do not replace your existing tests.
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class SecurityServiceCoverageTest {

    @Mock private SecurityRepository repo;
    @Mock private ImageService imageService;
    @Mock private StatusListener listener;

    private SecurityService service;

    @BeforeEach
    void setUp() {
        service = new SecurityService(repo, imageService);
        service.addStatusListener(listener);

        // Default: repo has no sensors unless a test overrides
        when(repo.getSensors()).thenReturn(new HashSet<>());
    }

    /**
     * setArmingStatus(): when already "cat visible" and we switch to ARMED_HOME,
     * alarm must go ALARM (Req. 11).
     */
    @Test
    void armedHome_whenCatAlreadyVisible_setsAlarm() {
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(true);
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(repo.getSensors()).thenReturn(Collections.emptySet());

        // Make the service think a cat is visible (via public API)
        service.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        // Now arm home -> should escalate to ALARM
        service.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(repo).setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(repo, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }

    /**
     * handleSensorActivated(): if alarm already ALARM, changes to sensors do not
     * affect the alarm (Req. 4). We drive the path via changeSensorActivationStatus.
     */
    @Test
    void whenAlarmAlreadyActive_andSensorActivated_alarmDoesNotChange() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        Sensor s = new Sensor("door-1", SensorType.DOOR);
        s.setActive(false);

        service.changeSensorActivationStatus(s, true);

        // Alarm must not be changed from ALARM
        verify(repo, never()).setAlarmStatus(ArgumentMatchers.any());
        // But the sensor update still gets persisted
        verify(repo).updateSensor(s);
    }

    /**
     * handleSensorDeactivated(): if alarm PENDING and (as seen by repo) all sensors
     * inactive, go back to NO_ALARM (Req. 3). We simulate "all inactive" by having
     * repo return an empty set.
     */
    @Test
    void pendingAlarm_andAllSensorsInactive_returnsToNoAlarm() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repo.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(repo.getSensors()).thenReturn(Collections.emptySet());

        Sensor s = new Sensor("win-1", SensorType.WINDOW);
        s.setActive(true);

        service.changeSensorActivationStatus(s, false);

        verify(repo).setAlarmStatus(AlarmStatus.NO_ALARM);
        verify(repo).updateSensor(s);
    }

    /**
     * catDetected(false): with system DISARMED and no active sensors,
     * service should set NO_ALARM (Req. 8).
     */
    @Test
    void catNotDetected_andSystemDisarmed_setsNoAlarmIfNoSensorsActive() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(repo.getSensors()).thenReturn(Collections.emptySet());
        when(imageService.imageContainsCat(any(BufferedImage.class), anyFloat())).thenReturn(false);

        // Start from a non-NO_ALARM to ensure we observe the transition
        service.setAlarmStatus(AlarmStatus.PENDING_ALARM);

        service.processImage(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB));

        verify(repo, atLeastOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);

        // Optional: read-back check if your repo impl reflects setAlarmStatus
        // (mock won't unless stubbed). This keeps it interaction-based.
    }

    /**
     * Bonus: setArmingStatus() should reset all sensors to inactive when arming.
     * This helps cover the loop branch.
     */
    @Test
    void arming_resetsAllSensorsToInactive() {
        when(repo.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);

        Sensor a = new Sensor("a", SensorType.DOOR); a.setActive(true);
        Sensor b = new Sensor("b", SensorType.WINDOW); b.setActive(false);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(a); sensors.add(b);
        when(repo.getSensors()).thenReturn(sensors);

        service.setArmingStatus(ArmingStatus.ARMED_AWAY);

        // Only the active one must be flipped & persisted
        assertEquals(false, a.getActive());
        verify(repo).updateSensor(a);
        verify(repo, never()).updateSensor(b);
    }
}
