package view;

import dao.ClienteDAO;
import dao.OrdemServicoDAO;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import model.Cliente;
import model.OrdemServico;
import model.Usuario;

/**
 * Ordem de Serviço: reproduz a ficha de papel da assistência.
 * Os dados do cliente (nome e Tel/Cel) são preenchidos aqui mesmo, junto com
 * o equipamento e o problema. Se o cliente já existir, é reaproveitado;
 * senão, é cadastrado automaticamente ao salvar.
 *
 * Dois modos:
 *  - Cadastro (os == null): usado pelo Caixa e pelo Administrador para anotar cliente,
 *    equipamento, problema e serviços a fazer. Não mostra peças, valores nem status.
 *  - Edição (os != null): só Administrador; mostra a ficha completa.
 *
 * Uso: TelaOrdemServico.abrir(this, usuario, null, aoSalvar)  // cadastro
 *      TelaOrdemServico.abrir(this, usuario, os, aoSalvar)    // alterar
 * Sem SQL aqui: tudo passa pelos DAOs.
 */
public class TelaOrdemServico extends JFrame {

    private static final DateTimeFormatter FMT_DATA =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final OrdemServicoDAO osDao = new OrdemServicoDAO();
    private final ClienteDAO clienteDao = new ClienteDAO();
    private final Usuario usuario;
    private final OrdemServico os; // null = nova OS
    private final Runnable aoSalvar;

    private JTextField txtCliente;
    private JTextField txtTelefone;
    private JLabel lblAviso;
    private JTextField txtData;
    private JTextField txtHora;
    private JComboBox<String> cmbStatus;
    private JRadioButton rdoPc;
    private JRadioButton rdoNotebook;
    private JCheckBox chkFonte;
    private JTextField txtAcessoriosOutros;
    private JCheckBox chkFormatacao;
    private JComboBox<String> cmbSistema;
    private JTextField txtSenha;
    private JCheckBox chkBackup;
    private JTextArea txtProblema;
    private JTextField txtTecnico;
    private JTextField txtDataFim;
    private JTextField txtHoraFim;
    private JTextArea txtPecas;
    private JTextArea txtServicos;
    private JTextArea txtOrcamento;
    private JTextField txtTotalPecas;
    private JTextField txtTotalServicos;
    private JTextField txtTotal;
    private JTextField txtRetirado;

    public static void abrir(Component pai, Usuario usuario, OrdemServico os, Runnable aoSalvar) {
        new TelaOrdemServico(usuario, os, aoSalvar).setVisible(true);
    }

    private TelaOrdemServico(Usuario usuario, OrdemServico os, Runnable aoSalvar) {
        this.usuario = usuario;
        this.os = os;
        this.aoSalvar = aoSalvar;
        initComponents();
        if (os != null) {
            preencher();
        } else {
            txtData.setText(LocalDate.now().format(FMT_DATA));
            txtHora.setText(LocalTime.now().format(FMT_HORA));
            txtTecnico.setText(usuario.getNome());
        }
    }

