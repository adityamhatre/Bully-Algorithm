import javax.swing.*;

/**
 * Created by Aditya on July 20, 2017.
 * Student ID: 1001429814
 */

public class MainGUI {
    private JComboBox comboBox1;
    private JButton changeStatusButton;
    private JTextArea logs;
    private JPanel rootPanel;

    public JButton getStartElectionButton() {
        return startElectionButton;
    }

    private JButton startElectionButton;

    public JComboBox getComboBox1() {
        return comboBox1;
    }

    public JButton getChangeStatusButton() {
        return changeStatusButton;
    }

    public JTextArea getLogs() {
        return logs;
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
