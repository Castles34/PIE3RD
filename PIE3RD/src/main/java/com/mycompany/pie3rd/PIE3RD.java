package com.mycompany.pie3rd;

import com.formdev.flatlaf.FlatLightLaf;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import util.Conexao;
import view.TelaLogin;

public class PIE3RD {

    public static void main(String[] args) {
        // Visual moderno (FlatLaf) em todas as telas, com cantos arredondados
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        FlatLightLaf.setup();

        try {
            Conexao.testarConexao(); // confere se o MySQL do servidor está respondendo
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Não foi possível conectar ao banco de dados.\n"
                    + "Confira se o MySQL está ligado no servidor e se o config.properties está correto.\n\n"
                    + "Detalhe: " + e.getMessage(),
                    "Erro de conexão", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
