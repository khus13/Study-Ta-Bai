package ph.studytabai.ui;

import ph.studytabai.AppContext;
import ph.studytabai.model.Standard;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/** Teacher-entered academic standards. Tag questions with the codes, and reports show mastery per code. */
public final class StandardsPanel extends JPanel {
    private final AppContext ctx;
    private final JPanel tableHolder = new JPanel(new BorderLayout());
    private final JTextField framework = new JTextField(14), code = new JTextField(12), desc = new JTextField(30);
    private JTable table;

    public StandardsPanel(AppContext ctx) {
        super(new BorderLayout(8, 8));
        this.ctx = ctx;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(Ui.heading("Academic Standards"), BorderLayout.NORTH);
        add(tableHolder, BorderLayout.CENTER);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT));
        form.add(Ui.labeled("Framework", framework));
        form.add(Ui.labeled("Code", code));
        form.add(Ui.labeled("Description", desc));
        form.add(Ui.button("Add", this::add));
        form.add(Ui.button("Delete Selected", this::delete));
        add(form, BorderLayout.SOUTH);
        reload();
    }

    private void reload() {
        List<List<String>> rows = new ArrayList<>();
        for (Standard s : ctx.standards.findAll()) rows.add(List.of(s.getFramework(), s.getCode(), s.getDescription()));
        table = Ui.table(List.of("Framework", "Code", "Description"), rows);
        tableHolder.removeAll();
        tableHolder.add(Ui.scroll(table));
        tableHolder.revalidate();
        tableHolder.repaint();
    }

    private void add() {
        try {
            ctx.standards.save(new Standard(framework.getText(), code.getText(), desc.getText()));
            code.setText(""); desc.setText("");
            reload();
        } catch (IllegalArgumentException e) {
            Ui.error(this, e.getMessage());
        }
    }

    private void delete() {
        int r = table.getSelectedRow();
        if (r < 0) return;
        int mr = table.convertRowIndexToModel(r);
        ctx.standards.delete(table.getModel().getValueAt(mr, 0) + ":" + table.getModel().getValueAt(mr, 1));
        reload();
    }
}
