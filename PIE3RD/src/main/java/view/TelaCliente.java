package view;

import dao.ClienteDAO;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import model.Cliente;

/**
 * Cadastro e busca de clientes.
 *  - Administrador: cadastra, edita, exclui e busca.
 *  - Caixa: cadastra novos clientes e busca (não edita nem exclui).
 * Sem SQL aqui: tudo passa pelo ClienteDAO.
 */
public class TelaCliente extends JFrame {

    private final ClienteDAO dao = new ClienteDAO();
    private final boolean administrador;
    private List<Cliente> lista = new ArrayList<>();

    private JTextField txtBusca;
    private JTextField txtCodigo;
    private JTextField txtNome;
    private JTextField txtWhatsapp;
    private JTable tblClientes;
    private DefaultTableModel modeloTabela;
    private JButton btnNovo;
    private JButton btnSalvar;
    private JButton btnExcluir;
    private JButton btnWhatsapp;
    private JButton btnFechar;

    public TelaCliente(boolean administrador) {
        this.administrador = administrador;
        initComponents();
        novo();
        carregarTabela();
    }

    private void initComponents() {
        setTitle("Clientes");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(720, 560);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // --- Formulário ---
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createTitledBorder("Dados do cliente"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtCodigo = new JTextField(6);
        txtCodigo.setEditable(false);
        txtNome = new JTextField(30);
        txtWhatsapp = new JTextField(15);

        linha(pnlForm, g, 0, "Código:", txtCodigo);
        linha(pnlForm, g, 1, "Nome:", txtNome);
        linha(pnlForm, g, 2, "WhatsApp (com DDD):", txtWhatsapp);

        btnNovo = new JButton("Novo");
        btnSalvar = new JButton("Salvar");
        btnExcluir = new JButton("Excluir");
        btnWhatsapp = new JButton("Abrir WhatsApp");
        btnFechar = new JButton("Fechar");
        btnNovo.addActionListener(e -> novo());
        btnSalvar.addActionListener(e -> salvar());
        btnExcluir.addActionListener(e -> excluir());
        btnWhatsapp.addActionListener(e -> abrirWhatsapp());
        btnFechar.addActionListener(e -> dispose());

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBotoes.add(btnNovo);
        pnlBotoes.add(btnSalvar);
        if (administrador) {
            pnlBotoes.add(btnExcluir); // só o Administrador exclui
        }
        pnlBotoes.add(btnWhatsapp);
        pnlBotoes.add(btnFechar);

        JPanel pnlTopo = new JPanel(new BorderLayout());
        pnlTopo.add(pnlForm, BorderLayout.CENTER);
        pnlTopo.add(pnlBotoes, BorderLayout.SOUTH);
        add(pnlTopo, BorderLayout.NORTH);

        // --- Busca + tabela ---
        txtBusca = new JTextField(25);
        txtBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { carregarTabela(); }
            public void removeUpdate(DocumentEvent e) { carregarTabela(); }
            public void changedUpdate(DocumentEvent e) { carregarTabela(); }
        });
        JPanel pnlBusca = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlBusca.add(new JLabel("Buscar por nome ou WhatsApp:"));
        pnlBusca.add(txtBusca);

