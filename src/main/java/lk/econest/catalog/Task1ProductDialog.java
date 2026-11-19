package lk.econest.catalog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Add/edit form with validation for a catalogue item. */
public final class Task1ProductDialog extends JDialog {
    private final JTextField skuField = new JTextField(20);
    private final JTextField nameField = new JTextField(20);
    private final JComboBox<Task1ProductType> typeField = new JComboBox<>(Task1ProductType.values());
    private final JTextField priceField = new JTextField(20);
    private final JComboBox<Integer> ratingField = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private Task1Product result;

    public Task1ProductDialog(java.awt.Window owner, Task1Product existing) {
        super(owner, existing == null ? "Add product" : "Edit product", ModalityType.APPLICATION_MODAL);
        JPanel fields = new JPanel(new GridLayout(0, 2, 10, 10));
        fields.setBorder(BorderFactory.createEmptyBorder(18, 18, 12, 18));
        fields.add(new JLabel("SKU")); fields.add(skuField);
        fields.add(new JLabel("Product name")); fields.add(nameField);
        fields.add(new JLabel("Category")); fields.add(typeField);
        fields.add(new JLabel("Price (LKR)")); fields.add(priceField);
        fields.add(new JLabel("Eco-rating (1–5)")); fields.add(ratingField);

        javax.swing.JButton save = new javax.swing.JButton("Save product");
        javax.swing.JButton cancel = new javax.swing.JButton("Cancel");
        JPanel actions = new JPanel();
        actions.add(cancel);
        actions.add(save);
        add(fields, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
        save.addActionListener(event -> saveProduct());
        cancel.addActionListener(event -> dispose());
        getRootPane().setDefaultButton(save);

        if (existing != null) {
            skuField.setText(existing.getSku());
            skuField.setEditable(false);
            nameField.setText(existing.getName());
            typeField.setSelectedItem(existing.getType());
            priceField.setText(existing.getPrice().toPlainString());
            ratingField.setSelectedItem(existing.getEcoRating());
        }
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void saveProduct() {
        try {
            BigDecimal price = new BigDecimal(priceField.getText().trim());
            result = new Task1Product(skuField.getText(), nameField.getText(),
                    (Task1ProductType) typeField.getSelectedItem(), price,
                    (Integer) ratingField.getSelectedItem());
            dispose();
        } catch (NumberFormatException ex) {
            showValidationError("Enter a valid price in LKR, such as 1250.00.");
        } catch (IllegalArgumentException ex) {
            showValidationError(ex.getMessage());
        }
    }

    private void showValidationError(String message) {
        JOptionPane.showMessageDialog(this, message, "Check product details", JOptionPane.WARNING_MESSAGE);
    }

    public Task1Product getResult() { return result; }
}
