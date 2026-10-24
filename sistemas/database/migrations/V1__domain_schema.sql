CREATE TABLE empregado (
    id BIGINT PRIMARY KEY,
    matricula VARCHAR(30) NOT NULL UNIQUE,
    nome_ficticio VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE comprovante_pagamento (
    id BIGINT PRIMARY KEY,
    empregado_id BIGINT NOT NULL REFERENCES empregado(id),
    competencia CHAR(7) NOT NULL CHECK (competencia REGEXP '^[0-9]{4}-(0[1-9]|1[0-2])$'),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('SALARY', 'BENEFIT')),
    descricao VARCHAR(200) NOT NULL,
    valor_bruto NUMERIC(15,2) NOT NULL CHECK (valor_bruto >= 0),
    descontos NUMERIC(15,2) NOT NULL CHECK (descontos >= 0),
    valor_liquido NUMERIC(15,2) NOT NULL,
    data_pagamento DATE NOT NULL
);

CREATE INDEX idx_pagamento_filtros ON comprovante_pagamento(empregado_id, competencia, tipo);

CREATE TABLE solicitacao (
    id CHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'PARTIAL', 'FAILED')),
    start_period CHAR(7) NOT NULL,
    end_period CHAR(7) NOT NULL,
    payment_types VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    started_at TIMESTAMP(6),
    completed_at TIMESTAMP(6),
    total_documents INTEGER NOT NULL DEFAULT 0,
    completed_documents INTEGER NOT NULL DEFAULT 0,
    failed_documents INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE TABLE solicitacao_empregado (
    solicitacao_id CHAR(36) NOT NULL REFERENCES solicitacao(id) ON DELETE CASCADE,
    empregado_id BIGINT NOT NULL REFERENCES empregado(id),
    PRIMARY KEY (solicitacao_id, empregado_id)
);

CREATE TABLE documento_gerado (
    id CHAR(36) PRIMARY KEY,
    solicitacao_id CHAR(36) NOT NULL REFERENCES solicitacao(id) ON DELETE CASCADE,
    comprovante_id BIGINT NOT NULL REFERENCES comprovante_pagamento(id),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    container_name VARCHAR(100),
    blob_name VARCHAR(500),
    checksum_sha256 CHAR(64),
    error_message TEXT,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (solicitacao_id, comprovante_id)
);

CREATE INDEX idx_solicitacao_created ON solicitacao(created_at DESC, id DESC);
CREATE INDEX idx_documento_solicitacao ON documento_gerado(solicitacao_id, status);
