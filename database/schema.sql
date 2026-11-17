
-- ============================================
-- BASE DE DATOS: GESTOR DE CITAS PELUQUERIA
-- ============================================

CREATE DATABASE IF NOT EXISTS hAIr
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE hAIr;


-- ============================================
-- 1. TABLA USUARIO
-- ============================================

CREATE TABLE usuario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    password_hash VARCHAR(255),
    google_sub VARCHAR(255) UNIQUE,
    rol ENUM('CLIENTE', 'ADMIN') NOT NULL DEFAULT 'CLIENTE',
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_usuario_autenticacion
    CHECK (password_hash IS NOT NULL OR google_sub IS NOT NULL)
);


-- ============================================
-- 2. TABLA ARCHIVO_BLOQUEO
-- ============================================

CREATE TABLE archivo_bloqueo (
    id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT NOT NULL,
    nombre_original VARCHAR(255) NOT NULL,
    nombre_almacenado VARCHAR(255) NOT NULL,
    tamano_bytes BIGINT UNSIGNED NOT NULL,
    hash_sha256 CHAR(64) NOT NULL,
    mes TINYINT UNSIGNED NOT NULL,
    anio SMALLINT UNSIGNED NOT NULL,
    dias_importados SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_archivo_admin
    FOREIGN KEY (admin_id)
    REFERENCES usuario(id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_archivo_mes
    CHECK (mes BETWEEN 1 AND 12),

    UNIQUE (mes, anio)
);


-- ============================================
-- 3. TABLA DIA_BLOQUEADO
-- ============================================

CREATE TABLE dia_bloqueado (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL UNIQUE,
    motivo VARCHAR(255),
    archivo_id INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_dia_archivo
    FOREIGN KEY (archivo_id)
    REFERENCES archivo_bloqueo(id)
    ON DELETE RESTRICT
);


-- ============================================
-- 4. TABLA CITA
-- ============================================

CREATE TABLE cita (
    id INT AUTO_INCREMENT PRIMARY KEY,
    usuario_id INT NOT NULL,
    inicio DATETIME NOT NULL,
    fin DATETIME NOT NULL,
    estado ENUM('CONFIRMADA', 'CANCELADA')
        NOT NULL DEFAULT 'CONFIRMADA',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_cita_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuario(id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_cita_fechas
    CHECK (fin > inicio),

    INDEX idx_cita_inicio_estado (inicio, estado)
);


-- ============================================
-- 5. TABLA BLOQUEO_HORARIO
-- ============================================

CREATE TABLE bloqueo_horario (
    id INT AUTO_INCREMENT PRIMARY KEY,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    motivo VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_bloqueo_horario
    CHECK (hora_fin > hora_inicio),

    INDEX idx_bloqueo_fecha (fecha)
);


-- ============================================
-- 6. TABLA CONFIRMACION_PENDIENTE
-- ============================================

CREATE TABLE confirmacion_pendiente (
    id CHAR(36) PRIMARY KEY,
    usuario_id INT NOT NULL,
    tipo ENUM('RESERVA', 'CANCELACION') NOT NULL,
    payload_json JSON NOT NULL,
    expira_en DATETIME NOT NULL,
    usada BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_confirmacion_usuario
    FOREIGN KEY (usuario_id)
    REFERENCES usuario(id)
    ON DELETE RESTRICT,

    INDEX idx_confirmacion_expira (expira_en)
);


-- ============================================
-- 7. TABLA ESTADISTICA_CITAS -- guarda cantidad de citas, nuevos usuarios, ganancias ganadas, tramo horaria mas citado (80 minutos por tramo), y dias mas citados
-- ============================================

CREATE TABLE estadistica_citas (
    id_estadistica INT AUTO_INCREMENT PRIMARY KEY,
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    total_clientes INT NOT NULL DEFAULT 0,
    nuevos_usuarios INT NOT NULL DEFAULT 0,
    dinero_ganado DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    hora_mas_citas VARCHAR(20),
    dia_mas_citas VARCHAR(15),

    CONSTRAINT chk_estadistica_fechas
    CHECK (fecha_fin >= fecha_inicio),

    UNIQUE (fecha_inicio, fecha_fin)
);