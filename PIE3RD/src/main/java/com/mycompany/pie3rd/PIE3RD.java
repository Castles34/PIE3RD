package com.mycompany.pie3rd;

import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import util.Conexao;
import view.TelaLogin;

public class PIE3RD {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // segue com o visual padrão
        }

        try {
            Conexao.inicializar(); // cria o banco e os usuários de teste, se preciso
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível iniciar o banco de dados:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
