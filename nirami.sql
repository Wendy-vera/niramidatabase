-- ============================================================
-- NIRAMI DATABASE
-- ============================================================

CREATE DATABASE IF NOT EXISTS nirami_prueba
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE nirami_prueba;

-- ============================================================
-- 1. USUARIOS
-- ============================================================
CREATE TABLE usuarios (
    id_usuario        INT           NOT NULL AUTO_INCREMENT,
    nombre            VARCHAR(100)  NOT NULL,
    correo            VARCHAR(150)  NOT NULL UNIQUE,
    telefono          VARCHAR(20),
    contrasena_hash   VARCHAR(255)  NOT NULL,
    tipo_usuario      ENUM('ADMIN', 'VENDEDOR', 'CLIENTE') NOT NULL,
    foto_perfil       VARCHAR(255)  DEFAULT NULL,
    estado            ENUM('ACTIVO','SUSPENDIDO','BLOQUEADO') NOT NULL DEFAULT 'ACTIVO',
    intentos_fallidos TINYINT       NOT NULL DEFAULT 0,
    bloqueado_hasta   DATETIME      DEFAULT NULL,  
    fecha_registro    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_sesion     DATETIME      DEFAULT NULL,
   
    es_admin_unico    VARCHAR(5) GENERATED ALWAYS AS (IF(tipo_usuario = 'ADMIN', 'ADMIN', NULL)) STORED,
   
    PRIMARY KEY (id_usuario),
    CONSTRAINT uq_admin_unico UNIQUE (es_admin_unico)
) ENGINE=InnoDB;

-- ============================================================
-- 2. SUBTIPOS: CLIENTES Y VENDEDORES
-- ============================================================
CREATE TABLE clientes (
    id_usuario       INT           NOT NULL,
    direccion_envio  VARCHAR(200)  DEFAULT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_cliente_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE vendedores (
    id_usuario       INT           NOT NULL,
    cuenta_bancaria  VARCHAR(50)   NOT NULL,
    biografia        TEXT          DEFAULT NULL,
    PRIMARY KEY (id_usuario),
    CONSTRAINT fk_vendedor_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 3. CATEGORÍAS
-- ============================================================
CREATE TABLE categorias (
    id_categoria   INT          NOT NULL AUTO_INCREMENT,
    nombre         VARCHAR(80)  NOT NULL UNIQUE,
    descripcion    TEXT,
    imagen         VARCHAR(255) DEFAULT NULL,
    estado         ENUM('ACTIVA','SUSPENDIDA','ELIMINADA') NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_categoria)
) ENGINE=InnoDB;

-- ============================================================
-- 4. PRODUCTOS (Puros: No conocen a ningún vendedor)
-- ============================================================
CREATE TABLE productos (
    id_producto         INT              NOT NULL AUTO_INCREMENT,
    nombre              VARCHAR(150)     NOT NULL,
    descripcion         TEXT             NOT NULL,
    precio              DECIMAL(12,2)    NOT NULL CHECK (precio > 0), -- Precio estándar del catálogo
    stock               INT              NOT NULL DEFAULT 0 CHECK (stock >= 0), -- Inventario global en el sistema
    id_categoria        INT              NOT NULL,
    estado              ENUM('PENDIENTE','APROBADO','RECHAZADO','SUSPENDIDO','EN_CORRECCION','ELIMINADO') NOT NULL DEFAULT 'PENDIENTE',
    vistas              INT              NOT NULL DEFAULT 0,
    fecha_creacion      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_producto),
    CONSTRAINT fk_prod_categoria FOREIGN KEY (id_categoria) REFERENCES categorias(id_categoria)
) ENGINE=InnoDB;

CREATE INDEX idx_prod_categoria ON productos(id_categoria);
CREATE INDEX idx_prod_estado    ON productos(estado);

-- ============================================================
-- 5. IMÁGENES DE PRODUCTOS
-- ============================================================
CREATE TABLE producto_imagenes (
    id_imagen    INT          NOT NULL AUTO_INCREMENT,
    id_producto  INT          NOT NULL,
    ruta         VARCHAR(255) NOT NULL,
    es_principal TINYINT(1)   NOT NULL DEFAULT 0,
    orden        TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id_imagen),
    CONSTRAINT fk_img_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 5.5 INVENTARIO DEL VENDEDOR (Opción B: Relación Vendedor-Producto)
-- ============================================================
CREATE TABLE vendedor_producto (
    id_vendedor    INT NOT NULL,
    id_producto    INT NOT NULL,
    stock_local    INT NOT NULL DEFAULT 0 CHECK (stock_local >= 0),
    es_creador     TINYINT(1) NOT NULL DEFAULT 0, -- Identifica si el vendedor sugirió el producto al catálogo puro
    PRIMARY KEY (id_vendedor, id_producto),
    CONSTRAINT fk_vp_vendedor FOREIGN KEY (id_vendedor) REFERENCES vendedores(id_usuario) ON DELETE CASCADE,
    CONSTRAINT fk_vp_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 6. REVISIONES / MODERACIÓN
-- ============================================================
CREATE TABLE revisiones_producto (
    id_revision    INT      NOT NULL AUTO_INCREMENT,
    id_producto    INT      NOT NULL,
    id_admin       INT      NOT NULL,
    accion         ENUM('APROBADO','RECHAZADO','EN_CORRECCION') NOT NULL,
    observacion    TEXT     DEFAULT NULL,
    fecha_revision DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_revision),
    CONSTRAINT fk_rev_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT fk_rev_admin    FOREIGN KEY (id_admin)    REFERENCES usuarios(id_usuario)
) ENGINE=InnoDB;

