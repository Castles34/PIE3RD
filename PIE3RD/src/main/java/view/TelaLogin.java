package view;

import dao.UsuarioDAO;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import model.Usuario;

/** Tela de login: identifica o usuário e o perfil (Administrador ou Caixa). */
public class TelaLogin extends JFrame {

    private final UsuarioDAO dao = new UsuarioDAO();

    private JTextField txtLogin;
    private JPasswordField txtSenha;
    private JButton btnEntrar;

    public TelaLogin() {
        initComponents();
    }

    private void initComponents() {
        setTitle("Controle de Ordens de Serviço - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel("Acesso ao sistema", JLabel.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 16f));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        add(titulo, g);

        txtLogin = new JTextField(15);
        txtSenha = new JPasswordField(15);
        btnEntrar = new JButton("Entrar");

        g.gridwidth = 1;
        g.gridy = 1; g.gridx = 0; add(new JLabel("Usuário:"), g);
        g.gridx = 1; add(txtLogin, g);
        g.gridy = 2; g.gridx = 0; add(new JLabel("Senha:"), g);
        g.gridx = 1; add(txtSenha, g);
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2; add(btnEntrar, g);

        btnEntrar.addActionListener(e -> entrar());
        getRootPane().setDefaultButton(btnEntrar); // Enter aciona o botão

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    private void entrar() {
        String login = txtLogin.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (login.isEmpty()) {
            aviso("Informe o usuário.");
            txtLogin.requestFocus();
            return;
        }
        if (senha.isEmpty()) {
            aviso("Informe a senha.");
            txtSenha.requestFocus();
            return;
        }

        try {
            Usuario u = dao.autenticar(login, senha);
            if (u == null) {
                aviso("Usuário ou senha incorretos.");
                txtSenha.setText("");
                txtSenha.requestFocus();
                return;
            }
            new TelaPrincipal(u).setVisible(true);
            dispose();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Erro ao acessar o banco de dados:\n" + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void aviso(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Atenção", JOptionPane.WARNING_MESSAGE);
    }
}
