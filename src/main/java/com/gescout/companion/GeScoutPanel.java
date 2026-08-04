package com.gescout.companion;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.Map;
import java.util.TreeMap;
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
    private final JTextArea observations = new JTextArea(
        "No GE slot observations yet.");
    private final JTextArea plans = new JTextArea(
        "Active GE Scout plans appear here after pairing.");
    private final Map<Integer, String> slotObservations = new TreeMap<>();

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
        observations.setEditable(false);
        observations.setLineWrap(false);
        observations.setRows(8);
        observations.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        observations.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        observations.setBorder(
            BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(ColorScheme.DARK_GRAY_COLOR);
        heading.add(new JLabel("GE Scout Companion"), BorderLayout.NORTH);
        heading.add(status, BorderLayout.SOUTH);
        JPanel content = new JPanel(new GridLayout(2, 1, 0, 8));
        content.setBackground(ColorScheme.DARK_GRAY_COLOR);
        content.add(new JScrollPane(observations));
        content.add(new JScrollPane(plans));
        add(heading, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
    }

    void setStatus(String value)
    {
        SwingUtilities.invokeLater(() -> status.setText(value));
    }

    void setPlans(String value)
    {
        SwingUtilities.invokeLater(() -> plans.setText(value));
    }

    void setObservation(
        int slot,
        String state,
        String itemName,
        int filledQuantity,
        int totalQuantity,
        int offerPrice,
        String eventType)
    {
        SwingUtilities.invokeLater(() ->
        {
            String item = itemName == null ? "Empty" : itemName;
            String source =
                "offer_changed".equals(eventType) ? "event" : "snapshot";
            slotObservations.put(
                slot,
                String.format(
                    "%d  %s — %s %,d/%,d @ %,d (%s)",
                    slot + 1,
                    item,
                    state,
                    filledQuantity,
                    totalQuantity,
                    offerPrice,
                    source));
            observations.setText(
                String.join("\n", slotObservations.values()));
        });
    }
}
