package util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Backup do banco: chama o mysqldump (instalado junto com o MySQL Server) e grava
 * um arquivo .sql com data e hora na pasta escolhida.
 * Roda no computador do técnico (servidor). A senha vai por um arquivo temporário,
 * nunca pela linha de comando.
 *
 * Para restaurar: mysql -u root -p os_manutencao < arquivo_do_backup.sql
 */
public class Backup {

    private static final String PASTA_MYSQL = "C:\\Program Files\\MySQL";

    /** Gera o backup dentro da pasta informada e devolve o arquivo criado. */
    public static File gerar(File pasta) throws IOException {
        if (pasta == null || !pasta.isDirectory()) {
            throw new IOException("Escolha uma pasta válida para salvar o backup.");
        }

        String host;
        String porta;
        String banco;
        String usuario;
        String senha;
        String configurado;
        try {
            host = Conexao.propriedade("db.host", "localhost");
            porta = Conexao.propriedade("db.porta", "3306");
            banco = Conexao.propriedade("db.banco", "os_manutencao");
            usuario = Conexao.propriedade("db.usuario", "os_app");
            senha = Conexao.propriedade("db.senha", "");
            configurado = Conexao.propriedade("db.mysqldump", "");
        } catch (SQLException e) {
            throw new IOException(e.getMessage(), e);
        }

        String nome = "backup_" + banco + "_"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".sql";
        File destino = new File(pasta, nome);

        // A senha fica num arquivo temporário (apagado no final), fora da linha de comando
        Path opcoes = Files.createTempFile("mysqldump_", ".cnf");
        try {
            String conteudo = "[client]\n"
                    + "host=" + host + "\n"
                    + "port=" + porta + "\n"
                    + "user=" + usuario + "\n"
                    + "password=\"" + senha.replace("\\", "\\\\").replace("\"", "\\\"") + "\"\n";
            Files.write(opcoes, conteudo.getBytes(StandardCharsets.UTF_8));

            ProcessBuilder pb = new ProcessBuilder(
                    localizarMysqldump(configurado),
                    "--defaults-extra-file=" + opcoes.toAbsolutePath(),
                    "--single-transaction",
                    "--skip-lock-tables",
                    "--no-tablespaces",
                    "--skip-triggers",
                    "--default-character-set=utf8mb4",
                    "--result-file=" + destino.getAbsolutePath(),
                    banco);
            pb.redirectErrorStream(true);

            Process p;
            try {
                p = pb.start();
            } catch (IOException e) {
                throw new IOException("Não foi possível executar o mysqldump.\n"
                        + "Ele é instalado junto com o MySQL Server. Se estiver em outra pasta, "
                        + "informe o caminho no config.properties:\n"
                        + "db.mysqldump=C:/Program Files/MySQL/MySQL Server 8.0/bin/mysqldump.exe", e);
            }
            String saida = new String(p.getInputStream().readAllBytes());
            boolean terminou;
            try {
                terminou = p.waitFor(120, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                p.destroyForcibly();
                throw new IOException("Backup interrompido.");
            }
            if (!terminou) {
                p.destroyForcibly();
                destino.delete();
                throw new IOException("O backup demorou demais e foi cancelado.");
            }
            if (p.exitValue() != 0) {
                destino.delete();
                throw new IOException("O mysqldump falhou:\n" + saida.trim());
            }
            return destino;
        } finally {
            Files.deleteIfExists(opcoes);
        }
    }

    /** Usa o caminho do config.properties; senão procura no Program Files; senão tenta o PATH. */
    private static String localizarMysqldump(String configurado) {
        if (!configurado.isEmpty() && new File(configurado).isFile()) {
            return configurado;
        }
        File base = new File(PASTA_MYSQL);
        File[] versoes = base.listFiles(f -> f.isDirectory() && f.getName().startsWith("MySQL Server"));
        if (versoes != null) {
            Arrays.sort(versoes); // a versão mais nova fica por último
            for (int i = versoes.length - 1; i >= 0; i--) {
                File exe = new File(versoes[i], "bin\\mysqldump.exe");
                if (exe.isFile()) {
                    return exe.getAbsolutePath();
                }
            }
        }
        return "mysqldump";
    }
}