        modeloTabela = new DefaultTableModel(new Object[]{"Código", "Nome", "WhatsApp"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblClientes = new JTable(modeloTabela);
        tblClientes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                aoSelecionar();
            }
        });

        JPanel pnlCentro = new JPanel(new BorderLayout());
        pnlCentro.setBorder(BorderFactory.createTitledBorder("Clientes cadastrados"));
        pnlCentro.add(pnlBusca, BorderLayout.NORTH);
        pnlCentro.add(new JScrollPane(tblClientes), BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
    }

    private void linha(JPanel p, GridBagConstraints g, int y, String rotulo, java.awt.Component campo) {
        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(rotulo), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(campo, g);
    }

    // ------------------------------------------------------------------
    // Ações
    // ------------------------------------------------------------------
    private void novo() {
        txtCodigo.setText("0");
        txtNome.setText("");
        txtWhatsapp.setText("");
        tblClientes.clearSelection();
        btnExcluir.setEnabled(false);
        btnWhatsapp.setEnabled(false);
        btnSalvar.setEnabled(true);
        txtNome.requestFocus();
    }

    /** Código 0 = inserir; senão, alterar (só o Administrador chega a alterar). */
    private void salvar() {
        String nome = txtNome.getText().trim();
        String digitos = txtWhatsapp.getText().replaceAll("\\D", "");

        if (nome.isEmpty()) {
            aviso("Informe o nome do cliente.");
            txtNome.requestFocus();
            return;
        }
        if (digitos.isEmpty()) {
            aviso("Informe o WhatsApp do cliente.");
            txtWhatsapp.requestFocus();
            return;
        }
        if (digitos.length() < 10 || digitos.length() > 11) {
            aviso("WhatsApp inválido. Digite o DDD e o número (10 ou 11 dígitos).");
            txtWhatsapp.requestFocus();
            return;
        }

        Cliente c = new Cliente();
        c.setId(Integer.parseInt(txtCodigo.getText()));
        c.setNome(nome);
        c.setWhatsapp(digitos);

        try {
            if (c.getId() == 0) {
                dao.inserir(c);
            } else {
                dao.alterar(c);
            }
            JOptionPane.showMessageDialog(this, "Registro salvo com sucesso!");
            novo();
            carregarTabela();
        } catch (SQLException ex) {
            erro("Erro ao salvar no banco de dados:\n" + ex.getMessage());
        }
    }

    private void excluir() {
        int id = Integer.parseInt(txtCodigo.getText());
        if (id == 0) {
            aviso("Selecione um cliente na tabela para excluir.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "Excluir o cliente \"" + txtNome.getText() + "\"?\nEsta ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.excluir(id);
            novo();
            carregarTabela();
        } catch (SQLException ex) {
            String m = String.valueOf(ex.getMessage()).toUpperCase();
            if (m.contains("FOREIGN KEY")) {
                aviso("Este cliente possui ordens de serviço e não pode ser excluído.");
            } else {
                erro("Erro ao excluir:\n" + ex.getMessage());
            }
        }
    }

    private void abrirWhatsapp() {
        int linha = tblClientes.getSelectedRow();
        if (linha < 0 || linha >= lista.size()) {
            aviso("Selecione um cliente na tabela.");
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(lista.get(linha).getLinkWhatsapp()));
        } catch (Exception ex) {
            erro("Não foi possível abrir o WhatsApp:\n" + ex.getMessage());
        }
    }

    private void carregarTabela() {
        try {
            lista = dao.buscar(txtBusca == null ? "" : txtBusca.getText());
        } catch (SQLException ex) {
            erro("Erro de conexão com o banco de dados:\n" + ex.getMessage());
            return;
        }
        modeloTabela.setRowCount(0);
        for (Cliente c : lista) {
            modeloTabela.addRow(new Object[]{c.getId(), c.getNome(), formatar(c.getWhatsapp())});
        }
    }

    /** Clique na tabela: Administrador carrega para editar; Caixa só consulta. */
    private void aoSelecionar() {
        int linha = tblClientes.getSelectedRow();
        if (linha < 0 || linha >= lista.size()) {
            return;
        }
        btnWhatsapp.setEnabled(true);
        if (administrador) {
            Cliente c = lista.get(linha);
            txtCodigo.setText(String.valueOf(c.getId()));
            txtNome.setText(c.getNome());
            txtWhatsapp.setText(formatar(c.getWhatsapp()));
            btnExcluir.setEnabled(true);
        }
    }

    /** 45999998888 -> (45) 99999-8888 */
    private String formatar(String d) {
        if (d == null) {
            return "";
        }
        if (d.length() == 11) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 7) + "-" + d.substring(7);
        }
        if (d.length() == 10) {
            return "(" + d.substring(0, 2) + ") " + d.substring(2, 6) + "-" + d.substring(6);
        }
        return d;
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
