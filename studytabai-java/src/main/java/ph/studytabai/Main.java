package ph.studytabai;

import ph.studytabai.storage.StorageException;
import ph.studytabai.ui.MainFrame;
import ph.studytabai.ui.Theme;

import javax.swing.*;
import java.nio.file.Path;

/** Study Ta Bai desktop app. Data is saved as serialized files in ./data (override with -Dstb.data=path). */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            try {
                AppContext ctx = new AppContext(Path.of(System.getProperty("stb.data", "data")));
                new MainFrame(ctx).setVisible(true);
            } catch (StorageException e) {
                JOptionPane.showMessageDialog(null, "Could not open the data folder:\n" + e.getMessage(), "Study Ta Bai", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
