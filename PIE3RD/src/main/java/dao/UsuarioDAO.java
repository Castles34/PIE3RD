package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import model.Usuario;
import util.Conexao;
import util.Hash;

public class UsuarioDAO {

    /** Retorna o usuário se login e senha conferem; senão, null. */
    public Usuario autenticar(String login, String senhaDigitada) throws SQLException {
        String sql = "SELECT id_usuario, nome, login, senha, perfil FROM usuario "
                + "WHERE login = ? AND senha = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, Hash.sha256(senhaDigitada));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id_usuario"));
                    u.setNome(rs.getString("nome"));
                    u.setLogin(rs.getString("login"));
                    u.setSenha(rs.getString("senha"));
                    u.setPerfil(rs.getString("perfil"));
                    return u;
                }
                return null;
            }
        }
    }
}
