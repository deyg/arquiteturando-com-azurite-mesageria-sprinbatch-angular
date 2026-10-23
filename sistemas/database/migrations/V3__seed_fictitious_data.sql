INSERT INTO empregado (id, matricula, nome_ficticio, ativo) VALUES
    (101, 'MAT-101', 'Ana Horizonte', TRUE),
    (102, 'MAT-102', 'Bruno Nascente', TRUE),
    (103, 'MAT-103', 'Carla Ipê', TRUE),
    (104, 'MAT-104', 'Diego Veredas', TRUE),
    (105, 'MAT-105', 'Elisa Maré', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO comprovante_pagamento (id, empregado_id, competencia, tipo, descricao, valor_bruto, descontos, valor_liquido, data_pagamento) VALUES
    (501,101,'2026-01','SALARY','Salário mensal',6500.00,1250.00,5250.00,'2026-01-30'),
    (502,101,'2026-01','BENEFIT','Auxílio alimentação',900.00,0.00,900.00,'2026-01-30'),
    (503,101,'2026-02','SALARY','Salário mensal',6500.00,1250.00,5250.00,'2026-02-27'),
    (504,101,'2026-02','BENEFIT','Auxílio alimentação',900.00,0.00,900.00,'2026-02-27'),
    (505,102,'2026-01','SALARY','Salário mensal',7200.00,1500.00,5700.00,'2026-01-30'),
    (506,102,'2026-01','BENEFIT','Auxílio alimentação',900.00,0.00,900.00,'2026-01-30'),
    (507,102,'2026-03','SALARY','Salário mensal',7200.00,1500.00,5700.00,'2026-03-31'),
    (508,102,'2026-03','BENEFIT','Auxílio alimentação',900.00,0.00,900.00,'2026-03-31'),
    (509,103,'2026-02','SALARY','Salário mensal',5800.00,980.00,4820.00,'2026-02-27'),
    (510,103,'2026-02','BENEFIT','Auxílio transporte',500.00,0.00,500.00,'2026-02-27'),
    (511,103,'2026-04','SALARY','Salário mensal',5800.00,980.00,4820.00,'2026-04-30'),
    (512,103,'2026-04','BENEFIT','Auxílio transporte',500.00,0.00,500.00,'2026-04-30'),
    (513,104,'2026-03','SALARY','Salário mensal',8100.00,1800.00,6300.00,'2026-03-31'),
    (514,104,'2026-03','BENEFIT','Auxílio alimentação',1000.00,0.00,1000.00,'2026-03-31'),
    (515,104,'2026-05','SALARY','Salário mensal',8100.00,1800.00,6300.00,'2026-05-29'),
    (516,104,'2026-05','BENEFIT','Auxílio alimentação',1000.00,0.00,1000.00,'2026-05-29'),
    (517,105,'2026-04','SALARY','Salário mensal',4900.00,720.00,4180.00,'2026-04-30'),
    (518,105,'2026-04','BENEFIT','Auxílio transporte',450.00,0.00,450.00,'2026-04-30'),
    (519,105,'2026-06','SALARY','Salário mensal',4900.00,720.00,4180.00,'2026-06-30'),
    (520,105,'2026-06','BENEFIT','Auxílio transporte',450.00,0.00,450.00,'2026-06-30')
ON CONFLICT (id) DO NOTHING;