    // ------------------------------------------------------------------
    // Montagem da interface (na ordem da ficha de papel)
    // ------------------------------------------------------------------
    private void initComponents() {
        setTitle(os == null ? "Cadastro de Cliente e Ordem de Serviço" : "Ordem de Serviço nº " + os.getId());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(760, os == null ? 640 : 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        txtCliente = new JTextField(30);
        txtTelefone = new JTextField(15);
        lblAviso = new JLabel(" ");
        lblAviso.setFont(lblAviso.getFont().deriveFont(Font.ITALIC, 11f));
        txtData = new JTextField(9);
        txtHora = new JTextField(5);
        cmbStatus = new JComboBox<>(OrdemServico.STATUS);

        rdoPc = new JRadioButton("PC", true);
        rdoNotebook = new JRadioButton("Notebook");
        ButtonGroup grupo = new ButtonGroup();
        grupo.add(rdoPc);
        grupo.add(rdoNotebook);

        chkFonte = new JCheckBox("Fonte");
        txtAcessoriosOutros = new JTextField(22);
        chkFormatacao = new JCheckBox("Formatação");
        cmbSistema = new JComboBox<>(new String[]{"", "Windows 10", "Windows 11"});
        cmbSistema.setEditable(true); // permite digitar outro sistema
        cmbSistema.setEnabled(false);
        chkFormatacao.addActionListener(e -> cmbSistema.setEnabled(chkFormatacao.isSelected()));
        txtSenha = new JTextField(20);
        chkBackup = new JCheckBox("Backup / salvar dados");

        txtProblema = area(4);
        txtTecnico = somenteLeitura(20);
        txtDataFim = somenteLeitura(9);
        txtHoraFim = somenteLeitura(5);
        txtPecas = area(3);
        txtServicos = area(3);
        txtOrcamento = area(2);
        txtTotalPecas = new JTextField(10);
        txtTotalServicos = new JTextField(10);
        txtTotal = somenteLeitura(10);
        txtTotal.setFont(txtTotal.getFont().deriveFont(Font.BOLD));
        txtRetirado = somenteLeitura(9);

        DocumentListener recalcula = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { recalcular(); }
            public void removeUpdate(DocumentEvent e) { recalcular(); }
            public void changedUpdate(DocumentEvent e) { recalcular(); }
        };
        txtTotalPecas.getDocument().addDocumentListener(recalcula);
        txtTotalServicos.getDocument().addDocumentListener(recalcula);

        // Ao sair do campo Tel/Cel, procura um cliente já cadastrado com esse número
        txtTelefone.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                buscarClientePeloTelefone();
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTHWEST;

        // Cadastro (os == null): só os dados de atendimento. Edição: ficha completa.
        boolean edicao = os != null;
        int y = 0;
        linha(form, g, y++, "Cliente:", txtCliente);
        linha(form, g, y++, "Tel / Cel:", juntar(txtTelefone, lblAviso));
        linha(form, g, y++, "Data / Hora:", juntar(txtData, new JLabel("às"), txtHora));
        if (edicao) {
            linha(form, g, y++, "Status:", cmbStatus);
        }
        linha(form, g, y++, "Equipamento:", juntar(rdoPc, rdoNotebook));
        linha(form, g, y++, "Acessórios:", juntar(chkFonte, new JLabel("Outros:"), txtAcessoriosOutros));
        linha(form, g, y++, "Formatação:", juntar(chkFormatacao, new JLabel("Sistema:"), cmbSistema));
        linha(form, g, y++, "Senha:", juntar(txtSenha, chkBackup));
        linha(form, g, y++, "Descrição dos Problemas / outros:", new JScrollPane(txtProblema));
        if (edicao) {
            linha(form, g, y++, "Tec. Resp.:", txtTecnico);
            linha(form, g, y++, "Finalizado:", juntar(txtDataFim, new JLabel("às"), txtHoraFim));
            linha(form, g, y++, "Peças:", new JScrollPane(txtPecas));
        }
        linha(form, g, y++, "Serviços a realizar:", new JScrollPane(txtServicos));
        if (edicao) {
            linha(form, g, y++, "Orçamento / Outros:", new JScrollPane(txtOrcamento));
            linha(form, g, y++, "Total Peças: R$", txtTotalPecas);
            linha(form, g, y++, "Total Serviços: R$", txtTotalServicos);
            linha(form, g, y++, "Total: R$", txtTotal);
            linha(form, g, y++, "Retirado em:", txtRetirado);
        }

        add(new JScrollPane(form), BorderLayout.CENTER);

        JButton btnSalvar = new JButton("Salvar");
        JButton btnCancelar = new JButton("Cancelar");
        btnSalvar.addActionListener(e -> salvar());
        btnCancelar.addActionListener(e -> dispose());
        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botoes.add(btnSalvar);
        botoes.add(btnCancelar);
        add(botoes, BorderLayout.SOUTH);
    }

    private JTextArea area(int linhas) {
        JTextArea a = new JTextArea(linhas, 30);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return a;
    }

    private JTextField somenteLeitura(int colunas) {
        JTextField t = new JTextField(colunas);
        t.setEditable(false);
        return t;
    }

    private JPanel juntar(Component... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (Component c : componentes) {
            p.add(c);
        }
        return p;
    }

    private void linha(JPanel p, GridBagConstraints g, int y, String rotulo, Component campo) {
        g.gridy = y;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(rotulo), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(campo, g);
    }

