package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    /** Cliente mais recente com esse WhatsApp (só dígitos), ou null. Usado para autopreencher a OS. */
    public Cliente buscarPorWhatsapp(String digitos) throws SQLException {
        String sql = "SELECT id_cliente, nome, whatsapp FROM cliente "
                + "WHERE whatsapp = ? ORDER BY id_cliente DESC LIMIT 1";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, digitos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapear(rs) : null;
            }
        }
    }

    /**
     * Usado ao salvar a OS: reaproveita o cliente que tem esse nome e WhatsApp;
     * se não existir, cadastra um novo. Assim o cadastro do cliente "vem junto" com a OS.
     */
    public Cliente obterOuCriar(String nome, String whatsapp) throws SQLException {
        try (Connection con = Conexao.getConnection()) {
            String busca = "SELECT id_cliente, nome, whatsapp FROM cliente "
                    + "WHERE whatsapp = ? AND nome = ? COLLATE NOCASE LIMIT 1";
            try (PreparedStatement ps = con.prepareStatement(busca)) {
                ps.setString(1, whatsapp);
                ps.setString(2, nome);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapear(rs);
                    }
                }
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO cliente (nome, whatsapp) VALUES (?, ?)")) {
                ps.setString(1, nome);
                ps.setString(2, whatsapp);
                ps.executeUpdate();
            }
            Cliente novo = new Cliente();
            novo.setNome(nome);
            novo.setWhatsapp(whatsapp);
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                novo.setId(rs.getInt(1));
            }
            return novo;
        }
    }

    static Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setNome(rs.getString("nome"));
        c.setWhatsapp(rs.getString("whatsapp"));
        return c;
    }
}
