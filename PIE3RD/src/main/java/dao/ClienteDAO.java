package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.Cliente;
import util.Conexao;

public class ClienteDAO {

    public void inserir(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nome, whatsapp) VALUES (?, ?)";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getWhatsapp());
            ps.executeUpdate();
        }
    }

    public void alterar(Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nome = ?, whatsapp = ? WHERE id_cliente = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNome());
            ps.setString(2, c.getWhatsapp());
            ps.setInt(3, c.getId());
            ps.executeUpdate();
        }
    }

    /** Falha com SQLException se o cliente tiver OS (chave estrangeira). */
    public void excluir(int id) throws SQLException {
        String sql = "DELETE FROM cliente WHERE id_cliente = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Cliente> listar() throws SQLException {
        return buscar("");
    }

    /** Busca por parte do nome ou do número de WhatsApp. Texto vazio lista todos. */
    public List<Cliente> buscar(String termo) throws SQLException {
        String sql = "SELECT id_cliente, nome, whatsapp FROM cliente "
                + "WHERE nome LIKE ? OR whatsapp LIKE ? ORDER BY nome";
        List<Cliente> lista = new ArrayList<>();
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String filtro = "%" + termo.trim() + "%";
            ps.setString(1, filtro);
            ps.setString(2, filtro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    static Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setNome(rs.getString("nome"));
        c.setWhatsapp(rs.getString("whatsapp"));
        return c;
    }
}