    // ------------------------------------------------------------------
    // Preenchimento ao editar
    // ------------------------------------------------------------------
    private void preencher() {
        txtCliente.setText(os.getCliente().getNome());
        txtTelefone.setText(formatarTelefone(os.getCliente().getWhatsapp()));
        txtData.setText(os.getDataEntrada().format(FMT_DATA));
        txtHora.setText(os.getHoraEntrada() == null ? "" : os.getHoraEntrada().format(FMT_HORA));
        cmbStatus.setSelectedItem(os.getStatus());
        if (OrdemServico.NOTEBOOK.equals(os.getTipoEquipamento())) {
            rdoNotebook.setSelected(true);
        } else {
            rdoPc.setSelected(true);
        }
        chkFonte.setSelected(os.isAcessorioFonte());
        txtAcessoriosOutros.setText(nulo(os.getAcessoriosOutros()));
        chkFormatacao.setSelected(os.isFormatacao());
        cmbSistema.setEnabled(os.isFormatacao());
        cmbSistema.setSelectedItem(nulo(os.getSistemaFormatacao()));
        txtSenha.setText(nulo(os.getSenhaEquipamento()));
        chkBackup.setSelected(os.isBackup());
        txtProblema.setText(nulo(os.getProblemaRelatado()));
        txtTecnico.setText(usuario.getNome());
        txtDataFim.setText(os.getDataConclusao() == null ? "" : os.getDataConclusao().format(FMT_DATA));
        txtHoraFim.setText(os.getHoraConclusao() == null ? "" : os.getHoraConclusao().format(FMT_HORA));
        txtPecas.setText(nulo(os.getPecas()));
        txtServicos.setText(nulo(os.getServicoARealizar()));
        txtOrcamento.setText(nulo(os.getOrcamentoOutros()));
        txtTotalPecas.setText(dinheiro(os.getTotalPecas()));
        txtTotalServicos.setText(dinheiro(os.getTotalServicos()));
        txtRetirado.setText(os.getDataRetirada() == null ? "" : os.getDataRetirada().format(FMT_DATA));
        recalcular();
    }

    // ------------------------------------------------------------------
    // Ações
    // ------------------------------------------------------------------
    private void buscarClientePeloTelefone() {
        String d = txtTelefone.getText().replaceAll("\\D", "");
        if (d.length() < 10 || d.length() > 11) {
            return;
        }
        try {
            Cliente c = clienteDao.buscarPorWhatsapp(d);
            if (c != null && txtCliente.getText().trim().isEmpty()) {
                txtCliente.setText(c.getNome());
                lblAviso.setText("Cliente já cadastrado: nome preenchido.");
            } else if (c == null) {
                lblAviso.setText("Cliente novo: será cadastrado ao salvar.");
            }
        } catch (SQLException ex) {
            lblAviso.setText(" "); // a falha de banco aparece de verdade ao salvar
        }
    }

    private void recalcular() {
        try {
            BigDecimal p = lerValor(txtTotalPecas.getText());
            BigDecimal s = lerValor(txtTotalServicos.getText());
            BigDecimal total = (p == null ? BigDecimal.ZERO : p).add(s == null ? BigDecimal.ZERO : s);
            txtTotal.setText(dinheiro(total));
        } catch (NumberFormatException ex) {
            txtTotal.setText("");
        }
    }

