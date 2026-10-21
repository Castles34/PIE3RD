package view;

import dao.OrdemServicoDAO;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.OrdemServico;
import model.Usuario;

/**
 * Tela principal. O conteúdo muda conforme o perfil:
 *  - Caixa: uma única ação, o cadastro (cliente + equipamento + problema + serviços).
 *  - Administrador: cadastro, clientes, painel de OS em aberto e alteração das OS.
 * A OS nasce no cadastro; a área de Ordens de Serviço só altera o que já existe.
 */
public class TelaPrincipal extends JFrame {

    private static final int COL_STATUS = 7;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"));

    private final Usuario usuario;
    private final OrdemServicoDAO osDao = new OrdemServicoDAO();

    private List<OrdemServico> emAberto = new ArrayList<>();
    private DefaultTableModel modeloPainel;
    private JTable tblPainel;
    private boolean carregando = false;

    public TelaPrincipal(Usuario usuario) {
        this.usuario = usuario;
        initComponents();
        if (usuario.isAdministrador()) {
            carregarPainel();
        }
    }

    private void initComponents() {
        setTitle("Controle de Ordens de Serviço - " + usuario.getNome()
                + " (" + usuario.getPerfil() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JMenuBar barra = new JMenuBar();

        JMenu mnCadastros = new JMenu("Cadastros");
        JMenuItem miCadastro = new JMenuItem("Novo cadastro (cliente + OS)");
        miCadastro.addActionListener(e -> abrirCadastro());
        mnCadastros.add(miCadastro);
        if (usuario.isAdministrador()) {
            JMenuItem miClientes = new JMenuItem("Clientes");
            miClientes.addActionListener(e -> abrirClientes());
            mnCadastros.add(miClientes);
        }
        barra.add(mnCadastros);

        // Só o Administrador enxerga as Ordens de Serviço e o backup
        if (usuario.isAdministrador()) {
            JMenu mnOS = new JMenu("Ordens de Serviço");
            JMenuItem miLista = new JMenuItem("Consultar / filtrar OS");
            miLista.addActionListener(e -> abrirListaOS());
            mnOS.add(miLista);
            barra.add(mnOS);
        }

        JMenu mnSistema = new JMenu("Sistema");
        if (usuario.isAdministrador()) {
            JMenuItem miBackup = new JMenuItem("Backup do banco");
            miBackup.addActionListener(e -> emConstrucao("Backup"));
            mnSistema.add(miBackup);
        }
        JMenuItem miSair = new JMenuItem("Sair (trocar usuário)");
        miSair.addActionListener(e -> sair());
        mnSistema.add(miSair);
        barra.add(mnSistema);

        setJMenuBar(barra);

        if (usuario.isAdministrador()) {
            add(montarPainelOS(), BorderLayout.CENTER);
        } else {
            add(montarTelaCaixa(), BorderLayout.CENTER);
        }
    }

    /** Caixa: uma única ação, o cadastro. */
    private JPanel montarTelaCaixa() {
        JButton btn = new JButton("Novo cadastro (cliente + ordem de serviço)");
        btn.setFont(btn.getFont().deriveFont(Font.BOLD, 18f));
        btn.addActionListener(e -> abrirCadastro());
        JPanel centro = new JPanel(new GridBagLayout());
        centro.add(btn);
        return centro;
    }

    // ------------------------------------------------------------------
    // Painel de OS em aberto (RF16) - só Administrador
    // ------------------------------------------------------------------
    private JPanel montarPainelOS() {
        modeloPainel = new DefaultTableModel(new Object[]{
            "Nº", "Cliente", "WhatsApp", "Equipamento", "Problema", "Entrada", "Valor", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == COL_STATUS; // status editável direto na lista
            }
        };
        tblPainel = new JTable(modeloPainel);
        tblPainel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPainel.setRowHeight(24);
        tblPainel.getColumnModel().getColumn(COL_STATUS)
                .setCellEditor(new DefaultCellEditor(new JComboBox<>(OrdemServico.STATUS)));
        tblPainel.getColumnModel().getColumn(COL_STATUS).setCellRenderer(new StatusRenderer());
        tblPainel.getColumnModel().getColumn(0).setMaxWidth(50);
        tblPainel.getColumnModel().getColumn(4).setPreferredWidth(220);

        // Mudou o status no menu suspenso: grava na hora
        modeloPainel.addTableModelListener(e -> {
            if (!carregando && e.getType() == TableModelEvent.UPDATE && e.getColumn() == COL_STATUS) {
                int linha = e.getFirstRow();
                if (linha >= 0 && linha < emAberto.size()) {
                    String novo = (String) modeloPainel.getValueAt(linha, COL_STATUS);
                    int id = emAberto.get(linha).getId();
                    SwingUtilities.invokeLater(() -> mudarStatus(id, novo));
                }
            }
        });

        // Duplo clique abre a OS para alterar
        tblPainel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblPainel.columnAtPoint(e.getPoint()) != COL_STATUS) {
                    editarSelecionada();
                }
            }
        });

        // Sem "Nova OS": a OS nasce no cadastro; aqui só se altera
        JButton btnEditar = new JButton("Alterar OS");
        JButton btnExcluir = new JButton("Excluir OS");
        JButton btnWhats = new JButton("Abrir WhatsApp");
        JButton btnAtualizar = new JButton("Atualizar");
        btnEditar.addActionListener(e -> editarSelecionada());
        btnExcluir.addActionListener(e -> excluirSelecionada());
        btnWhats.addActionListener(e -> abrirWhatsapp());
        btnAtualizar.addActionListener(e -> carregarPainel());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnEditar);
        botoes.add(btnExcluir);
        botoes.add(btnWhats);
        botoes.add(btnAtualizar);

        JPanel painel = new JPanel(new BorderLayout(6, 6));
        painel.setBorder(BorderFactory.createTitledBorder("OS em aberto ou em andamento"));
        painel.add(botoes, BorderLayout.NORTH);
        painel.add(new JScrollPane(tblPainel), BorderLayout.CENTER);
        painel.add(new JLabel("  Dica: troque o status direto na coluna Status. "
                + "Duplo clique em uma linha abre a OS para alterar. "
                + "Novos atendimentos entram por Cadastros > Novo cadastro."), BorderLayout.SOUTH);
        return painel;
    }

    private void carregarPainel() {
        try {
            emAberto = osDao.listarEmAberto();
        } catch (SQLException ex) {
            erro("Erro de conexão com o banco de dados:\n" + ex.getMessage());
            return;
        }
        carregando = true;
        modeloPainel.setRowCount(0);
        for (OrdemServico o : emAberto) {
            modeloPainel.addRow(new Object[]{
                o.getId(),
                o.getCliente().getNome(),
                formatarTelefone(o.getCliente().getWhatsapp()),
                o.getTipoEquipamento(),
                o.getProblemaRelatado(),
                o.getDataEntrada().format(FMT),
                o.getValor() == null ? "" : MOEDA.format(o.getValor()),
                o.getStatus()
            });
        }
        carregando = false;
    }

    private void mudarStatus(int idOs, String novoStatus) {
        try {
            osDao.atualizarStatus(idOs, novoStatus);
        } catch (SQLException ex) {
            erro("Erro ao atualizar o status:\n" + ex.getMessage());
        }
        carregarPainel(); // OS concluída ou entregue sai da lista de abertas
    }

    private OrdemServico selecionada() {
        int linha = tblPainel.getSelectedRow();
        if (linha < 0 || linha >= emAberto.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma OS na lista.",
                    "Atenção", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return emAberto.get(linha);
    }

    private void editarSelecionada() {
        OrdemServico o = selecionada();
        if (o != null) {
            TelaOrdemServico.abrir(this, usuario, o, this::carregarPainel);
        }
    }

    private void excluirSelecionada() {
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
            osDao.excluir(o.getId());
            carregarPainel();
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

    /** Cores por status: vermelho = aberta, amarelo = em andamento, laranja = aguardando peça. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean foco, int row, int col) {
            Component c = super.getTableCellRendererComponent(t, v, sel, foco, row, col);
            String s = String.valueOf(v);
            Color cor;
            switch (s) {
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

    // ------------------------------------------------------------------
    // Menu
    // ------------------------------------------------------------------
    /** Cadastro = ficha de entrada (cliente + equipamento + problema + serviços). */
    private void abrirCadastro() {
        Runnable aoSalvar = usuario.isAdministrador() ? this::carregarPainel : null;
        TelaOrdemServico.abrir(this, usuario, null, aoSalvar);
    }

    private void abrirClientes() {
        new TelaCliente(true).setVisible(true);
    }

    private void abrirListaOS() {
        emConstrucao("Consulta de OS");
    }

    private void emConstrucao(String nome) {
        JOptionPane.showMessageDialog(this, nome + " ainda não foi criada.");
    }

    private void erro(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    private void sair() {
        new TelaLogin().setVisible(true);
        dispose();
    }
}
