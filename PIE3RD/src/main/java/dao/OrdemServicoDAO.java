package dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;
import model.Cliente;
import model.OrdemServico;
import util.Conexao;

public class OrdemServicoDAO {

    // Mesma ordem usada em preencher(); id_usuario_responsavel entra à parte no INSERT.
    private static final String[] CAMPOS = {
        "id_cliente", "tipo_equipamento", "problema_relatado", "servico_a_realizar", "status",
        "data_entrada", "hora_entrada", "data_conclusao", "hora_conclusao", "data_retirada",
        "acessorio_fonte", "acessorios_outros", "formatacao", "sistema_formatacao",
        "senha_equipamento", "fazer_backup", "pecas", "orcamento_outros",
        "total_pecas", "total_servicos", "valor"
    };

    private static final String SELECT_BASE =
            "SELECT o.*, c.nome AS cliente_nome, c.whatsapp AS cliente_whatsapp, "
            + "u.nome AS tecnico_nome "
            + "FROM ordem_servico o "
            + "JOIN cliente c ON c.id_cliente = o.id_cliente "
            + "JOIN usuario u ON u.id_usuario = o.id_usuario_responsavel ";

    public void inserir(OrdemServico os) throws SQLException {
        if (os.getDataEntrada() == null) {
            os.setDataEntrada(LocalDate.now());
        }
        if (os.getHoraEntrada() == null) {
            os.setHoraEntrada(LocalTime.now().withSecond(0).withNano(0));
        }
        String sql = "INSERT INTO ordem_servico (" + String.join(", ", CAMPOS)
                + ", id_usuario_responsavel) VALUES (" + marcas(CAMPOS.length + 1) + ")";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, os);
            ps.setInt(CAMPOS.length + 1, os.getIdUsuarioResponsavel());
            ps.executeUpdate();
        }
    }

    public void alterar(OrdemServico os) throws SQLException {
        String sql = "UPDATE ordem_servico SET "
                + Arrays.stream(CAMPOS).map(c -> c + " = ?").collect(Collectors.joining(", "))
                + ", id_usuario_responsavel = ? WHERE id_os = ?";
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            preencher(ps, os);
            ps.setInt(CAMPOS.length + 1, os.getIdUsuarioResponsavel());
            ps.setInt(CAMPOS.length + 2, os.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Atualiza só o status (menu suspenso da lista). Concluída/Entregue preenchem a data
     * e hora de conclusão; Entregue preenche também a data de retirada.
     */
    public void atualizarStatus(int idOs, String novoStatus) throws SQLException {
        boolean concluiu = "Concluída".equals(novoStatus) || "Entregue".equals(novoStatus);
        String sql;
        if (concluiu) {
            sql = "UPDATE ordem_servico SET status = ?, "
                + "data_conclusao = COALESCE(data_conclusao, CURDATE()), "
                + "hora_conclusao = COALESCE(hora_conclusao, CURTIME()), "
                + "data_retirada = CASE WHEN ? = 'Entregue' "
                + "THEN COALESCE(data_retirada, CURDATE()) ELSE NULL END "
                + "WHERE id_os = ?";
        } else {
            sql = "UPDATE ordem_servico SET status = ?, data_conclusao = NULL, "
                + "hora_conclusao = NULL, data_retirada = NULL WHERE id_os = ?";
        }
        try (Connection con = Conexao.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, novoStatus);
            if (concluiu) {
                ps.setString(2, novoStatus);
                ps.setInt(3, idOs);
            } else {
                ps.setInt(2, idOs);
            }
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

    // ------------------------------------------------------------------
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

    private void preencher(PreparedStatement ps, OrdemServico os) throws SQLException {
        int i = 1;
        ps.setInt(i++, os.getCliente().getId());
        ps.setString(i++, os.getTipoEquipamento());
        ps.setString(i++, os.getProblemaRelatado());
        ps.setString(i++, os.getServicoARealizar());
        ps.setString(i++, os.getStatus());
        ps.setString(i++, txt(os.getDataEntrada()));
        ps.setString(i++, txt(os.getHoraEntrada()));
        ps.setString(i++, txt(os.getDataConclusao()));
        ps.setString(i++, txt(os.getHoraConclusao()));
        ps.setString(i++, txt(os.getDataRetirada()));
        ps.setInt(i++, os.isAcessorioFonte() ? 1 : 0);
        ps.setString(i++, os.getAcessoriosOutros());
        ps.setInt(i++, os.isFormatacao() ? 1 : 0);
        ps.setString(i++, os.getSistemaFormatacao());
        ps.setString(i++, os.getSenhaEquipamento());
        ps.setInt(i++, os.isBackup() ? 1 : 0);
        ps.setString(i++, os.getPecas());
        ps.setString(i++, os.getOrcamentoOutros());
        ps.setString(i++, dec(os.getTotalPecas()));
        ps.setString(i++, dec(os.getTotalServicos()));
        ps.setString(i++, dec(os.getValor()));
    }

    private OrdemServico mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id_cliente"));
        c.setNome(rs.getString("cliente_nome"));
        c.setWhatsapp(rs.getString("cliente_whatsapp"));

        OrdemServico os = new OrdemServico();
        os.setId(rs.getInt("id_os"));
        os.setCliente(c);
        os.setIdUsuarioResponsavel(rs.getInt("id_usuario_responsavel"));
        os.setTecnicoNome(rs.getString("tecnico_nome"));
        os.setTipoEquipamento(rs.getString("tipo_equipamento"));
        os.setProblemaRelatado(rs.getString("problema_relatado"));
        os.setServicoARealizar(rs.getString("servico_a_realizar"));
        os.setStatus(rs.getString("status"));
        os.setDataEntrada(data(rs.getString("data_entrada")));
        os.setHoraEntrada(hora(rs.getString("hora_entrada")));
        os.setDataConclusao(data(rs.getString("data_conclusao")));
        os.setHoraConclusao(hora(rs.getString("hora_conclusao")));
        os.setDataRetirada(data(rs.getString("data_retirada")));
        os.setAcessorioFonte(rs.getInt("acessorio_fonte") != 0);
        os.setAcessoriosOutros(rs.getString("acessorios_outros"));
        os.setFormatacao(rs.getInt("formatacao") != 0);
        os.setSistemaFormatacao(rs.getString("sistema_formatacao"));
        os.setSenhaEquipamento(rs.getString("senha_equipamento"));
        os.setBackup(rs.getInt("fazer_backup") != 0);
        os.setPecas(rs.getString("pecas"));
        os.setOrcamentoOutros(rs.getString("orcamento_outros"));
        os.setTotalPecas(decimal(rs.getString("total_pecas")));
        os.setTotalServicos(decimal(rs.getString("total_servicos")));
        os.setValor(decimal(rs.getString("valor")));
        return os;
    }

    private static String marcas(int n) {
        return String.join(", ", Collections.nCopies(n, "?"));
    }

    private static String txt(Object o) {
        return o == null ? null : o.toString();
    }

    private static String dec(BigDecimal v) {
        return v == null ? null : v.toPlainString();
    }

    private static LocalDate data(String s) {
        return (s == null || s.isEmpty()) ? null : LocalDate.parse(s);
    }

    private static LocalTime hora(String s) {
        return (s == null || s.isEmpty()) ? null : LocalTime.parse(s);
    }

    private static BigDecimal decimal(String s) {
        return (s == null || s.isEmpty()) ? null : new BigDecimal(s);
    }
}
