package com.github.glassmc.installer;

import com.formdev.flatlaf.FlatLightLaf;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class InstallerFrame extends JFrame {

    public static void main(String[] args) {
        FlatLightLaf.setup();
        new InstallerFrame().start();
    }

    private ButtonGroup environmentGroup;
    private JComboBox<String> versionComboBox;
    private JTextField pathField;

    public void start() {
        this.setSize(300, 150);
        this.setTitle("Glass Installer");
        this.setLayout(new BorderLayout());

        JPanel top = new JPanel();

        JLabel glassmc = new JLabel();
        glassmc.setText("Glass Installer");
        glassmc.setFont(glassmc.getFont().deriveFont(25f));
        top.add(glassmc);

        this.add(top, BorderLayout.NORTH);

        JPanel environmentPanel = new JPanel();
        this.environmentGroup = new ButtonGroup();
        JRadioButton client = new JRadioButton();
        client.setActionCommand("Client");
        client.setText("Client");
        client.setSelected(true);
        client.addActionListener(this::onSelectEnvironment);
        environmentGroup.add(client);
        environmentPanel.add(client);

        JRadioButton server = new JRadioButton();
        server.setActionCommand("Server");
        server.setText("Server");
        server.addActionListener(this::onSelectEnvironment);
        environmentGroup.add(server);
        environmentPanel.add(server);

        this.versionComboBox = new JComboBox<>();
        versionComboBox.addItem("1.7.10");
        versionComboBox.addItem("1.8.9");
        versionComboBox.addItem("1.12.2");
        versionComboBox.addItem("1.17.1");

        environmentPanel.add(versionComboBox);

        this.add(environmentPanel, BorderLayout.CENTER);

        JPanel test = new JPanel();
        test.setLayout(new FlowLayout());
        this.pathField = new JTextField();
        this.pathField.setPreferredSize(new Dimension(200, 20));
        test.add(this.pathField);

        JButton installButton = new JButton();
        installButton.setText("Install");
        installButton.addActionListener(this::install);
        test.add(installButton);

        this.add(test, BorderLayout.SOUTH);

        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        this.onSelectEnvironment(new ActionEvent(new Object(), 0, "Client"));
    }

    public void onSelectEnvironment(ActionEvent event) {
        String path = this.getDefaultPath(event.getActionCommand());
        this.pathField.setText(path);
    }

    private String getDefaultPath(String environment) {
        if(environment.equals("Client")) {
            String os = System.getProperty("os.name").toLowerCase();
            if(os.contains("win")) {
                return System.getProperty("APPDATA") + "/.minecraft";
            } else if(os.contains("nux")) {
                return System.getProperty("user.home") + "/.minecraft";
            }
        }
        return null;
    }

    public void install(ActionEvent event) {
        new Thread(() -> {
            String environment = this.environmentGroup.getSelection().getActionCommand();
            String version = (String) this.versionComboBox.getSelectedItem();

            if(environment.equals("Client")) {
                try {
                    URL url = new URL("https://api.github.com/repos/glassmc/loader/tags");
                    InputStream inputStream = url.openStream();
                    byte[] bytes = new byte[inputStream.available()];
                    inputStream.read(bytes);
                    String string = new String(bytes);
                    String latestTag = string.substring(string.indexOf("\"name\"") + 8, string.indexOf("\"zipball_url\"") - 2);
                    String latestTagSimple = latestTag.substring(1);

                    File minecraftHome = new File(this.pathField.getText());
                    File latestLoaderFile = new File(minecraftHome, "libraries/com/github/glassmc/loader-client/" + latestTagSimple + "/loader-client-" + latestTagSimple + ".jar");
                    URL latestLoaderClient = new URL("https://github.com/glassmc/loader/releases/download/" + latestTag + "/loader-client-" + latestTagSimple + ".jar");
                    InputStream latestLoaderStream = latestLoaderClient.openStream();
                    FileOutputStream fileOutputStream = new FileOutputStream(latestLoaderFile);

                    IOUtils.copy(latestLoaderStream, fileOutputStream);

                    String versionName = "Glass-" + version;
                    File versionFolder = new File(minecraftHome, "versions/" + versionName);
                    versionFolder.mkdirs();
                    File versionJson = new File(versionFolder, versionName + ".json");

                    String json = IOUtils.toString(InstallerFrame.class.getClassLoader().getResourceAsStream("jsonTemplate.json"), StandardCharsets.UTF_8);
                    json = json.replace("%ID%", versionName).replace("%VERSION%", version).replace("%LOADER_VERSION%", latestTagSimple).replace("%MAIN_CLASS%", "com.github.glassmc.loader.client.GlassClientMain");

                    FileWriter fileWriter = new FileWriter(versionJson);
                    fileWriter.write(json);
                    fileWriter.close();

                    JOptionPane.showMessageDialog(null, "Successfully installed Glass. You can now close this installer.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
