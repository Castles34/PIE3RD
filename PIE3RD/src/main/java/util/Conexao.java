package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Conexão com o SQLite (arquivo os_manutencao.db, criado na pasta do projeto).
 * Na primeira execução, cria as tabelas lendo o banco.sql do classpath
 * (coloque o banco.sql em src/main/resources).
 */
public class Conexao {

    private static final String URL = "jdbc:sqlite:os_manutencao.db";

    public static Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(URL);
        try (Statement st = con.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON"); // ativa as chaves estrangeiras
        }
        return con;
    }

    /** Cria as tabelas e usuários de teste se ainda não existirem. */
    public static void inicializar() throws SQLException {
        String script = lerScript();
        try (Connection con = getConnection(); Statement st = con.createStatement()) {
            for (String comando : script.split(";")) {
                if (!comando.trim().isEmpty()) {
                    st.execute(comando);
                }
            }
        }
    }

    private static String lerScript() throws SQLException {
        try (InputStream in = Conexao.class.getResourceAsStream("/banco.sql")) {
            if (in == null) {
                throw new SQLException("Arquivo banco.sql não encontrado em src/main/resources.");
            }
            try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return r.lines()
                        .map(l -> l.contains("--") ? l.substring(0, l.indexOf("--")) : l)
                        .collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new SQLException("Erro ao ler banco.sql: " + e.getMessage(), e);
        }
    }

    // main temporário só para testar; apague depois
    public static void main(String[] args) {
        try {
            inicializar();
            System.out.println("Conexão e banco OK!");
        } catch (SQLException e) {
            System.out.println("Falhou: " + e.getMessage());
        }
    }
}
