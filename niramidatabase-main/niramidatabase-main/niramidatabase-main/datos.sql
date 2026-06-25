UPDATE usuarios
SET tipo_usuario = 'ADMIN'
WHERE correo = 'admin@nirami.com';

INSERT INTO categorias (id_categoria, nombre, descripcion, imagen, estado) VALUES
(1, 'Madera', 'Figuras, tallados, utensilios de cocina, cofres y artesanías en madera.', NULL, 'ACTIVA'),
(2, 'Cerámica', 'Vajillas, jarrones, macetas y piezas decorativas en barro y cerámica.', NULL, 'ACTIVA'),
(3, 'Textiles', 'Tapetes, cojines, mantas, ruanas/ponchos y ropa artesanal.', NULL, 'ACTIVA'),
(4, 'Joyería', 'Aretes, collares, pulseras y anillos artesanales.', NULL, 'ACTIVA'),
(5, 'Souvenirs', 'Recuerdos, llaveros, imanes y detalles representativos.', NULL, 'ACTIVA'),
(6, 'Otros', 'Categoría general para productos que no clasifican en las anteriores.', NULL, 'ACTIVA');

select * from categorias;
select * from productos;
select * from revisiones_producto;
select * from vendedor_producto;
select * from vendedores;
select * from usuarios;
select * from historial_usuario;
select * from clientes;
select * from ordenes;
select * from orden_detalle;
select * from pagos_vendedor;