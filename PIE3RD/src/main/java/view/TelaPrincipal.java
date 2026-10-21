package view;

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import model.Usuario;

/**
 * Tela principal. O menu muda conforme o perfil:
 *  - Administrador: Clientes, Ordens de Serviço e backup.
 *  - Caixa: somente Clientes.
 */
public class TelaPrincipal extends JFrame {

    private final Usuario usuario;

    public TelaPrincipal(Usuario usuario) {
        this.usuario = usuario;
        initComponents();
    }

    private void initComponents() {
        setTitle("Controle de Ordens de Serviço - " + usuario.getNome()
                + " (" + usuario.getPerfil() + ")");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JMenuBar barra = new JMenuBar();

        JMenu mnCadastros = new JMenu("Cadastros");
        JMenuItem miClientes = new JMenuItem("Clientes");
        miClientes.addActionListener(e -> abrirClientes());
        mnCadastros.add(miClientes);
        barra.add(mnCadastros);

        // Só o Administrador enxerga as Ordens de Serviço e o backup
        if (usuario.isAdministrador()) {
            JMenu mnOS = new JMenu("Ordens de Serviço");
            JMenuItem miNova = new JMenuItem("Nova OS");
            miNova.addActionListener(e -> abrirNovaOS());
            JMenuItem miLista = new JMenuItem("Consultar / filtrar OS");
            miLista.addActionListener(e -> abrirListaOS());
            mnOS.add(miNova);
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

        // Placeholder: aqui entra o painel de OS em aberto (RF16) para o Administrador
        String texto = usuario.isAdministrador()
                ? "Painel de OS em aberto (a construir)"
                : "Use o menu Cadastros para cadastrar ou buscar clientes";
        add(new JLabel(texto, JLabel.CENTER), BorderLayout.CENTER);
    }

    // As telas abaixo ainda não existem; troque cada chamada quando criar a tela.
    private void abrirClientes() { new TelaCliente(usuario.isAdministrador()).setVisible(true); }
    private void abrirNovaOS()   { emConstrucao("Nova OS"); }
    private void abrirListaOS()  { emConstrucao("Consulta de OS"); }

    private void emConstrucao(String nome) {
        JOptionPane.showMessageDialog(this, nome + " ainda não foi criada.");
    }

    private void sair() {
        new TelaLogin().setVisible(true);
        dispose();
    }
}
