package com.gescout.companion;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

final class GeScoutPanel extends PluginPanel
{
    private final JLabel status = new JLabel("Not paired");
    private final JTextArea plans = new JTextArea(
        "Active GE Scout plans appear here after pairing.");

    GeScoutPanel()
    {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        status.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        plans.setEditable(false);
        plans.setLineWrap(true);
        plans.setWrapStyleWord(true);
        plans.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        plans.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        plans.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(ColorScheme.DARK_GRAY_COLOR);
        heading.add(new JLabel("GE Scout Companion"), BorderLayout.NORTH);
        heading.add(status, BorderLayout.SOUTH);
        add(heading, BorderLayout.NORTH);
        add(new JScrollPane(plans), BorderLayout.CENTER);
    }

    void setStatus(String value)
    {
        SwingUtilities.invokeLater(() -> status.setText(value));
    }

    void setPlans(String value)
    {
        SwingUtilities.invokeLater(() -> plans.setText(value));
    }
}
