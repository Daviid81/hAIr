CREATE TABLE IF NOT EXISTS usuario (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    telefono VARCHAR(30) NULL,
    password_hash VARCHAR(60) NULL,
    google_sub VARCHAR(255) NULL,
    rol ENUM('CLIENTE', 'ADMIN') NOT NULL DEFAULT 'CLIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT uq_usuario_google_sub UNIQUE (google_sub),
    CONSTRAINT chk_usuario_credencial CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
