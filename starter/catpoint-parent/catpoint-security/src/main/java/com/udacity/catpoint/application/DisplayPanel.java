package com.udacity.catpoint.application;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.service.SecurityService;
import com.udacity.catpoint.service.StyleService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * Displays the current status of the system. Implements the StatusListener
 * interface so that it can be notified whenever the status changes.
 */
public class DisplayPanel extends JPanel implements StatusListener {

    private JLabel currentStatusLabel;
    private SecurityService securityService;

    public DisplayPanel(SecurityService securityService) {
        super();
        setLayout(new MigLayout());

        this.securityService = securityService;
        securityService.addStatusListener(this);

        JLabel panelLabel = new JLabel("Very Secure Home Security");
        JLabel systemStatusLabel = new JLabel("System Status:");
        currentStatusLabel = new JLabel();

        panelLabel.setFont(StyleService.HEADING_FONT);

        // initialize UI based on current alarm status
        notify(securityService.getAlarmStatus());

        add(panelLabel, "span 2, wrap");
        add(systemStatusLabel);
        add(currentStatusLabel, "wrap");
    }

    @Override
    public void notify(AlarmStatus status) {
        currentStatusLabel.setText(status.getDescription());
        currentStatusLabel.setBackground(status.getColor());
        currentStatusLabel.setOpaque(true);
        repaint();
    }

    @Override
    public void catDetected(boolean catDetected) {
        // no special UI here
    }

    @Override
    public void sensorStatusChanged() {
        // refresh the displayed alarm status when sensors change
        notify(securityService.getAlarmStatus());
    }
}