    private void salvar() {
        String nome = txtCliente.getText().trim();
        String telefone = txtTelefone.getText().replaceAll("\\D", "");
        String problema = txtProblema.getText().trim();

        if (nome.isEmpty()) {
            aviso("Informe o nome do cliente.", txtCliente);
            return;
        }
        if (telefone.isEmpty()) {
            aviso("Informe o Tel / Cel do cliente.", txtTelefone);
            return;
        }
        if (telefone.length() < 10 || telefone.length() > 11) {
            aviso("Tel / Cel inválido. Digite o DDD e o número (10 ou 11 dígitos).", txtTelefone);
            return;
        }

        LocalDate dataEntrada;
        try {
            dataEntrada = LocalDate.parse(txtData.getText().trim(), FMT_DATA);
        } catch (DateTimeParseException ex) {
            aviso("Data inválida. Use o formato dd/mm/aaaa.", txtData);
            return;
        }
        LocalTime horaEntrada;
        try {
            horaEntrada = LocalTime.parse(txtHora.getText().trim(), FMT_HORA);
        } catch (DateTimeParseException ex) {
            aviso("Hora inválida. Use o formato hh:mm (exemplo: 14:30).", txtHora);
            return;
        }
        if (problema.isEmpty()) {
            aviso("Descreva o problema relatado.", txtProblema);
            return;
        }

        BigDecimal totalPecas;
        BigDecimal totalServicos;
        try {
            totalPecas = lerValor(txtTotalPecas.getText());
        } catch (NumberFormatException ex) {
            aviso("Total de peças inválido. Exemplo: 150,00", txtTotalPecas);
            return;
        }
        try {
            totalServicos = lerValor(txtTotalServicos.getText());
        } catch (NumberFormatException ex) {
            aviso("Total de serviços inválido. Exemplo: 80,00", txtTotalServicos);
            return;
        }
        BigDecimal total = null;
        if (totalPecas != null || totalServicos != null) {
            total = (totalPecas == null ? BigDecimal.ZERO : totalPecas)
                    .add(totalServicos == null ? BigDecimal.ZERO : totalServicos);
        }

        String status = (String) cmbStatus.getSelectedItem();
        boolean concluida = "Concluída".equals(status) || "Entregue".equals(status);

        try {
            Cliente cliente = clienteDao.obterOuCriar(nome, telefone);

            OrdemServico o = (os == null) ? new OrdemServico() : os;
            o.setCliente(cliente);
            o.setTipoEquipamento(rdoNotebook.isSelected() ? OrdemServico.NOTEBOOK : OrdemServico.COMPUTADOR);
            o.setDataEntrada(dataEntrada);
            o.setHoraEntrada(horaEntrada);
            o.setAcessorioFonte(chkFonte.isSelected());
            o.setAcessoriosOutros(txtAcessoriosOutros.getText().trim());
            o.setFormatacao(chkFormatacao.isSelected());
            Object sistema = cmbSistema.getEditor().getItem();
            o.setSistemaFormatacao(chkFormatacao.isSelected() && sistema != null
                    ? sistema.toString().trim() : null);
            o.setSenhaEquipamento(txtSenha.getText().trim());
            o.setBackup(chkBackup.isSelected());
            o.setProblemaRelatado(problema);
            o.setPecas(txtPecas.getText().trim());
            o.setServicoARealizar(txtServicos.getText().trim());
            o.setOrcamentoOutros(txtOrcamento.getText().trim());
            o.setTotalPecas(totalPecas);
            o.setTotalServicos(totalServicos);
            o.setValor(total);
            o.setStatus(status);

            // Finalizado: mantém a data existente, usa agora ao concluir, limpa se reabriu
            if (concluida) {
                if (o.getDataConclusao() == null) {
                    o.setDataConclusao(LocalDate.now());
                    o.setHoraConclusao(LocalTime.now().withSecond(0).withNano(0));
                }
            } else {
                o.setDataConclusao(null);
                o.setHoraConclusao(null);
            }
            // Retirado: só quando o status é Entregue
            if ("Entregue".equals(status)) {
                if (o.getDataRetirada() == null) {
                    o.setDataRetirada(LocalDate.now());
                }
            } else {
                o.setDataRetirada(null);
            }
            // Responsável = quem salvou por último (o técnico assume a OS ao editá-la)
            o.setIdUsuarioResponsavel(usuario.getId());

            if (os == null) {
                osDao.inserir(o);
            } else {
                osDao.alterar(o);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar no banco de dados:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this, "Registro salvo com sucesso!");
        if (aoSalvar != null) {
            aoSalvar.run();
        }
        dispose();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    /** Aceita 150,00 / 1.250,50 / 150.5. Vazio = null. Lança NumberFormatException se inválido. */
    private BigDecimal lerValor(String texto) {
        String n = texto.replace("R$", "").trim();
        if (n.isEmpty()) {
            return null;
        }
        if (n.contains(",")) {
            n = n.replace(".", "").replace(",", ".");
        }
        BigDecimal v = new BigDecimal(n);
        if (v.signum() < 0) {
            throw new NumberFormatException("negativo");
        }
        return v;
    }

    private String dinheiro(BigDecimal v) {
        return v == null ? "" : v.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
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

    private String nulo(String s) {
        return s == null ? "" : s;
    }

    private void aviso(String msg, Component campo) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
        campo.requestFocus();
    }
}
