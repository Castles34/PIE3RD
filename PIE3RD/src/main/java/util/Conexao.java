package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexão com o MySQL. Os dados (servidor, usuário, senha) ficam no arquivo
 * config.properties, na pasta do projeto, e NÃO vão para o GitHub.
 * Copie o config.properties.exemplo para config.properties e ajuste.
 *
 * Em cada computador o host é diferente:
 *   - no servidor (PC do técnico): localhost
 *   - nos PCs do caixa: o IP do servidor (ex.: 192.168.0.10)
 */
public class Conexao {

    private static final String ARQUIVO = "config.properties";
    private static Properties config;

    private static synchronized Properties config() throws SQLException {
        if (config == null) {
            File f = new File(ARQUIVO);
            if (!f.exists()) {
                throw new SQLException("Arquivo " + ARQUIVO + " não encontrado em:\n"
                        + f.getAbsoluteFile().getParent()
                        + "\nCopie o config.properties.exemplo para config.properties e ajuste.");
            }
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(f);
                 InputStreamReader leitor = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(leitor);
            } catch (IOException e) {
                throw new SQLException("Não foi possível ler o " + ARQUIVO + ": " + e.getMessage(), e);
            }
            config = p;
        }
        return config;
    }

    public static Connection getConnection() throws SQLException {
        Properties c = config();
        String url = "jdbc:mysql://" + c.getProperty("db.host", "localhost").trim()
                + ":" + c.getProperty("db.porta", "3306").trim()
                + "/" + c.getProperty("db.banco", "os_manutencao").trim()
                + "?useSSL=false&allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Sao_Paulo&characterEncoding=UTF-8"
                + "&connectTimeout=5000";
        return DriverManager.getConnection(url,
                c.getProperty("db.usuario", "os_app").trim(),
                c.getProperty("db.senha", "").trim());
    }

    /** Abre e fecha uma conexão só para conferir se o servidor responde. */
    public static void testarConexao() throws SQLException {
        try (Connection con = getConnection()) {
            // conectou: nada a fazer
        }
    }

    // main temporário só para testar; apague depois
    public static void main(String[] args) {
        try {
            testarConexao();
            System.out.println("Conexão com o MySQL OK!");
        } catch (SQLException e) {
            System.out.println("Falhou: " + e.getMessage());
        }
    }
}
