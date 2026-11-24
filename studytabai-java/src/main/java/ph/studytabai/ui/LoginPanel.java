package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Teacher;
import ph.studytabai.service.AuthException;

import javax.swing.*;
import java.awt.*;

public final class LoginPanel extends ProvinceScene {
    public LoginPanel(MainFrame frame, AppContext ctx) {
        super(false);
        setLayout(new GridBagLayout());
        WoodCard form = new WoodCard(new GridLayout(0, 1, 6, 6));
        form.setPreferredSize(new Dimension(520, 440));
        JTextField name = new JTextField(24);
        JTextField email = new JTextField(24);
        JPasswordField pw = new JPasswordField(24);
        JLabel msg = new JLabel(" ");
        msg.getAccessibleContext().setAccessibleName("Status message");

        JPanel nameRow = Ui.labeled("Display name (for new accounts)", name);
        form.add(Ui.heading("Teacher login"));
        form.add(Ui.labeled("Email", email));
        form.add(Ui.labeled("Password (at least 8 characters)", pw));
        form.add(nameRow);

        Runnable login = () -> {
            try {
                Teacher t = ctx.auth.login(email.getText(), new String(pw.getPassword()));
                pw.setText("");
                msg.setText(" ");
                frame.showTeacher(t);
            } catch (AuthException ex) {
                msg.setText(ex.getMessage());
                msg.setForeground(Theme.feedbackColor(false));
            }
        };
        Runnable register = () -> {
            try {
                Teacher t = ctx.auth.register(name.getText(), email.getText(), new String(pw.getPassword()));
                pw.setText("");
                msg.setText(" ");
                frame.showTeacher(t);
            } catch (AuthException ex) {
                msg.setText(ex.getMessage());
                msg.setForeground(Theme.feedbackColor(false));
            }
        };
        JButton loginBtn = Ui.button("Log in", login);
        JButton regBtn = Ui.button("Create account", register);
        JButton back = Ui.button("Back", () -> frame.showRole());
        pw.addActionListener(e -> login.run());
        form.add(msg);
        form.add(Ui.flow(loginBtn, regBtn, back));
        add(form);
    }
}
