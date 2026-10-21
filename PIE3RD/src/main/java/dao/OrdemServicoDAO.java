package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Cliente;
import model.OrdemServico;
import util.Conexao;

public class OrdemServicoDAO {

    private static final String SELECT_BASE =
            "SELECT o.id_os, o.id_cliente, c.nome, c.whatsapp, o.id_usuario_responsavel, "
            + "o.tipo_equipamento, o.problema_relatado, o.servico_a_realizar, o.status, "
            + "o.data_entrada, o.data_conclusao, o.valor "
            + "FROM ordem_servico o JOIN cliente c ON c.id_cliente = o.id_cliente ";

    public void inserir(OrdemServico os) throws SQLException {
        String sql = "INSERT INTO ordem_servico (id_cliente, id_usuario_responsavel, "
                + "tipo_equipamento, problema_relatado, servico_a_realizar, status, "
                + "data_entrada, data_conclusao, valor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, os.getCliente().getId());
            ps.setInt(2, os.getIdUsuarioResponsavel());
            ps.setString(3, os.getTipoEquipamento());
            ps.setString(4, os.getProblemaRelatado());
            ps.setString(5, os.getServicoARealizar());
            ps.setString(6, os.getStatus());
            LocalDate entrada = os.getDataEntrada() != null ? os.getDataEntrada() : LocalDate.now();
            ps.setString(7, entrada.toString());
            ps.setString(8, os.getDataConclusao() == null ? null : os.getDataConclusao().toString());
            ps.setString(9, os.getValor() == null ? null : os.getValor().toPlainString());
            ps.executeUpdate();
        }
    }

    public void alterar(OrdemServico os) throws SQLException {
        String sql = "UPDATE ordem_servico SET id_cliente = ?, tipo_equipamento = ?, "
                + "problema_relatado = ?, servico_a_realizar = ?, status = ?, "
                + "data_entrada = ?, data_conclusao = ?, valor = ? WHERE id_os = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, os.getCliente().getId());
            ps.setString(2, os.getTipoEquipamento());
            ps.setString(3, os.getProblemaRelatado());
            ps.setString(4, os.getServicoARealizar());
            ps.setString(5, os.getStatus());
            ps.setString(6, os.getDataEntrada().toString());
            ps.setString(7, os.getDataConclusao() == null ? null : os.getDataConclusao().toString());
            ps.setString(8, os.getValor() == null ? null : os.getValor().toPlainString());
            ps.setInt(9, os.getId());
            ps.executeUpdate();
        }
    }

    /** Atualiza só o status (ex.: menu suspenso na lista). Preenche a data de conclusão. */
    public void atualizarStatus(int idOs, String novoStatus) throws SQLException {
        boolean concluiu = "Concluída".equals(novoStatus) || "Entregue".equals(novoStatus);
        String sql = concluiu
                ? "UPDATE ordem_servico SET status = ?, "
                  + "data_conclusao = COALESCE(data_conclusao, date('now')) WHERE id_os = ?"
                : "UPDATE ordem_servico SET status = ?, data_conclusao = NULL WHERE id_os = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novoStatus);
            ps.setInt(2, idOs);
            ps.executeUpdate();
        }
    }

    public void excluir(int idOs) throws SQLException {
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM ordem_servico WHERE id_os = ?")) {
            ps.setInt(1, idOs);
            ps.executeUpdate();
        }
    }

    /** Painel inicial: OS Aberta, Em andamento ou Aguardando peça. */
    public List<OrdemServico> listarEmAberto() throws SQLException {
        String sql = SELECT_BASE
                + "WHERE o.status IN ('Aberta', 'Em andamento', 'Aguardando peça') "
                + "ORDER BY o.data_entrada, o.id_os";
        return consultar(sql);
    }

    public List<OrdemServico> listarPorCliente(int idCliente) throws SQLException {
        String sql = SELECT_BASE + "WHERE o.id_cliente = ? ORDER BY o.data_entrada DESC, o.id_os DESC";
        return consultar(sql, idCliente);
    }

    /** Filtros opcionais: passe null (ou texto vazio) para ignorar um filtro. */
    public List<OrdemServico> filtrar(String status, String tipo,
            LocalDate dataInicio, LocalDate dataFim) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_BASE + "WHERE 1 = 1 ");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isEmpty()) {
            sql.append("AND o.status = ? ");
            params.add(status);
        }
        if (tipo != null && !tipo.isEmpty()) {
            sql.append("AND o.tipo_equipamento = ? ");
            params.add(tipo);
        }
        if (dataInicio != null) {
            sql.append("AND o.data_entrada >= ? ");
            params.add(dataInicio.toString());
        }
        if (dataFim != null) {
            sql.append("AND o.data_entrada <= ? ");
            params.add(dataFim.toString());
        }
        sql.append("ORDER BY o.data_entrada DESC, o.id_os DESC");
        return consultar(sql.toString(), params.toArray());
    }

    private List<OrdemServico> consultar(String sql, Object... params) throws SQLException {
        List<OrdemServico> lista = new ArrayList<>();
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        }
        return lista;
    }

    private OrdemServico mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setNome(rs.getString("nome"));
        c.setWhatsapp(rs.getString("whatsapp"));

        OrdemServico os = new OrdemServico();
        os.setId(rs.getInt("id_os"));
        os.setCliente(c);
        os.setIdUsuarioResponsavel(rs.getInt("id_usuario_responsavel"));
        os.setTipoEquipamento(rs.getString("tipo_equipamento"));
        os.setProblemaRelatado(rs.getString("problema_relatado"));
        os.setServicoARealizar(rs.getString("servico_a_realizar"));
        os.setStatus(rs.getString("status"));
        os.setDataEntrada(LocalDate.parse(rs.getString("data_entrada")));
        String conclusao = rs.getString("data_conclusao");
        os.setDataConclusao(conclusao == null ? null : LocalDate.parse(conclusao));
        String valor = rs.getString("valor");
        os.setValor(valor == null ? null : new BigDecimal(valor));
        return os;
    }
}
