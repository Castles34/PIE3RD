package model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrdemServico {

    public static final String[] STATUS = {
        "Aberta", "Em andamento", "Aguardando peça", "Concluída", "Entregue"
    };
    public static final String[] TIPOS = {"Notebook", "Computador"};

    private int id;
    private Cliente cliente;
    private int idUsuarioResponsavel;
    private String tipoEquipamento;
    private String problemaRelatado;
    private String servicoARealizar;
    private String status = "Aberta";
    private LocalDate dataEntrada;
    private LocalDate dataConclusao;
    private BigDecimal valor;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public int getIdUsuarioResponsavel() { return idUsuarioResponsavel; }
    public void setIdUsuarioResponsavel(int id) { this.idUsuarioResponsavel = id; }

    public String getTipoEquipamento() { return tipoEquipamento; }
    public void setTipoEquipamento(String tipo) { this.tipoEquipamento = tipo; }

    public String getProblemaRelatado() { return problemaRelatado; }
    public void setProblemaRelatado(String p) { this.problemaRelatado = p; }

    public String getServicoARealizar() { return servicoARealizar; }
    public void setServicoARealizar(String s) { this.servicoARealizar = s; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate d) { this.dataEntrada = d; }

    public LocalDate getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDate d) { this.dataConclusao = d; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
