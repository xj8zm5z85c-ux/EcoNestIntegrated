package views;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import model.Task2Client;
import lk.econest.db.ClientRepository;

/** Task 2 UI - manages household and office client details. */
public class Task2ClientManagerApp extends JFrame {
    private JTextField txtId = new JTextField();
    private JTextField txtName = new JTextField();
    private JComboBox<String> cmbType = new JComboBox<String>(new String[]{"Household", "Office"});
    private JTextField txtPhone = new JTextField();
    private JTextField txtEmail = new JTextField();
    private JTextArea txtAddress = new JTextArea(3, 20);
    private ArrayList<Task2Client> clientList = new ArrayList<Task2Client>();
    private final ClientRepository repository;
    private DefaultTableModel tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Type", "Phone", "Email", "Address"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private JTable clientTable = new JTable(tableModel);

    public Task2ClientManagerApp() {
        this(new ClientRepository());
    }

    public Task2ClientManagerApp(ClientRepository repository) {
        setTitle("EcoNest Client Management");
        this.repository = repository;
        setSize(900, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildInterface();
        loadClients();
    }

    private void loadClients() {
        try {
            clientList = new ArrayList<>(repository.loadAll());
            refreshTable();
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Clients could not be loaded.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buildInterface() {
        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.setBorder(BorderFactory.createTitledBorder("Client Details"));
        form.add(new JLabel("Client ID")); form.add(txtId);
        form.add(new JLabel("Client Name")); form.add(txtName);
        form.add(new JLabel("Client Type")); form.add(cmbType);
        form.add(new JLabel("Phone")); form.add(txtPhone);
        form.add(new JLabel("Email")); form.add(txtEmail);
        form.add(new JLabel("Address")); form.add(new JScrollPane(txtAddress));

        JButton btnAdd = new JButton("Add");
        JButton btnUpdate = new JButton("Update");
        JButton btnDelete = new JButton("Delete");
        JButton btnClear = new JButton("Clear");
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnAdd); buttonPanel.add(btnUpdate); buttonPanel.add(btnDelete); buttonPanel.add(btnClear);

        btnAdd.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { addClient(); } });
        btnUpdate.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { updateClient(); } });
        btnDelete.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { deleteClient(); } });
        btnClear.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent e) { clearForm(); } });
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) { if (!e.getValueIsAdjusting()) showSelectedClient(); }
        });

        JPanel top = new JPanel(new BorderLayout()); top.add(form, BorderLayout.CENTER); top.add(buttonPanel, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(clientTable), BorderLayout.CENTER);
    }

    private boolean validInput() {
        if (txtId.getText().trim().equals("") || txtName.getText().trim().equals("") || txtPhone.getText().trim().equals("") || txtEmail.getText().trim().equals("") || txtAddress.getText().trim().equals("")) {
            JOptionPane.showMessageDialog(this, "Please complete every field."); return false;
        }
        if (txtEmail.getText().indexOf("@") == -1) {
            JOptionPane.showMessageDialog(this, "Enter an email address containing @."); return false;
        }
        return true;
    }

    private int findClient(String id) {
        for (int i = 0; i < clientList.size(); i++) {
            if (clientList.get(i).getClientId().equalsIgnoreCase(id)) return i;
        }
        return -1;
    }

    private Task2Client getFormClient() {
        return new Task2Client(txtId.getText().trim(), txtName.getText().trim(), (String) cmbType.getSelectedItem(), txtPhone.getText().trim(), txtEmail.getText().trim(), txtAddress.getText().trim());
    }

    private void addClient() {
        if (!validInput()) return;
        if (findClient(txtId.getText().trim()) != -1) { JOptionPane.showMessageDialog(this, "This client ID already exists."); return; }
        Task2Client client = getFormClient();
        try {
            repository.insert(client);
            clientList.add(client); refreshTable(); clearForm();
            JOptionPane.showMessageDialog(this, "Client added.");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Client could not be added.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateClient() {
        if (!validInput()) return;
        int index = findClient(txtId.getText().trim());
        if (index == -1) { JOptionPane.showMessageDialog(this, "Select a client from the table to update."); return; }
        Task2Client client = getFormClient();
        try {
            repository.update(client);
            clientList.set(index, client); refreshTable(); clearForm();
            JOptionPane.showMessageDialog(this, "Client updated.");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Client could not be updated.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteClient() {
        int row = clientTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a client from the table first."); return; }
        try {
            repository.delete(clientList.get(row).getClientId());
            clientList.remove(row); refreshTable(); clearForm();
            JOptionPane.showMessageDialog(this, "Client deleted.");
        } catch (IOException exception) {
            JOptionPane.showMessageDialog(this, "Client could not be deleted.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSelectedClient() {
        int row = clientTable.getSelectedRow();
        if (row == -1) return;
        Task2Client client = clientList.get(row);
        txtId.setText(client.getClientId()); txtId.setEditable(false);
        txtName.setText(client.getName()); cmbType.setSelectedItem(client.getClientType());
        txtPhone.setText(client.getPhone()); txtEmail.setText(client.getEmail()); txtAddress.setText(client.getAddress());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (int i = 0; i < clientList.size(); i++) {
            Task2Client client = clientList.get(i);
            tableModel.addRow(new Object[]{client.getClientId(), client.getName(), client.getClientType(), client.getPhone(), client.getEmail(), client.getAddress()});
        }
    }

    private void clearForm() {
        txtId.setText(""); txtId.setEditable(true); txtName.setText(""); cmbType.setSelectedIndex(0);
        txtPhone.setText(""); txtEmail.setText(""); txtAddress.setText(""); clientTable.clearSelection();
    }

    public static void main(String[] args) {
        Task2ClientManagerApp app = new Task2ClientManagerApp();
        app.setVisible(true);
    }
}
