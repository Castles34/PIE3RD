package model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Ordem de Serviço: replica a ficha de papel (cliente, equipamento, problema, valores). */
public class OrdemServico {

    public static final String[] STATUS = {
        "Aberta", "Em andamento", "Aguardando peça", "Concluída", "Entregue"
    };
    public static final String COMPUTADOR = "Computador"; // "PC" na ficha
    public static final String NOTEBOOK = "Notebook";
    public static final String[] TIPOS = {NOTEBOOK, COMPUTADOR};

    private int id;
    private Cliente cliente;
    private int idUsuarioResponsavel;
    private String tecnicoNome; // só leitura (vem do banco)
    private String tipoEquipamento = COMPUTADOR;
    private String problemaRelatado;
    private String servicoARealizar;
    private String status = "Aberta";
    private LocalDate dataEntrada;
    private LocalTime horaEntrada;
    private LocalDate dataConclusao;
    private LocalTime horaConclusao;
    private LocalDate dataRetirada;
    private boolean acessorioFonte;
    private String acessoriosOutros;
    private boolean formatacao;
    private String sistemaFormatacao;
    private String senhaEquipamento;
    private boolean backup;
    private String pecas;
    private String orcamentoOutros;
    private BigDecimal totalPecas;
    private BigDecimal totalServicos;
    private BigDecimal valor; // total

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public int getIdUsuarioResponsavel() { return idUsuarioResponsavel; }
    public void setIdUsuarioResponsavel(int id) { this.idUsuarioResponsavel = id; }

    public String getTecnicoNome() { return tecnicoNome; }
    public void setTecnicoNome(String tecnicoNome) { this.tecnicoNome = tecnicoNome; }

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

    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime h) { this.horaEntrada = h; }

    public LocalDate getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(LocalDate d) { this.dataConclusao = d; }

    public LocalTime getHoraConclusao() { return horaConclusao; }
    public void setHoraConclusao(LocalTime h) { this.horaConclusao = h; }

    public LocalDate getDataRetirada() { return dataRetirada; }
    public void setDataRetirada(LocalDate d) { this.dataRetirada = d; }

    public boolean isAcessorioFonte() { return acessorioFonte; }
    public void setAcessorioFonte(boolean b) { this.acessorioFonte = b; }

    public String getAcessoriosOutros() { return acessoriosOutros; }
    public void setAcessoriosOutros(String s) { this.acessoriosOutros = s; }

    public boolean isFormatacao() { return formatacao; }
    public void setFormatacao(boolean b) { this.formatacao = b; }

    public String getSistemaFormatacao() { return sistemaFormatacao; }
    public void setSistemaFormatacao(String s) { this.sistemaFormatacao = s; }

    public String getSenhaEquipamento() { return senhaEquipamento; }
    public void setSenhaEquipamento(String s) { this.senhaEquipamento = s; }

    public boolean isBackup() { return backup; }
    public void setBackup(boolean b) { this.backup = b; }

    public String getPecas() { return pecas; }
    public void setPecas(String s) { this.pecas = s; }

    public String getOrcamentoOutros() { return orcamentoOutros; }
    public void setOrcamentoOutros(String s) { this.orcamentoOutros = s; }

    public BigDecimal getTotalPecas() { return totalPecas; }
    public void setTotalPecas(BigDecimal v) { this.totalPecas = v; }

    public BigDecimal getTotalServicos() { return totalServicos; }
    public void setTotalServicos(BigDecimal v) { this.totalServicos = v; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
