-- Banco do Sistema de Ordens de Servico (SQLite) - versao 2
-- A OS replica a ficha de papel da assistencia (cliente + equipamento + problema).
-- O arquivo do banco e criado automaticamente: os_manutencao.db

PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS usuario (
    id_usuario INTEGER PRIMARY KEY AUTOINCREMENT,
    nome       TEXT NOT NULL,
    login      TEXT NOT NULL UNIQUE,
    senha      TEXT NOT NULL,  -- hash SHA-256, nunca texto puro
    perfil     TEXT NOT NULL CHECK (perfil IN ('Administrador', 'Caixa'))
);

CREATE TABLE IF NOT EXISTS cliente (
    id_cliente INTEGER PRIMARY KEY AUTOINCREMENT,
    nome       TEXT NOT NULL,
    whatsapp   TEXT NOT NULL  -- somente digitos, com DDD
);

CREATE TABLE IF NOT EXISTS ordem_servico (
    id_os                  INTEGER PRIMARY KEY AUTOINCREMENT,
    id_cliente             INTEGER NOT NULL,
    id_usuario_responsavel INTEGER NOT NULL,
    tipo_equipamento       TEXT NOT NULL CHECK (tipo_equipamento IN ('Notebook', 'Computador')),
    problema_relatado      TEXT NOT NULL,
    servico_a_realizar     TEXT,
    status                 TEXT NOT NULL DEFAULT 'Aberta'
        CHECK (status IN ('Aberta', 'Em andamento', 'Aguardando peça', 'Concluída', 'Entregue')),
    data_entrada           TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    hora_entrada           TEXT,
    data_conclusao         TEXT,
    hora_conclusao         TEXT,
    data_retirada          TEXT,
    acessorio_fonte        INTEGER NOT NULL DEFAULT 0,
    acessorios_outros      TEXT,
    formatacao             INTEGER NOT NULL DEFAULT 0,
    sistema_formatacao     TEXT,
    senha_equipamento      TEXT,
    backup                 INTEGER NOT NULL DEFAULT 0,
    pecas                  TEXT,
    orcamento_outros       TEXT,
    total_pecas            REAL,
    total_servicos         REAL,
    valor                  REAL,  -- total = pecas + servicos
    FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    FOREIGN KEY (id_usuario_responsavel) REFERENCES usuario (id_usuario)
);

-- Usuarios de teste (senhas: admin123 e caixa123)
INSERT OR IGNORE INTO usuario (nome, login, senha, perfil) VALUES
 ('Tecnico', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Administrador'),
 ('Funcionario', 'caixa', '59c7c57e6516f4a4ae85214acbd322b724cb755806c4183587a5007c9ca3af23', 'Caixa');