-- ============================================================
-- 7. FAVORITOS
-- ============================================================
CREATE TABLE favoritos (
    id_cliente  INT      NOT NULL,
    id_producto INT      NOT NULL,
    fecha_add   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_cliente, id_producto),
    CONSTRAINT fk_fav_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_usuario)  ON DELETE CASCADE,
    CONSTRAINT fk_fav_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 8. ÓRDENES
-- ============================================================
CREATE TABLE ordenes (
    id_orden     INT           NOT NULL AUTO_INCREMENT,
    id_cliente   INT           NOT NULL,
    subtotal     DECIMAL(12,2) NOT NULL,
    iva          DECIMAL(12,2) NOT NULL,
    total        DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_total CHECK (ABS(total - (subtotal + iva)) < 0.01),
    estado_pago  ENUM('PENDIENTE','APROBADO','RECHAZADO','REEMBOLSADO') NOT NULL DEFAULT 'PENDIENTE',
    estado_orden ENUM('ACTIVA','COMPLETADA','CANCELADA') NOT NULL DEFAULT 'ACTIVA',
    direccion    VARCHAR(200)  NOT NULL,
    fecha_orden  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_orden),
    CONSTRAINT fk_orden_cliente FOREIGN KEY (id_cliente) REFERENCES clientes(id_usuario)
) ENGINE=InnoDB;

-- ============================================================
-- 9. DETALLE DE ÓRDENES (Aquí se relaciona Producto y Vendedor)
-- ============================================================
CREATE TABLE orden_detalle (
    id_detalle      INT           NOT NULL AUTO_INCREMENT,
    id_orden        INT           NOT NULL,
    id_producto     INT           NOT NULL, -- El producto del catálogo puro
    id_vendedor     INT           NOT NULL, -- <- TU REGLA: El vendedor se amarra directamente al detalle de la venta
    cantidad        INT           NOT NULL CHECK (cantidad > 0),
    precio_unitario DECIMAL(12,2) NOT NULL,
    subtotal_linea  DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_subtotal_linea CHECK (ABS(subtotal_linea - (cantidad * precio_unitario)) < 0.01),
    estado_envio    ENUM('PROCESANDO','DESPACHADO','ENTREGADO','DEVUELTO') NOT NULL DEFAULT 'PROCESANDO',
    PRIMARY KEY (id_detalle),
    CONSTRAINT fk_det_orden    FOREIGN KEY (id_orden)    REFERENCES ordenes(id_orden),
    CONSTRAINT fk_det_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto),
    CONSTRAINT fk_det_vendedor FOREIGN KEY (id_vendedor) REFERENCES vendedores(id_usuario) -- Relación establecida
) ENGINE=InnoDB;

CREATE INDEX idx_det_orden    ON orden_detalle(id_orden);
CREATE INDEX idx_det_producto ON orden_detalle(id_producto);
CREATE INDEX idx_det_vendedor ON orden_detalle(id_vendedor);

-- ============================================================
-- 10. HISTORIAL DE USUARIO
-- ============================================================
CREATE TABLE historial_usuario (
    id_historial     INT          NOT NULL AUTO_INCREMENT,
    id_cliente       INT          NOT NULL,
    id_producto      INT          DEFAULT NULL,
    accion           ENUM('VER_PRODUCTO','AGREGAR_FAVORITO','QUITAR_FAVORITO','COMPRAR','BUSQUEDA') NOT NULL,
    termino_busqueda VARCHAR(255) DEFAULT NULL,
    detalle          VARCHAR(255) DEFAULT NULL,
    ip_origen        VARCHAR(45)  DEFAULT NULL,
    fecha            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_historial),
    CONSTRAINT fk_hist_cliente  FOREIGN KEY (id_cliente)  REFERENCES clientes(id_usuario)  ON DELETE CASCADE,
    CONSTRAINT fk_hist_producto FOREIGN KEY (id_producto) REFERENCES productos(id_producto) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ============================================================
-- 11. NOTIFICACIONES
-- ============================================================
CREATE TABLE notificaciones (
    id_notif   INT          NOT NULL AUTO_INCREMENT,
    id_usuario INT          NOT NULL,
    mensaje    VARCHAR(300) NOT NULL,
    tipo       ENUM('INFO','EXITO','ADVERTENCIA','ERROR') NOT NULL DEFAULT 'INFO',
    leida      TINYINT(1)   NOT NULL DEFAULT 0,
    fecha      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_notif),
    CONSTRAINT fk_notif_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- 12. PAGOS A VENDEDORES
-- ============================================================
CREATE TABLE pagos_vendedor (
    id_pago             INT           NOT NULL AUTO_INCREMENT,
    id_vendedor         INT           NOT NULL,
    id_detalle          INT           NOT NULL,
    monto               DECIMAL(12,2) NOT NULL,
    porcentaje_comision DECIMAL(5,2)  NOT NULL DEFAULT 0.00,
    monto_comision      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    monto_neto          DECIMAL(12,2) NOT NULL,
    CONSTRAINT chk_monto_neto CHECK (ABS(monto_neto - (monto - monto_comision)) < 0.01),
    estado              ENUM('PENDIENTE','PAGADO') NOT NULL DEFAULT 'PENDIENTE',
    fecha_generado      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_pago          DATETIME      DEFAULT NULL,
    PRIMARY KEY (id_pago),
    CONSTRAINT uq_pago_detalle  UNIQUE  (id_detalle),
    CONSTRAINT fk_pago_vendedor FOREIGN KEY (id_vendedor) REFERENCES vendedores(id_usuario),
    CONSTRAINT fk_pago_detalle  FOREIGN KEY (id_detalle)  REFERENCES orden_detalle(id_detalle)
) ENGINE=InnoDB;

CREATE INDEX idx_pago_vendedor ON pagos_vendedor(id_vendedor, estado);

select * from productos;
select * from usuarios;

