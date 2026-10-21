-- Banco do Sistema de Ordens de Servico (MySQL 8) - versao 3
-- A OS replica a ficha de papel da assistencia (cliente + equipamento + problema).
-- Rodar UMA VEZ no computador que sera o servidor (MySQL Workbench: abrir o script e clicar no raio).

CREATE DATABASE IF NOT EXISTS os_manutencao
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE os_manutencao;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(100) NOT NULL,
    login      VARCHAR(50)  NOT NULL UNIQUE,
    senha      VARCHAR(64)  NOT NULL,  -- hash SHA-256, nunca texto puro
    perfil     ENUM('Administrador', 'Caixa') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente INT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(120) NOT NULL,
    whatsapp   VARCHAR(15)  NOT NULL,  -- somente digitos, com DDD
    INDEX idx_cliente_whatsapp (whatsapp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ordem_servico (
    id_os                  INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente             INT NOT NULL,
    id_usuario_responsavel INT NOT NULL,
    tipo_equipamento       ENUM('Notebook', 'Computador') NOT NULL,
    problema_relatado      TEXT NOT NULL,
    servico_a_realizar     TEXT,
    status                 ENUM('Aberta', 'Em andamento', 'Aguardando peça', 'Concluída', 'Entregue')
                           NOT NULL DEFAULT 'Aberta',
    data_entrada           DATE NOT NULL,
    hora_entrada           TIME,
    data_conclusao         DATE,
    hora_conclusao         TIME,
    data_retirada          DATE,
    acessorio_fonte        TINYINT(1) NOT NULL DEFAULT 0,
    acessorios_outros      VARCHAR(255),
    formatacao             TINYINT(1) NOT NULL DEFAULT 0,
    sistema_formatacao     VARCHAR(60),
    senha_equipamento      VARCHAR(100),
    fazer_backup           TINYINT(1) NOT NULL DEFAULT 0,
    pecas                  TEXT,
    orcamento_outros       TEXT,
    total_pecas            DECIMAL(10,2),
    total_servicos         DECIMAL(10,2),
    valor                  DECIMAL(10,2),  -- total = pecas + servicos
    INDEX idx_os_status (status),
    INDEX idx_os_data (data_entrada),
    CONSTRAINT fk_os_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_os_usuario FOREIGN KEY (id_usuario_responsavel) REFERENCES usuario (id_usuario)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Usuarios de teste (senhas: admin123 e caixa123)
INSERT IGNORE INTO usuario (nome, login, senha, perfil) VALUES
 ('Tecnico', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Administrador'),
 ('Funcionario', 'caixa', '59c7c57e6516f4a4ae85214acbd322b724cb755806c4183587a5007c9ca3af23', 'Caixa');

-- ---------------------------------------------------------------------------
-- Usuario do SISTEMA no MySQL (nao use o root no programa). Troque a senha!
-- Rodar tambem no servidor, uma vez:
--
--   CREATE USER 'os_app'@'%' IDENTIFIED BY 'TROQUE_ESTA_SENHA';
--   GRANT SELECT, INSERT, UPDATE, DELETE ON os_manutencao.* TO 'os_app'@'%';
--   FLUSH PRIVILEGES;
-- ---------------------------------------------------------------------------
