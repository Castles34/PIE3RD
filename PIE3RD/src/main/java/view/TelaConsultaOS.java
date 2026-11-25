package view;

import dao.OrdemServicoDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.net.URI;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.OrdemServico;
import model.Usuario;

/**
 * Consulta e filtros de OS (somente Administrador).
 * Filtra por cliente (nome ou WhatsApp), status, equipamento e período de entrada.
 * O botão "Histórico do cliente" mostra todas as OS do cliente selecionado.
 * Sem SQL aqui: tudo passa pelo OrdemServicoDAO.
 */
public class TelaConsultaOS extends JFrame {

    private static final DateTimeFormatter FMT_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FMT_EXIBE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));
    private static final int COL_STATUS = 9;

    private final OrdemServicoDAO dao = new OrdemServicoDAO();
    private final Usuario usuario;
    private final Runnable aoAlterar; // atualiza o painel da tela principal
    private List<OrdemServico> resultado = new ArrayList<>();

    private JTextField txtCliente;
    private JComboBox<String> cmbStatus;
    private JComboBox<String> cmbTipo;
    private JTextField txtDe;
    private JTextField txtAte;
    private JTable tblOS;
    private DefaultTableModel modelo;
    private JLabel lblResumo;

    public TelaConsultaOS(Usuario usuario, Runnable aoAlterar) {
        this.usuario = usuario;
        this.aoAlterar = aoAlterar;
        initComponents();
        filtrar();
    }

    private void initComponents() {
        setTitle("Consultar / filtrar Ordens de Serviço");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        // --- Filtros ---
        txtCliente = new JTextField(22);
        String[] status = new String[OrdemServico.STATUS.length + 1];
        status[0] = "Todos";
        System.arraycopy(OrdemServico.STATUS, 0, status, 1, OrdemServico.STATUS.length);
        cmbStatus = new JComboBox<>(status);
        cmbTipo = new JComboBox<>(new String[]{"Todos", "PC", "Notebook"});
        txtDe = new JTextField(9);
        txtAte = new JTextField(9);

        JButton btnFiltrar = new JButton("Filtrar");
        JButton btnLimpar = new JButton("Limpar filtros");
        btnFiltrar.addActionListener(e -> filtrar());
        btnLimpar.addActionListener(e -> limpar());
        txtCliente.addActionListener(e -> filtrar());
        txtDe.addActionListener(e -> filtrar());
        txtAte.addActionListener(e -> filtrar());
        getRootPane().setDefaultButton(btnFiltrar);

        JPanel linha1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha1.add(new JLabel("Cliente (nome ou WhatsApp):"));
        linha1.add(txtCliente);
        linha1.add(new JLabel("Status:"));
        linha1.add(cmbStatus);
        linha1.add(new JLabel("Equipamento:"));
        linha1.add(cmbTipo);

        JPanel linha2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        linha2.add(new JLabel("Entrada de (dd/mm/aaaa):"));
        linha2.add(txtDe);
        linha2.add(new JLabel("até:"));
        linha2.add(txtAte);
        linha2.add(btnFiltrar);
        linha2.add(btnLimpar);

        JPanel filtros = new JPanel(new GridLayout(2, 1));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));
        filtros.add(linha1);
        filtros.add(linha2);
        add(filtros, BorderLayout.NORTH);

        // --- Tabela ---
        modelo = new DefaultTableModel(new Object[]{
            "Nº", "Cliente", "WhatsApp", "Equipamento", "Problema",
            "Entrada", "Finalizado", "Retirado", "Valor", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblOS = new JTable(modelo);
        tblOS.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblOS.setRowHeight(24);
        tblOS.getColumnModel().getColumn(0).setMaxWidth(50);
        tblOS.getColumnModel().getColumn(4).setPreferredWidth(230);
        tblOS.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());
        tblOS.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    alterar();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(tblOS);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado (duplo clique abre a OS)"));
        add(scroll, BorderLayout.CENTER);

        // --- Rodapé ---
        lblResumo = new JLabel(" ");
        JButton btnAlterar = new JButton("Alterar OS");
        JButton btnExcluir = new JButton("Excluir OS");
        JButton btnHistorico = new JButton("Histórico do cliente");
        JButton btnWhats = new JButton("Abrir WhatsApp");
        JButton btnFechar = new JButton("Fechar");
        btnAlterar.addActionListener(e -> alterar());
        btnExcluir.addActionListener(e -> excluir());
        btnHistorico.addActionListener(e -> historico());
        btnWhats.addActionListener(e -> abrirWhatsapp());
        btnFechar.addActionListener(e -> dispose());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(btnAlterar);
        botoes.add(btnExcluir);
        botoes.add(btnHistorico);
        botoes.add(btnWhats);
        botoes.add(btnFechar);

        JPanel rodape = new JPanel(new BorderLayout());
        rodape.setBorder(BorderFactory.createEmptyBorder(0, 8, 4, 8));
        rodape.add(lblResumo, BorderLayout.WEST);
        rodape.add(botoes, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);
    }

    // ------------------------------------------------------------------
    // Ações
    // ------------------------------------------------------------------
    private void filtrar() {
        LocalDate de = null;
        LocalDate ate = null;
        try {
            if (!txtDe.getText().trim().isEmpty()) {
                de = LocalDate.parse(txtDe.getText().trim(), FMT_DATA);
            }
        } catch (DateTimeParseException ex) {
            aviso("Data inicial inválida. Use o formato dd/mm/aaaa.", txtDe);
            return;
        }
        try {
            if (!txtAte.getText().trim().isEmpty()) {
                ate = LocalDate.parse(txtAte.getText().trim(), FMT_DATA);
            }
        } catch (DateTimeParseException ex) {
            aviso("Data final inválida. Use o formato dd/mm/aaaa.", txtAte);
            return;
        }
        if (de != null && ate != null && de.isAfter(ate)) {
            aviso("A data inicial não pode ser depois da data final.", txtDe);
            return;
        }

        String status = cmbStatus.getSelectedIndex() == 0 ? null : (String) cmbStatus.getSelectedItem();
        String tipo = null;
        if (cmbTipo.getSelectedIndex() == 1) {
            tipo = OrdemServico.COMPUTADOR;
        } else if (cmbTipo.getSelectedIndex() == 2) {
            tipo = OrdemServico.NOTEBOOK;
        }

        // Só dígitos e símbolos de telefone: busca pelo número gravado (sem máscara)
        String termo = txtCliente.getText().trim();
        if (termo.matches("[\\d\\s()\\-]+")) {
            termo = termo.replaceAll("\\D", "");
        }

        try {
            resultado = dao.filtrar(status, tipo, de, ate, termo);
        } catch (SQLException ex) {
            erro("Erro de conexão com o banco de dados:\n" + ex.getMessage());
            return;
        }
        preencherTabela();
    }

    private void preencherTabela() {
        modelo.setRowCount(0);
        BigDecimal total = BigDecimal.ZERO;
        for (OrdemServico o : resultado) {
            modelo.addRow(new Object[]{
                o.getId(),
                o.getCliente().getNome(),
                formatarTelefone(o.getCliente().getWhatsapp()),
                OrdemServico.NOTEBOOK.equals(o.getTipoEquipamento()) ? "Notebook" : "PC",
                o.getProblemaRelatado(),
                o.getDataEntrada().format(FMT_EXIBE),
                o.getDataConclusao() == null ? "" : o.getDataConclusao().format(FMT_EXIBE),
                o.getDataRetirada() == null ? "" : o.getDataRetirada().format(FMT_EXIBE),
                o.getValor() == null ? "" : MOEDA.format(o.getValor()),
                o.getStatus()
            });
            if (o.getValor() != null) {
                total = total.add(o.getValor());
            }
        }
        lblResumo.setText(resultado.size() + " OS encontrada(s)   |   Total dos valores: " + MOEDA.format(total));
    }

    private void limpar() {
        txtCliente.setText("");
        cmbStatus.setSelectedIndex(0);
        cmbTipo.setSelectedIndex(0);
        txtDe.setText("");
        txtAte.setText("");
        filtrar();
    }

    /** Mostra todas as OS do cliente da linha selecionada, de qualquer status e data. */
    private void historico() {
        OrdemServico o = selecionada();
        if (o == null) {
            return;
        }
        cmbStatus.setSelectedIndex(0);
        cmbTipo.setSelectedIndex(0);
        txtDe.setText("");
        txtAte.setText("");
        txtCliente.setText(o.getCliente().getWhatsapp());
        filtrar();
    }

    private void alterar() {
        OrdemServico o = selecionada();
        if (o == null) {
            return;
        }
        TelaOrdemServico.abrir(this, usuario, o, this::atualizarTudo);
    }

    private void excluir() {
        OrdemServico o = selecionada();
        if (o == null) {
            return;
        }
        int r = JOptionPane.showConfirmDialog(this,
                "Excluir a OS nº " + o.getId() + " de " + o.getCliente().getNome()
                + "?\nEsta ação não pode ser desfeita.",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.excluir(o.getId());
            atualizarTudo();
        } catch (SQLException ex) {
            erro("Erro ao excluir a OS:\n" + ex.getMessage());
        }
    }

    private void abrirWhatsapp() {
        OrdemServico o = selecionada();
        if (o == null) {
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(o.getCliente().getLinkWhatsapp()));
        } catch (Exception ex) {
            erro("Não foi possível abrir o WhatsApp:\n" + ex.getMessage());
        }
    }

    private void atualizarTudo() {
        filtrar();
        if (aoAlterar != null) {
            aoAlterar.run();
        }
    }

    private OrdemServico selecionada() {
        int linha = tblOS.getSelectedRow();
        if (linha < 0 || linha >= resultado.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma OS na lista.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return resultado.get(linha);
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    /** Cores por status: vermelho = aberta, amarelo = em andamento, laranja = aguardando peça, verde = pronta. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean foco, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
            Color cor;
            switch (String.valueOf(v)) {
                case "Aberta":          cor = new Color(255, 205, 205); break;
                case "Em andamento":    cor = new Color(255, 243, 176); break;
                case "Aguardando peça": cor = new Color(255, 224, 178); break;
                default:                cor = new Color(200, 235, 200); break;
            }
            c.setBackground(cor);
            c.setForeground(Color.BLACK);
            return c;
        }
    }

    private String formatarTelefone(String d) {
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

    private void aviso(String msg, Component campo) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
        campo.requestFocus();
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
